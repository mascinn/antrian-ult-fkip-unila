package com.example.data.remote

import com.example.BuildConfig

/**
 * Reads Supabase public configuration injected via Secrets Gradle Plugin (BuildConfig).
 * Never embeds or accesses SUPABASE_SECRET_KEY on the Android client.
 */
object SupabaseConfig {
    private const val PLACEHOLDER_URL = "YOUR_SUPABASE_URL"
    private const val PLACEHOLDER_KEY = "YOUR_SUPABASE_PUBLISHABLE_KEY"

    val url: String
        get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    val publishableKey: String
        get() = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isConfigured: Boolean
        get() = url.startsWith("https://", ignoreCase = true) &&
            url != PLACEHOLDER_URL &&
            publishableKey.isNotBlank() &&
            publishableKey != PLACEHOLDER_KEY

    fun retrofitBaseUrl(): String {
        return if (isConfigured) "$url/" else "https://placeholder.supabase.co/"
    }

    fun realtimeWebSocketUrl(): String? {
        if (!isConfigured) return null
        val wsBase = url
            .replaceFirst("https://", "wss://", ignoreCase = true)
            .replaceFirst("http://", "ws://", ignoreCase = true)
        return "$wsBase/realtime/v1/websocket?apikey=$publishableKey&vsn=1.0.0"
    }
}
