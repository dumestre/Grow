package com.daime.grow.domain.model

data class NutrientLog(
    val id: Long = 0,
    val plantId: Long,
    val week: Int,
    val ec: Double,
    val ph: Double,
    val createdAt: Long
)

