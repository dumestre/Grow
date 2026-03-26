package com.daime.grow.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.local.dao.HarvestDao
import com.daime.grow.data.local.dao.PlantDao
import com.daime.grow.data.local.entity.HarvestBatchEntity
import com.daime.grow.data.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class HarvestBatchWithPhoto(
    val batch: HarvestBatchEntity,
    val photoUri: String?
)

@HiltViewModel
class PosColhetaViewModel @Inject constructor(
    application: Application,
    private val harvestDao: HarvestDao,
    private val plantDao: PlantDao
) : AndroidViewModel(application) {
    
    private val scheduler = ReminderScheduler(application)

    val dryingBatches: StateFlow<List<HarvestBatchWithPhoto>> = harvestDao.observeDryingBatches()
        .map { batches ->
            batches.map { batch ->
                val photoUri = runBlocking { plantDao.getPlantById(batch.plantId)?.photoUri }
                HarvestBatchWithPhoto(batch, photoUri)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val curingBatches: StateFlow<List<HarvestBatchWithPhoto>> = harvestDao.observeCuringBatches()
        .map { batches ->
            batches.map { batch ->
                val photoUri = runBlocking { plantDao.getPlantById(batch.plantId)?.photoUri }
                HarvestBatchWithPhoto(batch, photoUri)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateHumidity(id: Long, humidity: Float) {
        viewModelScope.launch {
            val batch = harvestDao.getBatchById(id) ?: return@launch
            harvestDao.updateBatch(batch.copy(currentHumidity = humidity))
        }
    }

    fun burp(id: Long) {
        viewModelScope.launch {
            val batch = harvestDao.getBatchById(id) ?: return@launch
            val now = System.currentTimeMillis()
            
            val daysSinceHarvest = ((now - batch.harvestDate) / (1000 * 60 * 60 * 24)).toInt()
            
            val nextIntervalHours = when (batch.status) {
                "DRYING" -> when {
                    daysSinceHarvest < 3 -> 12
                    daysSinceHarvest < 7 -> 24
                    else -> 48
                }
                "CURING" -> when {
                    daysSinceHarvest < 14 -> 24
                    daysSinceHarvest < 28 -> 48
                    daysSinceHarvest < 42 -> 72
                    else -> 168
                }
                else -> 24
            }
            
            val nextBurpTime = now + (nextIntervalHours * 60 * 60 * 1000L)
            harvestDao.updateBatch(batch.copy(lastBurpDate = now, nextBurpDate = nextBurpTime))
            
            scheduler.scheduleBurpReminder(id, nextIntervalHours.toLong())
        }
    }

    fun startCuring(id: Long) {
        viewModelScope.launch {
            val batch = harvestDao.getBatchById(id) ?: return@launch
            harvestDao.updateBatch(batch.copy(status = "CURING"))
            scheduler.scheduleBurpReminder(id, 24)
        }
    }
}
