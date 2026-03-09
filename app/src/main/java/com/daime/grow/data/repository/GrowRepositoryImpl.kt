package com.daime.grow.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.daime.grow.data.backup.BackupManager
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.local.entity.ChecklistItemEntity
import com.daime.grow.data.local.entity.NutrientLogEntity
import com.daime.grow.data.local.entity.PlantEntity
import com.daime.grow.data.local.entity.PlantEventEntity
import com.daime.grow.data.local.entity.WateringLogEntity
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.reminder.ReminderScheduler
import com.daime.grow.data.remote.SupabaseClient
import com.daime.grow.data.remote.model.MuralPostDto
import com.daime.grow.data.remote.model.MuralUserDto
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
import io.github.jan_tennert.supabase.postgrest.from
import io.github.jan_tennert.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class GrowRepositoryImpl(
    private val appContext: Context,
    private val database: GrowDatabase,
    private val scheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val securityRepository: SecurityPreferencesRepository? = null
) : GrowRepository {

    private val plantDao = database.plantDao()
    private val plantEventDao = database.plantEventDao()
    private val wateringDao = database.wateringLogDao()
    private val nutrientDao = database.nutrientLogDao()
    private val checklistDao = database.checklistDao()
    private val muralDao = database.muralDao()
    private val supabase = SupabaseClient.client

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
        shareOnMural: Boolean
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
                    sharedOnMural = shareOnMural
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

        // Sincronização com Supabase se compartilhado
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
            // Nota: Precisamos converter o ID local do usuário para o UUID do Supabase
            // Para simplificar agora, buscaremos o usuário remoto pelo username local
            val localUser = muralDao.getUserById(userId) ?: return
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
            e.printStackTrace()
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
    }

    override suspend fun updatePlantStage(plantId: Long, stage: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            plantDao.updateStage(plantId, stage)
            
            // Verifica se já existem itens de checklist para esta nova fase
            val currentChecklist = checklistDao.getByPlantId(plantId)
            val hasPhaseItems = currentChecklist.any { it.phase == stage }
            
            if (!hasPhaseItems) {
                val newTasks = ChecklistFactory.defaultChecklist(plantId, stage, now)
                    .map { item ->
                        ChecklistItemEntity(
                            plantId = item.plantId,
                            phase = item.phase,
                            task = item.task,
                            done = item.done,
                            createdAt = item.createdAt
                        )
                    }
                checklistDao.insertAll(newTasks)
            }

            plantEventDao.insert(
                PlantEventEntity(
                    plantId = plantId,
                    type = "Fase",
                    note = "Fase alterada para $stage",
                    createdAt = now
                )
            )
        }
        plantDao.observePlant(plantId).first()?.toDomain()?.let { scheduler.scheduleForPlant(it) }
    }

    override suspend fun deletePlant(plantId: Long) {
        val photoUri = plantDao.observePlant(plantId).first()?.photoUri
        database.withTransaction {
            plantDao.deleteById(plantId)
        }
        scheduler.cancelForPlant(plantId)
        deletePhotoIfOwned(appContext, photoUri)
    }

    override suspend fun updatePlantsOrder(orderedIds: List<Long>) {
        if (orderedIds.isEmpty()) return
        database.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                plantDao.updateSortOrder(id, index)
            }
        }
    }

    override suspend fun seedDataIfNeeded() {
        if (plantDao.count() > 0) return
        val now = System.currentTimeMillis()
        val ids = mutableListOf<Long>()

        database.withTransaction {
            ids += plantDao.insert(
                PlantEntity(
                    name = "Green Apple",
                    strain = "Hybrid",
                    stage = PlantStage.VEGETATIVE,
                    medium = "Solo orgânico",
                    days = 24,
                    photoUri = null,
                    nextWateringDate = now + 2 * 24L * 60L * 60L * 1_000L,
                    sortOrder = 0,
                    createdAt = now
                )
            )
            ids += plantDao.insert(
                PlantEntity(
                    name = "Sunrise",
                    strain = "Sativa",
                    stage = PlantStage.FLOWER,
                    medium = "Coco + perlita",
                    days = 52,
                    photoUri = null,
                    nextWateringDate = now + 1 * 24L * 60L * 60L * 1_000L,
                    sortOrder = 1,
                    createdAt = now
                )
            )

            ids.forEachIndexed { index, id ->
                val stage = if (index == 0) PlantStage.VEGETATIVE else PlantStage.FLOWER
                checklistDao.insertAll(
                    ChecklistFactory.defaultChecklist(id, stage, now).map {
                        ChecklistItemEntity(
                            plantId = it.plantId,
                            phase = it.phase,
                            task = it.task,
                            done = false,
                            createdAt = now
                        )
                    }
                )
                plantEventDao.insert(
                    PlantEventEntity(
                        plantId = id,
                        type = "Seed",
                        note = "Seed data inicial",
                        createdAt = now
                    )
                )
            }
        }

        ids.forEach { id ->
            plantDao.observePlant(id).first()?.toDomain()?.let { scheduler.scheduleForPlant(it) }
        }
    }

    override fun observeSecurityPreferences(): Flow<SecurityPreferences> {
        return requireNotNull(securityRepository) { "Security repository is required" }.observe()
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        requireNotNull(securityRepository).setLockEnabled(enabled)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        requireNotNull(securityRepository).setBiometricEnabled(enabled)
    }

    override suspend fun updatePin(pin: String) {
        requireNotNull(securityRepository).updatePin(pin)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return requireNotNull(securityRepository).verifyPin(pin)
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
                supabase.from("mural_users").insert(
                    MuralUserDto(username = username)
                )
            } catch (e: Exception) {
                e.printStackTrace()
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
    sharedOnMural = sharedOnMural
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
    nextWateringDate = nextDate ?: nextWateringDate, // Fallback if nextDate is null
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
