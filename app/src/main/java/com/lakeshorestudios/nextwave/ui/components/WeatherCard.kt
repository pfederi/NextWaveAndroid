package com.example.netxwave.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.netxwave.data.models.WeatherInfo
import kotlin.math.roundToInt

/**
 * A card that displays weather information
 */
@Composable
fun WeatherCard(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
    whiteColor: Boolean = false
) {
    val textColor = if (whiteColor) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (whiteColor) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (whiteColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${weatherInfo.temperature.roundToInt()}°C",
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = weatherInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryTextColor
                    )
                }
                
                AsyncImage(
                    model = weatherInfo.iconUrl,
                    contentDescription = "Weather icon",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wind info
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Wind",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = "${weatherInfo.windSpeed.roundToInt()} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Humidity info
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Humidity",
                    tint = secondaryTextColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = "${weatherInfo.humidity}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

/**
 * A compact version of the weather card for use in station cards
 */
@Composable
fun CompactWeatherInfo(
    weatherInfo: WeatherInfo,
    modifier: Modifier = Modifier,
    whiteColor: Boolean = false
) {
    val textColor = if (whiteColor) Color.White else MaterialTheme.colorScheme.onSurface
    
    Row(
        modifier = modifier,
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
            text = "${weatherInfo.temperature.roundToInt()}°C",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
} 