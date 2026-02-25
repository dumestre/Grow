package com.daime.grow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daime.grow.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist_items WHERE plantId = :plantId ORDER BY createdAt ASC")
    fun observeByPlantId(plantId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllNow(): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("UPDATE checklist_items SET done = :done WHERE id = :itemId")
    suspend fun toggle(itemId: Long, done: Boolean)

    @Query("DELETE FROM checklist_items")
    suspend fun clearAll()
}

