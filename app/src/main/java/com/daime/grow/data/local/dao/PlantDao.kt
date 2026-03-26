package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daime.grow.data.local.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query(
        """
        SELECT * FROM plants
        WHERE (:query = '' OR name LIKE '%' || :query || '%' OR strain LIKE '%' || :query || '%')
        AND (:stageFilter = 'Todas' OR stage = :stageFilter)
        ORDER BY
            sortOrder ASC,
            CASE WHEN :sortAsc = 1 THEN days END ASC,
            CASE WHEN :sortAsc = 0 THEN days END DESC,
            createdAt DESC
        """
    )
    fun observePlants(query: String, stageFilter: String, sortAsc: Int): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :plantId LIMIT 1")
    fun observePlant(plantId: Long): Flow<PlantEntity?>

    @Query("SELECT * FROM plants")
    suspend fun getAllNow(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :plantId LIMIT 1")
    suspend fun getPlantById(plantId: Long): PlantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PlantEntity>)

    @Update
    suspend fun update(plant: PlantEntity)

    @Query("UPDATE plants SET nextWateringDate = :nextWateringDate WHERE id = :plantId")
    suspend fun updateNextWateringDate(plantId: Long, nextWateringDate: Long)

    @Query("UPDATE plants SET stage = :stage WHERE id = :plantId")
    suspend fun updateStage(plantId: Long, stage: String)

    @Query("UPDATE plants SET photoUri = :photoUri WHERE id = :plantId")
    suspend fun updatePhoto(plantId: Long, photoUri: String?)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM plants")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE plants SET sortOrder = :sortOrder WHERE id = :plantId")
    suspend fun updateSortOrder(plantId: Long, sortOrder: Int)

    @Query("DELETE FROM plants WHERE id = :plantId")
    suspend fun deleteById(plantId: Long)

    @Query("SELECT COUNT(*) FROM plants")
    suspend fun count(): Int

    @Query("DELETE FROM plants")
    suspend fun clearAll()
}

