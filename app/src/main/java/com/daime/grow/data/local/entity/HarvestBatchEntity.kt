package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "harvest_batches")
data class HarvestBatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plantId: Long,
    val plantName: String,
    val strain: String,
    val harvestDate: Long,
    val status: String, // "DRYING", "CURING", "FINISHED"
    val currentHumidity: Float?,
    val currentTemperature: Float?,
    val lastBurpDate: Long?,
    val nextBurpDate: Long?,
    val weightGrams: Float? = null
)
