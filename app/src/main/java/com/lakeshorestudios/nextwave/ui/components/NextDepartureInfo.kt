package com.example.netxwave.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.netxwave.data.models.Departure
import com.example.netxwave.data.models.DepartureStatus

/**
 * A component that displays information about the next departure
 */
@Composable
fun NextDepartureInfo(
    departure: Departure,
    modifier: Modifier = Modifier,
    whiteColor: Boolean = false
) {
    val textColor = if (whiteColor) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (whiteColor) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsBoat,
            contentDescription = "Boat",
            tint = secondaryTextColor
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = departure.time,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Wave ${departure.waveNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "to ${departure.destination}",
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
        
        // Status indicator
        if (departure.status == DepartureStatus.NOW) {
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "NOW",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 