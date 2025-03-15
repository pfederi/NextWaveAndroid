package com.example.nextwave.ui.station

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.nextwave.data.api.TransportApiClient
import com.example.nextwave.data.api.WeatherApiClient
import com.example.nextwave.data.models.Departure
import com.example.nextwave.data.models.Station
import com.example.nextwave.data.models.WeatherInfo
import com.example.nextwave.data.repository.FavoritesManager
import com.example.nextwave.data.repository.NextWaveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

data class StationScreenState(
    val isLoading: Boolean = true,
    val station: Station? = null,
    val departures: List<Departure> = emptyList(),
    val error: String? = null,
    val isFavorite: Boolean = false,
    val weatherInfo: WeatherInfo? = null
)

/**
 * ViewModel for the station screen
 */
class StationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    
    private val repository = NextWaveRepository.getInstance(application)
    private val transportApiClient = TransportApiClient.getInstance()
    private val weatherApiClient = WeatherApiClient.getInstance()
    private val favoritesManager = FavoritesManager.getInstance(application)
    
    private val _uiState = MutableStateFlow(StationScreenState())
    val uiState: StateFlow<StationScreenState> = _uiState.asStateFlow()
    
    init {
        // Get station ID from saved state handle
        val stationId = savedStateHandle.get<String>("stationId")
        
        if (stationId != null) {
            loadStation(stationId)
        } else {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    error = "No station ID provided"
                )
            }
        }
        
        // Check if station is a favorite
        viewModelScope.launch {
            favoritesManager.favorites.collect { favorites ->
                val stationId = savedStateHandle.get<String>("stationId")
                val isFavorite = favorites.any { it.id == stationId }
                
                _uiState.update { currentState ->
                    currentState.copy(isFavorite = isFavorite)
                }
            }
        }
    }
    
    /**
     * Load station details
     */
    private fun loadStation(stationId: String) {
        viewModelScope.launch {
            try {
                repository.getStationById(stationId).collect { station ->
                    if (station != null) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                station = station
                            )
                        }
                        
                        // Load departures for the station
                        loadDepartures(station)
                        
                        // Load weather for the station
                        loadWeather(station)
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
                        error = "Failed to load station: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load departures for a station
     */
    private fun loadDepartures(station: Station) {
        viewModelScope.launch {
            try {
                val departures = transportApiClient.getDepartures(station.id, Date())
                
                _uiState.update { currentState ->
                    currentState.copy(departures = departures)
                }
            } catch (e: Exception) {
                // Just log the error, don't update UI state as this is not critical
                android.util.Log.e("StationViewModel", "Error loading departures: ${e.message}")
            }
        }
    }
    
    /**
     * Load weather for a station
     */
    private fun loadWeather(station: Station) {
        viewModelScope.launch {
            try {
                val weatherInfo = weatherApiClient.getCurrentWeather(
                    station.latitude,
                    station.longitude
                )
                
                if (weatherInfo != null) {
                    _uiState.update { currentState ->
                        currentState.copy(weatherInfo = weatherInfo)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StationViewModel", "Error loading weather: ${e.message}")
            }
        }
    }
    
    /**
     * Toggle favorite status of the current station
     */
    fun toggleFavorite() {
        val station = _uiState.value.station ?: return
        
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                favoritesManager.removeFavorite(station)
            } else {
                favoritesManager.addFavorite(station)
            }
        }
    }
    
    /**
     * Refresh departures for the current station
     */
    fun refreshDepartures() {
        val station = _uiState.value.station ?: return
        loadDepartures(station)
        loadWeather(station)
    }
} 