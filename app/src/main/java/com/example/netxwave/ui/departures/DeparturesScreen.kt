package com.example.nextwave.ui.departures

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextwave.data.models.Departure
import com.example.nextwave.data.models.DepartureStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.nextwave.ui.common.StationSelectionSheet
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.nextwave.data.models.WeatherInfo

/**
 * Departure view for a specific station
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(
    @Suppress("UNUSED_PARAMETER") stationId: String,
    onBackClick: () -> Unit,
    viewModel: DeparturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Define custom colors
    val headerBackgroundColor = androidx.compose.ui.graphics.Color(0xFFD6E5F3)
    val headerTextColor = androidx.compose.ui.graphics.Color.Black
    val mainBackgroundColor = androidx.compose.ui.graphics.Color(0xFFF0FAFF)
    val borderColor = androidx.compose.ui.graphics.Color(0xFFC9CCCE)
    val whiteColor = androidx.compose.ui.graphics.Color.White
    
    // Station selection sheet
    StationSelectionSheet(
        show = uiState.showStationSelection,
        stations = uiState.stations,
        onStationSelected = { station -> viewModel.selectStation(station) },
        onDismiss = { viewModel.toggleStationSelection(false) },
        currentStation = uiState.station
    )
    
    // Dialog for maximum number of favorites
    if (uiState.showMaxFavoritesDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMaxFavoritesDialog() },
            title = { Text("Maximum Favorites Reached") },
            text = { Text("You can have a maximum of 5 favorite stations. Please remove one before adding another.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissMaxFavoritesDialog() }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NextWave", color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = headerTextColor
                        )
                    }
                },
                actions = {
                    // Favorite icon
                    IconButton(
                        onClick = { 
                            uiState.station?.let { station ->
                                viewModel.toggleFavorite(station)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = headerTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor,
                    titleContentColor = headerTextColor,
                    navigationIconContentColor = headerTextColor
                )
            )
        },
        containerColor = mainBackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date selection - always show
            DateSelector(
                date = uiState.selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )
            
            // Station selection - always show
            StationHeader(
                station = uiState.station?.name ?: "",
                onStationClick = { viewModel.toggleStationSelection(true) },
                borderColor = borderColor,
                backgroundColor = whiteColor
            )
            
            // Content based on status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                } else if (uiState.error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error occurred",
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Departure list with sequential wave numbers
                    val departuresWithWaveNumbers = uiState.departures.mapIndexed { index, departure ->
                        departure.copy(waveNumber = index + 1)
                    }
                    
                    // Check if the selected date is the current day
                    val today = Calendar.getInstance()
                    val selectedDate = Calendar.getInstance().apply { time = uiState.selectedDate }
                    val isToday = today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                  today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    
                    // Find the index of the first non-missed departure
                    val firstUpcomingIndex = if (isToday) {
                        departuresWithWaveNumbers.indexOfFirst { it.status != DepartureStatus.MISSED }
                    } else {
                        0 // If not today, start at the beginning
                    }.coerceAtLeast(0) // Make sure the index is not -1
                    
                    // Create a LazyListState and scroll to the first upcoming departure
                    val listState = rememberLazyListState()
                    
                    // Scroll to the first upcoming departure when the list is loaded
                    LaunchedEffect(departuresWithWaveNumbers, firstUpcomingIndex) {
                        if (departuresWithWaveNumbers.isNotEmpty() && firstUpcomingIndex >= 0) {
                            listState.animateScrollToItem(index = firstUpcomingIndex)
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(departuresWithWaveNumbers) { departure ->
                            DepartureItem(
                                departure = departure,
                                showStatus = isToday,
                                backgroundColor = whiteColor,
                                weatherInfo = uiState.weatherInfo[departure.time],
                                showWeatherInfo = uiState.showWeatherInfo
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    date: Date,
    onDateSelected: (Date) -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
    val formattedDate = dateFormat.format(date)
    
    // Current date for comparison
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)
    
    // Maximum date (today + 8 days)
    val maxDate = Calendar.getInstance()
    maxDate.time = today.time
    maxDate.add(Calendar.DAY_OF_MONTH, 8)
    
    // Date for comparison
    val selectedDate = Calendar.getInstance()
    selectedDate.time = date
    selectedDate.set(Calendar.HOUR_OF_DAY, 0)
    selectedDate.set(Calendar.MINUTE, 0)
    selectedDate.set(Calendar.SECOND, 0)
    selectedDate.set(Calendar.MILLISECOND, 0)
    
    // Check if the selected date is today
    val isToday = selectedDate.timeInMillis == today.timeInMillis
    
    // We are at the maximum date if the current day is the 7th day (and the next day would be the 8th)
    // or if we are already at or beyond the 8th day
    val isMaxDate = selectedDate.timeInMillis >= maxDate.timeInMillis - (24 * 60 * 60 * 1000)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button - hidden if today is selected
        if (!isToday) {
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    
                    // Only allow if the resulting date is not before today
                    if (calendar.timeInMillis >= today.timeInMillis) {
                        onDateSelected(calendar.time)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Previous day"
                )
            }
        } else {
            // Placeholder with the same size as the button to keep the layout stable
            Spacer(modifier = Modifier.size(48.dp)) // Standard IconButton size
        }
        
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Forward button - hidden if maximum date is reached
        if (!isMaxDate) {
            IconButton(
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    
                    // Only allow if the resulting date is not after the maximum date
                    if (calendar.timeInMillis <= maxDate.timeInMillis) {
                        onDateSelected(calendar.time)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Next day"
                )
            }
        } else {
            // Placeholder with the same size as the button to keep the layout stable
            Spacer(modifier = Modifier.size(48.dp)) // Standard IconButton size
        }
    }
}

@Composable
fun StationHeader(
    station: String,
    onStationClick: () -> Unit,
    borderColor: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .clickable(onClick = onStationClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = station,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Select station",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartureItem(
    departure: Departure,
    showStatus: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    weatherInfo: WeatherInfo? = null,
    showWeatherInfo: Boolean = true
) {
    // Debug log for the values
    android.util.Log.d("DepartureItem", "nextStation: ${departure.nextStation}, destination: ${departure.destination}")
    
    val currentTime = Calendar.getInstance()
    val departureTime = Calendar.getInstance()
    
    // Parse departure time
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    try {
        val parsedTime = timeFormat.parse(departure.time)
        if (parsedTime != null) {
            departureTime.time = parsedTime
            
            // Set year, month, and day to today
            departureTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
            departureTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
            departureTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))
        }
    } catch (e: Exception) {
        // Handle parsing error
    }
    
    // Calculate time difference in minutes
    val diffInMillis = departureTime.timeInMillis - currentTime.timeInMillis
    val diffInMinutes = diffInMillis / (1000 * 60)
    
    val statusColor = when (departure.status) {
        DepartureStatus.MISSED -> Color.Red.copy(alpha = 0.7f)
        DepartureStatus.NOW -> Color.Green.copy(alpha = 0.7f)
        DepartureStatus.PLANNED -> Color.Black
    }
    
    // Determine status text based on time difference
    val statusText = when {
        departure.status == DepartureStatus.MISSED -> "missed"
        departure.status == DepartureStatus.NOW || diffInMinutes < 3 -> "now"
        diffInMinutes < 60 -> "${diffInMinutes}min"
        else -> {
            val hours = diffInMinutes / 60
            val mins = diffInMinutes % 60
            "${hours}h${if (mins > 0) " ${mins}min" else ""}"
        }
    }
    
    // Opacity for past departures - only on the current day
    val contentAlpha = if (showStatus && departure.status == DepartureStatus.MISSED) 0.5f else 1.0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(contentAlpha),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left column: Time and Status
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time at the top
                Text(
                    text = departure.time,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 0.dp),
                    textAlign = TextAlign.Center
                )
                
                // Status below - only show if it's the current day
                if (showStatus) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        modifier = Modifier.padding(top = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Space between time and wave columns
            Spacer(modifier = Modifier.width(16.dp))
            
            // Middle column: Wave symbol and number
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                // Wave number with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp) // Padding at the top to align with the time
                ) {
                    // Wave icon
                    Icon(
                        imageVector = Icons.Outlined.Water,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Wave number
                    Text(
                        text = "${departure.waveNumber}. Wave",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Weather information (if available) - aligned with wave number
                    // Only show weather for non-missed departures
                    if (weatherInfo != null && departure.status != DepartureStatus.MISSED && showWeatherInfo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                        ) {
                            // Weather icon
                            AsyncImage(
                                model = weatherInfo.iconUrl,
                                contentDescription = "Weather icon",
                                modifier = Modifier.size(24.dp),
                                contentScale = ContentScale.Fit
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            // Temperature with one decimal place
                            Text(
                                text = String.format("%.1f°", weatherInfo.temperature),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Separator
                            Text(
                                text = " | ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            // Convert m/s to knots (1 m/s = 1.94384 knots)
                            val windSpeedKnots = weatherInfo.windSpeed * 1.94384
                            val windDirection = getWindDirection(weatherInfo.windDeg)
                            
                            // Wind speed and direction
                            Text(
                                text = "${windSpeedKnots.toInt()} kn $windDirection",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Second line: Journey number and destination - on one line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    // Journey number as chip - only show if a valid number is available
                    if (departure.journeyNumber.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = departure.journeyNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    // Arrow symbol and destination in a shared Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Arrow symbol as icon (ChevronRight)
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Destination
                        Text(
                            text = departure.nextStation,
                            style = MaterialTheme.typography.bodyMedium
                        )
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