package com.lakeshorestudios.nextwave.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lakeshorestudios.nextwave.data.api.TransportApiClient
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PersonStanding
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.Wind
import com.lakeshorestudios.nextwave.data.models.Departure
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.Station
import com.lakeshorestudios.nextwave.data.models.WeatherInfo
import com.lakeshorestudios.nextwave.data.models.getWetsuitThickness
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlinx.coroutines.delay

@Composable
fun FavoriteStationCard(
    station: Station,
    onStationSelected: () -> Unit,
    weatherInfo: WeatherInfo? = null,
    whiteColor: Color = MaterialTheme.colorScheme.onSurface,
    lakeEnvironmentData: LakeEnvironmentData? = null
) {
    // Load the next departure only when not in edit mode
    val transportApiClient = TransportApiClient.getInstance()
    var nextDeparture by remember { mutableStateOf<Departure?>(null) }
    
    // Choose a random "no waves" message for this card
    // Use the station ID as seed so the message stays consistent for each station
    val noWavesMessage = remember(station.id) {
        com.lakeshorestudios.nextwave.ui.home.noWavesMessages[
            Random(station.id.hashCode().toLong()).nextInt(
                com.lakeshorestudios.nextwave.ui.home.noWavesMessages.size
            )
        ]
    }
    
    // Load the next departure
    LaunchedEffect(station.id) {
        try {
            val currentDate = Date()
            val calendar = Calendar.getInstance()
            
            val departures = transportApiClient.getDepartures(station.id, currentDate)
            
            // Filter departures to get only future departures
            val futureDepartures = departures.filter { departure ->
                try {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val departureTime = Calendar.getInstance()
                    val parsedTime = timeFormat.parse(departure.time)
                    
                    if (parsedTime != null) {
                        departureTime.time = parsedTime
                        departureTime.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                        departureTime.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                        departureTime.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                        
                        // Is the departure in the future?
                        departureTime.timeInMillis > calendar.timeInMillis
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            
            // Get the next departure (first future departure)
            nextDeparture = futureDepartures.firstOrNull()
            
        } catch (e: Exception) {
            // Handle error silently, just don't show next departure
        }
    }
    
    // Refresh departures periodically (every 60 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 60 seconds
            try {
                val currentDate = Date()
                val calendar = Calendar.getInstance()
                
                val departures = transportApiClient.getDepartures(station.id, currentDate)
                
                // Filter departures to get only future departures
                val futureDepartures = departures.filter { departure ->
                    try {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val departureTime = Calendar.getInstance()
                        val parsedTime = timeFormat.parse(departure.time)
                        
                        if (parsedTime != null) {
                            departureTime.time = parsedTime
                            departureTime.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                            departureTime.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                            departureTime.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                            
                            // Is the departure in the future?
                            departureTime.timeInMillis > calendar.timeInMillis
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Get the next departure (first future departure)
                nextDeparture = futureDepartures.firstOrNull()
                
            } catch (e: Exception) {
                // Handle error silently, just don't show next departure
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onStationSelected() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column with station info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Station name row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Station name
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Next Wave row
                    if (nextDeparture != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Water,
                                contentDescription = "Wave",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Wave: ${nextDeparture?.time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = noWavesMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right column with chevron
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Go to station",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Weather section (only if weather info is available)
            weatherInfo?.let {
                // Divider between next wave and weather
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                
                if (nextDeparture != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val sep = " | "
                        val sepColor = MaterialTheme.colorScheme.outline
                        val s = MaterialTheme.typography.bodySmall
                        val c = MaterialTheme.colorScheme.onSurface
                        val iconSize = 14.dp

                        AsyncImage(
                            model = weatherInfo.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(text = " ${String.format("%.1f°", weatherInfo.temperature)}", style = s, color = c)
                        Text(text = sep, style = s, color = sepColor)

                        lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                            Icon(imageVector = Lucide.Droplet, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                            Text(text = " ${String.format("%.1f°", waterTemp)}", style = s, color = c)
                            Text(text = sep, style = s, color = sepColor)
                        }

                        val kn = (weatherInfo.windSpeed * 1.94384).toInt()
                        val dir = getWindDirection(weatherInfo.windDeg)
                        Icon(imageVector = Lucide.Wind, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                        Text(text = " $kn kn $dir", style = s, color = c)

                        lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                            val wetsuit = getWetsuitThickness(waterTemp, weatherInfo.temperature)
                            if (wetsuit != null) {
                                Text(text = sep, style = s, color = sepColor)
                                Icon(imageVector = Lucide.PersonStanding, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                                Text(text = " $wetsuit", style = s, color = c)
                            }
                        }

                        lakeEnvironmentData?.waterLevelDifference?.let { diff ->
                            val isHigher = diff.startsWith("+")
                            Text(text = sep, style = s, color = sepColor)
                            Icon(imageVector = if (isHigher) Lucide.TrendingUp else Lucide.TrendingDown, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                            Text(text = " $diff", style = s, color = c)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Weather forecast for tomorrow",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            val sep = " | "
                            val sepColor = MaterialTheme.colorScheme.outline
                            val s = MaterialTheme.typography.bodySmall
                            val c = MaterialTheme.colorScheme.onSurface
                            val iconSize = 14.dp

                            AsyncImage(
                                model = weatherInfo.iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                contentScale = ContentScale.Fit
                            )
                            val minTemp = weatherInfo.tempMin.toInt()
                            val maxTemp = weatherInfo.tempMax.toInt()
                            Text(text = " $minTemp°/$maxTemp°", style = s, color = c)
                            Text(text = sep, style = s, color = sepColor)

                            lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                                Icon(imageVector = Lucide.Droplet, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                                Text(text = " ${String.format("%.1f°", waterTemp)}", style = s, color = c)
                                Text(text = sep, style = s, color = sepColor)
                            }

                            val maxKn = ((weatherInfo.maxWindSpeed ?: weatherInfo.windSpeed) * 1.94384).toInt()
                            Icon(imageVector = Lucide.Wind, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                            Text(text = " max. $maxKn kn", style = s, color = c)
                        }
                    }
                }
            }
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