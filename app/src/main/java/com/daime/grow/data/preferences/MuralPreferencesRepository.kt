package com.daime.grow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.muralDataStore by preferencesDataStore(name = "mural_settings")

class MuralPreferencesRepository(private val context: Context) {
    private object Keys {
        val currentUserId = longPreferencesKey("current_user_id")
    }

    val currentUserId: Flow<Long?> = context.muralDataStore.data.map { preferences ->
        preferences[Keys.currentUserId]
    }

    suspend fun saveUserId(userId: Long) {
        context.muralDataStore.edit { preferences ->
            preferences[Keys.currentUserId] = userId
        }
    }
}
