package com.example.nextwave.data.api

import com.example.nextwave.data.models.Station
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit service interface for Next Wave API
 */
interface NextWaveApiService {
    
    /**
     * Get all stations
     */
    @GET("stations")
    suspend fun getAllStations(): List<Station>
    
    /**
     * Get station details by ID
     */
    @GET("stations/{id}")
    suspend fun getStationById(@Path("id") stationId: String): Station
} 