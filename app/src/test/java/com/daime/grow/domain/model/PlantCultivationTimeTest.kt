package com.daime.grow.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PlantCultivationTimeTest {

    private val zoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun calculateCultivationDays_keepsBaseCountOnCreationDay() {
        val createdAt = ZonedDateTime.of(2026, 4, 14, 22, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val sameDay = ZonedDateTime.of(2026, 4, 14, 23, 59, 0, 0, zoneId).toInstant().toEpochMilli()

        val result = calculateCultivationDays(
            baseDays = 21,
            createdAt = createdAt,
            now = sameDay,
            zoneId = zoneId
        )

        assertThat(result).isEqualTo(21)
    }

    @Test
    fun calculateCultivationDays_advancesAtLocalMidnight() {
        val createdAt = ZonedDateTime.of(2026, 4, 14, 22, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val nextDay = ZonedDateTime.of(2026, 4, 15, 0, 1, 0, 0, zoneId).toInstant().toEpochMilli()

        val result = calculateCultivationDays(
            baseDays = 21,
            createdAt = createdAt,
            now = nextDay,
            zoneId = zoneId
        )

        assertThat(result).isEqualTo(22)
    }
}
