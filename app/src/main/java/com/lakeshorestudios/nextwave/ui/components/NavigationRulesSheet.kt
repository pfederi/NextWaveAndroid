package com.lakeshorestudios.nextwave.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.MoveDown
import com.composables.icons.lucide.Ruler
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.TriangleAlert

/**
 * Navigation rules / wakethieving rules bottom sheet.
 * Matches the iOS NavigationRulesModal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRulesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Title
            Text(
                text = "Wakethieving Rules",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Safe Distance Requirements
            RulesSection(
                icon = Lucide.Ruler,
                iconColor = Color(0xFFD32F2F),
                title = "Safe Distance Requirements"
            ) {
                Text(
                    text = "50 meters on each side",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Maintain at least 50 meters distance from priority vessels (passenger ships) on both sides.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Lucide.Info, null, Modifier.size(16.dp), tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "50 meters ≈ one boat length for most ships.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Identifying Priority Vessels
            RulesSection(
                icon = Lucide.Eye,
                iconColor = Color(0xFF1976D2),
                title = "Identifying Priority Vessels"
            ) {
                Text(
                    text = "Priority vessels are marked with:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleItem(
                    icon = Lucide.Sun,
                    iconColor = Color(0xFFFFA000),
                    title = "During daytime:",
                    description = "Green ball at the highest point."
                )
                RuleItem(
                    icon = Lucide.Moon,
                    iconColor = Color(0xFF1976D2),
                    title = "At night:",
                    description = "White light at bow, green light on starboard, red light on port, and green light at highest point."
                )
            }

            // Critical Safety Rules
            RulesSection(
                icon = Lucide.TriangleAlert,
                iconColor = Color(0xFFE65100),
                title = "Critical Safety Rules"
            ) {
                RuleItem(
                    icon = Lucide.CircleX,
                    iconColor = Color(0xFFD32F2F),
                    title = "NEVER ride in front of the ship",
                    description = "Always stay behind or to the side."
                )
                RuleItem(
                    icon = Lucide.MoveDown,
                    iconColor = Color(0xFFE65100),
                    title = "Best waves are further back anyway",
                    description = "You'll get better waves staying behind."
                )
                RuleItem(
                    icon = Lucide.ArrowRight,
                    iconColor = Color(0xFF1976D2),
                    title = "Leave the priority route quickly",
                    description = "Don't linger in shipping lanes."
                )
            }

            // Required Safety Equipment
            RulesSection(
                icon = Lucide.ShieldCheck,
                iconColor = Color(0xFF388E3C),
                title = "Required Safety Equipment"
            ) {
                RuleItem(
                    icon = Lucide.CircleAlert,
                    iconColor = Color(0xFFE65100),
                    title = "Highly visible head protection",
                    description = "Wear bright, easily visible headgear for safety."
                )
                RuleItem(
                    icon = Lucide.ShieldCheck,
                    iconColor = Color(0xFF1976D2),
                    title = "Life jacket required outside shore zone (300m)",
                    description = "Minimum 50N buoyancy required when leaving 300m shore zone."
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Lucide.Info, null, Modifier.size(16.dp), tint = Color(0xFF388E3C))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Restube offers inflatable life jackets perfect for this requirement.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Why This Matters
            RulesSection(
                icon = Lucide.Info,
                iconColor = Color(0xFF7B1FA2),
                title = "Why This Matters"
            ) {
                Text(
                    text = "In an emergency stop, the captain must put the ship in reverse. This creates a powerful suction from the propeller that can be extremely dangerous.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Lucide.TriangleAlert, null, Modifier.size(16.dp), tint = Color(0xFFD32F2F))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Following these rules prevents accidents and keeps wakethieving legal!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            // Join Our Community
            RulesSection(
                icon = Lucide.Heart,
                iconColor = Color(0xFFC2185B),
                title = "Join Our Community"
            ) {
                Text(
                    text = "Be part of the responsible pumpfoiling movement in Switzerland.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://responsible.pumpfoiling.community/"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Swiss Pumpfoilers Code of Conduct")
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enjoy your waves responsibly!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun RulesSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    content: @Composable () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = iconColor)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun RuleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = iconColor)
        Spacer(Modifier.width(10.dp))
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
