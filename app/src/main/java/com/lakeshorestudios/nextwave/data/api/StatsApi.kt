package com.lakeshorestudios.nextwave.data.api

import com.lakeshorestudios.nextwave.data.models.LeaderboardEntry
import com.lakeshorestudios.nextwave.data.models.StationWaveCount
import com.lakeshorestudios.nextwave.data.models.WaveStats
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Read-only gamification RPCs. Mirrors iOS API/StatsAPI.swift. */
object StatsApi {

    @Serializable
    data class LeaderboardParams(
        @SerialName("p_station_id") val stationId: String?,
        @SerialName("p_limit") val limit: Int
    )

    /** Caller's raw metrics — `user_wave_stats()` returns a single row. */
    suspend fun stats(): WaveStats {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("user_wave_stats")
            .decodeList<WaveStats>()
            .firstOrNull() ?: WaveStats.empty
    }

    /** Caller's wave count per station, most-ridden first. */
    suspend fun stationCounts(): List<StationWaveCount> {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("user_station_counts")
            .decodeList<StationWaveCount>()
    }

    /** Global (stationId == null) or per-station leaderboard: top `limit` named users + own row. */
    suspend fun leaderboard(stationId: String?, limit: Int = 50): List<LeaderboardEntry> {
        SupabaseManager.ensureSession()
        return SupabaseManager.client.postgrest
            .rpc("wave_leaderboard", LeaderboardParams(stationId, limit))
            .decodeList<LeaderboardEntry>()
    }
}
