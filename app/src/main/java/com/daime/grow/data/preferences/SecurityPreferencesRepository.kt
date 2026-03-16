package com.daime.grow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daime.grow.domain.model.DarkThemeMode
import com.daime.grow.domain.model.SecurityPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.securityDataStore by preferencesDataStore(name = "security_settings")

class SecurityPreferencesRepository(private val context: Context) {
    private object Keys {
        val lockEnabled = booleanPreferencesKey("lock_enabled")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
        val pinHash = stringPreferencesKey("pin_hash")
        val maskHomeIcon = booleanPreferencesKey("mask_home_icon")
        val maskStoreCatalog = booleanPreferencesKey("mask_store_catalog")
        val legacyUseAlternativeIcons = booleanPreferencesKey("use_alternative_icons")
        val darkTheme = intPreferencesKey("dark_theme_mode")
    }

    fun observe(): Flow<SecurityPreferences> = context.securityDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val legacyMask = prefs[Keys.legacyUseAlternativeIcons]
            SecurityPreferences(
                lockEnabled = prefs[Keys.lockEnabled] ?: false,
                biometricEnabled = prefs[Keys.biometricEnabled] ?: false,
                pinHash = prefs[Keys.pinHash] ?: "",
                maskHomeIcon = prefs[Keys.maskHomeIcon] ?: legacyMask ?: true,
                maskStoreCatalog = prefs[Keys.maskStoreCatalog] ?: legacyMask ?: true,
                darkTheme = DarkThemeMode.entries.getOrElse(prefs[Keys.darkTheme] ?: 0) { DarkThemeMode.SYSTEM }
            )
        }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.lockEnabled] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.biometricEnabled] = enabled }
    }

    suspend fun updatePin(pin: String) {
        context.securityDataStore.edit { it[Keys.pinHash] = hashPin(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.securityDataStore.data.map { it[Keys.pinHash] ?: "" }
        val stored = prefs.first()
        if (stored.isBlank()) return false
        return stored == hashPin(pin)
    }

    suspend fun setMaskHomeIcon(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.maskHomeIcon] = enabled }
    }

    suspend fun setMaskStoreCatalog(enabled: Boolean) {
        context.securityDataStore.edit { it[Keys.maskStoreCatalog] = enabled }
    }

    suspend fun setAllMasking(enabled: Boolean) {
        context.securityDataStore.edit {
            it[Keys.maskHomeIcon] = enabled
            it[Keys.maskStoreCatalog] = enabled
        }
    }

    suspend fun setDarkThemeMode(mode: DarkThemeMode) {
        context.securityDataStore.edit { it[Keys.darkTheme] = mode.ordinal }
    }

    private fun hashPin(pin: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
