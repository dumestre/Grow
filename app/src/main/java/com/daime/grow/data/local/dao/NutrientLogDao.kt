package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daime.grow.data.local.entity.NutrientLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutrientLogDao {
    @Query("SELECT * FROM nutrient_logs WHERE plantId = :plantId ORDER BY createdAt DESC")
    fun observeByPlantId(plantId: Long): Flow<List<NutrientLogEntity>>

    @Query("SELECT * FROM nutrient_logs")
    suspend fun getAllNow(): List<NutrientLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NutrientLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<NutrientLogEntity>)

    @Query("DELETE FROM nutrient_logs")
    suspend fun clearAll()
}

