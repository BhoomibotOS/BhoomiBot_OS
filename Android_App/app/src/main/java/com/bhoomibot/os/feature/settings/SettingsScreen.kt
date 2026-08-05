package com.bhoomibot.os.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SignalGreen

/**
 * Settings screen — "Control Calibration". Lets the operator tune the manual-control step sizes
 * (drive speed per tap, max speed ceiling, PTO step, hydraulic step) via four sliders, plus a
 * card that navigates on to the VCU connection settings ([onConnectionClick]).
 *
 * Pure UI: it reads/edits values through [SettingsViewModel], which writes straight into the
 * shared in-memory [ControlCalibrationStore] that [ManualViewModel] reads at drive time — so a
 * change made here is felt on the Manual screen right away. [onBackClick] returns to the caller.
 */
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onConnectionClick: () -> Unit = {},
    onLiveLinkClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    // Live calibration values from the SettingsViewModel (updates when any slider changes).
    val calibration by viewModel.calibration.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: back arrow + "Settings / CONTROL CALIBRATION" title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("CONTROL CALIBRATION", color = MutedText, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(20.dp))
        // A card grouping all the manual-control tuning sliders.
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("MANUAL CONTROL STEPS", fontWeight = FontWeight.ExtraBold)
                Text("Defines the amount changed by each manual adjustment.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                // Slider 1: how much speed each digital drive tap adds (1–25 m/s per tap).
                CalibrationSlider("Drive speed", "${calibration.driveStepMetersPerSecond} m/s per tap", calibration.driveStepMetersPerSecond, 1..25, viewModel::setDriveStep)
                // Slider 2: top speed ceiling. Min is the drive-step value so taps can always increase speed (step..50 m/s).
                CalibrationSlider("Maximum speed", "${calibration.maximumSpeedMetersPerSecond} m/s limit", calibration.maximumSpeedMetersPerSecond, calibration.driveStepMetersPerSecond..50, viewModel::setMaximumSpeed)
                // Slider 3: PTO speed increment per step (1–25 %).
                CalibrationSlider("PTO speed", "${calibration.ptoStepPercent}% increments", calibration.ptoStepPercent, 1..25, viewModel::setPtoStep)
                // Slider 4: hydraulic height increment per step (1–25 %).
                CalibrationSlider("Hydraulic height", "${calibration.hydraulicHeightStepPercent}% increments", calibration.hydraulicHeightStepPercent, 1..25, viewModel::setHydraulicStep)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Entry point to the VCU connection configuration screen.
        Card(onClick = onConnectionClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CONNECTION", fontWeight = FontWeight.ExtraBold)
                    Text("Bluetooth / WiFi settings for the VCU.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(">", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Entry point to the internet live-link configuration (relay URL, Robot ID, session code, video quality).
        Card(onClick = onLiveLinkClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LIVE LINK", fontWeight = FontWeight.ExtraBold)
                    Text("Relay URL, Robot ID and session code for the internet live feed.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(">", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// One reusable row: a label + current-value text on the left, and a slider on the right.
// `range` limits the slider; moving it calls onValueChanged with the clamped integer value.
@Composable
private fun CalibrationSlider(label: String, valueLabel: String, value: Int, range: IntRange, onValueChanged: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Bold); Text(valueLabel, color = SignalGreen, style = MaterialTheme.typography.labelSmall) }
        Slider(value = value.toFloat(), onValueChange = { onValueChanged(it.toInt().coerceIn(range.first, range.last)) }, valueRange = range.first.toFloat()..range.last.toFloat(), modifier = Modifier.weight(1f))
    }
}
