package com.daime.grow.ui.viewmodel

import android.net.Uri
import com.daime.grow.domain.model.NutrientLog
import com.daime.grow.domain.model.Plant
import com.daime.grow.domain.model.PlantDetails
import com.daime.grow.domain.model.SecurityPreferences
import com.daime.grow.domain.repository.GrowRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: HomeFakeRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = HomeFakeRepository()
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun query_usesDebounce_beforeLoading() = runTest {
        val collectJob = launch { viewModel.uiState.collect { } }

        viewModel.onQueryChange("g")
        viewModel.onQueryChange("gr")
        viewModel.onQueryChange("grow")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertThat(repository.observedQueries.last()).isEqualTo("grow")
        collectJob.cancel()
    }

    @Test
    fun requestDelete_thenUndo_doesNotDeletePlant() = runTest {
        val plant = samplePlant()
        repository.setPlants(listOf(plant))
        val events = mutableListOf<HomeUiEvent>()
        val collectJob = launch { viewModel.uiState.collect { } }
        val eventJob = launch { viewModel.events.collect { events += it } }
        advanceTimeBy(400)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.plants).containsExactly(plant)

        viewModel.requestDelete(plant)
        runCurrent()
        assertThat(viewModel.uiState.value.plants).isEmpty()

        advanceTimeBy(100)
        viewModel.undoDelete()
        runCurrent()
        assertThat(viewModel.uiState.value.plants).containsExactly(plant)

        advanceTimeBy(5_500)
        advanceUntilIdle()

        assertThat(events).isNotEmpty()
        assertThat(repository.deletedIds).isEmpty()
        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun requestDelete_hidesImmediately_andDeletesAfterDelay() = runTest {
        val plant = samplePlant()
        repository.setPlants(listOf(plant))
        val collectJob = launch { viewModel.uiState.collect { } }
        advanceTimeBy(400)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.plants).containsExactly(plant)

        viewModel.requestDelete(plant)
        runCurrent()
        assertThat(viewModel.uiState.value.plants).isEmpty()

        advanceTimeBy(5_500)
        runCurrent()
        assertThat(repository.deletedIds).containsExactly(plant.id)
        collectJob.cancel()
    }

    private fun samplePlant() = Plant(
        id = 10L,
        name = "Plant A",
        strain = "Hybrid",
        stage = "Vegetativo",
        medium = "Solo",
        days = 20,
        photoUri = null,
        nextWateringDate = null,
        createdAt = 1L
    )
}

private class HomeFakeRepository : GrowRepository {
    val observedQueries = mutableListOf<String>()
    val deletedIds = mutableListOf<Long>()
    private val plants = MutableStateFlow<List<Plant>>(emptyList())
    private val security = MutableStateFlow(SecurityPreferences())

    fun setPlants(value: List<Plant>) {
        plants.value = value
    }

    override fun observePlants(query: String, stageFilter: String, sortAsc: Boolean): Flow<List<Plant>> {
        observedQueries += query
        return plants
    }

    override fun observePlantDetails(plantId: Long): Flow<PlantDetails?> = flowOf(null)

    override suspend fun addPlant(name: String, strain: String, stage: String, medium: String, days: Int, photoUri: String?): Long = 1L
    override suspend fun addQuickEvent(plantId: Long, type: String, note: String) {}
    override suspend fun addWatering(plantId: Long, volumeMl: Int, intervalDays: Int, substrate: String) {}
    override suspend fun addNutrient(log: NutrientLog) {}
    override suspend fun toggleChecklist(itemId: Long, done: Boolean) {}
    override suspend fun updatePlantStage(plantId: Long, stage: String) {}
    override suspend fun deletePlant(plantId: Long) {
        deletedIds += plantId
        plants.value = plants.value.filterNot { it.id == plantId }
    }
    override suspend fun updatePlantsOrder(orderedIds: List<Long>) {}
    override suspend fun seedDataIfNeeded() {}

    override fun observeSecurityPreferences(): Flow<SecurityPreferences> = security
    override suspend fun setLockEnabled(enabled: Boolean) {}
    override suspend fun setBiometricEnabled(enabled: Boolean) {}
    override suspend fun updatePin(pin: String) {}
    override suspend fun verifyPin(pin: String): Boolean = false
    override suspend fun exportBackup(uri: Uri) {}
    override suspend fun importBackup(uri: Uri) {}
}
