package com.lakeshorestudios.nextwave.data.repository

import android.content.Context
import android.util.Log
import com.lakeshorestudios.nextwave.data.api.AlplakesApiClient
import com.lakeshorestudios.nextwave.data.api.SunTimesApiClient
import com.lakeshorestudios.nextwave.data.api.VesselDataApiClient
import com.lakeshorestudios.nextwave.data.models.AverageWaterLevels
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.SunTimes
import com.lakeshorestudios.nextwave.data.models.WaterLevel
import com.lakeshorestudios.nextwave.data.models.WaterTemperature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date

/**
 * Repository that manages water temperature, water levels, and sun times data.
 * Coordinates data from Alplakes API, VesselData API, and Sunrise-Sunset API.
 */
class LakeDataRepository(context: Context) {

    private val alplakesApiClient = AlplakesApiClient.getInstance(context)
    private val vesselDataApiClient = VesselDataApiClient.getInstance()
    private val sunTimesApiClient = SunTimesApiClient.getInstance()

    /**
     * Get all environmental data for a lake on a specific date.
     * Fetches water temperature, water level, and sun times in parallel.
     */
    fun getLakeEnvironmentData(lakeName: String, date: Date): Flow<LakeEnvironmentData> = flow {
        try {
            val data = coroutineScope {
                val tempDeferred = async { alplakesApiClient.getWaterTemperature(lakeName) }
                val levelDeferred = async { vesselDataApiClient.getWaterLevel(lakeName) }
                val sunTimesDeferred = async { sunTimesApiClient.getSunTimes(date) }

                val temp = tempDeferred.await()
                val level = levelDeferred.await()
                val sunTimes = sunTimesDeferred.await()

                // Calculate water level difference from annual average
                val waterLevelDiff = calculateWaterLevelDifference(lakeName, level?.level)

                LakeEnvironmentData(
                    waterTemperature = temp?.temperature,
                    waterLevel = level?.level,
                    waterLevelDifference = waterLevelDiff,
                    sunTimes = sunTimes
                )
            }
            emit(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lake environment data for $lakeName: ${e.message}")
            emit(LakeEnvironmentData())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get water temperature only for a lake.
     */
    suspend fun getWaterTemperature(lakeName: String): WaterTemperature? {
        return alplakesApiClient.getWaterTemperature(lakeName)
    }

    /**
     * Get water level only for a lake.
     */
    suspend fun getWaterLevel(lakeName: String): WaterLevel? {
        return vesselDataApiClient.getWaterLevel(lakeName)
    }

    /**
     * Get sun times for a specific date.
     */
    suspend fun getSunTimes(date: Date): SunTimes? {
        return sunTimesApiClient.getSunTimes(date)
    }

    /**
     * Calculate the difference between current water level and annual average.
     * Returns e.g. "+5 cm" or "-3 cm", or null if not calculable.
     */
    private fun calculateWaterLevelDifference(lakeName: String, levelString: String?): String? {
        if (levelString.isNullOrBlank()) return null
        val average = AverageWaterLevels.getAverage(lakeName) ?: return null

        // Parse the level value, e.g. "405.81 m.ü.M." -> 405.81
        val currentLevel = levelString.replace(Regex("[^0-9.]"), " ").trim().split(" ")
            .firstOrNull()?.toDoubleOrNull() ?: return null

        val diffMeters = currentLevel - average
        val diffCm = (diffMeters * 100).toInt()

        return if (diffCm >= 0) "+$diffCm cm" else "$diffCm cm"
    }

    companion object {
        private const val TAG = "LakeDataRepository"

        @Volatile
        private var instance: LakeDataRepository? = null

        fun getInstance(context: Context): LakeDataRepository {
            return instance ?: synchronized(this) {
                instance ?: LakeDataRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
