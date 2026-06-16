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

    // Wave Check-in settings
    private var _enableWaveCheckIn = mutableStateOf(true)
    val enableWaveCheckIn: Boolean get() = _enableWaveCheckIn.value

    private var _checkinName = mutableStateOf("")
    val checkinName: String get() = _checkinName.value

    private var _checkinAnonymous = mutableStateOf(false)
    val checkinAnonymous: Boolean get() = _checkinAnonymous.value

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
        _enableWaveCheckIn.value = sharedPreferences.getBoolean(KEY_ENABLE_WAVE_CHECKIN, true)
        _checkinName.value = sharedPreferences.getString(KEY_CHECKIN_NAME, "") ?: ""
        _checkinAnonymous.value = sharedPreferences.getBoolean(KEY_CHECKIN_ANONYMOUS, false)
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

    fun setEnableWaveCheckIn(enabled: Boolean) {
        _enableWaveCheckIn.value = enabled
        viewModelScope.launch {
            sharedPreferences.edit().putBoolean(KEY_ENABLE_WAVE_CHECKIN, enabled).apply()
        }
    }

    fun setCheckinIdentity(name: String, anonymous: Boolean) {
        _checkinName.value = name
        _checkinAnonymous.value = anonymous
        viewModelScope.launch {
            sharedPreferences.edit()
                .putString(KEY_CHECKIN_NAME, name)
                .putBoolean(KEY_CHECKIN_ANONYMOUS, anonymous)
                .apply()
        }
    }

    /** Display name to store on a check-in, or null for anonymous / unset. */
    fun checkinDisplayName(): String? =
        if (_checkinAnonymous.value) null else _checkinName.value.trim().ifEmpty { null }

    /** True once the user has chosen a name OR opted into anonymous. */
    fun hasCheckinIdentity(): Boolean =
        _checkinAnonymous.value || _checkinName.value.trim().isNotEmpty()

    companion object {
        const val KEY_SHOW_NEAREST_STATION = "show_nearest_station"
        const val KEY_SHOW_WEATHER_INFO = "show_weather_info"
        const val KEY_SHOW_PROMO_TILES = "show_promo_tiles"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ENABLE_WAVE_CHECKIN = "enable_wave_checkin"
        const val KEY_CHECKIN_NAME = "checkin_name"
        const val KEY_CHECKIN_ANONYMOUS = "checkin_anonymous"
    }
} 