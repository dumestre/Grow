package com.daime.grow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val strain: String,
    val stage: String,
    val medium: String,
    val days: Int,
    val photoUri: String?,
    val nextWateringDate: Long?,
    val sortOrder: Int = 0,
    val createdAt: Long
)

