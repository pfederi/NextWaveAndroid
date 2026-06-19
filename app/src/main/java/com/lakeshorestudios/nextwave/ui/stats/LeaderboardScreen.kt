package com.lakeshorestudios.nextwave.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lakeshorestudios.nextwave.data.models.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    stationId: String?,
    title: String,
    onBackClick: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val loadFailed by viewModel.loadFailed.collectAsState()

    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground

    LaunchedEffect(stationId) { viewModel.loadLeaderboard(stationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = headerTextColor)
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
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loadFailed -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Couldn't load the leaderboard.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { viewModel.loadLeaderboard(stationId) }) {
                        Text("Retry")
                    }
                }
                leaderboard.isEmpty() -> Text(
                    "No rides recorded yet — be the first! 🌊",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> {
                    val sorted = remember(leaderboard) { leaderboard.sortedBy { it.rank } }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        items(sorted) { entry -> LeaderboardRow(entry) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val highlight = if (entry.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = entry.displayName ?: "Anonymous",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.Water, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text("${entry.totalWaves}", style = MaterialTheme.typography.titleMedium)
    }
}
