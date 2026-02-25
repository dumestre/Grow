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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddPlantViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRepository
    private lateinit var viewModel: AddPlantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRepository()
        viewModel = AddPlantViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun save_withValidFields_callsRepository() = runTest {
        viewModel.onNameChange("A")
        viewModel.onStrainChange("S")
        viewModel.onMediumChange("Solo")
        viewModel.onDaysChange("14")

        var createdId = 0L
        viewModel.save { createdId = it }
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repository.createdPlants).hasSize(1)
        assertThat(createdId).isEqualTo(100L)
    }
}

private class FakeRepository : GrowRepository {
    val createdPlants = mutableListOf<String>()
    private val security = MutableStateFlow(SecurityPreferences())

    override fun observePlants(query: String, stageFilter: String, sortAsc: Boolean): Flow<List<Plant>> = flowOf(emptyList())
    override fun observePlantDetails(plantId: Long): Flow<PlantDetails?> = flowOf(null)

    override suspend fun addPlant(name: String, strain: String, stage: String, medium: String, days: Int, photoUri: String?): Long {
        createdPlants += "$name-$strain-$stage-$medium-$days-$photoUri"
        return 100L
    }

    override suspend fun addQuickEvent(plantId: Long, type: String, note: String) {}
    override suspend fun addWatering(plantId: Long, volumeMl: Int, intervalDays: Int, substrate: String) {}
    override suspend fun addNutrient(log: NutrientLog) {}
    override suspend fun toggleChecklist(itemId: Long, done: Boolean) {}
    override suspend fun deletePlant(plantId: Long) {}
    override suspend fun seedDataIfNeeded() {}

    override fun observeSecurityPreferences(): Flow<SecurityPreferences> = security
    override suspend fun setLockEnabled(enabled: Boolean) {}
    override suspend fun setBiometricEnabled(enabled: Boolean) {}
    override suspend fun updatePin(pin: String) {}
    override suspend fun verifyPin(pin: String): Boolean = false

    override suspend fun exportBackup(uri: Uri) {}
    override suspend fun importBackup(uri: Uri) {}
}
