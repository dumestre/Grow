package com.daime.grow.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WateringReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val plantId = inputData.getLong("plantId", -1L)
        if (plantId <= 0L) return Result.success()
        val plantName = inputData.getString("plantName") ?: "Planta"
        NotificationHelper.showWateringReminder(applicationContext, plantId, plantName)
        return Result.success()
    }
}

