package com.bhoomibot.os.feature.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
fun PlaybackScreen(
    missionId: String,
    onBackClick: () -> Unit,
    viewModel: PlaybackViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("MISSION PLAYBACK", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Mission ID: $missionId", style = MaterialTheme.typography.labelSmall)
            
            Spacer(Modifier.height(24.dp))
            
            // Progress Readout
            LinearProgressIndicator(
                progress = if (state.totalSteps > 0) state.currentStepIndex.toFloat() / state.totalSteps else 0f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SignalGreen
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (state.isIntervened) {
                CorrectionAlertCard(
                    onSave = { 
                        // Simplified GPS capture for UI demo
                        viewModel.saveCorrection(0.0, 0.0) 
                    },
                    onDiscard = { /* Reset state */ }
                )
            }
        }
    }
}

@Composable
fun CorrectionAlertCard(onSave: () -> Unit, onDiscard: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SafetyRed.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, SafetyRed)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = SafetyRed)
                Spacer(Modifier.width(8.dp))
                Text("MANUAL INTERVENTION DETECTED", color = SafetyRed, fontWeight = FontWeight.Bold)
            }
            Text("You corrected the robot's movement. Should I learn this new path for the future?", style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)) {
                    Text("LEARN FIX")
                }
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("DISCARD")
                }
            }
        }
    }
}
