package com.bhoomibot.os.feature.robot

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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SmartToy
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

// Primary home for the ROBOT-MOUNTED phone (the on-board computer).
// Surfaces the robot's own system health (uptime, CPU temp, battery, mission progress, motor/hydraulic
// health, connection) and links into AI/Vision, Diagnostics, and the robot-only detail sections
// (Logs / Developer / Maintenance). State comes from MockRobotData.systemStatus.
@Composable
fun RobotHomeScreen(navController: NavController) {
    val sys = MockRobotData.systemStatus
    // Robot-only actions. Logs/Developer/Maintenance open the generic RobotSection detail screen.
    val actions = listOf(
        RobotAction("Settings", Icons.Default.Settings, "VCU connection and live-link options", AppRoute.RobotSettings),
        RobotAction("Skill Library", Icons.Default.PlaylistPlay, "Manage & replay validated skills", AppRoute.SkillLibrary),
        RobotAction("AI & Vision", Icons.Default.CameraAlt, "AI inference + live perception", AppRoute.Camera),
        RobotAction("Diagnostics", Icons.Default.Analytics, "Subsystem health + faults", AppRoute.Diagnostics),
        RobotAction("Logs", Icons.Default.ReceiptLong, "System event log", AppRoute.robotSection("Logs", "System event log")),
        RobotAction("Developer", Icons.Default.Code, "Developer tools + console", AppRoute.robotSection("Developer", "Developer tools")),
        RobotAction("Maintenance", Icons.Default.Build, "Service + maintenance", AppRoute.robotSection("Maintenance", "Maintenance schedule"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Header: robot badge + product name / console label.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.SmartToy, "BhoomiBot", tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("BhoomiBot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text("ROBOT CONSOLE", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        // System vitals: uptime, CPU temp, connection + signal bars, current mode.
        SystemVitalsCard(sys.systemUptime, sys.cpuTempC, sys.connectionType, sys.signalStrength, sys.currentMode)
        Spacer(Modifier.height(14.dp))
        // Active mission progress.
        MissionProgressCard(sys.missionName, sys.missionProgress)
        Spacer(Modifier.height(14.dp))
        // Battery + motor/hydraulic health side by side.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BatteryCard(sys.batteryPercent, Modifier.weight(1f))
            HealthCard("Motor", sys.motorHealth, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        HealthCard("Hydraulic", sys.hydraulicHealth, Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text("ON-BOARD", style = MaterialTheme.typography.labelLarge, color = MutedText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        // Robot action cards (single column, full-width with description).
        actions.forEach { action ->
            RobotActionCard(action) { navController.navigate(action.route) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// System vitals card: uptime, CPU temp, connection type with signal bars, current mode.
@Composable
private fun SystemVitalsCard(uptime: String, cpuTempC: Int, connection: String, signal: Int, mode: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("SYSTEM", style = MaterialTheme.typography.labelMedium, color = MutedText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            VitalRow("Uptime", uptime)
            VitalRow("CPU Temp", "$cpuTempC°C")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Signal", color = MutedText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                // Four bars; filled up to `signal` (0-4).
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { i -> Surface(shape = RoundedCornerShape(2.dp), color = if (i < signal) SignalGreen else MaterialTheme.colorScheme.surface, modifier = Modifier.size(width = 10.dp, height = (8 + i * 4).dp)) {} }
                }
                Spacer(Modifier.width(8.dp))
                Text(connection, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
            VitalRow("Mode", mode)
        }
    }
}

// One label/value line inside the vitals card.
@Composable
private fun VitalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// Active mission card: name + progress bar + percentage.
@Composable
private fun MissionProgressCard(missionName: String, progress: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("ACTIVE MISSION", style = MaterialTheme.typography.labelMedium, color = MutedText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(missionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = SignalGreen, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("$progress% complete", color = SignalGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// Battery card: percentage + progress bar.
@Composable
private fun BatteryCard(percent: Int, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("BATTERY", style = MaterialTheme.typography.labelMedium, color = MutedText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("$percent%", color = SignalGreen, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { percent.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = SignalGreen, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

// Health card: shows a subsystem's health label, green when "Nominal" else amber/red.
@Composable
private fun HealthCard(label: String, health: String, modifier: Modifier) {
    val nominal = health.equals("Nominal", ignoreCase = true)
    val color = if (nominal) SignalGreen else SafetyRed
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.16f), modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Build, null, tint = color, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MutedText, fontWeight = FontWeight.Bold)
                Text(health, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

// A full-width robot action card: icon + title + description; tapping routes to the destination.
@Composable
private fun RobotActionCard(action: RobotAction, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = SignalGreen.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) {
                Icon(action.icon, action.label, tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(action.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(action.description, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// One robot action entry (title, icon, description, destination route).
private data class RobotAction(val label: String, val icon: ImageVector, val description: String, val route: String)
