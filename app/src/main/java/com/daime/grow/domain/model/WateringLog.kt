package com.daime.grow.domain.model

data class WateringLog(
    val id: Long = 0,
    val plantId: Long,
    val volumeMl: Int,
    val intervalDays: Int,
    val substrate: String,
    val nextWateringDate: Long,
    val createdAt: Long
)

