package com.lakeshorestudios.nextwave.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lakeshorestudios.nextwave.ui.departures.DeparturesScreen
import com.lakeshorestudios.nextwave.ui.home.HomeScreen
import com.lakeshorestudios.nextwave.ui.settings.SettingsScreen
import com.lakeshorestudios.nextwave.ui.stats.StatsScreen
import com.lakeshorestudios.nextwave.ui.stats.LeaderboardScreen

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val HOME_SCREEN = "home"
    const val SETTINGS_SCREEN = "settings"
    const val DEPARTURES_SCREEN = "departures/{stationId}"
    const val STATION_SELECT_SCREEN = "station_select"
    const val STATS_SCREEN = "stats"
    const val LEADERBOARD_SCREEN = "leaderboard?stationId={stationId}"

    // Helper function to create departures route with parameter
    fun departuresRoute(stationId: String) = "departures/$stationId"

    fun leaderboardRoute(stationId: String? = null): String =
        if (stationId == null) "leaderboard" else "leaderboard?stationId=$stationId"
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
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS_SCREEN) },
                onBadgesClick = { navController.navigate(NavRoutes.STATS_SCREEN) },
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
                onBackClick = { navController.popBackStack() },
                onLeaderboardClick = { sid -> navController.navigate(NavRoutes.leaderboardRoute(sid)) }
            )
        }
        
        // Station select screen
        composable(NavRoutes.STATION_SELECT_SCREEN) {
            // Temporarily we use the HomeScreen component, which already contains a station list
            HomeScreen(
                onSettingsClick = {
                    navController.navigate(NavRoutes.SETTINGS_SCREEN)
                },
                onBadgesClick = { navController.navigate(NavRoutes.STATS_SCREEN) },
                onStationSelected = { station ->
                    navController.navigate(NavRoutes.departuresRoute(station.id)) {
                        // Pop up to the current departures screen to replace it
                        popUpTo(NavRoutes.DEPARTURES_SCREEN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.STATS_SCREEN) {
            StatsScreen(
                onBackClick = { navController.popBackStack() },
                onLeaderboardClick = { navController.navigate(NavRoutes.leaderboardRoute(null)) }
            )
        }

        composable(
            route = NavRoutes.LEADERBOARD_SCREEN,
            arguments = listOf(
                androidx.navigation.navArgument("stationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString("stationId")
            LeaderboardScreen(
                stationId = stationId,
                title = "Leaderboard",
                onBackClick = { navController.popBackStack() }
            )
        }
    }
} 