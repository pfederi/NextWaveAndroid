package com.lakeshorestudios.nextwave.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog

@Composable
fun WaveCheckinBadge(
    count: Int,
    names: List<String>,
    isMine: Boolean,
    onToggle: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val tint = when {
        isMine -> Color(0xFF007AFF)
        count > 0 -> MaterialTheme.colorScheme.onSurface
        else -> Color.Gray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { showDetails = true }
    ) {
        Icon(
            imageVector = if (isMine) Icons.Filled.Group else Icons.Outlined.Group,
            contentDescription = "Wave check-ins",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$count", style = MaterialTheme.typography.bodyMedium, color = tint)
        }
    }

    if (showDetails) {
        val anonymous = (count - names.size).coerceAtLeast(0)
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Text(
                    if (count == 0) "No one yet — be the first!"
                    else "$count riding this wave 🌊"
                )
            },
            text = {
                Column {
                    names.forEach { name ->
                        Text("• $name", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (anonymous > 0) {
                        Text(
                            "• $anonymous anonymous",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDetails = false
                    onToggle()
                }) {
                    Text(
                        text = if (isMine) "Maybe next wave" else "I'm in! 🤙",
                        color = if (isMine) Color.Red else Color(0xFF007AFF)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetails = false }) { Text("Close") }
            }
        )
    }
}
