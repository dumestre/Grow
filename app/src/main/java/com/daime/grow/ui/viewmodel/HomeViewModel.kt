package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val stageFilter: String = PlantStage.ALL,
    val sortAscending: Boolean = true,
    val plants: List<Plant> = emptyList()
)

private data class HomeFilters(
    val query: String,
    val stageFilter: String,
    val sortAscending: Boolean
)

sealed interface HomeUiEvent {
    data class ShowDeleteUndo(val plantName: String) : HomeUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val repository: GrowRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val stageFilter = MutableStateFlow(PlantStage.ALL)
    private val sortAscending = MutableStateFlow(true)
    private val _events = MutableSharedFlow<HomeUiEvent>()
    val events = _events.asSharedFlow()

    private var pendingDelete: Plant? = null
    private var pendingDeleteJob: Job? = null
    private val pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())

    private val debouncedQuery = query
        .debounce(350)
        .distinctUntilChanged()

    private val filters = combine(debouncedQuery, stageFilter, sortAscending) { q, stage, asc ->
        HomeFilters(q, stage, asc)
    }

    // Placeholders para teste corrigidos
    private val placeholders = listOf(
        Plant(
            id = -1,
            name = "Gorilla Glue #4",
            strain = "Gorilla Glue",
            medium = "Solo Orgânico",
            stage = PlantStage.VEGETATIVE,
            days = 42,
            photoUri = null,
            nextWateringDate = null,
            createdAt = System.currentTimeMillis()
        ),
        Plant(
            id = -2,
            name = "Purple Haze",
            strain = "Sativa",
            medium = "Coco",
            stage = PlantStage.FLOWER,
            days = 15,
            photoUri = null,
            nextWateringDate = null,
            createdAt = System.currentTimeMillis()
        ),
        Plant(
            id = -3,
            name = "Northern Lights",
            strain = "Indica",
            medium = "Hidroponia",
            stage = PlantStage.SEEDLING,
            days = 5,
            photoUri = null,
            nextWateringDate = null,
            createdAt = System.currentTimeMillis()
        ),
        Plant(
            id = -4,
            name = "Lemon Haze",
            strain = "Lemon",
            medium = "Solo Mix",
            stage = PlantStage.VEGETATIVE,
            days = 28,
            photoUri = null,
            nextWateringDate = null,
            createdAt = System.currentTimeMillis()
        )
    )

    val uiState: StateFlow<HomeUiState> = filters
        .flatMapLatest { f ->
            combine(
                repository.observePlants(f.query, f.stageFilter, f.sortAscending),
                pendingDeleteIds
            ) { plants, hiddenIds ->
                // Se não houver plantas reais, usa os placeholders
                val plantsToShow = if (plants.isEmpty() && f.query.isBlank() && f.stageFilter == PlantStage.ALL) {
                    placeholders
                } else {
                    plants.filterNot { it.id in hiddenIds }
                }

                HomeUiState(
                    query = f.query,
                    stageFilter = f.stageFilter,
                    sortAscending = f.sortAscending,
                    plants = plantsToShow
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onStageChange(value: String) {
        stageFilter.value = value
    }

    fun toggleSort() {
        sortAscending.value = !sortAscending.value
    }

    fun requestDelete(plant: Plant) {
        if (plant.id < 0) return // Não deleta placeholders
        pendingDeleteJob?.cancel()
        pendingDelete?.let { previous ->
            pendingDeleteIds.update { it - previous.id }
        }
        pendingDelete = plant
        pendingDeleteIds.update { it + plant.id }
        pendingDeleteJob = viewModelScope.launch {
            _events.emit(HomeUiEvent.ShowDeleteUndo(plant.name))
            delay(5_000)
            repository.deletePlant(plant.id)
            pendingDeleteIds.update { it - plant.id }
            pendingDelete = null
            pendingDeleteJob = null
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDelete?.let { plant ->
            pendingDeleteIds.update { it - plant.id }
        }
        pendingDeleteJob = null
        pendingDelete = null
    }

    fun deletePlantImmediately(plantId: Long) {
        if (plantId < 0) return
        viewModelScope.launch {
            repository.deletePlant(plantId)
        }
    }

    fun updatePlantsOrder(orderedIds: List<Long>) {
        if (orderedIds.any { it < 0 }) return
        viewModelScope.launch {
            repository.updatePlantsOrder(orderedIds)
        }
    }
}
