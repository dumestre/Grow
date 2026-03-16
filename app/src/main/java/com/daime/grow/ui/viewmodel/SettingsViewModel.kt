package com.daime.grow.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.SecurityPreferences
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: GrowRepository
) : ViewModel() {

    sealed interface UiEvent {
        data object InvalidPin : UiEvent
        data object PinUpdated : UiEvent
        data object BackupExported : UiEvent
        data object BackupExportError : UiEvent
        data object BackupImported : UiEvent
        data object BackupImportError : UiEvent
    }

    val security: StateFlow<SecurityPreferences> = repository.observeSecurityPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SecurityPreferences()
        )

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLockEnabled(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBiometricEnabled(enabled) }
    }

    fun updatePin(pin: String) {
        if (pin.length < 4) {
            viewModelScope.launch { _events.emit(UiEvent.InvalidPin) }
            return
        }
        viewModelScope.launch {
            repository.updatePin(pin)
            _events.emit(UiEvent.PinUpdated)
        }
    }

    fun setMaskHomeIcon(enabled: Boolean) {
        viewModelScope.launch { repository.setMaskHomeIcon(enabled) }
    }

    fun setMaskStoreCatalog(enabled: Boolean) {
        viewModelScope.launch { repository.setMaskStoreCatalog(enabled) }
    }

    fun setDarkThemeMode(mode: com.daime.grow.domain.model.DarkThemeMode) {
        viewModelScope.launch { repository.setDarkThemeMode(mode) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.exportBackup(uri) }
                .onSuccess { _events.emit(UiEvent.BackupExported) }
                .onFailure { _events.emit(UiEvent.BackupExportError) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.importBackup(uri) }
                .onSuccess { _events.emit(UiEvent.BackupImported) }
                .onFailure { _events.emit(UiEvent.BackupImportError) }
        }
    }
}
