package com.daime.grow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.muralDataStore by preferencesDataStore(name = "mural_settings")

class MuralPreferencesRepository(private val context: Context) {
    private object Keys {
        val currentUserUuid = stringPreferencesKey("current_user_uuid")
    }

    val currentUserUuid: Flow<String?> = context.muralDataStore.data.map { preferences ->
        preferences[Keys.currentUserUuid]
    }

    suspend fun saveUserUuid(userUuid: String) {
        context.muralDataStore.edit { preferences ->
            preferences[Keys.currentUserUuid] = userUuid
        }
    }

    suspend fun clearUserUuid() {
        context.muralDataStore.edit { preferences ->
            preferences.remove(Keys.currentUserUuid)
        }
    }
}
