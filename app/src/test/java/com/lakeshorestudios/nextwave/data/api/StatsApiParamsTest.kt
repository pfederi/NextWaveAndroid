package com.lakeshorestudios.nextwave.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsApiParamsTest {

    @Test
    fun leaderboardParamsSerializeWithRpcKeys() {
        val json = Json { encodeDefaults = true }
        val params = StatsApi.LeaderboardParams(stationId = "Thalwil_8503671", limit = 50)
        val encoded = json.encodeToString(StatsApi.LeaderboardParams.serializer(), params)
        assertEquals("""{"p_station_id":"Thalwil_8503671","p_limit":50}""", encoded)
    }

    @Test
    fun leaderboardParamsAllowNullStation() {
        val json = Json { encodeDefaults = true }
        val params = StatsApi.LeaderboardParams(stationId = null, limit = 25)
        val encoded = json.encodeToString(StatsApi.LeaderboardParams.serializer(), params)
        assertEquals("""{"p_station_id":null,"p_limit":25}""", encoded)
    }
}
