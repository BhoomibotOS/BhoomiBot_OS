package com.bhoomibot.os.feature.mission

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
fun PlaybackScreen(
    onBack: () -> Unit,
    viewModel: PlaybackViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text(state.missionName.ifEmpty { "Mission Replay" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        // Mission Stats
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("STATUS", style = MaterialTheme.typography.labelSmall)
                Text(if (state.isActive) "EXECUTING" else "IDLE", color = if (state.isActive) SignalGreen else Color.Gray, fontWeight = FontWeight.Black)
                
                if (state.steps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("PLAN STEPS", style = MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(state.steps) { step ->
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = SignalGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${step.skillId} ${step.parameters}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("REMAINING STEPS", style = MaterialTheme.typography.labelSmall)
                    Text("${state.remainingWaypoints}", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Controls
        Button(
            onClick = { if (state.isActive) viewModel.stopMission() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Icon(Icons.Default.Stop, null)
            Spacer(Modifier.width(8.dp))
            Text("EMERGENCY STOP")
        }
    }
}
