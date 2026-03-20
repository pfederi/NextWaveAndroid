package com.lakeshorestudios.nextwave.ui.home

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lakeshorestudios.nextwave.data.api.TransportApiClient
import com.lakeshorestudios.nextwave.data.api.PromoTileApiClient
import com.lakeshorestudios.nextwave.data.models.Departure
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.PromoTile
import com.lakeshorestudios.nextwave.data.models.Station
import com.lakeshorestudios.nextwave.data.models.WeatherInfo
import com.lakeshorestudios.nextwave.data.repository.FavoritesManager
import com.lakeshorestudios.nextwave.data.repository.LakeDataRepository
import com.lakeshorestudios.nextwave.data.repository.NextWaveRepository
import com.lakeshorestudios.nextwave.data.repository.WeatherRepository
import com.lakeshorestudios.nextwave.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt

data class HomeScreenState(
    val isLoading: Boolean = true,
    val stations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val nearestStation: Station? = null,
    val nearestStationDistanceKm: Double? = null,
    val nextDeparture: Departure? = null,
    val favoriteStations: List<Station> = emptyList(),
    val error: String? = null,
    val isEditingFavorites: Boolean = false,
    val showNearestStation: Boolean = true,
    val showWeatherInfo: Boolean = true,
    val currentLocation: Location? = null,
    val weatherInfo: Map<String, WeatherInfo> = emptyMap(),
    val lakeEnvironmentData: Map<String, LakeEnvironmentData> = emptyMap(),
    val promoTiles: List<PromoTile> = emptyList(),
    val dismissedPromoTileIds: Set<String> = emptySet(),
    val showPromoTiles: Boolean = true
)

