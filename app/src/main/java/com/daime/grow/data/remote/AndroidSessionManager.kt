package com.daime.grow.data.remote

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Gerenciador de sessão customizado para Android que evita logs de erro barulhentos
 * quando nenhuma sessão é encontrada.
 */
class AndroidSessionManager(context: Context) : SessionManager {
    private val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val key = "session"
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString(key, json.encodeToString(session)).apply()
    }

    override suspend fun loadSession(): UserSession {
        val sessionStr = prefs.getString(key, null)
            ?: throw NoSuchElementException("Nenhuma sessão encontrada")
        return json.decodeFromString(sessionStr)
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(key).apply()
    }
}
