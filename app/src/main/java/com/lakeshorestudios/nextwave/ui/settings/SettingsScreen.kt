package com.example.netxwave.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    
    // Definiere die benutzerdefinierten Farben
    val headerBackgroundColor = Color(0xFFD6E5F3)
    val headerTextColor = Color.Black
    val mainBackgroundColor = Color(0xFFF0FAFF)
    val cardBackgroundColor = Color.White
    val accentColor = MaterialTheme.colorScheme.primary
    
    // Setze die Statusleiste auf die gleiche Farbe wie der Header
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = headerBackgroundColor,
        darkIcons = true // Dunkle Icons, da der Header hell ist
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
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 0.dp)
                ) {
                    // Show Nearest Station Toggle
                    SettingsToggle(
                        icon = Icons.Outlined.LocationOn,
                        title = "Show Nearest Station",
                        checked = viewModel.showNearestStation,
                        onCheckedChange = { viewModel.setShowNearestStation(it) }
                    )
                    
                    Divider(modifier = Modifier.padding(start = 56.dp, end = 0.dp))
                    
                    // Show Weather Information Toggle
                    SettingsToggle(
                        icon = Icons.Filled.WbSunny,
                        title = "Show Weather Information",
                        checked = viewModel.showWeatherInfo,
                        onCheckedChange = { viewModel.setShowWeatherInfo(it) }
                    )
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
                        BulletPoint("Keep a safe distance from the ship")
                        BulletPoint("Don't ride directly behind the boat")
                        BulletPoint("Be respectful to other water users")
                        BulletPoint("Follow local regulations")
                    }
                }
            )
            
            // How it Works
            ExpandableInfoSection(
                title = "How it Works",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        NumberedPoint(1, "Select your station")
                        NumberedPoint(2, "Check the timetable")
                        NumberedPoint(3, "Enjoy your ride!")
                    }
                }
            )
            
            // Features
            ExpandableInfoSection(
                title = "Features",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        BulletPoint("Real-time boat schedule tracking")
                        BulletPoint("Easy station selection on Swiss Lakes")
                        BulletPoint("Precise wave timing information")
                    }
                }
            )
            
            // Other Useful Foiling Apps
            ExpandableInfoSection(
                title = "Other Useful Foiling Apps",
                content = {
                    Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        LinkBulletPoint("Foil Mates") {
                            openUrl(context, "https://play.google.com/store/search?q=foil%20mates&c=apps&hl=en")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Links
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                openUrl(context, "https://nextwaveapp.ch/privacy.html")
                            }
                            .padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = "Visit pumpfoiling.community",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                openUrl(context, "https://pumpfoiling.community")
                            }
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
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = "Love",
                    tint = Color(0xFF3B82F6), // Blau wie in der iOS-App
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " by Lakeshore Studios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
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
                color = Color.Gray,
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
    onCheckedChange: (Boolean) -> Unit
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
            containerColor = Color.White
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