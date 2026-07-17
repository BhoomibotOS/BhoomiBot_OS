package com.bhoomibot.os.feature.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.navigation.AppRoute
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.viewmodel.DashboardViewModel

// Legacy operator home screen. It predates OperatorHomeScreen but is still reachable via
// AppRoute.Dashboard. Layout is: header + an expandable robot-status card + a 2-per-row grid of
// feature shortcuts. Unlike the newer role homes (which read static MockRobotData), this screen
// gets its robot status from DashboardViewModel, which in turn reads RobotRepository.status().

// One feature shortcut on the grid: display title + icon + the nav route tapping it opens.
private data class DashboardFeature(val title: String, val icon: ImageVector, val route: String)

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit, viewModel: DashboardViewModel = viewModel()) {
    // The six operation shortcuts shown as tappable cards on the home screen.
    // Each entry pairs a display title + icon with the navigation route it opens.
    val features = listOf(
        DashboardFeature("Manual", Icons.Default.SportsEsports, AppRoute.Manual),         // Manual drive controls
        DashboardFeature("Autonomous", Icons.Default.SmartToy, AppRoute.Autonomous),     // Autonomous (future)
        DashboardFeature("Camera", Icons.Default.CameraAlt, AppRoute.Camera),            // Camera feed (future)
        DashboardFeature("Diagnostics", Icons.Default.Analytics, AppRoute.Diagnostics),  // Diagnostics (future)
        DashboardFeature("Map", Icons.Default.Map, AppRoute.Map),                        // Map (future)
        DashboardFeature("Settings", Icons.Default.Settings, AppRoute.Settings)          // Control calibration
    )

    // Scrollable column holding the whole dashboard layout.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        DashboardHeader()                 // App logo + "BhoomiBot / FIELD OPERATIONS CONSOLE"
        Spacer(Modifier.height(24.dp))
        RobotStatusCard(viewModel.status) // Live robot health card (tap to expand/collapse)
        Spacer(Modifier.height(28.dp))
        // Section label above the operation shortcut grid.
        Text("OPERATIONS", style = MaterialTheme.typography.labelLarge, color = MutedText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // Lay the 6 features out two-per-row (chunked(2)) with a gap between rows.
        features.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { feature ->
                    // Tapping a card navigates to that feature's route.
                    FeatureCard(feature, Modifier.weight(1f)) { onNavigate(feature.route) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Round robot-icon badge on the left of the header.
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(46.dp)) {
            Icon(Icons.Default.SmartToy, "BhoomiBot", tint = SignalGreen, modifier = Modifier.padding(10.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("BhoomiBot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("FIELD OPERATIONS CONSOLE", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RobotStatusCard(status: RobotStatus) {
    // Whether the detail rows (Mode/Mission/GPS/...) are shown. Starts expanded.
    var expanded by remember { mutableStateOf(true) }
    // Animates the battery bar fill from 0→current% smoothly.
    val batteryProgress by animateFloatAsState(
        targetValue = status.batteryPercent.coerceIn(0, 100) / 100f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "batteryProgress"
    )
    // Green when online, red when offline — drives the status dot and icon color.
    val statusColor = if (status.isOnline) SignalGreen else SafetyRed

    // Whole card is tappable to toggle the expanded/collapsed detail section.
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online/offline status icon bubble.
                Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.16f), modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.SmartToy, null, tint = statusColor, modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ROBOT STATUS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Small colored dot: green = ONLINE, red = OFFLINE.
                        Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(7.dp)) {}
                        Spacer(Modifier.width(6.dp))
                        Text(if (status.isOnline) "ONLINE" else "OFFLINE", color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                // Chevron that flips up/down to indicate expand/collapse.
                AnimatedContent(targetState = expanded, label = "statusChevron") { isExpanded ->
                    Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (isExpanded) "Collapse status" else "Expand status", tint = MutedText)
                }
            }
            Spacer(Modifier.height(18.dp))
            // Battery label (left) + percentage (right).
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("BATTERY", style = MaterialTheme.typography.labelMedium, color = MutedText, fontWeight = FontWeight.Bold)
                Text("${status.batteryPercent}%", style = MaterialTheme.typography.titleMedium, color = SignalGreen, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            // The animated battery progress bar.
            LinearProgressIndicator(
                progress = batteryProgress,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = SignalGreen,
                trackColor = MaterialTheme.colorScheme.surface
            )
            // Extra detail rows appear only when the card is expanded (with fade/slide animation).
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(280, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing))
            ) {
                Column(Modifier.padding(top = 18.dp)) {
                    // Each row shows one subsystem's state: Mode, Mission, GPS, Camera, AI.
                    StatusRow("Mode", status.mode); StatusRow("Mission", status.mission)
                    StatusRow("GPS", status.gpsStatus); StatusRow("Camera", status.cameraStatus); StatusRow("AI", status.aiStatus)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    // One label/value line inside the expanded status card (e.g. "GPS   Connected").
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FeatureCard(feature: DashboardFeature, modifier: Modifier, onClick: () -> Unit) {
    // A square shortcut card: icon bubble on top, feature title underneath.
    // Tapping it runs the provided onClick (navigates to the feature).
    Card(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 7.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(14.dp), color = SignalGreen.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                Icon(feature.icon, feature.title, tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Text(feature.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
