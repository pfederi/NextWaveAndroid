package com.lakeshorestudios.nextwave.data.models

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveStatsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesWaveStatsFromRpcJsonKeys() {
        val raw = """
            {
              "total_waves": 12, "distinct_stations": 4, "distinct_lakes": 2,
              "max_waves_one_station": 6, "first_of_day_count": 1, "last_of_day_count": 0,
              "early_bird_count": 3, "lunch_count": 0, "night_owl_count": 1,
              "weekend_count": 5, "max_waves_one_day": 2, "has_anniversary": true,
              "solo_count": 1, "max_peer_count": 5, "trendsetter_count": 0,
              "seasons_ridden": ["spring","summer"], "current_streak_weeks": 2,
              "longest_streak_weeks": 3
            }
        """.trimIndent()
        val s = json.decodeFromString(WaveStats.serializer(), raw)
        assertEquals(12, s.totalWaves)
        assertEquals(2, s.distinctLakes)
        assertEquals(true, s.hasAnniversary)
        assertEquals(listOf("spring", "summer"), s.seasonsRidden)
        assertEquals(3, s.longestStreakWeeks)
    }

    @Test
    fun decodesLeaderboardEntry() {
        val raw = """{"rank":14,"display_name":"Patrick","total_waves":12,"is_me":true}"""
        val e = json.decodeFromString(LeaderboardEntry.serializer(), raw)
        assertEquals(14, e.rank)
        assertEquals("Patrick", e.displayName)
        assertEquals(true, e.isMe)
    }

    @Test
    fun emptyStatsAreAllZero() {
        assertEquals(0, WaveStats.empty.totalWaves)
        assertEquals(false, WaveStats.empty.hasAnniversary)
        assertEquals(emptyList<String>(), WaveStats.empty.seasonsRidden)
    }
}
