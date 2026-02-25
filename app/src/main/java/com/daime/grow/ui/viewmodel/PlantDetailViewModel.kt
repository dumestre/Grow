package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.NutrientLog
import com.daime.grow.domain.model.PlantDetails
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlantDetailUiState(
    val details: PlantDetails? = null,
    val wateringVolume: String = "",
    val wateringInterval: String = "",
    val wateringSubstrate: String = "",
    val nutrientWeek: String = "",
    val nutrientEc: String = "",
    val nutrientPh: String = ""
)

sealed interface PlantDetailUiEvent {
    data object WateringInvalid : PlantDetailUiEvent
    data object WateringSaved : PlantDetailUiEvent
    data object NutrientsInvalid : PlantDetailUiEvent
    data object NutrientsSaved : PlantDetailUiEvent
}

class PlantDetailViewModel(
    private val plantId: Long,
    private val repository: GrowRepository
) : ViewModel() {
    private val formState = MutableStateFlow(PlantDetailUiState())
    private val _events = MutableSharedFlow<PlantDetailUiEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<PlantDetailUiState> = combine(
        repository.observePlantDetails(plantId),
        formState
    ) { details, form ->
        form.copy(details = details)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState())

    fun addQuickAction(type: String) {
        viewModelScope.launch { repository.addQuickEvent(plantId, type) }
    }

    fun onWateringVolumeChange(value: String) {
        formState.value = formState.value.copy(wateringVolume = value.filter { it.isDigit() })
    }

    fun onWateringIntervalChange(value: String) {
        formState.value = formState.value.copy(wateringInterval = value.filter { it.isDigit() })
    }

    fun onWateringSubstrateChange(value: String) {
        formState.value = formState.value.copy(wateringSubstrate = value)
    }

    fun saveWatering() {
        val state = formState.value
        val volume = state.wateringVolume.toIntOrNull()
        val interval = state.wateringInterval.toIntOrNull()
        if (volume == null || interval == null || state.wateringSubstrate.isBlank()) {
            viewModelScope.launch { _events.emit(PlantDetailUiEvent.WateringInvalid) }
            return
        }

        viewModelScope.launch {
            repository.addWatering(plantId, volume, interval, state.wateringSubstrate)
            formState.value = formState.value.copy(
                wateringVolume = "",
                wateringInterval = "",
                wateringSubstrate = ""
            )
            _events.emit(PlantDetailUiEvent.WateringSaved)
        }
    }

    fun onNutrientWeekChange(value: String) {
        formState.value = formState.value.copy(nutrientWeek = value.filter { it.isDigit() })
    }

    fun onNutrientEcChange(value: String) {
        formState.value = formState.value.copy(nutrientEc = value)
    }

    fun onNutrientPhChange(value: String) {
        formState.value = formState.value.copy(nutrientPh = value)
    }

    fun saveNutrients() {
        val state = formState.value
        val week = state.nutrientWeek.toIntOrNull()
        val ec = state.nutrientEc.toDoubleOrNull()
        val ph = state.nutrientPh.toDoubleOrNull()
        if (week == null || ec == null || ph == null) {
            viewModelScope.launch { _events.emit(PlantDetailUiEvent.NutrientsInvalid) }
            return
        }

        viewModelScope.launch {
            repository.addNutrient(
                NutrientLog(
                    plantId = plantId,
                    week = week,
                    ec = ec,
                    ph = ph,
                    createdAt = System.currentTimeMillis()
                )
            )
            formState.value = formState.value.copy(
                nutrientWeek = "",
                nutrientEc = "",
                nutrientPh = ""
            )
            _events.emit(PlantDetailUiEvent.NutrientsSaved)
        }
    }

    fun toggleChecklist(itemId: Long, done: Boolean) {
        viewModelScope.launch { repository.toggleChecklist(itemId, done) }
    }
}

