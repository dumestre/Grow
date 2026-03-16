package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daime.grow.data.local.entity.HarvestBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HarvestDao {
    @Query("SELECT * FROM harvest_batches WHERE status = 'DRYING' ORDER BY harvestDate DESC")
    fun observeDryingBatches(): Flow<List<HarvestBatchEntity>>

    @Query("SELECT * FROM harvest_batches WHERE status = 'CURING' ORDER BY harvestDate DESC")
    fun observeCuringBatches(): Flow<List<HarvestBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: HarvestBatchEntity): Long

    @Update
    suspend fun updateBatch(batch: HarvestBatchEntity)

    @Query("SELECT * FROM harvest_batches WHERE id = :id")
    suspend fun getBatchById(id: Long): HarvestBatchEntity?

    @Query("DELETE FROM harvest_batches WHERE id = :id")
    suspend fun deleteBatch(id: Long)
}
