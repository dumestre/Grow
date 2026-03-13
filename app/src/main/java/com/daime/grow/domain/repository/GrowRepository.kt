package com.daime.grow.domain.repository

import android.net.Uri
import com.daime.grow.data.local.dao.CommentWithUser
import com.daime.grow.data.local.dao.MuralPostWithPlant
import com.daime.grow.domain.model.NutrientLog
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantDetails
import com.daime.grow.domain.model.SecurityPreferences
import kotlinx.coroutines.flow.Flow

interface GrowRepository {
    fun observePlants(query: String, stageFilter: String, sortAsc: Boolean): Flow<List<Plant>>
    fun observePlantDetails(plantId: Long): Flow<PlantDetails?>
    suspend fun addPlant(
        name: String,
        strain: String,
        stage: String,
        medium: String,
        days: Int,
        photoUri: String?,
        shareOnMural: Boolean = false
    ): Long

    suspend fun addQuickEvent(plantId: Long, type: String, note: String = "")
    suspend fun addWatering(plantId: Long, volumeMl: Int, intervalDays: Int, substrate: String)
    suspend fun addNutrient(log: NutrientLog)
    suspend fun toggleChecklist(itemId: Long, done: Boolean)
    suspend fun updatePlantStage(plantId: Long, stage: String)
    suspend fun deletePlant(plantId: Long)
    suspend fun updatePlantsOrder(orderedIds: List<Long>)
    suspend fun seedDataIfNeeded()

    fun observeSecurityPreferences(): Flow<SecurityPreferences>
    suspend fun setLockEnabled(enabled: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun updatePin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun setAlternativeIcons(enabled: Boolean)

    suspend fun exportBackup(uri: Uri)
    suspend fun importBackup(uri: Uri)

    // Mural
    fun observeMuralPosts(): Flow<List<MuralPostWithPlant>>
    fun observeMuralPost(postId: Long): Flow<MuralPostWithPlant?>
    fun observeComments(postId: Long): Flow<List<CommentWithUser>>
    suspend fun addComment(postId: Long, userId: Long, content: String, parentId: Long? = null)
    suspend fun createOrGetUser(username: String): Long
    suspend fun getCurrentUserId(): Long?
}
