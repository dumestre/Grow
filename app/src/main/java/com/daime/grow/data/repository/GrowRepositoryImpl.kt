package com.daime.grow.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.daime.grow.data.backup.BackupManager
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.local.entity.ChecklistItemEntity
import com.daime.grow.data.local.entity.HarvestBatchEntity
import com.daime.grow.data.local.entity.NutrientLogEntity
import com.daime.grow.data.local.entity.PlantEntity
import com.daime.grow.data.local.entity.PlantEventEntity
import com.daime.grow.data.local.entity.WateringLogEntity
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.remote.model.AppConfigDto
import com.daime.grow.data.reminder.ReminderScheduler
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralPostDto
import com.daime.grow.data.remote.model.MuralUserDto
import com.daime.grow.data.remote.model.PlantDto
import com.daime.grow.domain.model.ChecklistItem
import com.daime.grow.domain.model.NutrientLog
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantDetails
import com.daime.grow.domain.model.PlantEvent
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.domain.model.SecurityPreferences
import com.daime.grow.domain.model.WateringLog
import com.daime.grow.domain.repository.GrowRepository
import com.daime.grow.domain.usecase.ChecklistFactory
import com.daime.grow.ui.util.ImageUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private const val TAG = "GrowRepository"

@Singleton
class GrowRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val database: GrowDatabase,
    private val scheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val securityRepository: SecurityPreferencesRepository
) : GrowRepository {

    private val plantDao = database.plantDao()
    private val plantEventDao = database.plantEventDao()
    private val wateringDao = database.wateringLogDao()
    private val nutrientDao = database.nutrientLogDao()
    private val checklistDao = database.checklistDao()
    private val muralDao = database.muralDao()
    private val harvestDao = database.harvestDao()
    private val supabase = SupabaseClient.clientOrNull

    override fun observePlants(query: String, stageFilter: String, sortAsc: Boolean): Flow<List<Plant>> {
        return plantDao.observePlants(query.trim(), stageFilter, if (sortAsc) 1 else 0)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observePlantDetails(plantId: Long): Flow<PlantDetails?> {
        return combine(
            plantDao.observePlant(plantId),
            plantEventDao.observeByPlantId(plantId),
            wateringDao.observeByPlantId(plantId),
            nutrientDao.observeByPlantId(plantId),
            checklistDao.observeByPlantId(plantId)
        ) { plant, events, watering, nutrients, checklist ->
            val p = plant ?: return@combine null
            PlantDetails(
                plant = p.toDomain(),
                events = events.map { it.toDomain() },
                wateringLogs = watering.map { it.toDomain() },
                nutrientLogs = nutrients.map { it.toDomain() },
                checklistItems = checklist.map { it.toDomain() }
            )
        }
    }

    override suspend fun addPlant(
        name: String,
        strain: String,
        stage: String,
        medium: String,
        days: Int,
        photoUri: String?,
        shareOnMural: Boolean,
        isHydroponic: Boolean
    ): Long {
        val now = System.currentTimeMillis()
        var createdId = 0L
        database.withTransaction {
            val nextSortOrder = plantDao.maxSortOrder() + 1
            createdId = plantDao.insert(
                PlantEntity(
                    name = name,
                    strain = strain,
                    stage = stage,
                    medium = medium,
                    days = days,
                    photoUri = photoUri,
                    nextWateringDate = null,
                    sortOrder = nextSortOrder,
                    createdAt = now,
                    sharedOnMural = shareOnMural,
                    isHydroponic = isHydroponic
                )
            )

            val checklist = ChecklistFactory.defaultChecklist(createdId, stage, now)
                .map { item ->
                    ChecklistItemEntity(
                        plantId = item.plantId,
                        phase = item.phase,
                        task = item.task,
                        done = item.done,
                        createdAt = item.createdAt
                    )
                }
            checklistDao.insertAll(checklist)

            plantEventDao.insert(
                PlantEventEntity(
                    plantId = createdId,
                    type = "Cadastro",
                    note = "Planta criada",
                    createdAt = now
                )
            )

            if (shareOnMural) {
                muralDao.insertPost(
                    com.daime.grow.data.local.entity.MuralPostEntity(
                        plantId = createdId,
                        createdAt = now
                    )
                )
            }
        }

        // Sincronização com Supabase
        syncPlantsToRemote()

        // Sincronização com Supabase se compartilhado no Mural
        if (shareOnMural) {
            syncToSupabase(name, strain, stage, medium, days, photoUri)
        }

        val createdPlant = plantDao.observePlant(createdId).first()
        createdPlant?.toDomain()?.let { scheduler.scheduleForPlant(it) }
        return createdId
    }

    private suspend fun syncToSupabase(
        name: String,
        strain: String,
        stage: String,
        medium: String,
        days: Int,
        photoUri: String?
    ) {
        val supabase = supabase ?: return
        try {
            val userId = getCurrentUserId() ?: return
            var remotePhotoUrl: String? = null

            // 1. Upload da foto se existir
            if (photoUri != null) {
                val bytes = ImageUtils.compressImageToWebP(appContext, Uri.parse(photoUri))
                if (bytes != null) {
                    val fileName = "plant_${UUID.randomUUID()}.webp"
                    val bucket = supabase.storage.from("plant-photos")
                    bucket.upload(fileName, bytes)
                    remotePhotoUrl = bucket.publicUrl(fileName)
                }
            }

            // 2. Enviar Post para o Supabase
            val localUser = muralDao.getUser(userId) ?: return
            val remoteUser = supabase.from("mural_users")
                .select { filter { eq("username", localUser.username) } }
                .decodeSingle<MuralUserDto>()

            supabase.from("mural_posts").insert(
                MuralPostDto(
                    user_id = remoteUser.id!!,
                    plant_name = name,
                    strain = strain,
                    stage = stage,
                    medium = medium,
                    days = days,
                    photo_url = remotePhotoUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar com Supabase: ${e.message}")
        }
    }

    override suspend fun addQuickEvent(plantId: Long, type: String, note: String) {
        plantEventDao.insert(
            PlantEventEntity(
                plantId = plantId,
                type = type,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun addWatering(plantId: Long, volumeMl: Int, intervalDays: Int, substrate: String) {
        val now = System.currentTimeMillis()
        val nextDate = now + intervalDays * 24L * 60L * 60L * 1_000L

        database.withTransaction {
            wateringDao.insert(
                WateringLogEntity(
                    plantId = plantId,
                    volumeMl = volumeMl,
                    intervalDays = intervalDays,
                    substrate = substrate,
                    nextWateringDate = nextDate,
                    createdAt = now
                )
            )
            plantDao.updateNextWateringDate(plantId, nextDate)
            plantEventDao.insert(
                PlantEventEntity(
                    plantId = plantId,
                    type = "Rega",
                    note = "${volumeMl}ml / ${intervalDays}d",
                    createdAt = now
                )
            )
        }

        plantDao.observePlant(plantId).first()?.toDomain()?.let { scheduler.scheduleForPlant(it) }
    }

    override suspend fun addNutrient(log: NutrientLog) {
        nutrientDao.insert(
            NutrientLogEntity(
                plantId = log.plantId,
                week = log.week,
                ec = log.ec,
                ph = log.ph,
                createdAt = System.currentTimeMillis()
            )
        )
        addQuickEvent(log.plantId, "Nutrientes", "Semana ${log.week} - EC ${log.ec} / pH ${log.ph}")
    }

    override suspend fun toggleChecklist(itemId: Long, done: Boolean) {
        checklistDao.toggle(itemId, done)
        syncPlantsToRemote()
    }

    override suspend fun updatePlantStage(plantId: Long, stage: String) {
        database.withTransaction {
            plantDao.updateStage(plantId, stage)
        }
        plantDao.observePlant(plantId).first()?.toDomain()?.let { scheduler.scheduleForPlant(it) }
        syncPlantsToRemote()
    }

    override suspend fun updatePlantPhoto(plantId: Long, photoUri: String?) {
        val currentPhoto = plantDao.observePlant(plantId).first()?.photoUri
        database.withTransaction {
            plantDao.updatePhoto(plantId, photoUri)
        }
        if (currentPhoto != null && currentPhoto != photoUri) {
            deletePhotoIfOwned(appContext, currentPhoto)
        }
        syncPlantsToRemote()
    }

    override suspend fun deletePlant(plantId: Long) {
        val photoUri = plantDao.observePlant(plantId).first()?.photoUri
        database.withTransaction {
            plantDao.deleteById(plantId)
        }
        scheduler.cancelForPlant(plantId)
        deletePhotoIfOwned(appContext, photoUri)
        syncPlantsToRemote()
    }

    override suspend fun updatePlantsOrder(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        database.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                plantDao.updateSortOrder(id, index)
            }
        }
        syncPlantsToRemote()
    }

    override suspend fun createHarvestBatch(plantId: Long, plantName: String, strain: String, harvestDate: Long) {
        harvestDao.insertBatch(
            com.daime.grow.data.local.entity.HarvestBatchEntity(
                plantId = plantId,
                plantName = plantName,
                strain = strain,
                harvestDate = harvestDate,
                status = "DRYING",
                currentHumidity = null,
                currentTemperature = null,
                lastBurpDate = null,
                nextBurpDate = null
            )
        )
    }

    override suspend fun seedDataIfNeeded() {
        // Sincroniza a configuração remota do Supabase que controla o mascaramento
        syncRemoteConfig()
        // Não cria mais plantas placeholder - o usuário deve adicionar suas próprias plantas
    }

    override fun observeSecurityPreferences(): Flow<SecurityPreferences> {
        // Agora retorna diretamente o estado local, que é atualizado via Supabase
        return securityRepository.observe()
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        securityRepository.setLockEnabled(enabled)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        securityRepository.setBiometricEnabled(enabled)
    }

    override suspend fun updatePin(pin: String) {
        securityRepository.updatePin(pin)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return securityRepository.verifyPin(pin)
    }

    override suspend fun setMaskHomeIcon(enabled: Boolean) {
        securityRepository.setMaskHomeIcon(enabled)
    }

    override suspend fun setMaskStoreCatalog(enabled: Boolean) {
        securityRepository.setMaskStoreCatalog(enabled)
    }

    override suspend fun setDarkThemeMode(mode: com.daime.grow.domain.model.DarkThemeMode) {
        securityRepository.setDarkThemeMode(mode)
    }

    private suspend fun syncRemoteConfig() {
        val supabase = supabase ?: return
        try {
            Log.d(TAG, "Sincronizando Remote Config do Supabase...")
            // Busca a chave 'use_alternative_icons' que controla o mascaramento global
            val config = supabase.from("app_config")
                .select { filter { eq("key", "use_alternative_icons") } }
                .decodeSingleOrNull<AppConfigDto>()
            
            Log.d(TAG, "Config recebida: $config")
            
            config?.let {
                Log.d(TAG, "Atualizando mascaramento para: ${it.value_bool}")
                // Atualiza ambos os mascaramentos baseados na flag do Supabase
                securityRepository.setAllMasking(it.value_bool)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao sincronizar config remota: ${e.message}")
        }
    }

    override suspend fun exportBackup(uri: Uri) {
        backupManager.exportTo(uri)
    }

    override suspend fun importBackup(uri: Uri) {
        backupManager.importFrom(uri)
    }

    // Mural
    override fun observeMuralPosts(): Flow<List<com.daime.grow.data.local.dao.MuralPostWithPlant>> {
        return muralDao.observeMuralPostsWithPlants()
    }

    override fun observeMuralPost(postId: Long): Flow<com.daime.grow.data.local.dao.MuralPostWithPlant?> {
        return muralDao.observeMuralPostsWithPlants().map { posts ->
            posts.find { it.id == postId }
        }
    }

    override fun observeComments(postId: Long): Flow<List<com.daime.grow.data.local.dao.CommentWithUser>> {
        return muralDao.observeCommentsWithUsers(postId)
    }

    override suspend fun addComment(postId: Long, userId: Long, content: String, parentId: Long?) {
        muralDao.insertComment(
            com.daime.grow.data.local.entity.MuralCommentEntity(
                postId = postId,
                userId = userId,
                content = content,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun createOrGetUser(username: String): Long {
        var user = muralDao.getUserByUsername(username)
        if (user == null) {
            val now = System.currentTimeMillis()
            val localId = muralDao.insertUser(
                com.daime.grow.data.local.entity.MuralUserEntity(
                    username = username,
                    createdAt = now
                )
            )
            // Sincronizar usuário com Supabase
            try {
                supabase?.from("mural_users")?.insert(MuralUserDto(username = username))
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao criar usuário remoto: ${e.message}")
            }
            return localId
        }
        return user.id
    }

    private var currentUserId: Long? = null

    override suspend fun getCurrentUserId(): Long? = currentUserId

    fun setCurrentUserId(userId: Long) {
        currentUserId = userId
    }

    override suspend fun syncPlantsToRemote() {
        val supabase = supabase ?: return
        val userId = currentUserId ?: return

        try {
            val localUser = muralDao.getUser(userId) ?: return
            val remoteUser = supabase.from("mural_users")
                .select { filter { eq("username", localUser.username) } }
                .decodeSingleOrNull<MuralUserDto>() ?: return

            val plants = plantDao.getAllNow()
            val now = System.currentTimeMillis()

            for (plant in plants) {
                try {
                    var remotePhotoUrl: String? = null

                    if (plant.photoUri != null && !plant.photoUri!!.startsWith("http")) {
                        val bytes = ImageUtils.compressImageToWebP(appContext, Uri.parse(plant.photoUri))
                        if (bytes != null) {
                            val fileName = "plant_${UUID.randomUUID()}.webp"
                            val bucket = supabase.storage.from("plant-photos")
                            bucket.upload(fileName, bytes)
                            remotePhotoUrl = bucket.publicUrl(fileName)
                        }
                    } else if (plant.photoUri?.startsWith("http") == true) {
                        remotePhotoUrl = plant.photoUri
                    }

                    supabase.from("plants").upsert(
                        PlantDto(
                            user_id = remoteUser.id,
                            name = plant.name,
                            strain = plant.strain,
                            stage = plant.stage,
                            medium = plant.medium,
                            days = plant.days,
                            photo_url = remotePhotoUrl,
                            next_watering_date = plant.nextWateringDate,
                            sort_order = plant.sortOrder,
                            created_at = plant.createdAt,
                            updated_at = now,
                            is_hydroponic = plant.isHydroponic
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar planta ${plant.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar plantas: ${e.message}")
        }
    }

    override suspend fun syncPlantsFromRemote() {
        val supabase = supabase ?: return
        val userId = currentUserId ?: return

        try {
            val localUser = muralDao.getUser(userId) ?: return
            val remoteUser = supabase.from("mural_users")
                .select { filter { eq("username", localUser.username) } }
                .decodeSingleOrNull<MuralUserDto>() ?: return

            val remotePlants = supabase.from("plants")
                .select { filter { eq("user_id", remoteUser.id!!) } }
                .decodeList<PlantDto>()

            val now = System.currentTimeMillis()

            for (dto in remotePlants) {
                try {
                    val existingPlant = plantDao.getPlantById(dto.id?.toLongOrNull() ?: 0)

                    if (existingPlant != null) {
                        plantDao.update(
                            existingPlant.copy(
                                name = dto.name,
                                strain = dto.strain ?: "",
                                stage = dto.stage,
                                medium = dto.medium ?: "",
                                days = dto.days,
                                photoUri = dto.photo_url,
                                nextWateringDate = dto.next_watering_date,
                                sortOrder = dto.sort_order,
                                isHydroponic = dto.is_hydroponic
                            )
                        )
                    } else {
                        plantDao.insert(
                            PlantEntity(
                                name = dto.name,
                                strain = dto.strain ?: "",
                                stage = dto.stage,
                                medium = dto.medium ?: "",
                                days = dto.days,
                                photoUri = dto.photo_url,
                                nextWateringDate = dto.next_watering_date,
                                sortOrder = dto.sort_order,
                                createdAt = dto.created_at ?: now,
                                isHydroponic = dto.is_hydroponic
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao importar planta ${dto.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar plantas remotas: ${e.message}")
        }
    }
}

private fun deletePhotoIfOwned(appContext: Context, photoUri: String?) {
    if (photoUri.isNullOrBlank()) return
    runCatching {
        val uri = Uri.parse(photoUri)
        when (uri.scheme) {
            "file" -> uri.path?.let { path ->
                java.io.File(path).takeIf { it.exists() }?.delete()
            }
            "content" -> appContext.contentResolver.delete(uri, null, null)
        }
    }
}

private fun PlantEntity.toDomain() = Plant(
    id = id,
    name = name,
    strain = strain,
    stage = stage,
    medium = medium,
    days = days,
    photoUri = photoUri,
    nextWateringDate = nextWateringDate,
    createdAt = createdAt,
    sharedOnMural = sharedOnMural,
    isHydroponic = isHydroponic
)

private fun PlantEventEntity.toDomain() = PlantEvent(
    id = id,
    plantId = plantId,
    type = type,
    note = note,
    createdAt = createdAt
)

private fun WateringLogEntity.toDomain() = WateringLog(
    id = id,
    plantId = plantId,
    volumeMl = volumeMl,
    intervalDays = intervalDays,
    substrate = substrate,
    nextWateringDate = nextWateringDate ?: 0L,
    createdAt = createdAt
)

private fun NutrientLogEntity.toDomain() = NutrientLog(
    id = id,
    plantId = plantId,
    week = week,
    ec = ec,
    ph = ph,
    createdAt = createdAt
)

private fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id = id,
    plantId = plantId,
    phase = phase,
    task = task,
    done = done,
    createdAt = createdAt
)
