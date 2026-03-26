package com.daime.grow.ui.viewmodel

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daime.grow.domain.model.SecurityPreferences
import com.daime.grow.domain.repository.GrowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class LockUiState(
    val isReady: Boolean = false,
    val pinInput: String = "",
    val showLockScreen: Boolean = false,
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LockViewModel @Inject constructor(private val repository: GrowRepository) : ViewModel() {
    private val input = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)
    private val authenticated = MutableStateFlow(false)

    val uiState: StateFlow<LockUiState> = combine(
        repository.observeSecurityPreferences(),
        input,
        error,
        authenticated
    ) { prefs: SecurityPreferences, pinInput, currentError, isAuthenticated ->
        val hasPin = prefs.pinHash.isNotBlank()
        val shouldLock = prefs.lockEnabled && !isAuthenticated
        LockUiState(
            isReady = true,
            pinInput = pinInput,
            showLockScreen = shouldLock,
            biometricEnabled = prefs.biometricEnabled || !hasPin,
            pinEnabled = hasPin,
            error = currentError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockUiState())

    fun onPinInputChange(value: String) {
        input.value = value.take(6).filter { it.isDigit() }
        error.value = null
    }

    fun unlockWithPin() {
        viewModelScope.launch {
            val valid = repository.verifyPin(input.value)
            if (valid) {
                authenticated.value = true
                error.value = null
            } else {
                error.value = "PIN inválido"
            }
            input.value = ""
        }
    }

    fun tryBiometric(context: Context) {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            error.value = "Nao foi possivel abrir o desbloqueio do celular"
            return
        }

        val available = canUseBiometric(activity)
        if (!available) {
            error.value = "Biometria ou bloqueio do celular indisponível"
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated.value = true
                    error.value = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error.value = errString.toString()
                }

                override fun onAuthenticationFailed() {
                    error.value = "Falha na autenticacao"
                }
            }
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear Grow")
                .setSubtitle("Use biometria ou bloqueio do celular")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    private fun canUseBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
        return when (this) {
            is FragmentActivity -> this
            is ContextWrapper -> baseContext.findFragmentActivity()
            else -> null
        }
    }
}

