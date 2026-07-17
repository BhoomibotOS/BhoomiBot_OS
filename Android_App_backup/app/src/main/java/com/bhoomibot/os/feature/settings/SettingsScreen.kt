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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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

@Composable
fun SettingsScreen(onBackClick: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val calibration by viewModel.calibration.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("CONTROL CALIBRATION", color = MutedText, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Text("MANUAL CONTROL STEPS", fontWeight = FontWeight.ExtraBold)
                Text("Defines the amount changed by each manual adjustment.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(14.dp))
                CalibrationSlider("Drive speed", "${calibration.driveStepMetersPerSecond} m/s per tap", calibration.driveStepMetersPerSecond, 1..25, viewModel::setDriveStep)
                CalibrationSlider("Maximum speed", "${calibration.maximumSpeedMetersPerSecond} m/s limit", calibration.maximumSpeedMetersPerSecond, calibration.driveStepMetersPerSecond..50, viewModel::setMaximumSpeed)
                CalibrationSlider("PTO speed", "${calibration.ptoStepPercent}% increments", calibration.ptoStepPercent, 1..25, viewModel::setPtoStep)
                CalibrationSlider("Hydraulic height", "${calibration.hydraulicHeightStepPercent}% increments", calibration.hydraulicHeightStepPercent, 1..25, viewModel::setHydraulicStep)
            }
        }
    }
}

@Composable
private fun CalibrationSlider(label: String, valueLabel: String, value: Int, range: IntRange, onValueChanged: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Bold); Text(valueLabel, color = SignalGreen, style = MaterialTheme.typography.labelSmall) }
        Slider(value = value.toFloat(), onValueChange = { onValueChanged(it.toInt().coerceIn(range.first, range.last)) }, valueRange = range.first.toFloat()..range.last.toFloat(), modifier = Modifier.weight(1f))
    }
}
