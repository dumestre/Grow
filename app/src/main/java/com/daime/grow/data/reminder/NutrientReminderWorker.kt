package com.daime.grow.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NutrientReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val plantName = inputData.getString("plantName") ?: "Planta"
        NotificationHelper.showNotification(
            context = applicationContext,
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title = "Nutrientes",
            body = "${plantName}: revisar EC/pH e nutrientes"
        )
        return Result.success()
    }
}

