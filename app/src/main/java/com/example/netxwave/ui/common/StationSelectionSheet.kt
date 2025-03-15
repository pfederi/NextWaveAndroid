package com.example.nextwave.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextwave.data.models.Station

/**
 * Shows a bottom sheet for station selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationSelectionSheet(
    show: Boolean,
    stations: List<Station>,
    onStationSelected: (Station) -> Unit,
    onDismiss: () -> Unit,
    currentStation: Station? = null
) {
    if (show) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        
        // Group stations by lake
        val stationsByLake = stations.groupBy { it.lake }
        
        // Determine the lake of the currently selected station
        val currentLake = currentStation?.lake
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Select Station",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Expandable lake sections
                stationsByLake.forEach { (lake, lakeStations) ->
                    item {
                        ExpandableLakeSection(
                            lake = lake,
                            stations = lakeStations.sortedBy { it.name },
                            onStationSelected = { station ->
                                onStationSelected(station)
                                onDismiss()
                            },
                            // Automatically expand the lake of the current station
                            initiallyExpanded = lake == currentLake,
                            // Highlight the current station
                            currentStationId = currentStation?.id
                        )
                    }
                }
            }
        }
    }
}

/**
 * Expandable section for a lake and its stations
 */
@Composable
private fun ExpandableLakeSection(
    lake: String,
    stations: List<Station>,
    onStationSelected: (Station) -> Unit,
    initiallyExpanded: Boolean = false,
    currentStationId: String? = null
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Lake header (clickable to expand/collapse)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
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
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotationState)
            )
        }
        
        // Divider
        Divider()
        
        // Stations list (visible only when expanded)
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                stations.forEach { station ->
                    val isCurrentStation = station.id == currentStationId
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStationSelected(station) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyLarge,
                            // Highlight current station
                            fontWeight = if (isCurrentStation) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentStation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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