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
import com.bhoomibot.os.feature.connection.ConnectionOptionsScreen
import com.bhoomibot.os.feature.dashboard.DashboardScreen
import com.bhoomibot.os.feature.diagnostics.DiagnosticsScreen
import com.bhoomibot.os.feature.live.OperatorLiveScreen
import com.bhoomibot.os.feature.live.RobotLiveScreen
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

// The app's navigation module. This single file owns BOTH the list of route strings (AppRoute)
// and the NavHost graph (AppNavigation) that maps each route to the composable screen it shows.
// The app is single-Activity: MainActivity hosts AppNavigation(), which swaps screens in-place
// instead of launching new Activities. New screens are added by (1) adding a route constant to
// AppRoute and (2) adding a matching composable(...) entry in the NavHost below.

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
    const val ConnectionOptions = "connectionOptions"
    const val RobotLive = "robotLive"
    const val OperatorLive = "operatorLive"
    const val MissionPlanner = "missionPlanner"
    const val Notifications = "notifications"
    // A parameterized route: {title}/{subtitle} are path arguments filled in at navigation time.
    // Use the robotSection(...) helper to build a concrete route (e.g. "robotSection/Logs/System event log");
    // the composable(AppRoute.RobotSection) block below reads those arguments back out.
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
        // Live internet link: configure the relay, then the robot/operator go live.
        composable(AppRoute.ConnectionOptions) {
            ConnectionOptionsScreen(
                onBackClick = navController::popBackStack,
                onStart = { role ->
                    val dest = if (role == DeviceRole.ROBOT) AppRoute.RobotLive else AppRoute.OperatorLive
                    android.util.Log.d(
                        "BhoomiBotRelay",
                        "[GUI] onStart(role=$role) -> navigating to ${if (role == DeviceRole.ROBOT) "RobotLive" else "OperatorLive"}"
                    )
                    navController.navigate(dest) { popUpTo(AppRoute.ConnectionOptions) { inclusive = true } }
                }
            )
        }
        composable(AppRoute.RobotLive) {
            RobotLiveScreen(
                onBackClick = navController::popBackStack,
                onOpenOptions = { navController.navigate(AppRoute.ConnectionOptions) }
            )
        }
        composable(AppRoute.OperatorLive) {
            OperatorLiveScreen(
                onBackClick = navController::popBackStack,
                onOpenOptions = { navController.navigate(AppRoute.ConnectionOptions) }
            )
        }
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
