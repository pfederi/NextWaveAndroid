package com.example.netxwave

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.netxwave.ui.theme.NextWaveTheme
import com.example.netxwave.ui.navigation.NextWaveNavHost

/**
 * Main entry point for the Next Wave app
 */
class MainActivity : ComponentActivity() {
    
    // Request permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
            // Permission granted, location services can be used
            android.util.Log.d("MainActivity", "Location permission granted")
        } else {
            // Permission denied, app will use default station
            android.util.Log.d("MainActivity", "Location permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request location permissions
        requestLocationPermissions()
        
        setContent {
            NextWaveTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NextWaveNavHost()
                }
            }
        }
    }
    
    /**
     * Check and request location permissions if needed
     */
    private fun requestLocationPermissions() {
        when {
            // Check if permission is already granted
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                android.util.Log.d("MainActivity", "Location permission already granted")
            }
            
            // Should show rationale
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Show rationale and then request permission
                android.util.Log.d("MainActivity", "Should show permission rationale")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            
            // Request permission directly
            else -> {
                android.util.Log.d("MainActivity", "Requesting location permissions")
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
} 