package com.example.netxwave.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.netxwave.ui.departures.DeparturesScreen
import com.example.netxwave.ui.home.HomeScreen
import com.example.netxwave.ui.settings.SettingsScreen

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val HOME_SCREEN = "home"
    const val SETTINGS_SCREEN = "settings"
    const val DEPARTURES_SCREEN = "departures/{stationId}"
    const val STATION_SELECT_SCREEN = "station_select"
    
    // Helper function to create departures route with parameter
    fun departuresRoute(stationId: String) = "departures/$stationId"
}

/**
 * Main navigation component for the app
 */
@Composable
fun NextWaveNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.HOME_SCREEN
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        // Home screen
        composable(NavRoutes.HOME_SCREEN) {
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS_SCREEN)
                },
                onStationSelected = { station ->
                    navController.navigate(NavRoutes.departuresRoute(station.id))
                }
            )
        }
        
        // Settings screen
        composable(NavRoutes.SETTINGS_SCREEN) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Departures screen
        composable(
            route = NavRoutes.DEPARTURES_SCREEN,
            arguments = listOf(
                navArgument("stationId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
            DeparturesScreen(
                stationId = stationId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        // Station select screen
        composable(NavRoutes.STATION_SELECT_SCREEN) {
            // Temporarily we use the HomeScreen component, which already contains a station list
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS_SCREEN)
                },
                onStationSelected = { station ->
                    navController.navigate(NavRoutes.departuresRoute(station.id)) {
                        // Pop up to the current departures screen to replace it
                        popUpTo(NavRoutes.DEPARTURES_SCREEN) { inclusive = true }
                    }
                }
            )
        }
    }
} 