package com.daime.grow.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth

object SupabaseClient {
    private const val SUPABASE_URL = "https://dvyidxhutjmgtvkjkkkm.supabase.co"
    // Usando a Key Anon (Public) Completa que você passou
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR2eWlkeGh1dGptZ3R2a2pra2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMwMDYxNzYsImV4cCI6MjA4ODU4MjE3Nn0.nEM7LBIYMHWe0q549hmiuVDfXKreagXIX9MUE3Wprzc"

    val client = createSupabaseClient(SUPABASE_URL, SUPABASE_KEY) {
        install(Postgrest)
        install(Storage)
        install(Realtime)
        install(Auth)
    }
}