/**
 * ViewModel for the home screen
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = NextWaveRepository.getInstance(application)
    private val transportApiClient = TransportApiClient.getInstance()
    private val favoritesManager = FavoritesManager.getInstance(application)
    private val weatherRepository = WeatherRepository.getInstance(application)
    private val lakeDataRepository = LakeDataRepository.getInstance(application)
    private val promoTileApiClient = PromoTileApiClient.getInstance()
    private val sharedPreferences = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _uiState = MutableStateFlow(HomeScreenState())
    val uiState: StateFlow<HomeScreenState> = _uiState.asStateFlow()
    
    // Location listener for updates
    @Suppress("DEPRECATION")
    private val locationListener = object : LocationListener {
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onLocationChanged(location: Location) {
            android.util.Log.d("HomeViewModel", "Location updated: lat=${location.latitude}, lng=${location.longitude}")
            
            // Update the current location in the UI state
            _uiState.update { currentState ->
                currentState.copy(currentLocation = location)
            }
            
            // Recalculate nearest station with the new location
            updateNearestStation()
            
            // Update weather for the current location
            loadWeatherForCurrentLocation()
        }
        
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Not used in newer Android versions
        }
        
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onProviderEnabled(provider: String) {
            android.util.Log.d("HomeViewModel", "Location provider enabled: $provider")
        }
        
        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onProviderDisabled(provider: String) {
            android.util.Log.d("HomeViewModel", "Location provider disabled: $provider")
        }
    }
    
    // Preference change listener
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            SettingsViewModel.KEY_SHOW_NEAREST_STATION -> {
                val showNearestStation = prefs.getBoolean(key, true)
                _uiState.update { currentState ->
                    currentState.copy(showNearestStation = showNearestStation)
                }
            }
            SettingsViewModel.KEY_SHOW_WEATHER_INFO -> {
                val showWeatherInfo = prefs.getBoolean(key, true)
                _uiState.update { currentState ->
                    currentState.copy(showWeatherInfo = showWeatherInfo)
                }

                if (showWeatherInfo) {
                    loadWeatherData()
                }
            }
            SettingsViewModel.KEY_SHOW_PROMO_TILES -> {
                val showPromoTiles = prefs.getBoolean(key, true)
                _uiState.update { currentState ->
                    currentState.copy(showPromoTiles = showPromoTiles)
                }
            }
            "dismissed_promo_tiles" -> {
                val dismissed = prefs.getStringSet("dismissed_promo_tiles", emptySet()) ?: emptySet()
                _uiState.update { currentState ->
                    currentState.copy(dismissedPromoTileIds = dismissed)
                }
            }
        }
    }
    
    init {
        loadSettings()
        loadStations()
        loadFavorites()
        // Start regular updates of Next Wave information
        startNextDepartureUpdates()
        
        // Register preference change listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        
        // Start location updates
        startLocationUpdates()
        
        // Load weather data if enabled
        if (_uiState.value.showWeatherInfo) {
            loadWeatherData()
        }
        
        // Start regular weather updates
        startWeatherUpdates()

        // Load promo tiles
        loadPromoTiles()
        loadDismissedPromoTileIds()
    }
    
    /**
     * Load weather data for all relevant stations
     */
    private fun loadWeatherData() {
        viewModelScope.launch {
            try {
                // Load weather for nearest station
                loadWeatherForNearestStation()
                
                // Load weather for favorite stations
                loadWeatherForFavoriteStations()
                
                // Load weather for current location
                loadWeatherForCurrentLocation()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading weather data: ${e.message}")
            }
        }
    }
    
    /**
     * Load weather for the nearest station
     */
    private fun loadWeatherForNearestStation() {
        val nearestStation = _uiState.value.nearestStation ?: return
        
        viewModelScope.launch {
            try {
                weatherRepository.getWeatherForLocation(
                    nearestStation.latitude,
                    nearestStation.longitude
                ).collect { weatherInfo ->
                    updateWeatherInfo(nearestStation.id, weatherInfo)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading weather for nearest station: ${e.message}")
            }
        }
    }
    
    /**
     * Load weather for favorite stations
     */
    private fun loadWeatherForFavoriteStations() {
        val favoriteStations = _uiState.value.favoriteStations
        if (favoriteStations.isEmpty()) return
        
        viewModelScope.launch {
            favoriteStations.forEach { station ->
                try {
                    weatherRepository.getWeatherForLocation(
                        station.latitude,
                        station.longitude
                    ).collect { weatherInfo ->
                        updateWeatherInfo(station.id, weatherInfo)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error loading weather for station ${station.name}: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Load weather for the current location
     */
    private fun loadWeatherForCurrentLocation() {
        val currentLocation = _uiState.value.currentLocation ?: return
        
        viewModelScope.launch {
            try {
                weatherRepository.getWeatherForLocation(
                    currentLocation.latitude,
                    currentLocation.longitude
                ).collect { weatherInfo ->
                    updateWeatherInfo("current_location", weatherInfo)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading weather for current location: ${e.message}")
            }
        }
    }
    
    /**
     * Load lake environment data for a station (water temp, water level, sun times)
     */
    private fun loadLakeEnvironmentData(station: Station) {
        val lakeName = station.lake ?: return
        if (lakeName.isBlank()) return

        viewModelScope.launch {
            try {
                lakeDataRepository.getLakeEnvironmentData(lakeName, Date()).collect { data ->
                    _uiState.update { currentState ->
                        val updatedData = currentState.lakeEnvironmentData.toMutableMap().apply {
                            put(station.id, data)
                        }
                        currentState.copy(lakeEnvironmentData = updatedData)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading lake data for ${station.name}: ${e.message}")
            }
        }
    }

    /**
     * Update weather info in the UI state
     */
    private fun updateWeatherInfo(key: String, weatherInfo: WeatherInfo) {
        _uiState.update { currentState ->
            val updatedWeatherInfo = currentState.weatherInfo.toMutableMap().apply {
                put(key, weatherInfo)
            }
            currentState.copy(weatherInfo = updatedWeatherInfo)
        }
    }
    
    /**
     * Start location updates
     */
    private fun startLocationUpdates() {
        try {
            // Check if we have permission
            if (ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.e("HomeViewModel", "No location permission granted")
                return
            }
            
            // Try to get last known location first
            val lastKnownLocation = getCurrentLocation()
            if (lastKnownLocation != null) {
                android.util.Log.d("HomeViewModel", "Using last known location: lat=${lastKnownLocation.latitude}, lng=${lastKnownLocation.longitude}")
                _uiState.update { currentState ->
                    currentState.copy(currentLocation = lastKnownLocation)
                }
            }
            
            // Register for location updates
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                try {
                    android.util.Log.d("HomeViewModel", "Requesting location updates from provider: $provider")
                    locationManager.requestLocationUpdates(
                        provider,
                        60000, // Update every 60 seconds
                        100f,  // Or if moved 100 meters
                        locationListener
                    )
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error requesting location updates from $provider: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error starting location updates: ${e.message}")
        }
    }
    
    /**
     * Get the current device location
     */
    private fun getCurrentLocation(): Location? {
        try {
            // Check if we have permission
            if (ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No location permissions
                android.util.Log.d("HomeViewModel", "No location permissions")
                return null // Return null instead of test location
            }
            
            // Check if any location provider is enabled
            val providers = locationManager.getProviders(true)
            if (providers.isEmpty()) {
                android.util.Log.d("HomeViewModel", "No location providers enabled")
                return null // Return null instead of test location
            }
            
            // Try to get the last known location from all providers
            var bestLocation: Location? = null
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            }
            
            if (bestLocation != null) {
                return bestLocation
            } else {
                android.util.Log.d("HomeViewModel", "No location found")
                return null // Return null instead of test location
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error getting location: ${e.message}")
            return null // Return null instead of test location
        }
    }
    
    /**
     * Update the nearest station based on the current location
     */
    private fun updateNearestStation() {
        viewModelScope.launch {
            val currentLocation = _uiState.value.currentLocation
            val stations = _uiState.value.stations
            
            if (currentLocation != null && stations.isNotEmpty()) {
                val (nearestStation, distanceKm) = findNearestStationWithDistance(stations, currentLocation)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        nearestStation = nearestStation,
                        nearestStationDistanceKm = distanceKm
                    )
                }
                
                // Load next departure for nearest station
                nearestStation?.let { loadNextDeparture(it) }
                
                // Load weather for nearest station if weather info is enabled
                if (_uiState.value.showWeatherInfo) {
                    loadWeatherForNearestStation()
                }

                // Load lake environment data for nearest station
                nearestStation?.let { loadLakeEnvironmentData(it) }
            }
        }
    }

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        val showNearestStation = sharedPreferences.getBoolean(SettingsViewModel.KEY_SHOW_NEAREST_STATION, true)
        val showWeatherInfo = sharedPreferences.getBoolean(SettingsViewModel.KEY_SHOW_WEATHER_INFO, true)
        val showPromoTiles = sharedPreferences.getBoolean(SettingsViewModel.KEY_SHOW_PROMO_TILES, true)

        _uiState.update { currentState ->
            currentState.copy(
                showNearestStation = showNearestStation,
                showWeatherInfo = showWeatherInfo,
                showPromoTiles = showPromoTiles
            )
        }
    }
    
    /**
     * Load all stations
     */
    private fun loadStations() {
        viewModelScope.launch {
            try {
                repository.getAllStations().collect { stations ->
                    // Get current location
                    val currentLocation = _uiState.value.currentLocation ?: getCurrentLocation()
                    
                    // Find nearest station
                    val (nearestStation, distanceKm) = if (currentLocation != null) {
                        findNearestStationWithDistance(stations, currentLocation)
                    } else {
                        // If no location is available, return null for both
                        Pair(null, null)
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            stations = stations,
                            nearestStation = nearestStation,
                            nearestStationDistanceKm = distanceKm
                        )
                    }
                    
                    // Load next departure for nearest station
                    nearestStation?.let { loadNextDeparture(it) }
                    
                    // Load weather for nearest station if weather info is enabled
                    if (_uiState.value.showWeatherInfo && nearestStation != null) {
                        loadWeatherForNearestStation()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Failed to load stations: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load favorite stations
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            favoritesManager.favorites.collect { favorites ->
                _uiState.update { currentState ->
                    currentState.copy(
                        favoriteStations = favorites
                    )
                }
                
                // Load weather for favorite stations if weather info is enabled
                if (_uiState.value.showWeatherInfo) {
                    loadWeatherForFavoriteStations()
                }

                // Load lake environment data for favorites
                favorites.forEach { station -> loadLakeEnvironmentData(station) }
            }
        }
    }

    /**
     * Load the next departure for a station
     */
    private fun loadNextDeparture(station: Station) {
        viewModelScope.launch {
            try {
                // Get current date and time
                val currentDate = Date()
                val calendar = java.util.Calendar.getInstance()
                
                // Get departures for the station
                val departures = transportApiClient.getDepartures(station.id, currentDate)
                
                // Log the number of departures for debugging
                android.util.Log.d("HomeViewModel", "Loaded ${departures.size} departures for station ${station.name}")
                
                // Filter departures to get only future departures
                val futureDepartures = departures.filter { departure ->
                    try {
                        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val departureTime = java.util.Calendar.getInstance()
                        val parsedTime = timeFormat.parse(departure.time)
                        
                        if (parsedTime != null) {
                            departureTime.time = parsedTime
                            departureTime.set(java.util.Calendar.YEAR, calendar.get(java.util.Calendar.YEAR))
                            departureTime.set(java.util.Calendar.MONTH, calendar.get(java.util.Calendar.MONTH))
                            departureTime.set(java.util.Calendar.DAY_OF_MONTH, calendar.get(java.util.Calendar.DAY_OF_MONTH))
                            
                            // Is the departure in the future?
                            departureTime.timeInMillis > calendar.timeInMillis
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Error parsing departure time: ${e.message}")
                        false
                    }
                }
                
                // Get the next departure (first future departure)
                val nextDeparture = futureDepartures.firstOrNull()
                
                _uiState.update { currentState ->
                    currentState.copy(nextDeparture = nextDeparture)
                }
                
                // Load weather forecast based on availability of departures
                if (_uiState.value.showWeatherInfo) {
                    if (nextDeparture != null) {
                        // If we have a next departure, load forecast for that specific time
                        loadWeatherForecastForDeparture(station, nextDeparture)
                    } else {
                        // If no more departures for today, load tomorrow's forecast
                        loadTomorrowForecast(station)
                        
                        // Log that we're loading tomorrow's forecast
                        android.util.Log.d("HomeViewModel", "No departures available, loading tomorrow's forecast for ${station.name}")
                    }
                }
            } catch (e: Exception) {
                // Just log the error, don't update UI state as this is not critical
                android.util.Log.e("HomeViewModel", "Error loading next departure: ${e.message}")
                
                // If there's an error, still try to load tomorrow's forecast
                if (_uiState.value.showWeatherInfo) {
                    loadTomorrowForecast(station)
                    android.util.Log.d("HomeViewModel", "Error with departures, loading tomorrow's forecast for ${station.name}")
                }
            }
        }
    }
    
    /**
     * Load tomorrow's forecast for a station
     */
    private fun loadTomorrowForecast(station: Station) {
        viewModelScope.launch {
            try {
                weatherRepository.getForecastForLocation(
                    station.latitude,
                    station.longitude
                ).collect { weatherInfo ->
                    // Make sure the weatherInfo has forecastDate set to tomorrow
                    val tomorrow = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 12) // Noon tomorrow
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    
                    val updatedWeatherInfo = weatherInfo.copy(
                        forecastDate = tomorrow.time
                    )
                    
                    // Store the forecast with the station ID
                    updateWeatherInfo(station.id, updatedWeatherInfo)
                    android.util.Log.d("HomeViewModel", "Loaded tomorrow's forecast for station ${station.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading tomorrow's forecast: ${e.message}")
            }
        }
    }
    
    /**
     * Load weather forecast for a specific departure time
     */
    private fun loadWeatherForecastForDeparture(station: Station, departure: Departure) {
        viewModelScope.launch {
            try {
                // Parse the departure time
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val departureTime = java.util.Calendar.getInstance()
                val parsedTime = timeFormat.parse(departure.time)
                
                if (parsedTime != null) {
                    departureTime.time = parsedTime
                    
                    // Set to today's date
                    val today = java.util.Calendar.getInstance()
                    departureTime.set(java.util.Calendar.YEAR, today.get(java.util.Calendar.YEAR))
                    departureTime.set(java.util.Calendar.MONTH, today.get(java.util.Calendar.MONTH))
                    departureTime.set(java.util.Calendar.DAY_OF_MONTH, today.get(java.util.Calendar.DAY_OF_MONTH))
                    
                    // Get forecast for the departure time
                    weatherRepository.getForecastForSpecificTime(
                        station.latitude,
                        station.longitude,
                        departureTime.time
                    ).collect { weatherInfo ->
                        // Store the forecast with a special key that includes "departure_" prefix
                        updateWeatherInfo("departure_${station.id}", weatherInfo)
                        android.util.Log.d("HomeViewModel", "Loaded forecast for departure at ${departure.time} for station ${station.name}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading forecast for departure: ${e.message}")
            }
        }
    }
    
    /**
     * Find the nearest station to the current location
     */
    private fun findNearestStationWithDistance(stations: List<Station>, location: Location? = null): Pair<Station?, Double?> {
        // If no location is available or no stations, return null
        if (location == null || stations.isEmpty()) {
            return Pair(null, null)
        }
        
        var nearestStation: Station? = null
        var smallestDistance = Double.MAX_VALUE
        
        for (station in stations) {
            val stationLocation = Location("").apply {
                latitude = station.latitude
                longitude = station.longitude
            }
            
            val distanceInMeters = location.distanceTo(stationLocation)
            val distanceInKm = distanceInMeters / 1000.0
            
            if (distanceInKm < smallestDistance) {
                smallestDistance = distanceInKm
                nearestStation = station
            }
        }
        
        return Pair(nearestStation, (smallestDistance * 10).roundToInt() / 10.0)
    }
    
    fun selectStation(station: Station) {
        _uiState.update { currentState ->
            currentState.copy(selectedStation = station)
        }
    }

    /**
     * Toggles the edit mode for favorites
     */
    fun toggleFavoritesEditMode() {
        _uiState.update { currentState ->
            currentState.copy(
                isEditingFavorites = !currentState.isEditingFavorites
            )
        }
    }

    /**
     * Updates the order of favorites
     */
    fun updateFavoritesOrder(reorderedFavorites: List<Station>) {
        favoritesManager.updateFavoritesOrder(reorderedFavorites)
    }

    /**
     * Starts regular updates of Next Wave information
     */
    private fun startNextDepartureUpdates() {
        viewModelScope.launch {
            while (true) {
                // Update Next Wave information for the nearest station
                _uiState.value.nearestStation?.let { loadNextDeparture(it) }
                
                // Wait 60 seconds until the next update
                kotlinx.coroutines.delay(60000)
            }
        }
    }

    /**
     * Force refresh all weather data
     */
    fun refreshWeatherData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "Force refreshing all weather data")
                
                // Clear the weather repository cache
                weatherRepository.clearCache()
                
                // Load weather for nearest station
                loadWeatherForNearestStation()
                
                // Load weather for favorite stations
                loadWeatherForFavoriteStations()
                
                // Load weather for current location
                loadWeatherForCurrentLocation()
                
                // Also load forecast data
                loadForecastData()
                
                android.util.Log.d("HomeViewModel", "Weather data refresh completed")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error refreshing weather data: ${e.message}")
            }
        }
    }

    /**
     * Load forecast data for the nearest station and favorites
     */
    private fun loadForecastData() {
        viewModelScope.launch {
            try {
                // Load forecast for nearest station
                loadForecastForNearestStation()
                
                // Load forecast for favorite stations
                loadForecastForFavoriteStations()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading forecast data: ${e.message}")
            }
        }
    }

    /**
     * Load forecast for the nearest station
     */
    private fun loadForecastForNearestStation() {
        val nearestStation = _uiState.value.nearestStation ?: return
        
        viewModelScope.launch {
            try {
                weatherRepository.getForecastForLocation(
                    nearestStation.latitude,
                    nearestStation.longitude
                ).collect { _ ->
                    // Store forecast in a separate map or update UI as needed
                    android.util.Log.d("HomeViewModel", "Loaded forecast for nearest station: ${nearestStation.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading forecast for nearest station: ${e.message}")
            }
        }
    }

    /**
     * Load forecast for favorite stations
     */
    private fun loadForecastForFavoriteStations() {
        val favoriteStations = _uiState.value.favoriteStations
        if (favoriteStations.isEmpty()) return
        
        viewModelScope.launch {
            favoriteStations.forEach { station ->
                try {
                    weatherRepository.getForecastForLocation(
                        station.latitude,
                        station.longitude
                    ).collect { _ ->
                        // Store forecast in a separate map or update UI as needed
                        android.util.Log.d("HomeViewModel", "Loaded forecast for station ${station.name}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Error loading forecast for station ${station.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Starts regular updates of weather information
     */
    private fun startWeatherUpdates() {
        viewModelScope.launch {
            while (true) {
                // Wait 5 minutes until the next update
                kotlinx.coroutines.delay(5 * 60 * 1000)
                
                // Only update if weather info is enabled
                if (_uiState.value.showWeatherInfo) {
                    android.util.Log.d("HomeViewModel", "Scheduled weather update triggered")
                    refreshWeatherData()
                }
            }
        }
    }

    /**
     * Load promo tiles from API
     */
    private fun loadPromoTiles() {
        viewModelScope.launch {
            try {
                val tiles = promoTileApiClient.getPromoTiles()
                _uiState.update { it.copy(promoTiles = tiles) }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading promo tiles: ${e.message}")
            }
        }
    }

    /**
     * Load dismissed promo tile IDs from SharedPreferences
     */
    private fun loadDismissedPromoTileIds() {
        val dismissed = sharedPreferences.getStringSet("dismissed_promo_tiles", emptySet()) ?: emptySet()
        _uiState.update { it.copy(dismissedPromoTileIds = dismissed) }
    }

    /**
     * Dismiss a promo tile (persisted across sessions)
     */
    fun dismissPromoTile(tileId: String) {
        val newDismissed = _uiState.value.dismissedPromoTileIds + tileId
        _uiState.update { it.copy(dismissedPromoTileIds = newDismissed) }
        sharedPreferences.edit().putStringSet("dismissed_promo_tiles", newDismissed).apply()
    }

    override fun onCleared() {
        super.onCleared()
        // Unregister the listener when the ViewModel is cleared
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        
        // Remove location updates
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Error removing location updates: ${e.message}")
        }
    }
} 