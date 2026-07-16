package com.bhoomibot.os.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.feature.autonomous.AutonomousScreen
import com.bhoomibot.os.feature.camera.CameraScreen
import com.bhoomibot.os.feature.dashboard.DashboardScreen
import com.bhoomibot.os.feature.diagnostics.DiagnosticsScreen
import com.bhoomibot.os.feature.manual.ManualControlScreen
import com.bhoomibot.os.feature.map.MapScreen
import com.bhoomibot.os.feature.onboarding.OnboardingScreen
import com.bhoomibot.os.feature.operator.OperatorHomeScreen
import com.bhoomibot.os.feature.robot.RobotHomeScreen
import com.bhoomibot.os.feature.robot.RobotSectionScreen
import com.bhoomibot.os.feature.settings.SettingsScreen
import com.bhoomibot.os.feature.settings.ConnectionSettingsScreen
import com.bhoomibot.os.model.DeviceRole
import kotlinx.coroutines.launch

// Central list of every screen route. The role homes + onboarding are the new entries.
object AppRoute {
    const val Onboarding = "onboarding"
    const val OperatorHome = "operatorHome"
    const val RobotHome = "robotHome"
    const val Dashboard = "dashboard"        // legacy operator status screen (still reachable)
    const val Manual = "manual"
    const val Autonomous = "autonomous"
    const val Camera = "camera"
    const val Diagnostics = "diagnostics"
    const val Map = "map"
    const val Settings = "settings"
    const val ConnectionSettings = "connectionSettings"
    const val MissionPlanner = "missionPlanner"
    const val Notifications = "notifications"
    const val RobotSection = "robotSection/{title}/{subtitle}"
    fun robotSection(title: String, subtitle: String) = "robotSection/$title/$subtitle"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Always start on the role picker so the operator explicitly chooses OPERATOR or ROBOT
    // every time the app is opened (not only on first install). The selected role is still
    // persisted, but the prompt is shown on each launch.
    val startDestination = AppRoute.Onboarding

    NavHost(navController = navController, startDestination = startDestination) {
        // First-launch role picker.
        composable(AppRoute.Onboarding) {
            OnboardingScreen(onRoleSelected = { selected ->
                scope.launch {
                    DevicePreferences.setRole(context, selected)
                    val dest = if (selected == DeviceRole.OPERATOR) AppRoute.OperatorHome else AppRoute.RobotHome
                    navController.navigate(dest) { popUpTo(AppRoute.Onboarding) { inclusive = true } }
                }
            })
        }
        // Role homes.
        composable(AppRoute.OperatorHome) { OperatorHomeScreen(navController) }
        composable(AppRoute.RobotHome) { RobotHomeScreen(navController) }
        // Shared / legacy operator screens.
        composable(AppRoute.Dashboard) { DashboardScreen(onNavigate = navController::navigate) }
        composable(AppRoute.Manual) { ManualControlScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Autonomous) { AutonomousScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Camera) { CameraScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Diagnostics) { DiagnosticsScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Map) { MapScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Settings) {
            SettingsScreen(
                onBackClick = navController::popBackStack,
                onConnectionClick = { navController.navigate(AppRoute.ConnectionSettings) }
            )
        }
        composable(AppRoute.ConnectionSettings) { ConnectionSettingsScreen(onBackClick = navController::popBackStack) }
        // Operator extras.
        composable(AppRoute.MissionPlanner) { MissionPlannerScreen(onBackClick = navController::popBackStack) }
        composable(AppRoute.Notifications) { NotificationsScreen(onBackClick = navController::popBackStack) }
        // Robot detail placeholder (Diagnostics / Logs / Developer / Maintenance).
        composable(AppRoute.RobotSection) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val subtitle = backStackEntry.arguments?.getString("subtitle") ?: ""
            RobotSectionScreen(title = title, subtitle = subtitle, onBackClick = navController::popBackStack)
        }
    }
}
