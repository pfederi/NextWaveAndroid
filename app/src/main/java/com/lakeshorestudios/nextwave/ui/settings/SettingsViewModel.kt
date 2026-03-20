package com.lakeshorestudios.nextwave.ui.settings

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val sharedPreferences = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    
    // Display settings
    private var _showNearestStation = mutableStateOf(true)
    val showNearestStation: Boolean get() = _showNearestStation.value
    
    private var _showWeatherInfo = mutableStateOf(true)
    val showWeatherInfo: Boolean get() = _showWeatherInfo.value

    private var _showPromoTiles = mutableStateOf(true)
    val showPromoTiles: Boolean get() = _showPromoTiles.value

    // Theme: "system", "light", "dark"
    private var _themeMode = mutableStateOf("system")
    val themeMode: String get() = _themeMode.value

    init {
        loadSettings()
    }

    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        _showNearestStation.value = sharedPreferences.getBoolean(KEY_SHOW_NEAREST_STATION, true)
        _showWeatherInfo.value = sharedPreferences.getBoolean(KEY_SHOW_WEATHER_INFO, true)
        _showPromoTiles.value = sharedPreferences.getBoolean(KEY_SHOW_PROMO_TILES, true)
        _themeMode.value = sharedPreferences.getString(KEY_THEME_MODE, "system") ?: "system"
    }
    
    /**
     * Set whether to show the nearest station
     */
    fun setShowNearestStation(show: Boolean) {
        _showNearestStation.value = show
        viewModelScope.launch {
            sharedPreferences.edit().putBoolean(KEY_SHOW_NEAREST_STATION, show).apply()
        }
    }
    
    /**
     * Set whether to show weather information
     */
    fun setShowWeatherInfo(show: Boolean) {
        _showWeatherInfo.value = show
        viewModelScope.launch {
            sharedPreferences.edit().putBoolean(KEY_SHOW_WEATHER_INFO, show).apply()
        }
    }
    
    fun setShowPromoTiles(show: Boolean) {
        _showPromoTiles.value = show
        viewModelScope.launch {
            sharedPreferences.edit().putBoolean(KEY_SHOW_PROMO_TILES, show).apply()
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            sharedPreferences.edit().putString(KEY_THEME_MODE, mode).apply()
        }
    }

    fun resetDismissedPromoTiles() {
        viewModelScope.launch {
            sharedPreferences.edit().putStringSet("dismissed_promo_tiles", emptySet()).apply()
        }
    }

    companion object {
        const val KEY_SHOW_NEAREST_STATION = "show_nearest_station"
        const val KEY_SHOW_WEATHER_INFO = "show_weather_info"
        const val KEY_SHOW_PROMO_TILES = "show_promo_tiles"
        const val KEY_THEME_MODE = "theme_mode"
    }
} 