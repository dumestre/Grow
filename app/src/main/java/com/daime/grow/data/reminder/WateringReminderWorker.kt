package com.daime.grow.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WateringReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val plantName = inputData.getString("plantName") ?: "Planta"
        NotificationHelper.showNotification(
            context = applicationContext,
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = "Hora da rega",
            body = "${plantName}: lembrete de rega"
        )
        return Result.success()
    }
}

