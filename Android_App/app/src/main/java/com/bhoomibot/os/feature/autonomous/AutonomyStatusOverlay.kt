package com.bhoomibot.os.feature.autonomous

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

/**
 * Shared UI component that displays the current autonomy status on top of any screen.
 */
@Composable
fun AutonomyStatusOverlay(
    state: AutonomyState,
    alert: Pair<String, String>? = null,
    onClearError: () -> Unit,
    onEmergencyStop: () -> Unit,
    onDismissAlert: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ... (Banner and SafetyOverlay)
        
        // Incoming Alert Dialog
        if (alert != null && state != AutonomyState.EMERGENCY_STOP && state != AutonomyState.ERROR) {
            AlertDialog(
                onDismissRequest = onDismissAlert,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SafetyRed) },
                title = { Text("Robot Assistance Required") },
                text = { Text(alert.first) },
                confirmButton = {
                    Button(
                        onClick = { 
                            onEmergencyStop()
                            onDismissAlert()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)
                    ) {
                        Text("EMERGENCY STOP")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissAlert) {
                        Text("ACKNOWLEDGE")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusBanner(state: AutonomyState) {
    val (color, text) = when (state) {
        AutonomyState.RECORDING -> SafetyRed to "RECORDING MISSION"
        AutonomyState.EXECUTING -> SignalGreen to "EXECUTING MISSION"
        AutonomyState.PAUSED -> Color(0xFFFFB300) to "MISSION PAUSED"
        else -> Color.Gray to ""
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == AutonomyState.RECORDING) {
                // Pulse dot would go here, using simple Box for now
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (state == AutonomyState.PAUSED) Color.Black else Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun SafetyOverlay(
    state: AutonomyState,
    onClear: () -> Unit,
    onEmergencyStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SafetyRed.copy(alpha = 0.9f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (state == AutonomyState.EMERGENCY_STOP) "EMERGENCY STOP" else "SYSTEM ERROR",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "The robot has been halted for safety.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CLEAR & RESET SYSTEM", color = SafetyRed, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onEmergencyStop,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RE-ASSERT E-STOP", fontWeight = FontWeight.Bold)
            }
        }
    }
}
