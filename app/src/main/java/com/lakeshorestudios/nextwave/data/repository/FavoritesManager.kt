package com.example.netxwave.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.netxwave.data.models.Station
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for favorite stations
 */
class FavoritesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    // StateFlow for favorites
    private val _favorites = MutableStateFlow<List<Station>>(emptyList())
    val favorites: Flow<List<Station>> = _favorites.asStateFlow()
    
    init {
        // Load favorites at startup
        loadFavorites()
    }
    
    /**
     * Loads the saved favorites from SharedPreferences
     */
    private fun loadFavorites() {
        val favoritesJson = sharedPreferences.getString(KEY_FAVORITES, null)
        if (favoritesJson != null) {
            val type = object : TypeToken<List<Station>>() {}.type
            val favoritesList = gson.fromJson<List<Station>>(favoritesJson, type)
            _favorites.value = favoritesList
        }
    }
    
    /**
     * Saves the favorites to SharedPreferences
     */
    private fun saveFavorites() {
        val favoritesJson = gson.toJson(_favorites.value)
        sharedPreferences.edit().putString(KEY_FAVORITES, favoritesJson).apply()
    }
    
    /**
     * Adds a station to favorites or removes it if it's already present
     * @return FavoriteResult indicating the status of the operation
     */
    fun toggleFavorite(station: Station): FavoriteResult {
        val currentFavorites = _favorites.value.toMutableList()
        val existingIndex = currentFavorites.indexOfFirst { it.id == station.id }
        
        val result = if (existingIndex >= 0) {
            // Station is already a favorite, so remove it
            currentFavorites.removeAt(existingIndex)
            FavoriteResult.REMOVED
        } else {
            // Station is not yet a favorite, so add it (max. 5)
            if (currentFavorites.size < MAX_FAVORITES) {
                currentFavorites.add(station)
                FavoriteResult.ADDED
            } else {
                // Maximum number reached
                FavoriteResult.MAX_REACHED
            }
        }
        
        _favorites.value = currentFavorites
        saveFavorites()
        return result
    }
    
    /**
     * Checks if a station is a favorite
     */
    fun isFavorite(stationId: String): Boolean {
        return _favorites.value.any { it.id == stationId }
    }
    
    /**
     * Reorders the favorites
     * @param fromIndex The index of the station to be moved
     * @param toIndex The target index for the station
     */
    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val currentFavorites = _favorites.value.toMutableList()
        
        if (fromIndex in currentFavorites.indices && toIndex in currentFavorites.indices) {
            val station = currentFavorites.removeAt(fromIndex)
            currentFavorites.add(toIndex, station)
            
            _favorites.value = currentFavorites
            saveFavorites()
        }
    }
    
    /**
     * Updates the list of favorites with a new order
     * @param reorderedFavorites The newly ordered list of favorites
     */
    fun updateFavoritesOrder(reorderedFavorites: List<Station>) {
        _favorites.value = reorderedFavorites
        saveFavorites()
    }
    
    /**
     * Enum for the possible results of the toggleFavorite method
     */
    enum class FavoriteResult {
        ADDED,      // Station was added to favorites
        REMOVED,    // Station was removed from favorites
        MAX_REACHED // Maximum number of favorites reached
    }
    
    companion object {
        private const val PREFS_NAME = "nextwave_favorites"
        private const val KEY_FAVORITES = "favorite_stations"
        private const val MAX_FAVORITES = 5
        
        // Singleton instance
        @Volatile
        private var instance: FavoritesManager? = null
        
        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 