package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daime.grow.data.local.entity.WateringLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun observeByPlantId(plantId: Long): Flow<List<WateringLogEntity>>

    @Query("SELECT * FROM watering_logs")
    suspend fun getAllNow(): List<WateringLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WateringLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WateringLogEntity>)

    @Query("DELETE FROM watering_logs")
    suspend fun clearAll()
}

