package com.daime.grow.data.remote

import android.content.Context
import com.daime.grow.BuildConfig
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.logging.LogLevel


object SupabaseClient {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val clientOrNull: SupabaseClientType? by lazy {
        if (!isConfigured) return@lazy null
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            defaultLogLevel = LogLevel.WARNING
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Auth) {
                host = "callback"
                scheme = "com.daime.grow"
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()

                appContext?.let {
                    sessionManager = AndroidSessionManager(it)
                }
            }
        }
    }

    val client: SupabaseClientType
        get() = requireNotNull(clientOrNull) {
            "Supabase não configurado. Defina SUPABASE_URL e SUPABASE_ANON_KEY (env vars ou local.properties)."
        }
}
