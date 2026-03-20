package com.lakeshorestudios.nextwave.data.repository

import android.content.Context
import com.lakeshorestudios.nextwave.data.api.ApiClient
import com.lakeshorestudios.nextwave.data.models.Station
import com.lakeshorestudios.nextwave.data.utils.AssetManager
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
        val stationsFromAssets = AssetManager.loadStationsFromAssets(context)
        
        // Create a virtual station for Geneva that combines all Geneva stations
        val genevaStations = stationsFromAssets.filter { 
            it.name.startsWith("Genève", ignoreCase = true) 
        }
        
        val allStations = if (genevaStations.isNotEmpty()) {
            // Only add the virtual station if there are Geneva stations
            val genevaIds = genevaStations.map { it.id }
            val firstGenevaStation = genevaStations.first()
            
            val virtualGeneva = Station(
                id = "virtual_geneva",
                name = "Genève (tous)",
                latitude = firstGenevaStation.latitude,
                longitude = firstGenevaStation.longitude,
                city = "Genève",
                type = "Virtual",
                lake = firstGenevaStation.lake,
                waveRating = firstGenevaStation.waveRating,
                description = "Combined departures from all Geneva stations",
                isVirtual = true,
                childStationIds = genevaIds
            )
            
            stationsFromAssets + virtualGeneva
        } else {
            stationsFromAssets
        }
        
        emit(allStations)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get station details by ID as a Flow
     */
    fun getStationById(stationId: String): Flow<Station?> = flow {
        val stations = AssetManager.loadStationsFromAssets(context)
        
        // Check if this is the virtual Geneva station
        if (stationId == "virtual_geneva") {
            val genevaStations = stations.filter { 
                it.name.startsWith("Genève", ignoreCase = true) 
            }
            
            if (genevaStations.isNotEmpty()) {
                val genevaIds = genevaStations.map { it.id }
                val firstGenevaStation = genevaStations.first()
                
                val virtualGeneva = Station(
                    id = "virtual_geneva",
                    name = "Genève (tous)",
                    latitude = firstGenevaStation.latitude,
                    longitude = firstGenevaStation.longitude,
                    city = "Genève",
                    type = "Virtual",
                    lake = firstGenevaStation.lake,
                    waveRating = firstGenevaStation.waveRating,
                    description = "Combined departures from all Geneva stations",
                    isVirtual = true,
                    childStationIds = genevaIds
                )
                
                emit(virtualGeneva)
                return@flow
            }
        }
        
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