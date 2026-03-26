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
    val store: ViewModelProvider.Factory = singleFactory { StoreViewModel() }
    val mural: ViewModelProvider.Factory = singleFactory { 
        MuralViewModel(container.muralDao, container.muralPreferencesRepository) 
    }
    val notifications: ViewModelProvider.Factory = singleFactory {
        NotificationViewModel(container.database.notificationDao())
    }
    val posColheta: ViewModelProvider.Factory = singleFactory {
        PosColhetaViewModel(container.database.harvestDao())
    }

    val detail = DetailFactory(repository)

    class DetailFactory(private val repository: com.daime.grow.domain.repository.GrowRepository) {
        fun create(plantId: Long): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PlantDetailViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return PlantDetailViewModel(
                        savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("plantId" to plantId)),
                        repository = repository
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
            }
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
