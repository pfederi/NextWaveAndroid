package com.lakeshorestudios.nextwave.data.api

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Single Supabase client + anonymous session, mirroring the iOS SupabaseManager. */
object SupabaseManager {

    private const val SUPABASE_URL = "https://nextwaveapp.db.lakeshorestudios.ch"
    private const val SUPABASE_ANON_KEY =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzdXBhYmFzZSIsImlhdCI6MTc3MzIyMjMwMCwiZXhwIjo0OTI4ODk1OTAwLCJyb2xlIjoiYW5vbiJ9.grHX8Y9WcO08HrvamEgUpfcDvYJmjo6thF3rL9-wD3Y"

    private val sessionMutex = Mutex()

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }

    /** Ensures an anonymous session exists; returns the lowercase user id. */
    suspend fun ensureSession(): String {
        // Fast path: session already exists, no lock needed.
        client.auth.currentUserOrNull()?.let { return it.id.lowercase() }
        // Slow path: serialize session creation so only one coroutine signs in.
        sessionMutex.withLock {
            // Double-checked: another coroutine may have signed in while we waited.
            client.auth.currentUserOrNull()?.let { return it.id.lowercase() }
            client.auth.signInAnonymously()
            val user = client.auth.currentUserOrNull()
                ?: throw IllegalStateException("Anonymous sign-in failed")
            return user.id.lowercase()
        }
    }
}
