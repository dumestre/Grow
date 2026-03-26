package com.daime.grow.domain.model

data class Plant(
    val id: Long = 0,
    val name: String,
    val strain: String,
    val stage: String,
    val medium: String,
    val days: Int,
    val photoUri: String?,
    val nextWateringDate: Long?,
    val createdAt: Long,
    val sharedOnMural: Boolean = false,
    val isHydroponic: Boolean = false
)

