package com.example.netxwave.data.repository

import android.content.Context
import android.util.Log
import com.example.netxwave.data.api.WeatherApiClient
import com.example.netxwave.data.models.ForecastItem
import com.example.netxwave.data.models.PressureTrend
import com.example.netxwave.data.models.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for weather data
 */
class WeatherRepository(private val context: Context) {
    
    private val weatherApiClient = WeatherApiClient.getInstance()
    
    // Cache for weather data to avoid too many API calls
    private val weatherCache = ConcurrentHashMap<String, WeatherInfo>()
    
    // Cache for forecast data
    private val forecastCache = ConcurrentHashMap<String, WeatherInfo>()
    
    // Store pressure history for each location
    private val pressureHistory = ConcurrentHashMap<String, MutableList<Pair<Long, Int>>>()
    
    /**
     * Get weather for a location
     */
    fun getWeatherForLocation(latitude: Double, longitude: Double): Flow<WeatherInfo> = flow {
        try {
            // Check if we have cached data that is less than 5 minutes old
            val cacheKey = "${latitude}_${longitude}"
            val cachedWeather = weatherCache[cacheKey]
            
            if (cachedWeather != null && isCacheValid(cachedWeather.lastUpdated)) {
                Log.d("WeatherRepository", "Using cached weather data for $cacheKey")
                emit(cachedWeather)
                return@flow
            }
            
            // Fetch new data from API
            Log.d("WeatherRepository", "Fetching weather data for $cacheKey")
            val weatherResponse = weatherApiClient.getCurrentWeather(latitude, longitude)
            var weatherInfo = WeatherInfo.fromWeatherResponse(weatherResponse)
            
            // Calculate pressure trend
            val pressureTrend = calculatePressureTrend(cacheKey, weatherInfo.pressure)
            weatherInfo = weatherInfo.copy(pressureTrend = pressureTrend)
            
            // Cache the result
            weatherCache[cacheKey] = weatherInfo
            
            emit(weatherInfo)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather: ${e.message}")
            // If we have cached data, emit it even if it's old
            val cacheKey = "${latitude}_${longitude}"
            val cachedWeather = weatherCache[cacheKey]
            if (cachedWeather != null) {
                Log.d("WeatherRepository", "Using old cached weather data due to error")
                emit(cachedWeather)
            } else {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get weather forecast for a location
     */
    fun getForecastForLocation(latitude: Double, longitude: Double): Flow<WeatherInfo> = flow {
        try {
            // Check if we have cached data that is less than 1 hour old
            val cacheKey = "${latitude}_${longitude}_forecast"
            val cachedForecast = forecastCache[cacheKey]
            
            if (cachedForecast != null && isForecastCacheValid(cachedForecast.lastUpdated)) {
                Log.d("WeatherRepository", "Using cached forecast data for $cacheKey")
                emit(cachedForecast)
                return@flow
            }
            
            // Fetch new data from API
            Log.d("WeatherRepository", "Fetching forecast data for $cacheKey")
            val forecastResponse = weatherApiClient.getForecast(latitude, longitude)
            
            // Find forecast for tomorrow
            val tomorrowForecast = findTomorrowForecast(forecastResponse.list)
            if (tomorrowForecast != null) {
                val weather = tomorrowForecast.weather.firstOrNull()
                    ?: forecastResponse.list.firstOrNull()?.weather?.firstOrNull()
                
                if (weather != null) {
                    // Find morning and afternoon temperatures
                    val (morningTemp, afternoonTemp) = findDayTemperatures(forecastResponse.list)
                    
                    // Find max wind speed for tomorrow
                    val maxWindSpeed = findMaxWindSpeed(forecastResponse.list)
                    
                    val weatherInfo = WeatherInfo(
                        temperature = tomorrowForecast.main.temp,
                        tempMin = tomorrowForecast.main.tempMin,
                        tempMax = tomorrowForecast.main.tempMax,
                        morningTemp = morningTemp,
                        afternoonTemp = afternoonTemp,
                        windSpeed = tomorrowForecast.wind.speed,
                        maxWindSpeed = maxWindSpeed,
                        windDeg = tomorrowForecast.wind.deg,
                        windGust = tomorrowForecast.wind.gust,
                        pressure = tomorrowForecast.main.pressure,
                        description = weather.description,
                        iconUrl = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                        forecastDate = Date(tomorrowForecast.dt * 1000),
                        humidity = tomorrowForecast.main.humidity
                    )
                    
                    // Cache the result
                    forecastCache[cacheKey] = weatherInfo
                    
                    emit(weatherInfo)
                } else {
                    throw Exception("No weather data found in forecast")
                }
            } else {
                throw Exception("No forecast found for tomorrow")
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast: ${e.message}")
            // If we have cached data, emit it even if it's old
            val cacheKey = "${latitude}_${longitude}_forecast"
            val cachedForecast = forecastCache[cacheKey]
            if (cachedForecast != null) {
                Log.d("WeatherRepository", "Using old cached forecast data due to error")
                emit(cachedForecast)
            } else {
                throw e
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get weather and forecast for a location
     */
    fun getWeatherAndForecast(latitude: Double, longitude: Double): Flow<Pair<WeatherInfo, WeatherInfo?>> = flow {
        try {
            getWeatherForLocation(latitude, longitude).collect { weatherInfo ->
                try {
                    getForecastForLocation(latitude, longitude).collect { forecastInfo ->
                        emit(Pair(weatherInfo, forecastInfo))
                    }
                } catch (e: Exception) {
                    Log.e("WeatherRepository", "Error fetching forecast: ${e.message}")
                    emit(Pair(weatherInfo, null))
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather: ${e.message}")
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Find forecast for tomorrow
     */
    private fun findTomorrowForecast(forecastList: List<ForecastItem>): ForecastItem? {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 12) // Noon tomorrow
        val tomorrowNoon = calendar.timeInMillis / 1000
        
        // Find the forecast closest to noon tomorrow
        return forecastList.minByOrNull { Math.abs(it.dt - tomorrowNoon) }
    }
    
    /**
     * Find morning and afternoon temperatures for tomorrow
     */
    private fun findDayTemperatures(forecastList: List<ForecastItem>): Pair<Double?, Double?> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
        // Morning: 9 AM
        val morningCalendar = Calendar.getInstance()
        morningCalendar.timeInMillis = calendar.timeInMillis
        morningCalendar.set(Calendar.HOUR_OF_DAY, 9)
        morningCalendar.set(Calendar.MINUTE, 0)
        val morningTime = morningCalendar.timeInMillis / 1000
        
        // Afternoon: 3 PM
        val afternoonCalendar = Calendar.getInstance()
        afternoonCalendar.timeInMillis = calendar.timeInMillis
        afternoonCalendar.set(Calendar.HOUR_OF_DAY, 15)
        afternoonCalendar.set(Calendar.MINUTE, 0)
        val afternoonTime = afternoonCalendar.timeInMillis / 1000
        
        // Find forecasts closest to 9 AM and 3 PM
        val morningForecast = forecastList.minByOrNull { Math.abs(it.dt - morningTime) }
        val afternoonForecast = forecastList.minByOrNull { Math.abs(it.dt - afternoonTime) }
        
        return Pair(morningForecast?.main?.temp, afternoonForecast?.main?.temp)
    }
    
    /**
     * Find maximum wind speed for tomorrow
     */
    private fun findMaxWindSpeed(forecastList: List<ForecastItem>): Double {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis / 1000
        
        val tomorrowEnd = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis / 1000
        
        // Find all forecasts for tomorrow
        val tomorrowForecasts = forecastList.filter { it.dt in tomorrowStart..tomorrowEnd }
        
        // Find maximum wind speed
        return tomorrowForecasts.maxOfOrNull { it.wind.speed } ?: 0.0
    }
    
    /**
     * Calculate pressure trend based on historical data
     */
    private fun calculatePressureTrend(locationKey: String, currentPressure: Int): PressureTrend {
        // Get or create pressure history for this location
        val history = pressureHistory.getOrPut(locationKey) { mutableListOf() }
        
        // Add current pressure to history
        val currentTime = System.currentTimeMillis()
        history.add(Pair(currentTime, currentPressure))
        
        // Remove entries older than 6 hours
        val sixHoursAgo = currentTime - (6 * 60 * 60 * 1000)
        history.removeAll { it.first < sixHoursAgo }
        
        // If we have less than 2 entries, we can't determine a trend
        if (history.size < 2) {
            return PressureTrend.STABLE
        }
        
        // Sort by timestamp
        val sortedHistory = history.sortedBy { it.first }
        
        // Compare oldest and newest pressure
        val oldestPressure = sortedHistory.first().second
        val newestPressure = sortedHistory.last().second
        
        // Calculate difference
        val difference = newestPressure - oldestPressure
        
        // Define threshold for significant changes (2 hPa)
        val threshold = 2
        
        return when {
            difference > threshold -> PressureTrend.RISING
            difference < -threshold -> PressureTrend.FALLING
            else -> PressureTrend.STABLE
        }
    }
    
    /**
     * Check if cached data is still valid (less than 5 minutes old)
     */
    private fun isCacheValid(timestamp: Long): Boolean {
        val fiveMinutesInMillis = 5 * 60 * 1000
        return System.currentTimeMillis() - timestamp < fiveMinutesInMillis
    }
    
    /**
     * Check if forecast cached data is still valid (less than 1 hour old)
     */
    private fun isForecastCacheValid(timestamp: Long): Boolean {
        val oneHourInMillis = 60 * 60 * 1000
        return System.currentTimeMillis() - timestamp < oneHourInMillis
    }
    
    /**
     * Force refresh weather data for a location
     */
    fun forceRefreshWeather(latitude: Double, longitude: Double): Flow<WeatherInfo> = flow {
        try {
            // Clear cache for this location
            val cacheKey = "${latitude}_${longitude}"
            weatherCache.remove(cacheKey)
            
            Log.d("WeatherRepository", "Force refreshing weather data for $cacheKey")
            
            // Fetch new data from API
            val weatherResponse = weatherApiClient.getCurrentWeather(latitude, longitude)
            var weatherInfo = WeatherInfo.fromWeatherResponse(weatherResponse)
            
            // Calculate pressure trend
            val pressureTrend = calculatePressureTrend(cacheKey, weatherInfo.pressure)
            weatherInfo = weatherInfo.copy(pressureTrend = pressureTrend)
            
            // Cache the result
            weatherCache[cacheKey] = weatherInfo
            
            emit(weatherInfo)
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather during force refresh: ${e.message}")
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Force refresh forecast data for a location
     */
    fun forceRefreshForecast(latitude: Double, longitude: Double): Flow<WeatherInfo> = flow {
        try {
            // Clear cache for this location
            val cacheKey = "${latitude}_${longitude}_forecast"
            forecastCache.remove(cacheKey)
            
            Log.d("WeatherRepository", "Force refreshing forecast data for $cacheKey")
            
            // Get fresh forecast
            getForecastForLocation(latitude, longitude).collect { forecast ->
                emit(forecast)
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast during force refresh: ${e.message}")
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Clear all cached weather data
     */
    fun clearCache() {
        Log.d("WeatherRepository", "Clearing all cached weather data")
        weatherCache.clear()
        forecastCache.clear()
    }
    
    /**
     * Get forecast for a specific time (for wave departures)
     */
    fun getForecastForSpecificTime(latitude: Double, longitude: Double, time: Date): Flow<WeatherInfo> = flow {
        try {
            // Create a cache key that includes the time
            val calendar = Calendar.getInstance().apply { this.time = time }
            val timeString = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
            val cacheKey = "${latitude}_${longitude}_${calendar.get(Calendar.YEAR)}${calendar.get(Calendar.MONTH)}${calendar.get(Calendar.DAY_OF_MONTH)}_$timeString"
            
            // Check if we have cached data that is less than 1 hour old
            val cachedForecast = forecastCache[cacheKey]
            
            if (cachedForecast != null && isForecastCacheValid(cachedForecast.lastUpdated)) {
                Log.d("WeatherRepository", "Using cached forecast data for specific time: $cacheKey")
                emit(cachedForecast)
                return@flow
            }
            
            // Fetch new data from API
            Log.d("WeatherRepository", "Fetching forecast data for specific time: $cacheKey")
            val forecastResponse = weatherApiClient.getForecast(latitude, longitude)
            
            // Convert the target time to Unix timestamp (seconds)
            val targetTimestamp = time.time / 1000
            
            // Find the forecast closest to the target time
            val closestForecast = forecastResponse.list.minByOrNull { 
                Math.abs(it.dt - targetTimestamp) 
            }
            
            if (closestForecast != null) {
                val weather = closestForecast.weather.firstOrNull()
                    ?: forecastResponse.list.firstOrNull()?.weather?.firstOrNull()
                
                if (weather != null) {
                    val weatherInfo = WeatherInfo(
                        temperature = closestForecast.main.temp,
                        tempMin = closestForecast.main.tempMin,
                        tempMax = closestForecast.main.tempMax,
                        windSpeed = closestForecast.wind.speed,
                        windDeg = closestForecast.wind.deg,
                        windGust = closestForecast.wind.gust,
                        pressure = closestForecast.main.pressure,
                        description = weather.description,
                        iconUrl = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                        forecastDate = Date(closestForecast.dt * 1000),
                        humidity = closestForecast.main.humidity
                    )
                    
                    // Cache the result
                    forecastCache[cacheKey] = weatherInfo
                    
                    emit(weatherInfo)
                } else {
                    throw Exception("No weather data found in forecast")
                }
            } else {
                throw Exception("No forecast found for the specified time")
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching forecast for specific time: ${e.message}")
            throw e
        }
    }.flowOn(Dispatchers.IO)
    
    companion object {
        @Volatile
        private var instance: WeatherRepository? = null
        
        fun getInstance(context: Context): WeatherRepository {
            return instance ?: synchronized(this) {
                instance ?: WeatherRepository(context.applicationContext).also { instance = it }
            }
        }
    }
} 