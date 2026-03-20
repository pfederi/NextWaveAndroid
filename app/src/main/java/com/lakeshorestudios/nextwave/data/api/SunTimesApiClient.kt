package com.lakeshorestudios.nextwave.data.api

import android.util.Log
import com.google.gson.Gson
import com.lakeshorestudios.nextwave.data.models.SunTimes
import com.lakeshorestudios.nextwave.data.models.SunTimesResponse
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
 * API client for sunrise-sunset.org
 * Uses fixed Swiss center coordinates like the iOS app
 */
class SunTimesApiClient {

    // Cache by date string (yyyy-MM-dd)
    private val cache = ConcurrentHashMap<String, SunTimes>()

    /**
     * Get sun times for a specific date.
     * Uses fixed Swiss center coordinates (47.0136, 8.4324).
     */
    suspend fun getSunTimes(date: Date): SunTimes? = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateString = dateFormat.format(date)

            // Check cache
            cache[dateString]?.let { return@withContext it }

            val latitude = 47.0136
            val longitude = 8.4324
            val url = "https://api.sunrise-sunset.org/json?lat=$latitude&lng=$longitude&date=$dateString&formatted=0"

            Log.d(TAG, "Fetching sun times for $dateString")
            val responseText = URL(url).readTextWithTimeout()
            val response = Gson().fromJson(responseText, SunTimesResponse::class.java)

            if (response.status != "OK") {
                Log.e(TAG, "API returned status: ${response.status}")
                return@withContext null
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val swissTimeZone = TimeZone.getTimeZone("Europe/Zurich")

            val sunrise = isoFormat.parse(response.results.sunrise)
            val sunset = isoFormat.parse(response.results.sunset)
            val twilightBegin = isoFormat.parse(response.results.civilTwilightBegin)
            val twilightEnd = isoFormat.parse(response.results.civilTwilightEnd)

            if (sunrise != null && sunset != null && twilightBegin != null && twilightEnd != null) {
                val sunTimes = SunTimes(
                    sunrise = sunrise,
                    sunset = sunset,
                    civilTwilightBegin = twilightBegin,
                    civilTwilightEnd = twilightEnd
                )
                cache[dateString] = sunTimes
                Log.d(TAG, "Sun times for $dateString: sunrise=${formatTime(sunrise, swissTimeZone)}, sunset=${formatTime(sunset, swissTimeZone)}")
                sunTimes
            } else {
                Log.e(TAG, "Failed to parse sun times")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sun times: ${e.message}")
            null
        }
    }

    private fun formatTime(date: Date, timeZone: TimeZone): String {
        val format = SimpleDateFormat("HH:mm", Locale.US)
        format.timeZone = timeZone
        return format.format(date)
    }

    companion object {
        private const val TAG = "SunTimesApiClient"

        @Volatile
        private var instance: SunTimesApiClient? = null

        fun getInstance(): SunTimesApiClient {
            return instance ?: synchronized(this) {
                instance ?: SunTimesApiClient().also { instance = it }
            }
        }
    }
}
