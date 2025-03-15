package com.example.nextwave.ui.station

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DirectionsBoat
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nextwave.data.models.Departure
import com.example.nextwave.data.models.WeatherInfo
import com.example.nextwave.ui.common.WeatherIcon
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen showing details for a specific station
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    onBackClick: () -> Unit,
    viewModel: StationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Define custom colors
    val headerBackgroundColor = Color(0xFFD6E5F3)
    val headerTextColor = Color.Black
    val mainBackgroundColor = Color(0xFFF0FAFF)
    
    // Set the status bar to the same color as the header
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = headerBackgroundColor,
        darkIcons = true // Dark icons because the header is light
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.station?.name ?: "Station", color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = headerTextColor
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refreshDepartures() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = headerTextColor
                        )
                    }
                    
                    // Favorite button
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (uiState.isFavorite) Color.Red else headerTextColor
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
                StationContent(
                    stationName = uiState.station?.name ?: "Unknown Station",
                    departures = uiState.departures,
                    weatherInfo = uiState.weatherInfo
                )
            }
        }
    }
}

/**
 * Content for the station screen
 */
@Composable
fun StationContent(
    stationName: String,
    departures: List<Departure>,
    weatherInfo: WeatherInfo? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header with station info
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Station header with weather
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Station name and icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsBoat,
                        contentDescription = "Boat",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 8.dp)
                    )
                    
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Weather info
                if (weatherInfo != null) {
                    WeatherIcon(
                        weatherInfo = weatherInfo,
                        iconSize = 28
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Departures header
            Text(
                text = "Upcoming Waves",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (departures.isEmpty()) {
                Text(
                    text = "No upcoming waves available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Departures list
        items(departures) { departure ->
            DepartureItem(departure = departure, weatherInfo = weatherInfo)
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        
        // Bottom space
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Item for a single departure
 */
@Composable
fun DepartureItem(
    departure: Departure,
    weatherInfo: WeatherInfo? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = departure.time,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        
        // Wave number
        Card(
            modifier = Modifier
                .size(40.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = departure.number,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Journey info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = departure.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "→ ${departure.to}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Weather info (if available and matches the departure time)
        if (weatherInfo != null) {
            WeatherIcon(
                weatherInfo = weatherInfo,
                showDetails = false,
                iconSize = 24
            )
        }
    }
}