package com.bhoomibot.os.feature.map
import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen
// Map / location awareness screen (planned for the future).
// Placeholder for GPS position, field map and path planning. Shows "Module ready" for now.
@Composable fun MapScreen(onBackClick: () -> Unit) = OperationalScreen("Map", "Location awareness", onBackClick)
