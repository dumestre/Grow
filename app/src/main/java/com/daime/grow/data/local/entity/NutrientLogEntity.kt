package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nutrient_logs",
    foreignKeys = [
        ForeignKey(
            entity = PlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["plantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plantId")]
)
data class NutrientLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val week: Int,
    val ec: Double,
    val ph: Double,
    val createdAt: Long
)

