package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.ChecklistItem
import com.daime.grow.domain.model.NutrientLog
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantDetails
import com.daime.grow.domain.model.PlantEvent
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.domain.model.WateringLog
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    data object StageUpdated : PlantDetailUiEvent
}

class PlantDetailViewModel(
    private val plantId: Long,
    private val repository: GrowRepository
) : ViewModel() {
    private val formState = MutableStateFlow(PlantDetailUiState())
    private val _events = MutableSharedFlow<PlantDetailUiEvent>()
    val events = _events.asSharedFlow()

    // --- Placeholder para Detalhes ---
    private val placeholderDetails = PlantDetails(
        plant = Plant(
            id = -1,
            name = "Gorilla Glue #4",
            strain = "Gorilla Glue",
            medium = "Solo Orgânico",
            stage = PlantStage.VEGETATIVE,
            days = 42,
            photoUri = null,
            nextWateringDate = System.currentTimeMillis() + 86400000,
            createdAt = System.currentTimeMillis() - 3628800000
        ),
        events = listOf(
            PlantEvent(id = -1, plantId = -1, type = "Germinação", note = "Semente brotou em 3 dias", createdAt = System.currentTimeMillis() - 3628800000),
            PlantEvent(id = -2, plantId = -1, type = "Transplante", note = "Vaso de 10L", createdAt = System.currentTimeMillis() - 2592000000),
            PlantEvent(id = -3, plantId = -1, type = "Poda", note = "Topping no 4º nó", createdAt = System.currentTimeMillis() - 1296000000)
        ),
        wateringLogs = listOf(
            WateringLog(id = -1, plantId = -1, volumeMl = 500, intervalDays = 3, substrate = "Solo", nextWateringDate = System.currentTimeMillis(), createdAt = System.currentTimeMillis() - 259200000),
            WateringLog(id = -2, plantId = -1, volumeMl = 500, intervalDays = 3, substrate = "Solo", nextWateringDate = System.currentTimeMillis() - 259200000, createdAt = System.currentTimeMillis() - 518400000)
        ),
        nutrientLogs = listOf(
            NutrientLog(id = -1, plantId = -1, week = 4, ec = 1.2, ph = 6.3, createdAt = System.currentTimeMillis() - 259200000),
            NutrientLog(id = -2, plantId = -1, week = 3, ec = 1.0, ph = 6.2, createdAt = System.currentTimeMillis() - 518400000)
        ),
        checklistItems = listOf(
            ChecklistItem(id = -1, plantId = -1, phase = "VEGETATIVE", task = "LST Inicial", done = true, createdAt = System.currentTimeMillis()),
            ChecklistItem(id = -2, plantId = -1, phase = "VEGETATIVE", task = "Defoliação Leve", done = false, createdAt = System.currentTimeMillis()),
            ChecklistItem(id = -3, plantId = -1, phase = "VEGETATIVE", task = "Checar PH", done = true, createdAt = System.currentTimeMillis())
        )
    )

    private val detailsFlow: Flow<PlantDetails?> = if (plantId < 0) {
        flowOf(placeholderDetails)
    } else {
        repository.observePlantDetails(plantId)
    }

    val uiState: StateFlow<PlantDetailUiState> = combine(
        detailsFlow,
        formState
    ) { details, form ->
        form.copy(details = details)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState())

    fun addQuickAction(type: String, note: String = "") {
        if (plantId < 0) return
        viewModelScope.launch { repository.addQuickEvent(plantId, type, note) }
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
        if (plantId < 0) return
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
        if (plantId < 0) return
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

    fun toggleChecklist(item: ChecklistItem, done: Boolean) {
        if (plantId < 0) return
        viewModelScope.launch {
            repository.toggleChecklist(item.id, done)
            if (done) {
                repository.addQuickEvent(
                    plantId = plantId,
                    type = "Checklist",
                    note = "${item.phase}: ${item.task} concluida"
                )
            }
        }
    }

    fun updatePlantStage(stage: String) {
        if (plantId < 0) return
        val currentStage = uiState.value.details?.plant?.stage ?: return
        if (currentStage == stage) return
        viewModelScope.launch {
            repository.updatePlantStage(plantId, stage)
            _events.emit(PlantDetailUiEvent.StageUpdated)
        }
    }
}
