package com.bhoomibot.os.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhoomibot.os.feature.autonomous.AutonomousScreen
import com.bhoomibot.os.feature.camera.CameraScreen
import com.bhoomibot.os.feature.dashboard.DashboardScreen
import com.bhoomibot.os.feature.diagnostics.DiagnosticsScreen
import com.bhoomibot.os.feature.manual.ManualControlScreen
import com.bhoomibot.os.feature.map.MapScreen
import com.bhoomibot.os.feature.settings.SettingsScreen

object AppRoute {
    const val Dashboard = "dashboard"
    const val Manual = "manual"
    const val Autonomous = "autonomous"
    const val Camera = "camera"
    const val Diagnostics = "diagnostics"
    const val Map = "map"
    const val Settings = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoute.Dashboard) {
        composable(AppRoute.Dashboard) { DashboardScreen(onNavigate = navController::navigate) }
        composable(AppRoute.Manual) { ManualControlScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Autonomous) { AutonomousScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Camera) { CameraScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Diagnostics) { DiagnosticsScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Map) { MapScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Settings) { SettingsScreen(onBackClick = navController::popBackStack) }
    }
}
