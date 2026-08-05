package com.bhoomibot.os.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.feature.autonomous.AutonomyStatusOverlay
import com.bhoomibot.os.feature.autonomous.AutonomyViewModel
import com.bhoomibot.os.feature.camera.CameraScreen
import com.bhoomibot.os.feature.connection.ConnectionOptionsScreen
import com.bhoomibot.os.feature.dashboard.DashboardScreen
import com.bhoomibot.os.feature.diagnostics.DiagnosticsScreen
import com.bhoomibot.os.feature.live.OperatorLiveScreen
import com.bhoomibot.os.feature.live.RobotLiveScreen
import com.bhoomibot.os.feature.manual.ManualControlScreen
import com.bhoomibot.os.feature.map.MapScreen
import com.bhoomibot.os.feature.mission.PlaybackScreen
import com.bhoomibot.os.feature.mission.PlaybackViewModel
import com.bhoomibot.os.feature.autonomous.agent.AgentScreen
import com.bhoomibot.os.feature.autonomous.skills.library.ui.SkillLibraryScreen
import com.bhoomibot.os.feature.onboarding.OnboardingScreen
import com.bhoomibot.os.feature.operator.OperatorHomeScreen
import com.bhoomibot.os.feature.robot.RobotHomeScreen
import com.bhoomibot.os.feature.robot.RobotSectionScreen
import com.bhoomibot.os.feature.settings.SettingsScreen
import com.bhoomibot.os.feature.settings.RobotSettingsScreen
import com.bhoomibot.os.feature.settings.ConnectionSettingsScreen
import com.bhoomibot.os.model.DeviceRole
import kotlinx.coroutines.launch

// Central list of every screen route.
object AppRoute {
    const val Onboarding = "onboarding"
    const val OperatorHome = "operatorHome"
    const val RobotHome = "robotHome"
    const val Dashboard = "dashboard"
    const val Manual = "manual"
    const val Camera = "camera"
    const val Diagnostics = "diagnostics"
    const val Map = "map"
    const val Settings = "settings"
    const val RobotSettings = "robotSettings"
    const val ConnectionSettings = "connectionSettings"
    const val ConnectionOptions = "connectionOptions"
    const val RobotLive = "robotLive"
    const val OperatorLive = "operatorLive"
    const val Playback = "playback/{missionId}"
    const val Notifications = "notifications"
    const val AgentChat = "agentChat"
    const val SkillLibrary = "skillLibrary"
    const val RobotSection = "robotSection/{title}/{subtitle}"
    
    fun robotSection(title: String, subtitle: String) = "robotSection/$title/$subtitle"
    fun playback(missionId: String) = "playback/$missionId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val autonomyViewModel: AutonomyViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val autonomyState by autonomyViewModel.uiState.collectAsState()

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = AppRoute.Onboarding) {
            composable(AppRoute.Onboarding) {
                OnboardingScreen(onRoleSelected = { selected ->
                    scope.launch {
                        DevicePreferences.setRole(context, selected)
                        val dest = if (selected == DeviceRole.OPERATOR) AppRoute.OperatorHome else AppRoute.RobotHome
                        navController.navigate(dest) { popUpTo(AppRoute.Onboarding) { inclusive = true } }
                    }
                })
            }
            composable(AppRoute.OperatorHome) { OperatorHomeScreen(navController) }
            composable(AppRoute.RobotHome) { RobotHomeScreen(navController) }
            composable(AppRoute.Dashboard) { DashboardScreen(onNavigate = navController::navigate) }
            composable(AppRoute.Manual) { ManualControlScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.Camera) { CameraScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.Diagnostics) { DiagnosticsScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.Map) { MapScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.Settings) {
                SettingsScreen(
                    onBackClick = navController::popBackStack,
                    onConnectionClick = { navController.navigate(AppRoute.ConnectionSettings) },
                    onLiveLinkClick = { navController.navigate(AppRoute.ConnectionOptions) }
                )
            }
            composable(AppRoute.ConnectionSettings) { ConnectionSettingsScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.RobotSettings) {
                RobotSettingsScreen(
                    onBackClick = navController::popBackStack,
                    onConnectionClick = { navController.navigate(AppRoute.ConnectionSettings) },
                    onLiveLinkClick = { navController.navigate(AppRoute.ConnectionOptions) }
                )
            }
            composable(AppRoute.ConnectionOptions) {
                ConnectionOptionsScreen(
                    onBackClick = navController::popBackStack,
                    onStart = { role ->
                        val dest = if (role == DeviceRole.ROBOT) AppRoute.RobotLive else AppRoute.OperatorLive
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
            composable(AppRoute.Playback) { backStackEntry ->
                val missionId = backStackEntry.arguments?.getString("missionId") ?: ""
                val viewModel: PlaybackViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                PlaybackScreen(
                    missionId = missionId,
                    viewModel = viewModel,
                    onBackClick = navController::popBackStack
                )
            }
            composable(AppRoute.Notifications) { NotificationsScreen(onBackClick = navController::popBackStack) }
            composable(AppRoute.AgentChat) { 
                AgentScreen(
                    onBackClick = navController::popBackStack,
                    onNavigateToManual = { navController.navigate(AppRoute.Manual) },
                    onNavigateToPlayback = { missionId -> navController.navigate(AppRoute.playback(missionId)) },
                    onNavigateToLibrary = { navController.navigate(AppRoute.SkillLibrary) }
                )
            }
            composable(AppRoute.SkillLibrary) {
                SkillLibraryScreen(onBackClick = navController::popBackStack)
            }
            composable(AppRoute.RobotSection) { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val subtitle = backStackEntry.arguments?.getString("subtitle") ?: ""
                RobotSectionScreen(title = title, subtitle = subtitle, onBackClick = navController::popBackStack)
            }
        }

        AutonomyStatusOverlay(
            state = autonomyState.state,
            alert = autonomyState.activeAlert,
            onClearError = { autonomyViewModel.clearError() },
            onEmergencyStop = { autonomyViewModel.onEmergencyStop() },
            onDismissAlert = { autonomyViewModel.dismissAlert() }
        )
    }
}
