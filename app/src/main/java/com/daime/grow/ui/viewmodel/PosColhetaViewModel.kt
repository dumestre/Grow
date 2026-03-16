package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.data.local.dao.HarvestDao
import com.daime.grow.data.local.entity.HarvestBatchEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosColhetaViewModel(
    private val harvestDao: HarvestDao
) : ViewModel() {

    val dryingBatches: StateFlow<List<HarvestBatchEntity>> = harvestDao.observeDryingBatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val curingBatches: StateFlow<List<HarvestBatchEntity>> = harvestDao.observeCuringBatches()
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
            // Próximo respiro em 24h por padrão
            val next = now + (24 * 60 * 60 * 1000)
            harvestDao.updateBatch(batch.copy(lastBurpDate = now, nextBurpDate = next))
        }
    }
}
