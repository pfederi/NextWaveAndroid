package com.lakeshorestudios.nextwave.ui.departures

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakeshorestudios.nextwave.data.models.Departure
import com.lakeshorestudios.nextwave.data.models.DepartureStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.lakeshorestudios.nextwave.ui.common.StationSelectionSheet
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import android.content.Intent
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.CloudSun
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.MoonStar
import com.composables.icons.lucide.PersonStanding
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Thermometer
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.Wind
import com.lakeshorestudios.nextwave.data.models.DaylightPhase
import com.lakeshorestudios.nextwave.data.models.LakeEnvironmentData
import com.lakeshorestudios.nextwave.data.models.SunTimes
import com.lakeshorestudios.nextwave.data.models.WeatherInfo
import com.lakeshorestudios.nextwave.data.models.getWetsuitThickness

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
    
    // Theme-aware colors
    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground
    val borderColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.border
    val whiteColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
    
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
                                showWeatherInfo = uiState.showWeatherInfo,
                                sunTimes = uiState.sunTimes,
                                selectedDate = uiState.selectedDate,
                                lakeEnvironmentData = uiState.lakeEnvironmentData,
                                isLoadingWeather = uiState.isLoadingWeather,
                                stationName = uiState.station?.name ?: ""
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
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Generate 7 days: today + 6
    val dates = (0..6).map { offset ->
        Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_MONTH, offset)
        }.time
    }

    val selectedCal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val selectedIndex = dates.indexOfFirst {
        val cal = Calendar.getInstance().apply { time = it }
        cal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
    }.coerceAtLeast(0)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll to selected chip
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(maxOf(0, selectedIndex - 1))
    }

    androidx.compose.foundation.lazy.LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(dates.size) { index ->
            val chipDate = dates[index]
            val isSelected = index == selectedIndex
            val isToday = index == 0

            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateNumFormat = SimpleDateFormat("dd. MMM", Locale.getDefault())
            val chipLabel = if (isToday) {
                "Today, ${dateNumFormat.format(chipDate)}"
            } else {
                "${dayFormat.format(chipDate)}, ${dateNumFormat.format(chipDate)}"
            }

            val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val chipBorder = if (!isSelected && isToday) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else if (!isSelected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            } else null

            Surface(
                modifier = Modifier
                    .clickable { onDateSelected(chipDate) },
                shape = RoundedCornerShape(20.dp),
                color = chipBg,
                border = chipBorder,
                shadowElevation = if (isSelected) 4.dp else 0.dp
            ) {
                Text(
                    text = chipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = chipTextColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
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
    showWeatherInfo: Boolean = true,
    sunTimes: SunTimes? = null,
    selectedDate: Date = Date(),
    lakeEnvironmentData: LakeEnvironmentData? = null,
    isLoadingWeather: Boolean = false,
    stationName: String = ""
) {
    val currentTime = Calendar.getInstance()
    val departureTime = Calendar.getInstance()

    // Parse departure time
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    try {
        val parsedTime = timeFormat.parse(departure.time)
        if (parsedTime != null) {
            departureTime.time = parsedTime
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
        DepartureStatus.PLANNED -> MaterialTheme.colorScheme.onSurface
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

    // Determine daylight phase
    val departureDateTime = getDepartureDateTime(departure.time, selectedDate)
    val daylightPhase = if (departureDateTime != null && sunTimes != null) {
        sunTimes.getDaylightPhase(departureDateTime)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(contentAlpha),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            // Top section: Time + Wave/Journey
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left column: Time and Status
                Column(
                    modifier = Modifier.width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = departure.time,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

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

                Spacer(modifier = Modifier.width(16.dp))

                // Right column: Wave + Journey
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Line 1: Wave number + share button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Water,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${departure.waveNumber}. Wave",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))

                        // Share button (only for non-missed departures)
                        if (departure.status != DepartureStatus.MISSED) {
                            val context = LocalContext.current
                            Icon(
                                imageVector = Lucide.Share2,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        val shareText = generateShareText(
                                            departure = departure,
                                            selectedDate = selectedDate,
                                            weatherInfo = weatherInfo,
                                            lakeEnvironmentData = lakeEnvironmentData,
                                            stationName = stationName
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share Wave")
                                        )
                                    }
                            )
                        }
                    }

                    // Line 2: Journey number and destination
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    ) {
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = departure.nextStation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Line 3: Weather/environment info (full width, right-aligned, tappable for legend)
            var showWeatherLegend by remember { mutableStateOf(false) }

            if (showWeatherLegend) {
                WeatherLegendSheet(onDismiss = { showWeatherLegend = false })
            }

            val hasWeatherData = weatherInfo != null || lakeEnvironmentData != null || daylightPhase != null

            // Loading indicator while weather data is being fetched
            if (departure.status != DepartureStatus.MISSED && showWeatherInfo &&
                isLoadingWeather && !hasWeatherData) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Loading weather...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (departure.status != DepartureStatus.MISSED && showWeatherInfo && hasWeatherData) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWeatherLegend = true }
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    val sep = " | "
                    val sepColor = MaterialTheme.colorScheme.outline
                    val s = MaterialTheme.typography.bodySmall
                    val c = MaterialTheme.colorScheme.onSurface
                    val iconSize = 14.dp

                    // Darkness icon (only for twilight/night)
                    daylightPhase?.let { phase ->
                        if (phase != DaylightPhase.DAY) {
                            Icon(
                                imageVector = when (phase) {
                                    DaylightPhase.TWILIGHT -> Lucide.MoonStar
                                    else -> Lucide.Moon
                                },
                                contentDescription = null,
                                modifier = Modifier.size(iconSize),
                                tint = c
                            )
                            Text(text = sep, style = s, color = sepColor)
                        }
                    }

                    // Weather icon + air temperature
                    weatherInfo?.let { wi ->
                        AsyncImage(
                            model = wi.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(text = " ${String.format("%.1f°", wi.temperature)}", style = s, color = c)
                        Text(text = sep, style = s, color = sepColor)
                    }

                    // Water temperature
                    lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                        Icon(
                            imageVector = Lucide.Droplet,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = c
                        )
                        Text(text = " ${String.format("%.1f°", waterTemp)}", style = s, color = c)
                        Text(text = sep, style = s, color = sepColor)
                    }

                    // Wind
                    weatherInfo?.let { wi ->
                        val kn = (wi.windSpeed * 1.94384).toInt()
                        val dir = getWindDirection(wi.windDeg)
                        Icon(
                            imageVector = Lucide.Wind,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = c
                        )
                        Text(text = " $kn kn $dir", style = s, color = c)
                    }

                    // Wetsuit thickness
                    lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
                        val airTemp = weatherInfo?.temperature
                        val wetsuit = getWetsuitThickness(waterTemp, airTemp)
                        if (wetsuit != null) {
                            Text(text = sep, style = s, color = sepColor)
                            Icon(
                                imageVector = Lucide.PersonStanding,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize),
                                tint = c
                            )
                            Text(text = " $wetsuit", style = s, color = c)
                        }
                    }

                    // Water level difference (only today)
                    val today = Calendar.getInstance()
                    val selDate = Calendar.getInstance().apply { time = selectedDate }
                    val isToday = today.get(Calendar.YEAR) == selDate.get(Calendar.YEAR) &&
                            today.get(Calendar.DAY_OF_YEAR) == selDate.get(Calendar.DAY_OF_YEAR)
                    if (isToday) {
                        lakeEnvironmentData?.waterLevelDifference?.let { diff ->
                            val isHigher = diff.startsWith("+")
                            Text(text = sep, style = s, color = sepColor)
                            Icon(
                                imageVector = if (isHigher) Lucide.TrendingUp else Lucide.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(iconSize),
                                tint = c
                            )
                            Text(text = " $diff", style = s, color = c)
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
 * Parse departure time string into a Date using the selected date as base
 */
/**
 * Generate share text for a departure (like iOS)
 */
private fun generateShareText(
    departure: Departure,
    selectedDate: Date,
    weatherInfo: WeatherInfo?,
    lakeEnvironmentData: LakeEnvironmentData?,
    stationName: String
): String {
    val dateFormat = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale("de", "CH"))
    val dateString = dateFormat.format(selectedDate)

    val introTexts = listOf(
        "\uD83E\uDD73 Let's share the next wave for a party wave!",
        "\uD83C\uDF0A Catch the wave with me!",
        "\uD83C\uDF0A Ready to ride the next wave together?",
        "\uD83D\uDEA2 All aboard for the next adventure!",
        "\uD83C\uDF0A Join me on this wave - it's going to be epic!"
    )
    val intro = introTexts.random()

    val sb = StringBuilder()
    sb.appendLine(intro)
    sb.appendLine()
    sb.appendLine("\uD83D\uDCCD $stationName \u2192 ${departure.nextStation}")
    sb.appendLine("\uD83D\uDCC5 $dateString")
    sb.appendLine("\uD83D\uDD50 ${departure.time} Uhr")
    if (departure.journeyNumber.isNotEmpty()) {
        sb.appendLine("\uD83D\uDEA2 Route ${departure.journeyNumber}")
    }

    weatherInfo?.let {
        sb.appendLine("\uD83C\uDF21\uFE0F ${String.format("%.1f°C", it.temperature)}")
    }

    lakeEnvironmentData?.waterTemperature?.let { waterTemp ->
        sb.appendLine("\uD83D\uDCA7 Water Temperature: ${String.format("%.1f°C", waterTemp)}")
        val airTemp = weatherInfo?.temperature
        val wetsuit = getWetsuitThickness(waterTemp, airTemp)
        if (wetsuit != null) {
            sb.appendLine("\uD83E\uDD38 Wetsuit: ${wetsuit}mm")
        }
    }

    lakeEnvironmentData?.waterLevelDifference?.let { diff ->
        sb.appendLine("\uD83D\uDCCA Water Level: $diff")
    }

    sb.appendLine()
    sb.appendLine("\uD83D\uDCF1 Shared via NextWave App")
    sb.append("\uD83D\uDD17 play.google.com/store/apps/details?id=com.lakeshorestudios.nextwave")

    return sb.toString()
}

private fun getDepartureDateTime(timeString: String, selectedDate: Date): Date? {
    return try {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parsedTime = timeFormat.parse(timeString) ?: return null
        val timeCal = Calendar.getInstance().apply { time = parsedTime }
        val dateCal = Calendar.getInstance().apply { time = selectedDate }

        Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }.time
    } catch (e: Exception) {
        null
    }
}

/**
 * Weather legend bottom sheet - explains all weather icons and data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherLegendSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Weather Legend",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Weather Information section
            Text(
                text = "Weather Information",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LegendItem(
                icon = { AsyncImage(model = "https://openweathermap.org/img/wn/02d@2x.png", contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit) },
                title = "Weather Condition",
                description = "Current weather condition (sunny, cloudy, rainy, etc.)"
            )
            LegendItem(
                icon = { Icon(Lucide.Thermometer, null, Modifier.size(20.dp)) },
                title = "Air Temperature",
                description = "Current air temperature in degrees Celsius"
            )
            LegendItem(
                icon = { Icon(Lucide.Droplet, null, Modifier.size(20.dp)) },
                title = "Water Temperature",
                description = "Current lake surface water temperature in degrees Celsius"
            )
            LegendItem(
                icon = { Icon(Lucide.Wind, null, Modifier.size(20.dp)) },
                title = "Wind Speed & Direction",
                description = "Wind speed in knots (kn) and direction (N, NE, E, SE, S, SW, W, NW)"
            )
            LegendItem(
                icon = { Icon(Lucide.PersonStanding, null, Modifier.size(20.dp)) },
                title = "Wetsuit Thickness",
                description = "Recommended wetsuit thickness in mm based on water temperature. If air + water temp < 30\u00B0C, one size thicker is recommended"
            )
            LegendItem(
                icon = { Icon(Lucide.Moon, null, Modifier.size(20.dp)) },
                title = "Night Time",
                description = "Moon icon indicates full darkness, moon with stars indicates twilight (dusk/dawn)"
            )
            LegendItem(
                icon = { Icon(Lucide.TrendingUp, null, Modifier.size(20.dp)) },
                title = "Water Level",
                description = "Difference from average water level in centimeters. Arrow up indicates higher, arrow down lower water level (only shown for today)"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Weather Conditions section
            Text(
                text = "Weather Conditions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LegendItem(
                icon = { Icon(Lucide.Sun, null, Modifier.size(20.dp)) },
                title = "Clear Sky",
                description = "Sunny weather with no clouds"
            )
            LegendItem(
                icon = { Icon(Lucide.CloudSun, null, Modifier.size(20.dp)) },
                title = "Partly Cloudy",
                description = "Mix of sun and clouds"
            )
            LegendItem(
                icon = { Icon(Lucide.Cloud, null, Modifier.size(20.dp)) },
                title = "Cloudy",
                description = "Overcast sky"
            )
            LegendItem(
                icon = { Icon(Lucide.CloudRain, null, Modifier.size(20.dp)) },
                title = "Rain",
                description = "Rainy weather"
            )
        }
    }
}

@Composable
private fun LegendItem(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

 