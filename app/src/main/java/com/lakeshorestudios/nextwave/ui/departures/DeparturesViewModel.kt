package com.lakeshorestudios.nextwave.ui.departures

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lakeshorestudios.nextwave.data.api.TransportApiClient
import com.lakeshorestudios.nextwave.data.api.TransportApiError
import com.lakeshorestudios.nextwave.data.models.Departure
import com.lakeshorestudios.nextwave.data.models.DepartureStatus
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.Station
import com.lakeshorestudios.nextwave.data.models.SunTimes
import com.lakeshorestudios.nextwave.data.models.WeatherInfo
import com.lakeshorestudios.nextwave.data.repository.FavoritesManager
import com.lakeshorestudios.nextwave.data.repository.LakeDataRepository
import com.lakeshorestudios.nextwave.data.repository.NextWaveRepository
import com.lakeshorestudios.nextwave.data.repository.WeatherRepository
import com.lakeshorestudios.nextwave.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
    val showWeatherInfo: Boolean = true,
    val isLoadingWeather: Boolean = false,
    val lakeEnvironmentData: LakeEnvironmentData? = null,
    val sunTimes: SunTimes? = null
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
    private val lakeDataRepository = LakeDataRepository.getInstance(application)
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
                val stations = repository.getAllStations().first()
                _uiState.update { currentState ->
                    currentState.copy(
                        stations = stations
                    )
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
                val station = repository.getStationById(stationId).firstOrNull()
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
                
                // Handle virtual Geneva station specially
                if (station.isVirtual && station.id == "virtual_geneva") {
                    loadCombinedDepartures(station)
                    return@launch
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
                    
                    if (processedDepartures.isEmpty() && isToday) {
                        // Check the next 7 days for future departures
                        val maxDaysToCheck = 7
                        for (i in 1..maxDaysToCheck) {
                            val futureDate = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, i)
                            }.time
                            
                            val futureDepartures = transportApiClient.getDepartures(apiStationId, futureDate)
                            if (futureDepartures.isNotEmpty()) {
                                hasFutureDepartures = true
                                break
                            }
                        }
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            departures = processedDepartures,
                            isLoading = false,
                            isLoadingWeather = true,
                            hasFutureDepartures = hasFutureDepartures,
                            error = null
                        )
                    }

                    // Load weather for departure times if enabled
                    if (_uiState.value.showWeatherInfo) {
                        loadWeatherForDepartures(station, processedDepartures, selectedDate)
                    }

                    // Load lake environment data (water temp, water level, sun times)
                    loadLakeEnvironmentData(station, _uiState.value.selectedDate)
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
     * Loads departures from all Geneva stations and combines them
     */
    private fun loadCombinedDepartures(station: Station) {
        viewModelScope.launch {
            try {
                val childStationIds = station.childStationIds
                
                // Load regular stations to get all Geneva stations
                val allStations = repository.getAllStations().first()
                val genevaStations = allStations.filter { it.id in childStationIds }
                
                val selectedDate = _uiState.value.selectedDate
                val allDepartures = mutableListOf<Departure>()
                var hasFutureDepartures = false
                
                // Check if the selected date is the current day
                val today = Calendar.getInstance()
                val selectedDateCal = Calendar.getInstance().apply { time = selectedDate }
                val isToday = today.get(Calendar.YEAR) == selectedDateCal.get(Calendar.YEAR) &&
                          today.get(Calendar.DAY_OF_YEAR) == selectedDateCal.get(Calendar.DAY_OF_YEAR)
                
                // Load departures for each Geneva station
                for (genevaStation in genevaStations) {
                    try {
                        val stationDepartures = transportApiClient.getDepartures(genevaStation.id, selectedDate)
                        
                        // Add station name to each departure
                        val departuresWithStation = stationDepartures.map { 
                            it.copy(
                                destination = "${it.destination} (from ${genevaStation.name})"
                            ) 
                        }
                        
                        // If it's not the current day, set all departures to PLANNED
                        val processedDepartures = if (!isToday) {
                            departuresWithStation.map { it.copy(status = DepartureStatus.PLANNED) }
                        } else {
                            departuresWithStation
                        }
                        
                        allDepartures.addAll(processedDepartures)
                        
                        // Check for future departures if needed
                        if (processedDepartures.isEmpty() && isToday) {
                            // Check the next 7 days for future departures
                            val maxDaysToCheck = 7
                            for (i in 1..maxDaysToCheck) {
                                val futureDate = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, i)
                                }.time
                                
                                val futureDepartures = transportApiClient.getDepartures(genevaStation.id, futureDate)
                                if (futureDepartures.isNotEmpty()) {
                                    hasFutureDepartures = true
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DeparturesViewModel", "Error loading departures for ${genevaStation.name}: ${e.message}")
                        // Continue with other stations
                    }
                }
                
                // Sort combined departures by time
                val sortedDepartures = allDepartures.sortedBy { it.time }
                
                _uiState.update { currentState ->
                    currentState.copy(
                        departures = sortedDepartures,
                        isLoading = false,
                        hasFutureDepartures = hasFutureDepartures,
                        error = null
                    )
                }
                
                // Load weather for departure times if enabled
                if (_uiState.value.showWeatherInfo) {
                    loadWeatherForDepartures(station, sortedDepartures, selectedDateCal)
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error loading combined departures: ${e.message}"
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
                            val weatherInfo = weatherRepository.getForecastForSpecificTime(
                                station.latitude,
                                station.longitude,
                                departureTime.time
                            ).first()
                            // Store the forecast with a key that includes the departure time
                            updateWeatherInfo("${departure.time}", weatherInfo)
                            android.util.Log.d("DeparturesViewModel", "Loaded forecast for departure at ${departure.time}")
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
     * Load lake environment data (water temperature, water level, sun times)
     */
    private fun loadLakeEnvironmentData(station: Station, date: Date) {
        viewModelScope.launch {
            try {
                val lakeName = station.lake
                if (lakeName.isNullOrBlank()) {
                    Log.d("DeparturesViewModel", "No lake name for station ${station.name}")
                    _uiState.update { it.copy(isLoadingWeather = false) }
                    return@launch
                }

                val data = lakeDataRepository.getLakeEnvironmentData(lakeName, date).first()
                _uiState.update { currentState ->
                    currentState.copy(
                        lakeEnvironmentData = data,
                        sunTimes = data.sunTimes,
                        isLoadingWeather = false
                    )
                }
                Log.d("DeparturesViewModel", "Loaded lake data for $lakeName: temp=${data.waterTemperature}, level=${data.waterLevel}, sunTimes=${data.sunTimes != null}")
            } catch (e: Exception) {
                Log.e("DeparturesViewModel", "Error loading lake environment data: ${e.message}")
                _uiState.update { it.copy(isLoadingWeather = false) }
            }
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
                isFavorite = isFavorite,
                lakeEnvironmentData = null // Reset when switching stations
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