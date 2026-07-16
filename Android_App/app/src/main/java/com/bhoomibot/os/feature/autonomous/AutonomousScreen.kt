package com.bhoomibot.os.feature.autonomous
import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen
// Autonomous mode screen (planned for the future).
// Placeholder for mission planning, AI navigation and auto-control. Shows "Module ready" for now.
@Composable fun AutonomousScreen(onBackClick: () -> Unit) = OperationalScreen("Autonomous", "Mission control", onBackClick)
