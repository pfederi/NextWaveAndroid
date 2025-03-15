package com.example.netxwave.ui.components

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
import com.example.netxwave.data.api.TransportApiClient
import com.example.netxwave.data.models.Departure
import com.example.netxwave.data.models.Station
import com.example.netxwave.data.models.WeatherInfo
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
    whiteColor: Color = Color.White
) {
    // Load the next departure only when not in edit mode
    val transportApiClient = TransportApiClient.getInstance()
    var nextDeparture by remember { mutableStateOf<Departure?>(null) }
    
    // List of messages for "no more waves today"
    val noWavesMessages = listOf(
        "No more waves today – back in the lineup tomorrow!",
        "Flat for now, but fresh sets rolling in tomorrow!",
        "Wave machine's off – catch the next swell tomorrow!",
        "Boat's are taking a break – tomorrow's a new ride!",
        "No wake waves left today – time to chill 'til sunrise!",
        "That's it for today – fresh waves incoming tomorrow!",
        "No waves, no worries – time to dry your wetsuit for tomorrow!",
        "The wave train's done for today – ride continues mañana!",
        "Today's waves are history – tomorrow's swell is brewing!",
        "Ship's on pause – fresh rides coming soon!",
        "That's all, folks! But don't worry, tomorrow's a new ride!",
        "No more bumps to ride – but tomorrow's looking rad!",
        "Last wave's gone – time to dream of tomorrow's rides!",
        "Aloha, da waves pau for today – but mo' coming tomorrow!",
        "Chill time, ʻohana! Waves gonna roll in fresh tomorrow!",
        "No more surf – the sea life needs some chill time too!",
        "Post-pumping high is real – but even the ships need a break!",
        "Waves are done, but that post-pumping high lasts all night!",
        "That post-pumping high hits different – but the waves are snoozing now!",
        "No more wake waves, just that sweet post-pumping afterglow!"
    )
    
    // Choose a random message for this card
    // Use the station ID as seed so the message stays consistent for each station
    val randomMessage = remember(station.id) {
        val random = Random(station.id.hashCode().toLong())
        noWavesMessages[random.nextInt(noWavesMessages.size)]
    }
    
    // Function to load departures
    suspend fun loadDepartures() {
        try {
            val departures = transportApiClient.getDepartures(station.id, Date())
            
            // Check if there are departures for today
            val now = Calendar.getInstance()
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            
            // Filter departures for today
            val todayDepartures = departures.filter { departure ->
                try {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val departureTime = Calendar.getInstance()
                    val parsedTime = timeFormat.parse(departure.time)
                    
                    if (parsedTime != null) {
                        departureTime.time = parsedTime
                        departureTime.set(Calendar.YEAR, now.get(Calendar.YEAR))
                        departureTime.set(Calendar.MONTH, now.get(Calendar.MONTH))
                        departureTime.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
                        
                        // Is the departure in the future?
                        departureTime.timeInMillis > now.timeInMillis && 
                        departureTime.timeInMillis < today.timeInMillis
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            
            // Set the next departure
            nextDeparture = todayDepartures.firstOrNull()
            
            // Debug log
            android.util.Log.d("FavoriteStationCard", 
                "Station: ${station.name}, " +
                "All departures: ${departures.size}, " +
                "Today departures: ${todayDepartures.size}, " +
                "Next departure: ${nextDeparture?.time ?: "none"}"
            )
            
        } catch (e: Exception) {
            // Ignore errors when loading departures
            android.util.Log.e("FavoriteStationCard", "Error loading departures: ${e.message}")
        }
    }
    
    // Initial loading of departures
    LaunchedEffect(station.id) {
        loadDepartures()
    }
    
    // Regular update of departures (every 60 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Wait 60 seconds
            loadDepartures() // Reload departures
        }
    }
    
    // Midnight switch - Check if a new day has started
    val currentDate = remember { mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Check every minute
            
            val newDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            if (newDate != currentDate.value) {
                // A new day has begun
                currentDate.value = newDate
                android.util.Log.d("FavoriteStationCard", "New day detected, reloading departures for ${station.name}")
                loadDepartures() // Load departures for the new day
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
            containerColor = whiteColor
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
                    
                    // Next Wave row with crossAxisAlignment for better alignment
                    Row(
                        verticalAlignment = Alignment.Top  // Change to Top to align the icon with the first line
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Water,
                            contentDescription = "Wave",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)  // Slight adjustment upward for better alignment with text
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (nextDeparture != null) {
                            // There is a departure for today
                            Text(
                                text = "Next Wave: ${nextDeparture?.time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            // No departures for today - always show a random message
                            Text(
                                text = randomMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                
                // Different layout based on whether we have a next departure or not
                if (nextDeparture != null) {
                    // Regular weather view for current time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left side: Weather icon and description
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Weather icon from URL
                            AsyncImage(
                                model = weatherInfo.iconUrl,
                                contentDescription = "Weather icon",
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Weather description with indication for the next wave time
                            Column {
                                Text(
                                    text = weatherInfo.description.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                
                                // Show the time
                                Text(
                                    text = "at ${nextDeparture?.time}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Right side: Temperature and wind
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Temperature with thermometer icon
                            Icon(
                                imageVector = Icons.Outlined.Thermostat,
                                contentDescription = "Temperature",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Text(
                                text = String.format("%.1f°C", weatherInfo.temperature),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Wind with icon
                            Icon(
                                imageVector = Icons.Outlined.Air,
                                contentDescription = "Wind",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            // Convert m/s to knots (1 m/s = 1.94384 knots)
                            val windSpeedKnots = weatherInfo.windSpeed * 1.94384
                            val windDirection = getWindDirection(weatherInfo.windDeg)
                            
                            Text(
                                text = String.format("%.0f kn %s", windSpeedKnots, windDirection),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Forecast view for tomorrow
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Forecast label
                        Text(
                            text = "Weather forecast for tomorrow",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Weather icon and description
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Weather icon from URL
                                AsyncImage(
                                    model = weatherInfo.iconUrl,
                                    contentDescription = "Weather icon",
                                    modifier = Modifier.size(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Weather description
                                Text(
                                    text = weatherInfo.description.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            
                            // Right side: Min/Max Temperature and Max wind
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Temperature with thermometer icon
                                Icon(
                                    imageVector = Icons.Outlined.Thermostat,
                                    contentDescription = "Temperature",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                // Min/Max temperature
                                val minTemp = weatherInfo.tempMin.toInt()
                                val maxTemp = weatherInfo.tempMax.toInt()
                                Text(
                                    text = "$minTemp° / $maxTemp°",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Wind with icon
                                Icon(
                                    imageVector = Icons.Outlined.Air,
                                    contentDescription = "Wind",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(2.dp))
                                
                                // Max wind speed in knots
                                val maxWindSpeedKnots = (weatherInfo.maxWindSpeed ?: weatherInfo.windSpeed) * 1.94384
                                
                                Text(
                                    text = String.format("max. %.1f kn", maxWindSpeedKnots),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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