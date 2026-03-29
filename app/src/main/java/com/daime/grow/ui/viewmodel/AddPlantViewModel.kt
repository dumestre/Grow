package com.daime.grow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.PlantStage
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class AddPlantUiState(
    val name: String = "",
    val strain: String = "",
    val stage: String = PlantStage.SEEDLING,
    val medium: String = "",
    val days: String = "",
    val photoUri: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val shareOnMural: Boolean = false,
    val isHydroponic: Boolean = false
)

sealed interface AddPlantUiEvent {
    data object RequiredFieldsError : AddPlantUiEvent
    data object Saved : AddPlantUiEvent
}

@HiltViewModel
class AddPlantViewModel @Inject constructor(
    private val repository: GrowRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddPlantUiState())
    val uiState: StateFlow<AddPlantUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<AddPlantUiEvent>()
    val events = _events.asSharedFlow()

    fun onNameChange(value: String) = update { copy(name = value, error = null) }
    fun onStrainChange(value: String) = update { copy(strain = value, error = null) }
    fun onStageChange(value: String) = update { copy(stage = value, error = null) }
    fun onMediumChange(value: String) = update { copy(medium = value, error = null) }
    fun onDaysChange(value: String) = update { copy(days = value.filter { c -> c.isDigit() }, error = null) }
    fun onPhotoSelected(uri: String?) = update { copy(photoUri = uri) }
    fun onShareOnMuralChange(value: Boolean) = update { copy(shareOnMural = value) }
    fun onHydroponicChange(value: Boolean) = update { copy(isHydroponic = value) }

    fun save(onSaved: (Long) -> Unit) {
        val state = _uiState.value
        val days = state.days.toIntOrNull()
        if (state.name.isBlank() || state.strain.isBlank() || state.medium.isBlank() || days == null) {
            viewModelScope.launch { _events.emit(AddPlantUiEvent.RequiredFieldsError) }
            update { copy(error = "Preencha todos os campos obrigatórios") }
            return
        }

        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val id = repository.addPlant(
                name = state.name.trim(),
                strain = state.strain.trim(),
                stage = state.stage,
                medium = state.medium.trim(),
                days = days,
                photoUri = state.photoUri,
                shareOnMural = state.shareOnMural,
                isHydroponic = state.isHydroponic
            )
            _events.emit(AddPlantUiEvent.Saved)
            update { AddPlantUiState() }
            onSaved(id)
        }
    }

    private inline fun update(block: AddPlantUiState.() -> AddPlantUiState) {
        _uiState.value = _uiState.value.block()
    }
}
