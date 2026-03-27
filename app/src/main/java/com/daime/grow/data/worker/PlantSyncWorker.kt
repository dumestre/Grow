package com.daime.grow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daime.grow.core.AppContainer
import com.daime.grow.data.local.GrowDatabase

class PlantSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = GrowDatabase.getInstance(applicationContext)
            val container = AppContainer(applicationContext)

            container.repository.syncPlantsToRemote()
            container.repository.syncPlantsFromRemote()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
