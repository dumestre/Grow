package com.daime.grow.domain.model

data class PlantEvent(
    val id: Long = 0,
    val plantId: Long,
    val type: String,
    val note: String,
    val createdAt: Long
)

