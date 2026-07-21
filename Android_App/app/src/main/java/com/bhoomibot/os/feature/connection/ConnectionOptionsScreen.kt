package com.bhoomibot.os.feature.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

/**
 * The "connection options in the GUI" screen for the internet live link.
 * Lets the operator/robot set the relay server URL, the shared Robot ID +
 * session code, video quality, and auto-reconnect. Saved values are used by
 * the live screens, which connect automatically.
 */
@Composable
fun ConnectionOptionsScreen(
    onBackClick: () -> Unit,
    onStart: (DeviceRole) -> Unit,
    viewModel: ConnectionOptionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Top bar: back + title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column {
                Text("Connection Options", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("LIVE LINK", color = MutedText, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Role (set at onboarding; shown for clarity).
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state.role == DeviceRole.ROBOT) Icons.Default.SmartToy else Icons.Default.Videocam,
                        null, tint = SignalGreen, modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("ROLE", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.role == DeviceRole.ROBOT) "Robot (broadcasts camera + telemetry)"
                            else "Operator (watches the live feed)",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("PHONE NETWORKS", fontWeight = FontWeight.ExtraBold)
                    Text("Are the robot and operator phones on the same network?", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    PhoneNetworkMode.values().forEach { mode ->
                        Button(
                            onClick = { viewModel.setNetworkMode(mode) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mode == state.networkMode) SignalGreen else MaterialTheme.colorScheme.surface,
                                contentColor = if (mode == state.networkMode) MaterialTheme.colorScheme.onPrimary else MutedText
                            )
                        ) { Text(mode.title, fontWeight = FontWeight.Bold) }
                        if (mode == state.networkMode) {
                            Spacer(Modifier.height(6.dp))
                            Text(mode.description, color = MutedText, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Relay server URL.
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("RELAY SERVER", fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (state.networkMode == PhoneNetworkMode.INTERNET) {
                            "Different networks uses the secure Render address: wss://bhoomibot-os.onrender.com"
                        } else {
                            "For a local relay, use ws://<phone-or-computer-IP>:8080. A secure wss:// URL also works."
                        },
                        color = MutedText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Use the relay's secure internet URL (wss://…). Use ws:// only on a trusted local network.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = viewModel::setServerUrl,
                        label = { Text("Server URL (ws://…)") },
                        singleLine = true,
                        isError = state.serverUrlError != null,
                        leadingIcon = { Icon(Icons.Default.Wifi, null) },
                        supportingText = state.serverUrlError?.let { error -> { Text(error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Quick-pick previously used relay URLs so the user doesn't
                    // have to retype the Render URL every time.
                    if (state.recentServerUrls.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Recent", color = MutedText, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.recentServerUrls.forEach { url ->
                                SuggestionChip(
                                    onClick = { viewModel.selectRecentServerUrl(url) },
                                    label = { Text(url, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Robot ID + session code (the shared "meet" keys).
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("SHARED KEYS", fontWeight = FontWeight.ExtraBold)
                    Text("Type the SAME Robot ID + session code on both phones.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.robotId,
                        onValueChange = viewModel::setRobotId,
                        label = { Text("Robot ID") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Sensors, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.sessionCode,
                        onValueChange = viewModel::setSessionCode,
                        label = { Text("Session code") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Videocam, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Auto-reconnect.
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AUTO RECONNECT", fontWeight = FontWeight.Bold)
                        Text("Re-establish the link if it drops.", color = MutedText, style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = state.autoReconnect, onCheckedChange = viewModel::setAutoReconnect)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Only the robot-mounted phone encodes and sends video. Keeping these
            // controls there avoids showing the operator settings that cannot affect
            // the remote camera.
            if (state.role == DeviceRole.ROBOT) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("VIDEO QUALITY", fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoQuality.values().forEach { q ->
                            Button(
                                onClick = { viewModel.setVideoQuality(q) },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (q == state.videoQuality) SignalGreen else MaterialTheme.colorScheme.surface,
                                    contentColor = if (q == state.videoQuality) MaterialTheme.colorScheme.onPrimary else MutedText
                                ),
                                elevation = null
                                // Show only the first word of the quality label (e.g. "Medium" from "Medium (…)") to fit the button.
                            ) { Text(q.label.substringBefore(" "), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("FRAME RATE: ${state.videoFps} fps", fontWeight = FontWeight.Bold)
                    Slider(
                        value = state.videoFps.toFloat(),
                        onValueChange = { viewModel.setVideoFps(it.toInt()) },
                        valueRange = 1f..30f, steps = 29
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            } else {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("REMOTE VIDEO", fontWeight = FontWeight.ExtraBold)
                        Text("Video quality is controlled by the robot-mounted phone.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Save + start.
            Button(
                onClick = {
                    android.util.Log.d(
                        "BhoomiBotRelay",
                        "[GUI] SAVE&START clicked — serverUrl='${state.serverUrl}' " +
                            "normalized='${state.normalizedServerUrl}' robotId='${state.robotId}' " +
                            "session='${state.sessionCode}' role=${state.role} " +
                            "networkMode=${state.networkMode} canStart=${state.canStart}"
                    )
                    // Visible on-screen confirmation (no terminal needed).
                    Toast.makeText(
                        context,
                        "Connecting to: ${state.normalizedServerUrl}",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.save { onStart(state.role) }
                },
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("SAVE & START LIVE", fontWeight = FontWeight.Bold) }

            if (state.saved) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, null, tint = SignalGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("Saved. Connecting…", color = SignalGreen, fontWeight = FontWeight.Bold)
                }
            }
            if (!state.canStart) {
                Spacer(Modifier.height(10.dp))
                Text("Enter a server URL, a Robot ID and a session code to continue.", color = SafetyRed, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
