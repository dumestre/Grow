package com.daime.grow.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daime.grow.domain.model.Plant
import java.util.concurrent.TimeUnit

class ReminderScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleForPlant(plant: Plant) {
        scheduleWateringReminder(plant)
        scheduleNutrientReminder(plant)
    }

    fun cancelForPlant(plantId: Long) {
        workManager.cancelUniqueWork("watering-$plantId")
        workManager.cancelUniqueWork("nutrient-$plantId")
    }

    private fun scheduleWateringReminder(plant: Plant) {
        val date = plant.nextWateringDate ?: return
        val delayMillis = (date - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val request = OneTimeWorkRequestBuilder<WateringReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong("plantId", plant.id)
                    .putString("plantName", plant.name)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "watering-${plant.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleNutrientReminder(plant: Plant) {
        val days = when (plant.stage) {
            "Muda" -> 10L
            "Vegetativo" -> 7L
            "Flora" -> 5L
            else -> 7L
        }

        val request = PeriodicWorkRequestBuilder<NutrientReminderWorker>(days, TimeUnit.DAYS)
            .setInputData(Data.Builder().putString("plantName", plant.name).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "nutrient-${plant.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

