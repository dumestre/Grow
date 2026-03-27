package com.daime.grow.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daime.grow.data.local.GrowDatabase
import com.daime.grow.data.preferences.SecurityPreferencesRepository
import com.daime.grow.data.reminder.ReminderScheduler
import com.daime.grow.data.backup.BackupManager
import com.daime.grow.data.repository.GrowRepositoryImpl

class PlantSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = GrowDatabase.getInstance(applicationContext)
            val securityRepository = SecurityPreferencesRepository(applicationContext)
            val reminderScheduler = ReminderScheduler(applicationContext)
            val backupManager = BackupManager(applicationContext, database)
            val repository = GrowRepositoryImpl(
                applicationContext,
                database,
                reminderScheduler,
                backupManager,
                securityRepository
            )

            repository.syncPlantsToRemote()
            repository.syncPlantsFromRemote()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
