package com.example.netxwave.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.netxwave.data.models.WeatherInfo

@Composable
fun ForecastWeatherInfo(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
    whiteColor: Boolean = false
) {
    val textColor = if (whiteColor) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (whiteColor) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Forecast label
        Text(
            text = "Weather forecast for tomorrow",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (whiteColor) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Weather description
        Text(
            text = weatherInfo.description.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Weather icon and temperature
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather icon
            AsyncImage(
                model = weatherInfo.iconUrl,
                contentDescription = "Weather icon",
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Min/Max temperature
            val minTemp = weatherInfo.tempMin.toInt()
            val maxTemp = weatherInfo.tempMax.toInt()
            
            Text(
                text = "$minTemp° / $maxTemp°",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Max wind speed
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Air,
                contentDescription = "Wind",
                tint = secondaryTextColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Max wind speed in knots
            val maxWindSpeedKnots = (weatherInfo.maxWindSpeed ?: weatherInfo.windSpeed) * 1.94384
            
            Text(
                text = String.format("max. %.1f kn", maxWindSpeedKnots),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
fun CompactWeatherInfoWithTime(
    weatherInfo: WeatherInfo,
    departureTime: String? = null,
    modifier: Modifier = Modifier,
    whiteColor: Boolean = false
) {
    val textColor = if (whiteColor) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (whiteColor) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = weatherInfo.iconUrl,
                contentDescription = "Weather icon",
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${weatherInfo.temperature.toInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        
        if (departureTime != null) {
            Text(
                text = "at $departureTime",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Air,
                contentDescription = "Wind",
                tint = secondaryTextColor,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Convert m/s to knots (1 m/s = 1.94384 knots)
            val windSpeedKnots = weatherInfo.windSpeed * 1.94384
            val windDirection = getWindDirection(weatherInfo.windDeg)
            
            Text(
                text = String.format("%.0f kn %s", windSpeedKnots, windDirection),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )
        }
    }
}

/**
 * Get wind direction abbreviation based on degrees
 */
private fun getWindDirection(degrees: Int): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((degrees + 22.5) % 360 / 45).toInt()
    return directions[index]
} 