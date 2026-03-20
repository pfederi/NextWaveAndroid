package com.lakeshorestudios.nextwave.ui.home

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
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PersonStanding
import com.composables.icons.lucide.ShieldAlert
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.Wind
import com.lakeshorestudios.nextwave.data.models.Departure
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.Station
import com.lakeshorestudios.nextwave.data.models.WeatherInfo
import com.lakeshorestudios.nextwave.data.models.getWetsuitThickness
import com.lakeshorestudios.nextwave.ui.common.StationSelectionSheet
import com.lakeshorestudios.nextwave.ui.components.FavoriteStationCard
import com.lakeshorestudios.nextwave.ui.components.NavigationRulesSheet
import com.lakeshorestudios.nextwave.ui.components.PromoTileCard
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp

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
    var showNavigationRules by remember { mutableStateOf(false) }

    // Show rules sheet
    if (showNavigationRules) {
        NavigationRulesSheet(onDismiss = { showNavigationRules = false })
    }

    // Theme-aware colors
    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground
    val borderColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.border
    val whiteColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
    
    // Set the status bar to match header
    val systemUiController = rememberSystemUiController()
    val isDark = com.lakeshorestudios.nextwave.ui.theme.LocalIsDarkTheme.current
    LaunchedEffect(headerBackgroundColor, isDark) {
        systemUiController.setStatusBarColor(
            color = headerBackgroundColor,
            darkIcons = !isDark
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
                    IconButton(onClick = { showNavigationRules = true }) {
                        Icon(
                            imageVector = Lucide.ShieldAlert,
                            contentDescription = "Wakethieving Rules",
                            tint = Color(0xFFE65100)
                        )
                    }
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
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
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
            
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Nearest Station
        if (nearestStation != null && showNearestStation) {
            item(key = "nearest_station") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nearest Station",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                NearestStationCard(
                    station = nearestStation,
                    distanceKm = nearestStationDistanceKm,
                    nextDeparture = nextDeparture,
                    onClick = onNearestStationClick,
                    borderColor = borderColor,
                    weatherInfo = uiState.weatherInfo[nearestStation.id]?.takeIf { uiState.showWeatherInfo },
                    lakeEnvironmentData = uiState.lakeEnvironmentData[nearestStation.id]
                )
            }
        } else if (showNearestStation) {
            // No location available, show a message instead
            item(key = "nearest_station_unavailable") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nearest Station",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Location services unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Please enable location permissions to see the nearest station to your current location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Favorite Stations Header
        if (favoriteStations.isNotEmpty()) {
            item(key = "favorites_header") {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
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
            // In edit mode: Up/Down buttons for reordering
            itemsIndexed(
                items = favoriteStations,
                key = { _, station -> station.id }
            ) { index, station ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Row {
                            // Move up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newList = favoriteStations.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        viewModel.updateFavoritesOrder(newList)
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                            // Move down
                            IconButton(
                                onClick = {
                                    if (index < favoriteStations.size - 1) {
                                        val newList = favoriteStations.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        viewModel.updateFavoritesOrder(newList)
                                    }
                                },
                                enabled = index < favoriteStations.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (index < favoriteStations.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
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
                    whiteColor = MaterialTheme.colorScheme.onSurface,
                    lakeEnvironmentData = uiState.lakeEnvironmentData[station.id]
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // Promo Tiles
        item(key = "promo_tiles") {
            val visiblePromoTiles = uiState.promoTiles.filter { it.id !in uiState.dismissedPromoTileIds }
            if (uiState.showPromoTiles && visiblePromoTiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                visiblePromoTiles.forEach { tile ->
                    PromoTileCard(
                        promoTile = tile,
                        onDismiss = { viewModel.dismissPromoTile(tile.id) }
                    )
                }
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
    distanceKm: Double?,
    nextDeparture: Departure?,
    onClick: (Station) -> Unit,
    borderColor: Color,
    weatherInfo: WeatherInfo? = null,
    lakeEnvironmentData: LakeEnvironmentData? = null
) {
    val noWavesMessage = remember(station.id) {
        noWavesMessages[java.util.Random(station.id.hashCode().toLong()).nextInt(noWavesMessages.size)]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick(station) },
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
                    if (nextDeparture != null) {
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
                            Text(
                                text = "Next Wave: ${nextDeparture.time}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = noWavesMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
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
            
            // Weather + lake data section (iOS-style inline)
            if (weatherInfo != null || lakeEnvironmentData != null) {
                // Divider
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

                        weatherInfo?.let { wi ->
                            AsyncImage(
                                model = wi.iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(text = " ${String.format("%.1f°", wi.temperature)}", style = s, color = c)
                            Text(text = sep, style = s, color = sepColor)
                        }

                        lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                            Icon(imageVector = Lucide.Droplet, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                            Text(text = " ${String.format("%.1f°", waterTemp)}", style = s, color = c)
                            Text(text = sep, style = s, color = sepColor)
                        }

                        weatherInfo?.let { wi ->
                            val kn = (wi.windSpeed * 1.94384).toInt()
                            val dir = getWindDirection(wi.windDeg)
                            Icon(imageVector = Lucide.Wind, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                            Text(text = " $kn kn $dir", style = s, color = c)
                        }

                        lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                            val airTemp = weatherInfo?.temperature
                            val wetsuit = getWetsuitThickness(waterTemp, airTemp)
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
                    // Forecast view for tomorrow
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

                            weatherInfo?.let { wi ->
                                AsyncImage(
                                    model = wi.iconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    contentScale = ContentScale.Fit
                                )
                                val minTemp = wi.tempMin.toInt()
                                val maxTemp = wi.tempMax.toInt()
                                Text(text = " $minTemp°/$maxTemp°", style = s, color = c)
                                Text(text = sep, style = s, color = sepColor)

                                lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                                    Icon(imageVector = Lucide.Droplet, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                                    Text(text = " ${String.format("%.1f°", waterTemp)}", style = s, color = c)
                                    Text(text = sep, style = s, color = sepColor)
                                }

                                val maxKn = ((wi.maxWindSpeed ?: wi.windSpeed) * 1.94384).toInt()
                                Icon(imageVector = Lucide.Wind, contentDescription = null, modifier = Modifier.size(iconSize), tint = c)
                                Text(text = " max. $maxKn kn", style = s, color = c)
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

/**
 * Random "no waves" messages (matching iOS NoWavesMessageService)
 */
internal val noWavesMessages = listOf(
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