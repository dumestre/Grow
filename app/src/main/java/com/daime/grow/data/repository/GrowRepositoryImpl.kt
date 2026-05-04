package com.daime.grow.data.repository

import android.content.Context
import android.provider.Settings
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
import com.daime.grow.data.preferences.MuralPreferencesRepository
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
import com.daime.grow.domain.model.calculateCultivationDays
import com.daime.grow.domain.model.millisUntilNextLocalMidnight
import com.daime.grow.domain.repository.GrowRepository
import com.daime.grow.domain.usecase.ChecklistFactory
import com.daime.grow.ui.util.ImageUtils
import com.daime.grow.widget.GrowWidgetUpdater
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private const val TAG = "GrowRepository"

@Singleton
class GrowRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val database: GrowDatabase,
    private val scheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val securityRepository: SecurityPreferencesRepository,
    private val muralPreferencesRepository: MuralPreferencesRepository
) : GrowRepository {

    private val plantDao = database.plantDao()
    private val plantEventDao = database.plantEventDao()
    private val wateringDao = database.wateringLogDao()
    private val nutrientDao = database.nutrientLogDao()
    private val checklistDao = database.checklistDao()
    private val muralDao = database.muralDao()
    private val harvestDao = database.harvestDao()
    private val supabaseClient get() = SupabaseClient.clientOrNull

    override fun observePlants(query: String, stageFilter: String, sortAsc: Boolean): Flow<List<Plant>> {
        return combine(
            plantDao.observePlants(query.trim(), stageFilter, if (sortAsc) 1 else 0),
            cultivationDayTicker()
        ) { plants, _ ->
            plants
                .sortedWith(comparePlants(sortAsc))
                .map { it.toDomain() }
        }
    }

    override fun observePlantDetails(plantId: Long): Flow<PlantDetails?> {
        val detailsFlow = combine(
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
        return combine(detailsFlow, cultivationDayTicker()) { details, _ -> details }
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
        Log.d(TAG, "addPlant: Iniciando criação da planta: $name")
        
        try {
            database.withTransaction {
                val nextSortOrder = plantDao.maxSortOrder() + 1
                Log.d(TAG, "addPlant: SortOrder=$nextSortOrder")
                
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
                Log.d(TAG, "addPlant: Planta inserida com ID=$createdId")

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
                Log.d(TAG, "addPlant: Checklist inserido (${checklist.size} itens)")

                plantEventDao.insert(
                    PlantEventEntity(
                        plantId = createdId,
                        type = "Cadastro",
                        note = "Planta criada",
                        createdAt = now
                    )
                )
                Log.d(TAG, "addPlant: Evento de cadastro inserido")

                if (shareOnMural) {
                    muralDao.insertPost(
                        com.daime.grow.data.local.entity.MuralPostEntity(
                            plantId = createdId,
                            createdAt = now
                        )
                    )
                    Log.d(TAG, "addPlant: Post do mural inserido")
                }
            }
            Log.d(TAG, "addPlant: Transação concluída com sucesso, ID=$createdId")

            // Salva diretamente no Supabase se usuário logado
            val userUuid = getCurrentUserId()
            if (userUuid != null) {
                withContext(Dispatchers.IO) {
                    insertPlantDirectlyToSupabase(
                        localId = createdId,
                        name = name,
                        strain = strain,
                        stage = stage,
                        medium = medium,
                        days = days,
                        photoUri = photoUri,
                        isHydroponic = isHydroponic,
                        sortOrder = plantDao.getPlantById(createdId)?.sortOrder ?: createdId.toInt()
                    )
                }
            }

            // Só posta no mural se solicitado (não duplica mais)
            if (shareOnMural && userUuid != null) {
                withContext(Dispatchers.IO) {
                    syncToSupabase(name, strain, stage, medium, days, photoUri)
                }
            }

            val createdPlant = plantDao.observePlant(createdId).first()
            createdPlant?.toDomain()?.let { scheduler.scheduleForPlant(it) }
            GrowWidgetUpdater.refreshAll(appContext)
             
            Log.d(TAG, "addPlant: Planta finalizada com ID=$createdId")
            return createdId
        } catch (e: Exception) {
            Log.e(TAG, "addPlant: ERRO ao criar planta: ${e.message}", e)
            throw e
        }
    }

    private suspend fun insertPlantDirectlyToSupabase(
        localId: Long,
        name: String,
        strain: String,
        stage: String,
        medium: String,
        days: Int,
        photoUri: String?,
        isHydroponic: Boolean,
        sortOrder: Int = localId.toInt()
    ) {
        val supabase = supabaseClient ?: return
        
        // Só salva no Supabase se usuário estiver logado com Google
        val userUuid = getCurrentUserId() ?: run {
            Log.d(TAG, "insertPlantDirectlyToSupabase: usuário não logado, pulando save no banco")
            return
        }
        try {
            val now = System.currentTimeMillis()
            
            var remotePhotoUrl: String? = null
            if (photoUri != null && !photoUri.startsWith("http")) {
                val bytes = ImageUtils.compressImageToWebP(appContext, Uri.parse(photoUri))
                if (bytes != null) {
                    val fileName = "plant_$localId.webp"
                    val bucket = supabase.storage.from("plant-photos")
                    bucket.upload(fileName, bytes)
                    remotePhotoUrl = bucket.publicUrl(fileName)
                }
            } else if (photoUri?.startsWith("http") == true) {
                remotePhotoUrl = photoUri
            }

            // Usa upsert para reaproveitar o registro remoto quando local_id ja existir.
            supabase.from("plants").upsert(
                PlantDto(
                    user_id = userUuid,
                    local_id = localId,
                    name = name,
                    strain = strain,
                    stage = stage,
                    medium = medium,
                    days = days,
                    photo_url = remotePhotoUrl,
                    next_watering_date = null,
                    sort_order = sortOrder,
                    created_at = now,
                    updated_at = now,
                    is_hydroponic = isHydroponic
                )
            ) {
                onConflict = "user_id,local_id"
            }
            Log.d(TAG, "insertPlantDirectlyToSupabase: Planta $name sincronizada com local_id=$localId")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir planta no Supabase: ${e.message}")
        }
    }

    private suspend fun syncToSupabase(
        name: String,
        strain: String,
        stage: String,
        medium: String,
        days: Int,
        photoUri: String?
    ) {
        val supabase = supabaseClient ?: return
        try {
            val userUuid = getCurrentUserId() ?: return
            var remotePhotoUrl: String? = null

            if (photoUri != null && !photoUri.startsWith("http")) {
                val bytes = ImageUtils.compressImageToWebP(appContext, Uri.parse(photoUri))
                if (bytes != null) {
                    val fileName = "plant_${UUID.randomUUID()}.webp"
                    val bucket = supabase.storage.from("plant-photos")
                    bucket.upload(fileName, bytes)
                    remotePhotoUrl = bucket.publicUrl(fileName)
                }
            } else {
                remotePhotoUrl = photoUri
            }

            supabase.from("mural_posts").insert(
                MuralPostDto(
                    user_id = userUuid,
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
        GrowWidgetUpdater.refreshAll(appContext)
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
        GrowWidgetUpdater.refreshAll(appContext)
    }

    override suspend fun updatePlantPhoto(plantId: Long, photoUri: String?) {
        val currentPhoto = plantDao.observePlant(plantId).first()?.photoUri
        database.withTransaction {
            plantDao.updatePhoto(plantId, photoUri)
        }
        withContext(Dispatchers.IO) {
            if (currentPhoto != null && currentPhoto != photoUri) {
                deletePhotoIfOwned(appContext, currentPhoto)
                deletePhotoFromSupabaseStorage(currentPhoto)
            }
            syncPlantsToRemote()
        }
        GrowWidgetUpdater.refreshAll(appContext)
    }

    override suspend fun deletePlant(plantId: Long) {
        val plant = plantDao.observePlant(plantId).first()
        val photoUri = plant?.photoUri
        
        // Primeiro deleta do Supabase se usuário logado
        val userUuid = getCurrentUserId()
        if (userUuid != null && plant != null) {
            deletePlantFromSupabase(plantId, userUuid)
        }
        
        database.withTransaction {
            plantDao.deleteById(plantId)
        }
        scheduler.cancelForPlant(plantId)
        deletePhotoIfOwned(appContext, photoUri)
        deletePhotoFromSupabaseStorage(photoUri)
        GrowWidgetUpdater.refreshAll(appContext)
    }

    override suspend fun shareToMural(plantId: Long) {
        val plant = plantDao.observePlant(plantId).first() ?: return
        if (plant.sharedOnMural) return

        database.withTransaction {
            plantDao.updateSharedOnMural(plantId, true)
            muralDao.insertPost(
                com.daime.grow.data.local.entity.MuralPostEntity(
                    plantId = plantId,
                    userId = getCurrentUserId(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        val userUuid = getCurrentUserId()
        if (userUuid != null) {
            withContext(Dispatchers.IO) {
                syncToSupabase(
                    name = plant.name,
                    strain = plant.strain,
                    stage = plant.stage,
                    medium = plant.medium,
                    days = plant.days,
                    photoUri = plant.photoUri
                )
            }
        }
    }

    override suspend fun removeFromMural(plantId: Long) {
        val posts = muralDao.observeMuralPosts().first()
        val post = posts.find { it.plantId == plantId } ?: return
        
        val userUuid = getCurrentUserId()
        if (userUuid != null && post.remoteId != null) {
            withContext(Dispatchers.IO) {
                try {
                    supabaseClient?.from("mural_posts")?.delete {
                        filter { eq("id", post.remoteId) }
                        filter { eq("user_id", userUuid) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao deletar post do mural no Supabase: ${e.message}")
                }
            }
        }

        database.withTransaction {
            muralDao.deletePost(post.id)
            plantDao.updateSharedOnMural(plantId, false)
        }
    }

    private suspend fun deletePlantFromSupabase(localId: Long, userUuid: String) {
        val supabase = supabaseClient ?: return
        try {
            supabase.from("plants").delete {
                filter { eq("local_id", localId) }
                filter { eq("user_id", userUuid) }
            }
            Log.d(TAG, "deletePlantFromSupabase: Planta $localId deletada do Supabase")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar planta do Supabase: ${e.message}")
        }
    }

    override suspend fun updatePlantsOrder(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        database.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                plantDao.updateSortOrder(id, index)
            }
        }
        syncPlantsToRemote()
        GrowWidgetUpdater.refreshAll(appContext)
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
        GrowWidgetUpdater.refreshAll(appContext)
    }

    override suspend fun seedDataIfNeeded() {
        // Sincroniza a configuração remota do Supabase que controla o mascaramento
        syncRemoteConfig()
        
        // Se usuário estiver logado, baixa dados do remoto
        if (getCurrentUserId() != null) {
            syncPlantsFromRemote()
        }
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
        // Desabilitado - não temos mais a tabela app_config
        // Pode ser habilitado novamente se precisar de config remota no futuro
    }

    override suspend fun exportBackup(uri: Uri) {
        backupManager.exportTo(uri)
    }

    override suspend fun importBackup(uri: Uri) {
        backupManager.importFrom(uri)
        GrowWidgetUpdater.refreshAll(appContext)
    }

    // Mural
    override fun observeMuralPosts(): Flow<List<com.daime.grow.data.local.dao.MuralPostWithPlant>> {
        return muralDao.observeMuralPostsWithPlants()
    }

    override fun observeComments(postId: Long): Flow<List<com.daime.grow.data.local.dao.CommentWithUser>> {
        return muralDao.observeCommentsWithUsers(postId)
    }

    override suspend fun addComment(postId: Long, userId: Long, content: String, parentId: String?) {
        muralDao.insertComment(
            com.daime.grow.data.local.entity.MuralCommentEntity(
                localPostId = postId,
                localUserId = userId,
                content = content,
                createdAt = System.currentTimeMillis(),
                parentId = parentId
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
            try {
                supabaseClient?.from("mural_users")?.insert(MuralUserDto(username = username))
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao criar usuário remoto: ${e.message}")
            }
            return localId
        }
        return user.id
    }

    private var cachedUserUuid: String? = null

    override suspend fun getCurrentUserId(): String? {
        if (cachedUserUuid != null) return cachedUserUuid
        return muralPreferencesRepository.currentUserUuid.first().also {
            cachedUserUuid = it
        }
    }

    fun setCurrentUserUuid(userUuid: String) {
        cachedUserUuid = userUuid
    }

private fun getDeviceUserId(): String {
        val prefs = appContext.getSharedPreferences("device_id", Context.MODE_PRIVATE)
        var deviceId = prefs.getString("device_uuid", null)
        if (deviceId == null) {
            val androidId = Settings.Secure.getString(
                appContext.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown_${UUID.randomUUID()}"
            // Use simple ID without special chars
            deviceId = "device_${androidId.take(16)}"
            prefs.edit().putString("device_uuid", deviceId).apply()
            Log.d(TAG, "Generated device ID: $deviceId")
        }
        return deviceId
    }

    override suspend fun syncPlantsToRemote() {
        withContext(Dispatchers.IO) {
            val supabase = supabaseClient ?: run {
                Log.w(TAG, "syncPlantsToRemote: Supabase não configurado, pulando")
                return@withContext
            }
            
            val userUuid = getCurrentUserId() ?: run {
                Log.d(TAG, "syncPlantsToRemote: usuário não logado, pulando sync")
                return@withContext
            }

            try {
                val plants = plantDao.getAllNow()
                Log.d(TAG, "syncPlantsToRemote: Found ${plants.size} plants locally")
                if (plants.isEmpty()) {
                    return@withContext
                }
                val now = System.currentTimeMillis()

                for (plant in plants) {
                    try {
                        var remotePhotoUrl: String? = null

                        if (plant.photoUri != null && !plant.photoUri.startsWith("http")) {
                            val bytes = ImageUtils.compressImageToWebP(appContext, Uri.parse(plant.photoUri))
                            if (bytes != null) {
                                val fileName = "plant_${plant.id}.webp"
                                val bucket = supabase.storage.from("plant-photos")
                                bucket.upload(fileName, bytes)
                                remotePhotoUrl = bucket.publicUrl(fileName)
                            }
                        } else if (plant.photoUri?.startsWith("http") == true) {
                            remotePhotoUrl = plant.photoUri
                        }

                        // Upsert usando local_id para evitar duplicatas no servidor
                        supabase.from("plants").upsert(
                            PlantDto(
                                user_id = userUuid,
                                local_id = plant.id,
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
                        ) {
                            onConflict = "user_id,local_id"
                        }
                        Log.d(TAG, "syncPlantsToRemote: Planta ${plant.name} sincronizada")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro ao sincronizar planta ${plant.name}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao sincronizar plantas: ${e.message}")
            }
        }
    }

    private suspend fun removeLocalDuplicates() {
        val allPlants = plantDao.getAllNow()
        val seen = mutableSetOf<Pair<String, Long>>()
        for (plant in allPlants) {
            // Chave de unicidade: Nome + Data de Criação
            val key = plant.name to plant.createdAt
            if (key in seen) {
                Log.w(TAG, "removeLocalDuplicates: Removendo planta duplicada: ${plant.name} (ID: ${plant.id})")
                plantDao.deleteById(plant.id)
            } else {
                seen.add(key)
            }
        }
    }

    override suspend fun syncPlantsFromRemote() {
        val supabase = supabaseClient ?: return
        
        val userUuid = getCurrentUserId() ?: run {
            Log.d(TAG, "syncPlantsFromRemote: usuário não logado, pulando download")
            return
        }

        // Limpa duplicatas locais antes de baixar novas
        removeLocalDuplicates()

        try {
            val remotePlants = supabase.from("plants")
                .select { filter { eq("user_id", userUuid) } }
                .decodeList<PlantDto>()

            val now = System.currentTimeMillis()

            for (dto in remotePlants) {
                try {
                    val dtoId = dto.id
                    
                    // Estratégia de busca robusta para evitar duplicatas:
                    // 1. Pela data de criação (mais confiável entre dispositivos)
                    // 2. Pelo local_id (se coincidir)
                    // 3. Pelo nome (fallback)
                    val createdAt = dto.created_at
                    val localId = dto.local_id
                    
                    val existingPlant = if (createdAt != null && createdAt > 0) {
                        plantDao.getPlantByCreatedAt(createdAt)
                    } else if (localId != null && localId > 0L) {
                        plantDao.getPlantById(localId)
                    } else {
                        plantDao.getPlantByName(dto.name)
                    }

                    if (existingPlant != null) {
                        // Atualiza campos remota para local
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
                        
                        // Se o local_id no remote estiver diferente do ID local atual, atualiza no remote
                        // Isso garante que cada dispositivo mapeie corretamente suas IDs locais
                        if (dtoId != null && dto.local_id != existingPlant.id) {
                            supabase.from("plants").update({ set("local_id", existingPlant.id) }) {
                                filter { eq("id", dtoId) }
                            }
                        }
                    } else {
                        // Cria nova planta local
                        val newId = plantDao.insert(
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
                        // Atualiza o remote com o novo local_id deste dispositivo
                        if (dtoId != null) {
                            supabase.from("plants").update({ set("local_id", newId) }) {
                                filter { eq("id", dtoId) }
                            }
                        }
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

private fun cultivationDayTicker(): Flow<Long> = flow {
    while (true) {
        val now = System.currentTimeMillis()
        emit(now)
        delay(millisUntilNextLocalMidnight(now) + 1_000)
    }
}

private fun comparePlants(sortAsc: Boolean): Comparator<PlantEntity> {
    return Comparator { first, second ->
        val sortOrderComparison = first.sortOrder.compareTo(second.sortOrder)
        if (sortOrderComparison != 0) {
            return@Comparator sortOrderComparison
        }

        val firstDays = calculateCultivationDays(first.days, first.createdAt)
        val secondDays = calculateCultivationDays(second.days, second.createdAt)
        val daysComparison = if (sortAsc) {
            firstDays.compareTo(secondDays)
        } else {
            secondDays.compareTo(firstDays)
        }

        if (daysComparison != 0) {
            daysComparison
        } else {
            second.createdAt.compareTo(first.createdAt)
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

private suspend fun deletePhotoFromSupabaseStorage(photoUrl: String?) {
    if (photoUrl.isNullOrBlank()) return
    try {
        val supabase = SupabaseClient.clientOrNull ?: return
        val url = photoUrl.removePrefix(supabase.storage.from("plant-photos").publicUrl(""))
        if (url.isNotBlank()) {
            supabase.storage.from("plant-photos").delete(url)
            Log.d(TAG, "Imagem deletada do Storage: $url")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao deletar imagem do Storage: ${e.message}")
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
    nextWateringDate = nextWateringDate,
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
