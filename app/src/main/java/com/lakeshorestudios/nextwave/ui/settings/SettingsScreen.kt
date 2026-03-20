package com.lakeshorestudios.nextwave.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import android.content.pm.PackageManager
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Theme-aware colors
    val headerBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerBackground
    val headerTextColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.headerText
    val mainBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.mainBackground
    val cardBackgroundColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
    val accentColor = MaterialTheme.colorScheme.primary
    
    val systemUiController = rememberSystemUiController()
    val isDark = com.lakeshorestudios.nextwave.ui.theme.LocalIsDarkTheme.current
    systemUiController.setStatusBarColor(
        color = headerBackgroundColor,
        darkIcons = !isDark
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = headerTextColor) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Theme Section
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                    modes.forEach { (mode, label) ->
                        val isSelected = viewModel.themeMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clickable { viewModel.setThemeMode(mode) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Options Section
            Text(
                text = "Display Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsToggle(
                        icon = Icons.Outlined.LocationOn,
                        title = "Show Nearest Station",
                        checked = viewModel.showNearestStation,
                        onCheckedChange = { viewModel.setShowNearestStation(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    SettingsToggle(
                        icon = Icons.Filled.WbSunny,
                        title = "Show Weather Information",
                        checked = viewModel.showWeatherInfo,
                        onCheckedChange = { viewModel.setShowWeatherInfo(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    SettingsToggle(
                        icon = Lucide.Image,
                        title = "Show Promo Tiles",
                        checked = viewModel.showPromoTiles,
                        onCheckedChange = { viewModel.setShowPromoTiles(it) }
                    )
                    Divider(modifier = Modifier.padding(start = 56.dp))
                    // Reset Dismissed Tiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.resetDismissedPromoTiles() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Reset Dismissed Tiles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Information Section
            Text(
                text = "Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Safety First
            ExpandableInfoSection(
                title = "Safety First",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        BulletPoint("Maintain at least 50 meters distance from priority vessels on both sides")
                        BulletPoint("NEVER ride in front of the ship – always stay behind or to the side")
                        BulletPoint("Leave the priority route as quickly as possible")
                        BulletPoint("Wear highly visible head protection")
                        BulletPoint("Life jacket required outside shore zone (300m)")
                        LinkBulletPoint("Pumpfoilers Code of Conduct") {
                            openUrl(context, "https://responsible.pumpfoiling.community/")
                        }
                    }
                }
            )

            // How it Works
            ExpandableInfoSection(
                title = "How it Works",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        NumberedPoint(1, "Select your favorite station on a Swiss lake")
                        NumberedPoint(2, "Check the departure schedule for upcoming waves")
                        NumberedPoint(3, "Review weather, water temperature and conditions")
                        NumberedPoint(4, "Head to the spot and enjoy your ride!")
                    }
                }
            )

            // Features
            ExpandableInfoSection(
                title = "Features",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        BulletPoint("Real-time boat schedule tracking across all Swiss lakes")
                        BulletPoint("Weather conditions including wind, temperature and pressure")
                        BulletPoint("Water temperature from Eawag Alplakes")
                        BulletPoint("Water level monitoring with difference to annual average")
                        BulletPoint("Wetsuit thickness recommendation based on conditions")
                        BulletPoint("Sunrise/sunset and twilight information")
                        BulletPoint("Nearest station detection via GPS")
                        BulletPoint("Up to 5 favorite stations")
                    }
                }
            )

            // Other Useful Foiling Apps
            ExpandableInfoSection(
                title = "Other Useful Foiling Apps",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        LinkBulletPoint("7lll Water") {
                            openUrl(context, "https://play.google.com/store/search?q=7lll+water&c=apps")
                        }
                        LinkBulletPoint("Foil Mates") {
                            openUrl(context, "https://play.google.com/store/search?q=foil+mates&c=apps")
                        }
                        LinkBulletPoint("Foilmotion") {
                            openUrl(context, "https://foilmotion.webchoice.ch/")
                        }
                        LinkBulletPoint("Foile") {
                            openUrl(context, "https://foile.ch")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Links Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "View Our Supporters",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { openUrl(context, "https://www.nextwaveapp.ch/supporters") }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Release Notes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { openUrl(context, "https://www.nextwaveapp.ch/release-notes") }
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { openUrl(context, "https://www.nextwaveapp.ch/privacy") }
                            .padding(vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Made with love
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Made with ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = "Love",
                    tint = Color(0xFF3B82F6), // Blau wie in der iOS-App
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " by ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Lakeshore Studios",
                    style = MaterialTheme.typography.bodyMedium.copy(color = accentColor),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable(onClick = { openUrl(context, "https://lakeshorestudios.ch/") })
                        .padding(horizontal = 2.dp)
                )
            }
            
            // Version - automatisch aus PackageInfo ausgelesen
            val packageInfo = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            
            val versionCode = remember {
                if (packageInfo != null) {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        packageInfo.versionCode
                    }
                } else {
                    1
                }
            }
            
            Text(
                text = "Version ${packageInfo?.versionName ?: "1.0"} ($versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ExpandableInfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.lakeshorestudios.nextwave.ui.theme.NextWaveColors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header (clickable to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationState)
                )
            }
            
            // Content (visible only when expanded)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NumberedPoint(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun LinkBulletPoint(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
} 