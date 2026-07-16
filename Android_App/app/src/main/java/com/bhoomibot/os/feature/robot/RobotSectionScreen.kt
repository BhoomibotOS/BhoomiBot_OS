package com.bhoomibot.os.feature.robot

import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen

// Generic robot-only detail screen (Diagnostics / Logs / Developer / Maintenance).
// The navigation graph passes the section title + subtitle; we render them through the shared
// OperationalScreen shell so every section gets the same back-arrow header and "Module ready" body.
// Each section becomes a real module later without changing this routing.
@Composable
fun RobotSectionScreen(title: String, subtitle: String, onBackClick: () -> Unit) =
    OperationalScreen(title = title, subtitle = subtitle, onBackClick = onBackClick)
