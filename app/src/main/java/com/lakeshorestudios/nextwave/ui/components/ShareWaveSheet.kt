package com.lakeshorestudios.nextwave.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lakeshorestudios.nextwave.data.utils.CalendarEventContent
import com.lakeshorestudios.nextwave.data.utils.ShareIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareWaveSheet(
    shareText: String,
    calendarContent: CalendarEventContent,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Share or save your next wave",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (ShareIntents.whatsAppInstalled(context)) {
                    ShareTile(
                        icon = Icons.Filled.Message,
                        background = Color(0xFF25D366),
                        label = "WhatsApp"
                    ) {
                        context.startActivity(ShareIntents.whatsApp(shareText))
                        onDismiss()
                    }
                }
                ShareTile(
                    icon = Icons.Filled.Sms,
                    background = Color(0xFF34C759),
                    label = "Messages"
                ) {
                    context.startActivity(ShareIntents.sms(shareText))
                    onDismiss()
                }
                ShareTile(
                    icon = Icons.Filled.Email,
                    background = Color(0xFF007AFF),
                    label = "Mail"
                ) {
                    context.startActivity(ShareIntents.mail("Next Wave", shareText))
                    onDismiss()
                }
                ShareTile(
                    icon = Icons.Filled.CalendarMonth,
                    background = Color(0xFFFF3B30),
                    label = "Calendar"
                ) {
                    context.startActivity(ShareIntents.calendar(calendarContent))
                    onDismiss()
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareTile(
    icon: ImageVector,
    background: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = background,
            modifier = Modifier.size(56.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
