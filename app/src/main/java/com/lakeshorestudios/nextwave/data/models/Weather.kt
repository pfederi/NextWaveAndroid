package com.example.netxwave.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Data class representing a weather response from OpenWeather API
 */
data class WeatherResponse(
    val coord: WeatherCoordinates,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Long,
    val name: String,
    val cod: Int
)

/**
 * Data class representing coordinates for weather
 */
data class WeatherCoordinates(
    val lon: Double,
    val lat: Double
)

/**
 * Data class representing weather condition
 */
data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

/**
 * Data class representing main weather data
 */
data class Main(
    val temp: Double,
    @SerializedName("feels_like")
    val feelsLike: Double,
    @SerializedName("temp_min")
    val tempMin: Double,
    @SerializedName("temp_max")
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("sea_level")
    val seaLevel: Int? = null,
    @SerializedName("grnd_level")
    val groundLevel: Int? = null
)

/**
 * Data class representing wind data
 */
data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double? = null
)

/**
 * Data class representing cloud data
 */
data class Clouds(
    val all: Int
)

/**
 * Data class representing system data
 */
data class Sys(
    val type: Int? = null,
    val id: Int? = null,
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

/**
 * Enum representing pressure trend
 */
enum class PressureTrend {
    RISING,
    FALLING,
    STABLE
}

/**
 * Data class representing simplified weather information for UI
 */
data class WeatherInfo(
    val temperature: Double,
    val tempMin: Double,
    val tempMax: Double,
    val morningTemp: Double? = null,
    val afternoonTemp: Double? = null,
    val windSpeed: Double,
    val maxWindSpeed: Double? = null,
    val windDeg: Int,
    val windGust: Double? = null,
    val pressure: Int,
    val description: String,
    val iconUrl: String,
    val pressureTrend: PressureTrend = PressureTrend.STABLE,
    val forecastDate: Date? = null,
    val humidity: Int,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Wind speed in knots (1 m/s = 1.94384 knots)
     */
    val windSpeedKnots: Double
        get() = windSpeed * 1.94384
    
    /**
     * Maximum wind speed in knots
     */
    val maxWindSpeedKnots: Double?
        get() = maxWindSpeed?.let { it * 1.94384 }
    
    /**
     * Wind gust in knots
     */
    val windGustKnots: Double?
        get() = windGust?.let { it * 1.94384 }
    
    /**
     * Wind direction as text (N, NE, E, etc.)
     */
    val windDirectionText: String
        get() = getWindDirectionText(windDeg)
    
    companion object {
        /**
         * Create WeatherInfo from WeatherResponse
         */
        fun fromWeatherResponse(response: WeatherResponse): WeatherInfo {
            val weather = response.weather.firstOrNull() ?: Weather(0, "Unknown", "Unknown", "01d")
            return WeatherInfo(
                temperature = response.main.temp,
                tempMin = response.main.tempMin,
                tempMax = response.main.tempMax,
                windSpeed = response.wind.speed,
                windDeg = response.wind.deg,
                windGust = response.wind.gust,
                pressure = response.main.pressure,
                description = weather.description,
                iconUrl = "https://openweathermap.org/img/wn/${weather.icon}@2x.png",
                humidity = response.main.humidity
            )
        }
        
        /**
         * Get wind direction text based on degrees
         */
        fun getWindDirectionText(degrees: Int): String {
            return when (degrees) {
                in 0..22, in 338..360 -> "N"
                in 23..67 -> "NE"
                in 68..112 -> "E"
                in 113..157 -> "SE"
                in 158..202 -> "S"
                in 203..247 -> "SW"
                in 248..292 -> "W"
                in 293..337 -> "NW"
                else -> "N/A"
            }
        }
    }
}

/**
 * Data class representing forecast response
 */
data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: City
)

/**
 * Data class representing a forecast item
 */
data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    @SerializedName("dt_txt")
    val dtTxt: String
)

/**
 * Data class representing a city
 */
data class City(
    val id: Long,
    val name: String,
    val coord: WeatherCoordinates,
    val country: String,
    val timezone: Int,
    val sunrise: Long,
    val sunset: Long
)

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
} 