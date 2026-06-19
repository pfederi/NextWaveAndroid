package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Caller's raw gamification metrics — one row from the `user_wave_stats` RPC. */
@Serializable
data class WaveStats(
    @SerialName("total_waves") val totalWaves: Int,
    @SerialName("distinct_stations") val distinctStations: Int,
    @SerialName("distinct_lakes") val distinctLakes: Int,
    @SerialName("max_waves_one_station") val maxWavesOneStation: Int,
    @SerialName("first_of_day_count") val firstOfDayCount: Int,
    @SerialName("last_of_day_count") val lastOfDayCount: Int,
    @SerialName("early_bird_count") val earlyBirdCount: Int,
    @SerialName("lunch_count") val lunchCount: Int,
    @SerialName("night_owl_count") val nightOwlCount: Int,
    @SerialName("weekend_count") val weekendCount: Int,
    @SerialName("max_waves_one_day") val maxWavesOneDay: Int,
    @SerialName("has_anniversary") val hasAnniversary: Boolean,
    @SerialName("solo_count") val soloCount: Int,
    @SerialName("max_peer_count") val maxPeerCount: Int,
    @SerialName("trendsetter_count") val trendsetterCount: Int,
    @SerialName("seasons_ridden") val seasonsRidden: List<String>,
    @SerialName("current_streak_weeks") val currentStreakWeeks: Int,
    @SerialName("longest_streak_weeks") val longestStreakWeeks: Int
) {
    companion object {
        val empty = WaveStats(
            totalWaves = 0, distinctStations = 0, distinctLakes = 0, maxWavesOneStation = 0,
            firstOfDayCount = 0, lastOfDayCount = 0, earlyBirdCount = 0, lunchCount = 0,
            nightOwlCount = 0, weekendCount = 0, maxWavesOneDay = 0, hasAnniversary = false,
            soloCount = 0, maxPeerCount = 0, trendsetterCount = 0, seasonsRidden = emptyList(),
            currentStreakWeeks = 0, longestStreakWeeks = 0
        )
    }
}

/** Caller's wave count at one station — a row from the `user_station_counts` RPC. */
@Serializable
data class StationWaveCount(
    @SerialName("station_id") val stationId: String,
    @SerialName("waves") val waves: Int
)

/** A leaderboard row from the `wave_leaderboard` RPC. */
@Serializable
data class LeaderboardEntry(
    @SerialName("rank") val rank: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("total_waves") val totalWaves: Int,
    @SerialName("is_me") val isMe: Boolean
)
