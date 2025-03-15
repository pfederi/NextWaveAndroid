package com.example.netxwave.data.repository

import android.content.Context
import com.example.netxwave.data.api.ApiClient
import com.example.netxwave.data.models.Station
import com.example.netxwave.data.utils.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository class that acts as a single source of truth for data
 */
class NextWaveRepository(private val context: Context) {
    
    private val apiService = ApiClient.apiService
    private val useMockData = false // Set to false as we don't use mock data anymore
    
    /**
     * Get all stations as a Flow
     */
    fun getAllStations(): Flow<List<Station>> = flow {
        delay(400) // Add a small delay to simulate network request
        val stations = AssetManager.loadStationsFromAssets(context)
        emit(stations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get station details by ID as a Flow
     */
    fun getStationById(stationId: String): Flow<Station?> = flow {
        delay(300)
        val stations = AssetManager.loadStationsFromAssets(context)
        emit(stations.find { it.id == stationId })
    }.flowOn(Dispatchers.IO)
    
    companion object {
        // Singleton instance
        @Volatile
        private var instance: NextWaveRepository? = null
        
        fun getInstance(context: Context): NextWaveRepository {
            return instance ?: synchronized(this) {
                instance ?: NextWaveRepository(context.applicationContext).also { instance = it }
            }
        }
    }
} 