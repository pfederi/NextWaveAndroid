package com.example.nextwave.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.nextwave.data.models.WeatherInfo

/**
 * Weather icon component that displays a weather icon based on the weather ID
 */
@Composable
fun WeatherIcon(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    iconSize: Int = 24
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Weather icon based on OpenWeather icon code
        val (icon, tint) = getWeatherIconAndTint(weatherInfo.weatherId)
        
        Icon(
            imageVector = icon,
            contentDescription = weatherInfo.description,
            tint = tint,
            modifier = Modifier.size(iconSize.dp)
        )
        
        if (showDetails) {
            Spacer(modifier = Modifier.width(4.dp))
            
            // Temperature
            Text(
                text = weatherInfo.getFormattedTemperature(),
                color = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Wind
            Text(
                text = "| ${weatherInfo.getFormattedWind()}",
                color = Color.DarkGray
            )
        }
    }
}

/**
 * Get the appropriate weather icon and tint based on the weather ID
 */
@Composable
fun getWeatherIconAndTint(weatherId: Int): Pair<ImageVector, Color> {
    return when (weatherId) {
        // Thunderstorm
        in 200..299 -> Pair(Icons.Outlined.Thunderstorm, Color(0xFF5C6BC0))
        
        // Drizzle and Rain
        in 300..399, in 500..599 -> Pair(Icons.Outlined.WaterDrop, Color(0xFF42A5F5))
        
        // Snow
        in 600..699 -> Pair(Icons.Outlined.WaterDrop, Color(0xFFECEFF1))
        
        // Atmosphere (fog, mist, etc.)
        in 700..799 -> Pair(Icons.Filled.Cloud, Color(0xFF90A4AE))
        
        // Clear
        800 -> Pair(Icons.Filled.WbSunny, Color(0xFFFFB74D))
        
        // Clouds
        else -> Pair(Icons.Filled.Cloud, Color(0xFF90A4AE))
    }
} 