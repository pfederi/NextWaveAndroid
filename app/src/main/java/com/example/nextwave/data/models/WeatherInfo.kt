package com.example.nextwave.data.models

/**
 * Data class representing weather information
 */
data class WeatherInfo(
    val temperature: Double,
    val weatherId: Int,
    val description: String,
    val iconCode: String,
    val windSpeed: Double,
    val windDirection: Double,
    val cityName: String
) {
    /**
     * Get a formatted temperature string (e.g. "5.8°")
     */
    fun getFormattedTemperature(): String {
        return String.format("%.1f°", temperature)
    }
    
    /**
     * Get a formatted wind string (e.g. "5 kn NE")
     */
    fun getFormattedWind(): String {
        // Convert m/s to knots (1 m/s = 1.94384 knots)
        val knots = windSpeed * 1.94384
        
        // Get wind direction as compass direction
        val direction = getWindDirection(windDirection)
        
        return String.format("%d kn %s", knots.toInt(), direction)
    }
    
    /**
     * Convert wind direction in degrees to compass direction
     */
    private fun getWindDirection(degrees: Double): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        return directions[(degrees % 360 / 45).toInt()]
    }
} 