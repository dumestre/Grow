package com.daime.grow.data.remote

import com.daime.grow.BuildConfig
import io.github.jan.supabase.SupabaseClient as SupabaseClientType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction


object SupabaseClient {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val clientOrNull: SupabaseClientType? by lazy {
        if (!isConfigured) return@lazy null
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Postgrest)
            install(Storage)
            install(Realtime)
            install(Auth) {
                host = "callback"
                scheme = "com.daime.grow"
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
        }
    }

    val client: SupabaseClientType
        get() = requireNotNull(clientOrNull) {
            "Supabase não configurado. Defina SUPABASE_URL e SUPABASE_ANON_KEY (env vars ou local.properties)."
        }
}
