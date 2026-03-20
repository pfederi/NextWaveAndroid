package com.lakeshorestudios.nextwave.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lakeshorestudios.nextwave.data.models.WaterLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * API client for the VesselData Vercel backend.
 * Endpoint: https://vesseldata-api.vercel.app/api/water-temperature
 * Returns water levels (and optionally temperature) for Swiss lakes.
 */
class VesselDataApiClient {

    // Cache: lakeName -> WaterLevel, valid for 24 hours
    private val cache = ConcurrentHashMap<String, WaterLevel>()
    private val cacheValidityMs = 24 * 60 * 60 * 1000L // 24 hours

    // All lakes data from last fetch
    private var allLakesCache: List<WaterLevel>? = null
    private var allLakesCacheTime: Long = 0

    /**
     * Get water level for a specific lake.
     */
    suspend fun getWaterLevel(lakeName: String): WaterLevel? = withContext(Dispatchers.IO) {
        try {
            // Check single-lake cache
            val cached = cache[lakeName]
            if (cached != null && System.currentTimeMillis() - cached.lastUpdated < cacheValidityMs) {
                return@withContext cached
            }

            // Fetch all lakes if needed
            val allLakes = fetchAllLakes()
            allLakes?.find { it.lakeName.equals(lakeName, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting water level for $lakeName: ${e.message}")
            cache[lakeName]
        }
    }

    /**
     * Fetch all lake water levels from the API.
     */
    private suspend fun fetchAllLakes(): List<WaterLevel>? {
        // Check if we have a recent fetch
        if (allLakesCache != null && System.currentTimeMillis() - allLakesCacheTime < cacheValidityMs) {
            return allLakesCache
        }

        try {
            val url = "https://vesseldata-api.vercel.app/api/water-temperature"
            Log.d(TAG, "Fetching water levels from: $url")

            val responseText = URL(url).readText()
            val response = Gson().fromJson(responseText, VesselDataResponse::class.java)

            val waterLevels = response.lakes?.map { lake ->
                val waterLevel = WaterLevel(
                    lakeName = lake.name,
                    level = lake.level ?: "",
                    temperature = lake.temperature
                )
                cache[lake.name] = waterLevel
                waterLevel
            } ?: emptyList()

            allLakesCache = waterLevels
            allLakesCacheTime = System.currentTimeMillis()

            Log.d(TAG, "Fetched water levels for ${waterLevels.size} lakes")
            return waterLevels
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching water levels: ${e.message}")
            return allLakesCache
        }
    }

    companion object {
        private const val TAG = "VesselDataApiClient"

        @Volatile
        private var instance: VesselDataApiClient? = null

        fun getInstance(): VesselDataApiClient {
            return instance ?: synchronized(this) {
                instance ?: VesselDataApiClient().also { instance = it }
            }
        }
    }
}

/**
 * Response model for the VesselData API
 */
data class VesselDataResponse(
    val lakes: List<VesselDataLake>? = null
)

data class VesselDataLake(
    val name: String,
    @SerializedName("waterLevel")
    val level: String? = null,
    val temperature: Double? = null
)
