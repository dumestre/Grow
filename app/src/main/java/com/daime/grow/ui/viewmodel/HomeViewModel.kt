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
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class HomeUiState(
    val query: String = "",
    val stageFilter: String = PlantStage.ALL,
    val sortAscending: Boolean = true,
    val plants: List<Plant> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

private data class HomeFilters(
    val query: String,
    val stageFilter: String,
    val sortAscending: Boolean
)

sealed interface HomeUiEvent {
    data class ShowDeleteUndo(val plantName: String) : HomeUiEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel @Inject constructor(
    private val repository: GrowRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.seedDataIfNeeded()
        }
    }

    private val query = MutableStateFlow("")
    private val stageFilter = MutableStateFlow(PlantStage.ALL)
    private val sortAscending = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
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

    val uiState: StateFlow<HomeUiState> = combine(
        query,
        filters.flatMapLatest { f ->
            combine(
                repository.observePlants(f.query, f.stageFilter, f.sortAscending),
                pendingDeleteIds,
                _isRefreshing
            ) { plants, hiddenIds, refreshing ->
                Triple(plants.filterNot { it.id in hiddenIds }, refreshing, f)
            }
        }
    ) { currentQuery, (filteredPlants, refreshing, f) ->
        HomeUiState(
            query = currentQuery,
            stageFilter = f.stageFilter,
            sortAscending = f.sortAscending,
            plants = filteredPlants,
            isLoading = filteredPlants.isEmpty() && currentQuery.isEmpty() && f.stageFilter == PlantStage.ALL,
            isRefreshing = refreshing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(isLoading = true))

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncPlantsFromRemote()
            _isRefreshing.value = false
        }
    }

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
        viewModelScope.launch {
            repository.deletePlant(plantId)
        }
    }

    fun updatePlantsOrder(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.updatePlantsOrder(orderedIds)
        }
    }
}
