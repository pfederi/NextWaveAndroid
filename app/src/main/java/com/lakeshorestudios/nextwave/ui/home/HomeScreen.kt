package com.example.netxwave.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.netxwave.data.models.Departure
import com.example.netxwave.data.models.Station
import com.example.netxwave.data.models.WeatherInfo
import com.example.netxwave.ui.common.StationSelectionSheet
import com.example.netxwave.ui.components.FavoriteStationCard
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

/**
 * Home screen with station selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onStationSelected: (Station) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStationSelection by remember { mutableStateOf(false) }
    
    // Define custom colors
    val headerBackgroundColor = Color(0xFFD6E5F3)
    val headerTextColor = Color.Black
    val mainBackgroundColor = Color(0xFFF0FAFF)
    val borderColor = Color(0xFFC9CCCE)
    val whiteColor = Color.White
    
    // Set the status bar to the same color as the header
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(key1 = headerBackgroundColor) {
        systemUiController.setStatusBarColor(
            color = headerBackgroundColor,
            darkIcons = true // Dark icons because the header is light
        )
    }
    
    // Station selection sheet
    StationSelectionSheet(
        show = showStationSelection,
        stations = uiState.stations,
        onStationSelected = { station ->
            viewModel.selectStation(station)
            onStationSelected(station)
        },
        onDismiss = { showStationSelection = false },
        currentStation = uiState.selectedStation
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NextWave", color = headerTextColor) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = headerTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor,
                    titleContentColor = headerTextColor,
                    actionIconContentColor = headerTextColor
                )
            )
        },
        containerColor = mainBackgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Unknown error occurred",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                HomeContent(
                    selectedStation = uiState.selectedStation,
                    onStationSelectorClick = { showStationSelection = true },
                    nearestStation = uiState.nearestStation,
                    nearestStationDistanceKm = uiState.nearestStationDistanceKm,
                    nextDeparture = uiState.nextDeparture,
                    onNearestStationClick = { station -> 
                        onStationSelected(station)
                    },
                    borderColor = borderColor,
                    whiteColor = whiteColor
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    selectedStation: Station?,
    onStationSelectorClick: () -> Unit,
    nearestStation: Station? = null,
    nearestStationDistanceKm: Double? = null,
    nextDeparture: Departure? = null,
    onNearestStationClick: (Station) -> Unit,
    borderColor: Color,
    whiteColor: Color
) {
    // Get favorites from the ViewModel
    val viewModel: HomeViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val favoriteStations = uiState.favoriteStations
    val isEditingFavorites = uiState.isEditingFavorites
    val showNearestStation = uiState.showNearestStation
    
    // Mutable state for favorites during edit mode
    val mutableFavorites = remember(favoriteStations, isEditingFavorites) {
        mutableStateOf(favoriteStations)
    }
    
    // Reorderable state for drag-and-drop
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            // Calculate the actual indices for the favorites list
            // We need to account for header items (header, nearest_station, favorites_header)
            val headerCount = if (nearestStation != null && showNearestStation) 3 else 2
            val fromIndex = from.index - headerCount
            val toIndex = to.index - headerCount
            
            if (fromIndex >= 0 && toIndex >= 0 && fromIndex < mutableFavorites.value.size && toIndex < mutableFavorites.value.size) {
                val newList = mutableFavorites.value.toMutableList()
                val item = newList.removeAt(fromIndex)
                newList.add(toIndex, item)
                mutableFavorites.value = newList
                
                // Update in ViewModel
                viewModel.updateFavoritesOrder(newList)
            }
        }
    )
    
    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .then(
                if (isEditingFavorites) {
                    Modifier.reorderable(reorderableState)
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Header area
        item(key = "header") {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Ahoy Wakethief 🏴‍☠️",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Select a station to catch some waves!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Station selection button - Material 3 Filled Card with Border
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStationSelectorClick() },
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = whiteColor,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = borderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Always show the LocationOn icon (Pindrop)
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = selectedStation?.name ?: "Select Station",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowRight,
                        contentDescription = "Open selection",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Nearest Station
        if (nearestStation != null && showNearestStation) {
            item(key = "nearest_station_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearest Station",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                
                NearestStationCard(
                    station = nearestStation, 
                    nextDeparture = nextDeparture,
                    distanceKm = nearestStationDistanceKm,
                    weatherInfo = if (uiState.showWeatherInfo) {
                        if (nextDeparture != null) 
                            uiState.weatherInfo["departure_${nearestStation.id}"] 
                        else 
                            uiState.weatherInfo[nearestStation.id]
                    } else null,
                    onStationSelected = { onNearestStationClick(nearestStation) },
                    whiteColor = Color.White
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Favorite Stations Header
        if (favoriteStations.isNotEmpty()) {
            item(key = "favorites_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorite Stations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                    
                    // Edit text to toggle edit mode - only show if more than one favorite
                    if (favoriteStations.size > 1) {
                        Text(
                            text = if (isEditingFavorites) "Done" else "Edit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { viewModel.toggleFavoritesEditMode() }
                                .padding(0.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        
        // Favorite Stations Items
        if (isEditingFavorites) {
            // In edit mode: Reorderable list
            itemsIndexed(
                items = mutableFavorites.value,
                key = { _, station -> station.id }
            ) { _, station ->
                ReorderableItem(
                    reorderableState = reorderableState,
                    key = station.id,
                    // Important: No padding here, but in the Card modifier
                ) { isDragging ->
                    val elevation = if (isDragging) 8.dp else 2.dp
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            // Important: detectReorderAfterLongPress must be directly on the Card
                            .detectReorderAfterLongPress(reorderableState),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(
                            defaultElevation = elevation
                        ),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (isDragging) Color(0xFFE3F2FD) else whiteColor
                        )
                    ) {
                        // Simplified view in edit mode - only name and drag handle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Station name
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Drag indicator on the right side
                            Icon(
                                imageVector = Icons.Outlined.DragIndicator,
                                contentDescription = "Drag to reorder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Normal mode: Non-reorderable list
            items(favoriteStations) { station ->
                FavoriteStationCard(
                    station = station,
                    weatherInfo = if (uiState.showWeatherInfo) {
                        uiState.weatherInfo["departure_${station.id}"] ?: uiState.weatherInfo[station.id]
                    } else null,
                    onStationSelected = { onNearestStationClick(station) },
                    whiteColor = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // Extra space at the bottom for better scrolling
        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Expandable section for a lake and its stations
 */
