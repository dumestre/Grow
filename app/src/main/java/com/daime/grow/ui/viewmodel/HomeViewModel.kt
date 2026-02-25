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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val debouncedQuery = query
        .debounce(350)
        .distinctUntilChanged()

    private val filters = combine(debouncedQuery, stageFilter, sortAscending) { q, stage, asc ->
        HomeFilters(q, stage, asc)
    }

    val uiState: StateFlow<HomeUiState> = filters
        .flatMapLatest { f ->
            repository.observePlants(f.query, f.stageFilter, f.sortAscending).map { plants ->
                HomeUiState(
                    query = f.query,
                    stageFilter = f.stageFilter,
                    sortAscending = f.sortAscending,
                    plants = plants
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
        pendingDeleteJob?.cancel()
        pendingDelete = plant
        pendingDeleteJob = viewModelScope.launch {
            _events.emit(HomeUiEvent.ShowDeleteUndo(plant.name))
            delay(5_000)
            repository.deletePlant(plant.id)
            pendingDelete = null
            pendingDeleteJob = null
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        pendingDelete = null
    }

    fun deletePlantImmediately(plantId: Long) {
        viewModelScope.launch {
            repository.deletePlant(plantId)
        }
    }

    fun ensureSeedData() {
        viewModelScope.launch {
            repository.seedDataIfNeeded()
        }
    }
}


