package com.bhoomibot.os.feature.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bhoomibot.os.model.MockRobotData
import com.bhoomibot.os.navigation.AppRoute
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

// Primary home for the handheld OPERATOR phone.
// Shows the robot's live-ish status and a 2-column grid of operator actions that route into the
// shared feature screens (Manual, Mission Planner, Camera, Map, Diagnostics, Notifications, Settings).
// State comes from the static MockRobotData object; swap for a repository feed when ESP32 is wired.
@Composable
fun OperatorHomeScreen(navController: NavController) {
    val status = MockRobotData.robotStatus
    // The operator actions shown as tappable cards. Each pairs a title + icon with its nav route.
    val actions = listOf(
        OperatorAction("Manual", Icons.Default.SportsEsports, AppRoute.Manual),
        OperatorAction("Live View", Icons.Default.Videocam, AppRoute.ConnectionOptions),
        OperatorAction("Mission Planner", Icons.Default.Map, AppRoute.MissionPlanner),
        OperatorAction("Camera", Icons.Default.CameraAlt, AppRoute.Camera),
        OperatorAction("Map", Icons.Default.Map, AppRoute.Map),
        OperatorAction("Diagnostics", Icons.Default.Analytics, AppRoute.Diagnostics),
        OperatorAction("Notifications", Icons.Default.Notifications, AppRoute.Notifications),
        OperatorAction("Autonomous", Icons.Default.SmartToy, AppRoute.Autonomous),
        OperatorAction("Settings", Icons.Default.Settings, AppRoute.Settings)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // App header: robot badge + product name / console label.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.SmartToy, "BhoomiBot", tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("BhoomiBot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("OPERATOR CONSOLE", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        // Compact robot status card (online dot + battery bar + key subsystem read-outs).
        OperatorStatusCard(status.isOnline, status.batteryPercent, status.mode, status.mission, status.gpsStatus)
        Spacer(Modifier.height(28.dp))
        Text("OPERATIONS", style = MaterialTheme.typography.labelLarge, color = MutedText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // 2-per-row action grid with a gap between rows.
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEach { action ->
                    OperatorActionCard(action, Modifier.weight(1f)) { navController.navigate(action.route) }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

// Status card: green/red online dot, animated battery bar, and a few subsystem rows.
@Composable
private fun OperatorStatusCard(isOnline: Boolean, batteryPercent: Int, mode: String, mission: String, gpsStatus: String) {
    val statusColor = if (isOnline) SignalGreen else SafetyRed
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = statusColor.copy(alpha = 0.16f), modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.SmartToy, null, tint = statusColor, modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ROBOT STATUS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(7.dp)) {}
                        Spacer(Modifier.width(6.dp))
                        Text(if (isOnline) "ONLINE" else "OFFLINE", color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text("$batteryPercent%", color = SignalGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { batteryPercent.coerceIn(0, 100) / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = SignalGreen,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(Modifier.height(16.dp))
            StatusLine("Mode", mode); StatusLine("Mission", mission); StatusLine("GPS", gpsStatus)
        }
    }
}

// One label/value line inside the status card (e.g. "GPS   Connected").
@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// A square shortcut card: icon bubble on top, action title underneath. Tapping routes to the feature.
@Composable
private fun OperatorActionCard(action: OperatorAction, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 7.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(shape = RoundedCornerShape(14.dp), color = SignalGreen.copy(alpha = 0.12f), modifier = Modifier.size(42.dp)) {
                Icon(action.icon, action.label, tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Text(action.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// One operator action entry (display title + icon + destination route).
private data class OperatorAction(val label: String, val icon: ImageVector, val route: String)