@Composable
fun ExpandableLakeSection(
    lake: String,
    stations: List<Station>,
    onStationSelected: (Station) -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Lake header (clickable to expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lake,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        // Divider
        Divider()
        
        // Stations list (visible only when expanded)
        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                stations.forEach { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationSelected(station) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Thin divider between stations
                    Divider(
                        modifier = Modifier.padding(start = 16.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NearestStationCard(
    station: Station, 
    nextDeparture: Departure?,
    distanceKm: Double?,
    weatherInfo: WeatherInfo? = null,
    onStationSelected: () -> Unit,
    whiteColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onStationSelected() },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
                    // Station name row with distance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NearMe,
                            contentDescription = "Nearest Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(28.dp)
                        )
                        
                        // Station name
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Distance in parentheses
                        if (distanceKm != null) {
                            Text(
                                text = " (${distanceKm} km)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Next Wave row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Water,
                            contentDescription = "Wave",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (nextDeparture != null) {
                            Text(
                                text = "Next Wave: ${nextDeparture.time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = "No waves available",
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
                
                if (nextDeparture != null) {
                    // Current weather for today's departure
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
                                model = it.iconUrl,
                                contentDescription = "Weather icon",
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Weather description
                            Text(
                                text = it.description.replaceFirstChar { c -> c.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
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
                                text = String.format("%.1f°C", it.temperature),
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
                            val windSpeedKnots = it.windSpeed * 1.94384
                            val windDirection = getWindDirection(it.windDeg)
                            
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
                                    model = it.iconUrl,
                                    contentDescription = "Weather icon",
                                    modifier = Modifier.size(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Weather description
                                Text(
                                    text = it.description.replaceFirstChar { c -> c.uppercase() },
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
                                val minTemp = it.tempMin.toInt()
                                val maxTemp = it.tempMax.toInt()
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
                                val maxWindSpeedKnots = (it.maxWindSpeed ?: it.windSpeed) * 1.94384
                                
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