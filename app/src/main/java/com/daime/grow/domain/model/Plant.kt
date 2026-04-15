package com.daime.grow.domain.model

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
) {
    val currentDays: Int
        get() = calculateCultivationDays(days, createdAt)
}

fun calculateCultivationDays(
    baseDays: Int,
    createdAt: Long,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Int {
    val createdDate = Instant.ofEpochMilli(createdAt).atZone(zoneId).toLocalDate()
    val currentDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
    val elapsedDays = ChronoUnit.DAYS.between(createdDate, currentDate).toInt().coerceAtLeast(0)
    return (baseDays + elapsedDays).coerceAtLeast(0)
}

fun millisUntilNextLocalMidnight(
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val nowDateTime = Instant.ofEpochMilli(now).atZone(zoneId)
    val nextMidnight = nowDateTime.toLocalDate().plusDays(1).atStartOfDay(zoneId)
    return ChronoUnit.MILLIS.between(nowDateTime, nextMidnight).coerceAtLeast(1)
}

