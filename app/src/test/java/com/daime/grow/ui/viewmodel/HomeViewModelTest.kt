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
        val events = mutableListOf<HomeUiEvent>()
        val eventJob = launch { viewModel.events.collect { events += it } }

        viewModel.requestDelete(
            Plant(
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
        )
        advanceTimeBy(100)
        viewModel.undoDelete()
        advanceTimeBy(5_500)
        advanceUntilIdle()

        assertThat(events).isNotEmpty()
        assertThat(repository.deletedIds).isEmpty()
        eventJob.cancel()
    }
}

private class HomeFakeRepository : GrowRepository {
    val observedQueries = mutableListOf<String>()
    val deletedIds = mutableListOf<Long>()
    private val plants = MutableStateFlow<List<Plant>>(emptyList())
    private val security = MutableStateFlow(SecurityPreferences())

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
    override suspend fun deletePlant(plantId: Long) {
        deletedIds += plantId
    }
    override suspend fun seedDataIfNeeded() {}

    override fun observeSecurityPreferences(): Flow<SecurityPreferences> = security
    override suspend fun setLockEnabled(enabled: Boolean) {}
    override suspend fun setBiometricEnabled(enabled: Boolean) {}
    override suspend fun updatePin(pin: String) {}
    override suspend fun verifyPin(pin: String): Boolean = false
    override suspend fun exportBackup(uri: Uri) {}
    override suspend fun importBackup(uri: Uri) {}
}
