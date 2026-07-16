package com.bhoomibot.os.feature.diagnostics
import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen
// Diagnostics screen (planned for the future).
// Placeholder for system-health / fault monitoring. Shows "Module ready" for now.
@Composable fun DiagnosticsScreen(onBackClick: () -> Unit) = OperationalScreen("Diagnostics", "System health", onBackClick)
