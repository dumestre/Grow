package com.daime.grow.ui.screen.ppfd

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PPFDUiState(
    val ppf: String = "",
    val area: String = "",
    val result: Double = 0.0
)

class PPFDViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PPFDUiState())
    val uiState: StateFlow<PPFDUiState> = _uiState.asStateFlow()

    fun updatePPF(value: String) {
        _uiState.update { it.copy(ppf = value.filter { c -> c.isDigit() }) }
    }

    fun updateArea(value: String) {
        _uiState.update { it.copy(area = value.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun calculate() {
        val ppf = _uiState.value.ppf.toDoubleOrNull() ?: 0.0
        val area = _uiState.value.area.toDoubleOrNull() ?: 0.0
        if (area > 0) {
            _uiState.update { it.copy(result = ppf / area) }
        }
    }
}
