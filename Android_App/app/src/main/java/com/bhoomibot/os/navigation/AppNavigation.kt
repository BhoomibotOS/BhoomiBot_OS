package com.bhoomibot.os.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bhoomibot.os.feature.autonomous.agent.AgentScreen
import com.bhoomibot.os.feature.dashboard.DashboardScreen
import com.bhoomibot.os.feature.manual.ManualControlScreen
import com.bhoomibot.os.feature.mission.PlaybackScreen
import com.bhoomibot.os.feature.settings.SettingsScreen
import com.bhoomibot.os.feature.autonomous.skills.library.ui.SkillLibraryScreen
import com.bhoomibot.os.feature.onboarding.OnboardingScreen
import com.bhoomibot.os.feature.camera.CameraScreen
import com.bhoomibot.os.feature.diagnostics.DiagnosticsScreen
import com.bhoomibot.os.feature.live.OperatorLiveScreen
import com.bhoomibot.os.feature.settings.RobotSettingsScreen
import com.bhoomibot.os.feature.robot.RobotSectionScreen
import com.bhoomibot.os.feature.operator.OperatorHomeScreen
import com.bhoomibot.os.feature.robot.RobotHomeScreen
import com.bhoomibot.os.feature.settings.ConnectionSettingsScreen
import com.bhoomibot.os.feature.connection.ConnectionOptionsScreen
import com.bhoomibot.os.model.DeviceRole
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = AppRoute.Onboarding) {
        composable(AppRoute.Onboarding) {
            OnboardingScreen(onRoleSelected = { role ->
                when (role) {
                    DeviceRole.OPERATOR -> navController.navigate(AppRoute.OperatorHome)
                    DeviceRole.ROBOT -> navController.navigate(AppRoute.RobotHome)
                }
            })
        }
        composable(AppRoute.OperatorHome) {
            OperatorHomeScreen(navController = navController)
        }
        composable(AppRoute.RobotHome) {
            RobotHomeScreen(navController = navController)
        }
        composable(AppRoute.Dashboard) {
            DashboardScreen(
                onNavigateToAgent = { navController.navigate(AppRoute.AgentChat) },
                onNavigateToManual = { navController.navigate(AppRoute.Manual) },
                onNavigateToSettings = { navController.navigate(AppRoute.Settings) }
            )
        }
        composable(AppRoute.AgentChat) {
            AgentScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToManual = { navController.navigate(AppRoute.Manual) },
                onNavigateToPlayback = { missionId, plan -> 
                    navController.navigate(AppRoute.playback(missionId)) 
                }
            )
        }
        composable(AppRoute.Manual) {
            ManualControlScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppRoute.Settings) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onConnectionClick = { navController.navigate(AppRoute.ConnectionSettings) },
                onLiveLinkClick = { navController.navigate(AppRoute.LiveLinkSettings) }
            )
        }
        composable("playback/{missionId}") {
            PlaybackScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoute.SkillLibrary) {
            SkillLibraryScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoute.OperatorLive) {
            OperatorLiveScreen(
                onBackClick = { navController.popBackStack() },
                onOpenOptions = { /* Handle options */ }
            )
        }
        composable(AppRoute.Diagnostics) {
            DiagnosticsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppRoute.Notifications) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppRoute.RobotSettings) {
            RobotSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onConnectionClick = { navController.navigate(AppRoute.ConnectionSettings) },
                onLiveLinkClick = { navController.navigate(AppRoute.LiveLinkSettings) }
            )
        }
        composable(AppRoute.Camera) {
            CameraScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppRoute.ConnectionSettings) {
            ConnectionSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(AppRoute.LiveLinkSettings) {
            ConnectionOptionsScreen(
                onBackClick = { navController.popBackStack() },
                onStart = { role ->
                    // Actually navigate and start the link based on the chosen role
                    if (role == DeviceRole.ROBOT) {
                        navController.navigate(AppRoute.RobotHome) {
                            popUpTo(AppRoute.LiveLinkSettings) { inclusive = true }
                        }
                    } else {
                        navController.navigate(AppRoute.OperatorLive) {
                            popUpTo(AppRoute.LiveLinkSettings) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(
            route = "robot_section/{title}/{description}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("description") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", StandardCharsets.UTF_8.toString())
            val description = URLDecoder.decode(backStackEntry.arguments?.getString("description") ?: "", StandardCharsets.UTF_8.toString())
            RobotSectionScreen(title, description, onBackClick = { navController.popBackStack() })
        }
    }
}
