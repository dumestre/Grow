package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daime.grow.data.local.entity.PlantEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantEventDao {
    @Query("SELECT * FROM plant_events WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun observeByPlantId(plantId: Long): Flow<List<PlantEventEntity>>

    @Query("SELECT * FROM plant_events")
    suspend fun getAllNow(): List<PlantEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: PlantEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<PlantEventEntity>)

    @Query("DELETE FROM plant_events")
    suspend fun clearAll()
}

