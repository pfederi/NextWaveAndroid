package com.lakeshorestudios.nextwave.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.lakeshorestudios.nextwave.data.utils.readTextWithTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * API client for ZSG ship deployment data.
 * Endpoint: https://vesseldata-api.vercel.app/api/ships
 * Returns ship names mapped to course numbers for upcoming days.
 */
class ShipApiClient {

    // Cache: "yyyy-MM-dd_courseNumber" -> shipName
    private val shipNameCache = ConcurrentHashMap<String, String>()
    @Volatile private var lastFetchDate: String? = null

    /**
     * Find the ship name for a given course number and date.
     * Returns null if not found or not a Zürichsee route.
     */
    suspend fun findShipName(courseNumber: String, date: Date): String? = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateString = dateFormat.format(date)
        val cacheKey = "${dateString}_${courseNumber.trimStart('0')}"

        // Check cache first
        shipNameCache[cacheKey]?.let { return@withContext it }

        // Fetch if we haven't fetched today
        if (lastFetchDate != dateString) {
            fetchShipData()
        }

        shipNameCache[cacheKey]
    }

    /**
     * Fetch all ship deployment data from the API.
     */
    private suspend fun fetchShipData() {
        try {
            val url = "https://vesseldata-api.vercel.app/api/ships"
            Log.d(TAG, "Fetching ship deployments from: $url")

            val responseText = URL(url).readTextWithTimeout()
            val response = Gson().fromJson(responseText, ShipResponse::class.java)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            response.dailyDeployments?.forEach { day ->
                day.routes?.forEach { route ->
                    val courseNum = route.courseNumber.trimStart('0')
                    val key = "${day.date}_$courseNum"
                    shipNameCache[key] = route.shipName
                }
            }

            lastFetchDate = dateFormat.format(Date())
            Log.d(TAG, "Cached ${shipNameCache.size} ship assignments")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ship data: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ShipApiClient"

        @Volatile
        private var instance: ShipApiClient? = null

        fun getInstance(): ShipApiClient {
            return instance ?: synchronized(this) {
                instance ?: ShipApiClient().also { instance = it }
            }
        }
    }
}

// API response models
data class ShipResponse(
    val dailyDeployments: List<ShipDailyDeployment>? = null
)

data class ShipDailyDeployment(
    val date: String,
    val routes: List<ShipRoute>? = null
)

data class ShipRoute(
    val shipName: String,
    val courseNumber: String
)
