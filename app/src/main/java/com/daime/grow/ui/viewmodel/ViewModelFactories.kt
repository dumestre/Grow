package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.daime.grow.core.AppContainer
import com.daime.grow.data.repository.GrowRepositoryImpl

class ViewModelFactories(container: AppContainer) {
    private val repository = GrowRepositoryImpl(
        appContext = container.appContext,
        database = container.database,
        scheduler = container.reminderScheduler,
        backupManager = container.backupManager,
        securityRepository = container.preferencesRepository
    )

    val home: ViewModelProvider.Factory = singleFactory { HomeViewModel(repository) }
    val addPlant: ViewModelProvider.Factory = singleFactory { AddPlantViewModel(repository) }
    val lock: ViewModelProvider.Factory = singleFactory { LockViewModel(repository) }
    val settings: ViewModelProvider.Factory = singleFactory { SettingsViewModel(repository) }
    val mural: ViewModelProvider.Factory = singleFactory { MuralViewModel(container.muralDao) }

    val detail = DetailFactory(repository)

    class DetailFactory(private val repository: com.daime.grow.domain.repository.GrowRepository) {
        fun create(plantId: Long): ViewModelProvider.Factory = singleFactory {
            PlantDetailViewModel(plantId = plantId, repository = repository)
        }
    }
}

private inline fun <reified T : ViewModel> singleFactory(crossinline provider: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <R : ViewModel> create(modelClass: Class<R>): R {
            if (modelClass.isAssignableFrom(T::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return provider() as R
            }
            throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
    }
}
