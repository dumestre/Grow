package com.daime.grow.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daime.grow.data.local.dao.HarvestDao

class BurpReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val batchId = inputData.getLong("batchId", -1L)
        if (batchId <= 0L) return Result.success()
        
        val harvestDao = applicationContext.let { 
            com.daime.grow.data.local.GrowDatabase.getInstance(it).harvestDao() 
        }
        
        val batch = harvestDao.getBatchById(batchId) ?: return Result.success()
        
        val isDrying = batch.status == "DRYING"
        val daysSinceHarvest = ((System.currentTimeMillis() - batch.harvestDate) / (1000 * 60 * 60 * 24)).toInt()
        
        val title = if (isDrying) {
            when {
                daysSinceHarvest < 3 -> "Hora do respiro! 🌬️"
                daysSinceHarvest < 7 -> "Hora do respiro! 🌬️"
                else -> "Verificar secagem 🌿"
            }
        } else {
            when {
                daysSinceHarvest < 14 -> "Hora do respiro! 🌬️"
                daysSinceHarvest < 28 -> "Verificar cura 🌱"
                else -> "Verificar umidade 💨"
            }
        }
        
        val body = if (isDrying) {
            when {
                daysSinceHarvest < 3 -> "${batch.plantName}: Abra o frasco 2x hoje (manhã e tarde)"
                daysSinceHarvest < 7 -> "${batch.plantName}: Abra o frasco 1x hoje"
                else -> "${batch.plantName}: Abra o frasco a cada 2-3 dias"
            }
        } else {
            when {
                daysSinceHarvest < 14 -> "${batch.plantName}: Abra o frasco por 5-10 minutos"
                daysSinceHarvest < 28 -> "${batch.plantName}: Abra o frasco a cada 2 dias"
                daysSinceHarvest < 42 -> "${batch.plantName}: Abra o frasco a cada 3-4 dias"
                else -> "${batch.plantName}: Abra o frasco semanalmente"
            }
        }
        
        NotificationHelper.showBurpReminder(applicationContext, batchId, title, body, isDrying)
        return Result.success()
    }
}
