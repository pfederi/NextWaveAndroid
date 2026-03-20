package com.lakeshorestudios.nextwave.data.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.lakeshorestudios.nextwave.data.models.WaterTemperature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lakeshorestudios.nextwave.data.utils.readTextWithTimeout
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * API client for Eawag Alplakes water temperature data.
 * Endpoint: https://alplakes-api.eawag.ch/simulations/1d/profile/simstrat/{lake}/{timestamp}
 */
class AlplakesApiClient(private val context: Context) {

    private val lakeMapping = mutableMapOf<String, String>()

    // Cache: lakeName -> WaterTemperature, valid for 3 hours
    private val cache = ConcurrentHashMap<String, WaterTemperature>()
    private val cacheValidityMs = 3 * 60 * 60 * 1000L // 3 hours

    init {
        loadLakeMapping()
    }

    private fun loadLakeMapping() {
        try {
            val json = context.assets.open("alplakes-lake-mapping.json").bufferedReader().readText()
            val type = object : TypeToken<Map<String, String>>() {}.type
            val mapping: Map<String, String> = Gson().fromJson(json, type)
            lakeMapping.putAll(mapping)
            Log.d(TAG, "Loaded ${lakeMapping.size} lake mappings")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lake mapping: ${e.message}")
        }
    }

    /**
     * Get the current surface water temperature for a lake.
     * Returns null if the lake is not mapped or the API call fails.
     */
    suspend fun getWaterTemperature(lakeName: String): WaterTemperature? = withContext(Dispatchers.IO) {
        try {
            // Check cache
            val cached = cache[lakeName]
            if (cached != null && System.currentTimeMillis() - cached.lastUpdated < cacheValidityMs) {
                Log.d(TAG, "Using cached water temperature for $lakeName: ${cached.temperature}°C")
                return@withContext cached
            }

            val alplakesName = lakeMapping[lakeName]
            if (alplakesName == null) {
                Log.d(TAG, "No Alplakes mapping found for lake: $lakeName")
                return@withContext null
            }

            // Build timestamp as 12 digits: yyyyMMddHHmm
            val dateFormat = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val timestamp = dateFormat.format(Date())

            val url = "https://alplakes-api.eawag.ch/simulations/1d/profile/simstrat/$alplakesName/$timestamp"
            Log.d(TAG, "Fetching water temperature for $lakeName from: $url")

            val responseText = URL(url).readTextWithTimeout()

            // Parse the response: surface temp is the last element in variables.T.data
            // (depth array goes from deep to shallow, last = surface at 0m)
            @Suppress("DEPRECATION")
            val json = JsonParser().parse(responseText).asJsonObject
            val tempArray = json.getAsJsonObject("variables")
                ?.getAsJsonObject("T")
                ?.getAsJsonArray("data")
            val surfaceTemp = tempArray?.lastOrNull()?.asDouble
            if (surfaceTemp != null) {
                val waterTemp = WaterTemperature(
                    lakeName = lakeName,
                    temperature = Math.round(surfaceTemp * 10.0) / 10.0
                )
                cache[lakeName] = waterTemp
                Log.d(TAG, "Water temperature for $lakeName: ${waterTemp.temperature}°C")
                waterTemp
            } else {
                Log.e(TAG, "No temperature data in response for $lakeName")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching water temperature for $lakeName: ${e.message}")
            // Return cached data if available, even if expired
            cache[lakeName]
        }
    }

    companion object {
        private const val TAG = "AlplakesApiClient"

        @Volatile
        private var instance: AlplakesApiClient? = null

        fun getInstance(context: Context): AlplakesApiClient {
            return instance ?: synchronized(this) {
                instance ?: AlplakesApiClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
