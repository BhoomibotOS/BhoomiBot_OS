package com.bhoomibot.os.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.WarningAmber

// Mission Planner screen (operator extra). Lets the operator assemble a field mission from mock
// options (farm, robot, mission type, attachment, speed) and shows an estimated duration plus
// Start / Pause / Resume / Cancel transport controls. All state is local (remember); no real
// backend yet — wire it to a MissionRepository when the planner service exists.
@Composable
fun MissionPlannerScreen(onBackClick: () -> Unit) {
    // Mock selection catalogs.
    val farms = listOf("Field A", "Field B", "Orchard North")
    val robots = listOf("BhoomiBot-01", "BhoomiBot-02")
    val missions = listOf("Spraying", "Seeding", "Weeding", "Mapping")
    val attachments = listOf("Sprayer", "Seeder", "Weeder", "None")

    // Local UI state.
    var selectedFarm by remember { mutableStateOf(farms.first()) }
    var selectedRobot by remember { mutableStateOf(robots.first()) }
    var selectedMission by remember { mutableStateOf(missions.first()) }
    var selectedAttachment by remember { mutableStateOf(attachments.first()) }
    var speed by remember { mutableIntStateOf(50) }
    var plannerState by remember { mutableStateOf(PlannerState.IDLE) }

    // Estimated time scales inversely with speed (mock: 120 min base at 50% speed).
    val eta = if (speed <= 0) "—" else run {
        val minutes = (120 * 50) / speed
        val h = minutes / 60
        val m = minutes % 60
        if (h > 0) "$h h ${m}m" else "${m}m"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Top bar: back arrow + title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Mission Planner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("PLAN FIELD OPERATION", color = MutedText, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(20.dp))

        // Selection groups.
        SelectorGroup("FARM", farms, selectedFarm) { selectedFarm = it }
        Spacer(Modifier.height(16.dp))
        SelectorGroup("ROBOT", robots, selectedRobot) { selectedRobot = it }
        Spacer(Modifier.height(16.dp))
        SelectorGroup("MISSION", missions, selectedMission) { selectedMission = it }
        Spacer(Modifier.height(16.dp))
        SelectorGroup("ATTACHMENT", attachments, selectedAttachment) { selectedAttachment = it }
        Spacer(Modifier.height(20.dp))

        // Speed card.
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SPEED", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("$speed%", color = SignalGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Slider(value = speed.toFloat(), onValueChange = { speed = it.toInt() }, valueRange = 0f..100f)
            }
        }
        Spacer(Modifier.height(16.dp))

        // Estimated time card.
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ESTIMATED TIME", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(eta, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f)) {
                    Text("STATUS", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(plannerState.label, color = plannerState.color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Progress bar reflecting planner state.
        Spacer(Modifier.height(14.dp))
        val progress = when (plannerState) {
            PlannerState.IDLE -> 0f
            PlannerState.RUNNING -> 0.55f
            PlannerState.PAUSED -> 0.55f
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = SignalGreen, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(20.dp))

        // Transport controls depend on the current planner state.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (plannerState) {
                PlannerState.IDLE -> {
                    ActionButton("Start", Icons.Default.PlayArrow, SignalGreen, Color(0xFF062112), Modifier.weight(1f)) { plannerState = PlannerState.RUNNING }
                }
                PlannerState.RUNNING -> {
                    ActionButton("Pause", Icons.Default.Pause, WarningAmber, Color(0xFF2A1C00), Modifier.weight(1f)) { plannerState = PlannerState.PAUSED }
                    ActionButton("Cancel", Icons.Default.Stop, SafetyRed, Color.White, Modifier.weight(1f)) { plannerState = PlannerState.IDLE }
                }
                PlannerState.PAUSED -> {
                    ActionButton("Resume", Icons.Default.Replay, SignalGreen, Color(0xFF062112), Modifier.weight(1f)) { plannerState = PlannerState.RUNNING }
                    ActionButton("Cancel", Icons.Default.Stop, SafetyRed, Color.White, Modifier.weight(1f)) { plannerState = PlannerState.IDLE }
                }
            }
        }
    }
}

// A labeled row of selectable chips (e.g. farm names). Tapping a chip selects it.
@Composable
private fun SelectorGroup(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SignalGreen.copy(alpha = 0.18f),
                        selectedLabelColor = SignalGreen
                    )
                )
            }
        }
    }
}

// A full-width action button with icon + label.
@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, containerColor: Color, contentColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(52.dp), shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)) {
        Icon(icon, null)
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.ExtraBold)
    }
}

// Lifecycle of a planned mission from the operator's perspective.
private enum class PlannerState(val label: String, val color: Color) {
    IDLE("Not started", MutedText),
    RUNNING("Running", SignalGreen),
    PAUSED("Paused", WarningAmber)
}
