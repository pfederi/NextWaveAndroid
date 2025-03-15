package com.example.netxwave.ui.departures

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.netxwave.data.api.TransportApiClient
import com.example.netxwave.data.api.TransportApiError
import com.example.netxwave.data.models.Departure
import com.example.netxwave.data.models.DepartureStatus
import com.example.netxwave.data.models.Station
import com.example.netxwave.data.models.WeatherInfo
import com.example.netxwave.data.repository.FavoritesManager
import com.example.netxwave.data.repository.NextWaveRepository
import com.example.netxwave.data.repository.WeatherRepository
import com.example.netxwave.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DeparturesScreenState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val stations: List<Station> = emptyList(),
    val departures: List<Departure> = emptyList(),
    val selectedDate: Date = Calendar.getInstance().time,
    val error: String? = null,
    val showStationSelection: Boolean = false,
    val hasFutureDepartures: Boolean = false,
    val isFavorite: Boolean = false,
    val showMaxFavoritesDialog: Boolean = false,
    val weatherInfo: Map<String, WeatherInfo> = emptyMap(),
    val showWeatherInfo: Boolean = true
)

/**
 * ViewModel for the departures view
 */
class DeparturesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    
    private val repository = NextWaveRepository.getInstance(application)
    private val transportApiClient = TransportApiClient.getInstance()
    private val favoritesManager = FavoritesManager.getInstance(application)
    private val weatherRepository = WeatherRepository.getInstance(application)
    private val sharedPreferences = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(DeparturesScreenState())
    val uiState: StateFlow<DeparturesScreenState> = _uiState.asStateFlow()
    
    private val stationId: String = savedStateHandle.get<String>("stationId") ?: ""
    
    // Preference change listener
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            SettingsViewModel.KEY_SHOW_WEATHER_INFO -> {
                val showWeatherInfo = prefs.getBoolean(key, true)
                _uiState.update { currentState ->
                    currentState.copy(showWeatherInfo = showWeatherInfo)
                }
                
                // If weather info is enabled, load weather data
                if (showWeatherInfo) {
                    _uiState.value.station?.let { station ->
                        loadWeatherForDepartures(station, _uiState.value.departures, Calendar.getInstance().apply {
                            time = _uiState.value.selectedDate
                        })
                    }
                }
            }
        }
    }
    
    init {
        loadSettings()
        loadStations()
        loadStation()
        
        // Register preference change listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        val showWeatherInfo = sharedPreferences.getBoolean(SettingsViewModel.KEY_SHOW_WEATHER_INFO, true)
        _uiState.update { currentState ->
            currentState.copy(showWeatherInfo = showWeatherInfo)
        }
    }
    
    /**
     * Loads all available stations
     */
    private fun loadStations() {
        viewModelScope.launch {
            try {
                repository.getAllStations().collect { stations ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            stations = stations
                        )
                    }
                }
            } catch (e: Exception) {
                // Error loading stations - we can still continue
                // with the currently selected station
            }
        }
    }
    
    /**
     * Loads the station and then the departure times
     */
    private fun loadStation() {
        viewModelScope.launch {
            try {
                repository.getStationById(stationId).collect { station ->
                    if (station != null) {
                        val isFavorite = favoritesManager.isFavorite(station.id)
                        _uiState.update { currentState ->
                            currentState.copy(
                                station = station,
                                isLoading = true, // Set isLoading to true while we load the departures
                                isFavorite = isFavorite
                            )
                        }
                        loadDepartures(station)
                    } else {
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = "Station not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error loading station: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Loads the departure times for the selected station and date
     */
    private fun loadDepartures(station: Station) {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(isLoading = true, error = null)
                }
                
                // Use the UIC number of the station as ID for the API
                val apiStationId = station.id
                
                try {
                    // Try to load departures from the API
                    val departures = transportApiClient.getDepartures(apiStationId, _uiState.value.selectedDate)
                    
                    // Check if the selected date is the current day
                    val today = Calendar.getInstance()
                    val selectedDate = Calendar.getInstance().apply { time = _uiState.value.selectedDate }
                    val isToday = today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                  today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    
                    // If it's not the current day, set all departures to PLANNED
                    val processedDepartures = if (!isToday) {
                        departures.map { it.copy(status = DepartureStatus.PLANNED) }
                    } else {
                        departures
                    }
                    
                    // If no departures were found for the current day, check if there are departures for future days
                    var hasFutureDepartures = false
                    
                    if (isToday && processedDepartures.isEmpty()) {
                        // Check the next 7 days
                        for (i in 1..7) {
                            val futureDate = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_MONTH, i)
                            }.time
                            
                            try {
                                val futureDepartures = transportApiClient.getDepartures(apiStationId, futureDate)
                                if (futureDepartures.isNotEmpty()) {
                                    hasFutureDepartures = true
                                    break
                                }
                            } catch (e: Exception) {
                                // Ignore errors when checking future days
                            }
                        }
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            departures = processedDepartures,
                            hasFutureDepartures = hasFutureDepartures,
                            error = if (processedDepartures.isEmpty()) {
                                if (hasFutureDepartures) {
                                    "No departures today. Check future dates."
                                } else {
                                    "No departures found"
                                }
                            } else null
                        )
                    }
                    
                    // Load weather forecasts for each departure
                    loadWeatherForDepartures(station, processedDepartures, selectedDate)
                    
                } catch (e: TransportApiError) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            error = "Error loading departures: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error loading departures: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load weather forecasts for each departure time
     */
    private fun loadWeatherForDepartures(station: Station, departures: List<Departure>, selectedDateCalendar: Calendar) {
        // Only load weather if the setting is enabled
        if (!_uiState.value.showWeatherInfo) {
            return
        }
        
        viewModelScope.launch {
            try {
                departures.forEach { departure ->
                    try {
                        // Parse the departure time
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val departureTime = Calendar.getInstance()
                        val parsedTime = timeFormat.parse(departure.time)
                        
                        if (parsedTime != null) {
                            departureTime.time = parsedTime
                            
                            // Set to the selected date
                            departureTime.set(Calendar.YEAR, selectedDateCalendar.get(Calendar.YEAR))
                            departureTime.set(Calendar.MONTH, selectedDateCalendar.get(Calendar.MONTH))
                            departureTime.set(Calendar.DAY_OF_MONTH, selectedDateCalendar.get(Calendar.DAY_OF_MONTH))
                            
                            // Get forecast for the departure time
                            weatherRepository.getForecastForSpecificTime(
                                station.latitude,
                                station.longitude,
                                departureTime.time
                            ).collect { weatherInfo ->
                                // Store the forecast with a key that includes the departure time
                                updateWeatherInfo("${departure.time}", weatherInfo)
                                android.util.Log.d("DeparturesViewModel", "Loaded forecast for departure at ${departure.time}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DeparturesViewModel", "Error loading forecast for departure at ${departure.time}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DeparturesViewModel", "Error loading weather forecasts: ${e.message}")
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
     * Helper function to check if two dates are on the same day
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Changes the selected date and loads new departure times
     */
    fun selectDate(date: Date) {
        // Check if the selected date is not more than 8 days in the future
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val maxDate = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_MONTH, 8)
        }
        
        val selectedDate = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if the selected date exceeds the maximum date
        val validDate = if (selectedDate.timeInMillis > maxDate.timeInMillis) {
            maxDate.time
        } else {
            date
        }
        
        _uiState.update { currentState ->
            currentState.copy(
                selectedDate = validDate,
                isLoading = true
            )
        }
        
        viewModelScope.launch {
            val station = _uiState.value.station ?: return@launch
            loadDepartures(station)
        }
    }
    
    /**
     * Shows or hides the station selection
     */
    fun toggleStationSelection(show: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                showStationSelection = show
            )
        }
    }
    
    /**
     * Selects a new station and loads its departures
     */
    fun selectStation(station: Station) {
        val isFavorite = favoritesManager.isFavorite(station.id)
        _uiState.update { currentState ->
            currentState.copy(
                station = station,
                isLoading = true,
                showStationSelection = false,
                isFavorite = isFavorite
            )
        }
        
        viewModelScope.launch {
            loadDepartures(station)
        }
    }
    
    /**
     * Adds the current station to favorites or removes it
     */
    fun toggleFavorite(station: Station) {
        val result = favoritesManager.toggleFavorite(station)
        _uiState.update { currentState ->
            when (result) {
                FavoritesManager.FavoriteResult.ADDED -> {
                    currentState.copy(
                        isFavorite = true,
                        showMaxFavoritesDialog = false
                    )
                }
                FavoritesManager.FavoriteResult.REMOVED -> {
                    currentState.copy(
                        isFavorite = false,
                        showMaxFavoritesDialog = false
                    )
                }
                FavoritesManager.FavoriteResult.MAX_REACHED -> {
                    currentState.copy(
                        showMaxFavoritesDialog = true
                    )
                }
            }
        }
    }
    
    /**
     * Closes the dialog for maximum favorites
     */
    fun dismissMaxFavoritesDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showMaxFavoritesDialog = false
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Unregister the preference change listener
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
} 