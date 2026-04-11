package com.daime.grow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_settings")

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val disclaimerAccepted = booleanPreferencesKey("disclaimer_accepted")
    }

    fun observeDisclaimerAccepted(): Flow<Boolean> = context.appDataStore.data
        .map { prefs -> prefs[Keys.disclaimerAccepted] ?: false }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.appDataStore.edit { it[Keys.disclaimerAccepted] = accepted }
    }
}
