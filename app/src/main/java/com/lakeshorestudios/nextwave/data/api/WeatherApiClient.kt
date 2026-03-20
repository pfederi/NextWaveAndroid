package com.lakeshorestudios.nextwave.data.api

import com.lakeshorestudios.nextwave.data.models.ForecastResponse
import com.lakeshorestudios.nextwave.data.models.WeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Interface for OpenWeather API endpoints
 */
interface WeatherApiService {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = API_KEY
    ): WeatherResponse
    
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String = API_KEY
    ): ForecastResponse
    
    companion object {
        private val API_KEY = com.lakeshorestudios.nextwave.BuildConfig.WEATHER_API_KEY
    }
}

/**
 * Client for the OpenWeather API
 */
class WeatherApiClient {
    private val apiService: WeatherApiService
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.lakeshorestudios.nextwave.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(WeatherApiService::class.java)
    }
    
    /**
     * Get current weather for a location
     */
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResponse {
        return apiService.getCurrentWeather(latitude, longitude)
    }
    
    /**
     * Get weather forecast for a location
     */
    suspend fun getForecast(latitude: Double, longitude: Double): ForecastResponse {
        return apiService.getForecast(latitude, longitude)
    }
    
    companion object {
        @Volatile
        private var instance: WeatherApiClient? = null
        
        fun getInstance(): WeatherApiClient {
            return instance ?: synchronized(this) {
                instance ?: WeatherApiClient().also { instance = it }
            }
        }
    }
} 