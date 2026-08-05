package com.bhoomibot.os.feature.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.vcu.ConnectionType

/**
 * Lets the operator configure how the phone talks to the ESP32/VCU: connection mode (Bluetooth,
 * WiFi hotspot, or auto), the Bluetooth MAC, the WiFi host/port, auto-reconnect, and the connect
 * timeout. Changes are held in the ViewModel and only written to DataStore when "Save" is tapped.
 */
@Composable
fun ConnectionSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: ConnectionSettingsViewModel = viewModel()
) {
    val prefs by viewModel.prefs.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    
    // True when the selected mode needs the Bluetooth MAC and/or the WiFi host+port.
    val needsBluetooth = prefs.connectionType != ConnectionType.WIFI_HOTSPOT
    val needsWifi = prefs.connectionType != ConnectionType.BLUETOOTH

    val context = LocalContext.current
    // Launches the Bluetooth permission dialog.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.toggleConnection()
        } else {
            Toast.makeText(context, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: back arrow + title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("CONNECTION", color = MutedText, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Mode selector.
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("CONNECTION MODE", fontWeight = FontWeight.ExtraBold)
                    Text("How the phone reaches the VCU.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    ConnectionTypeSelector(
                        selected = prefs.connectionType,
                        onSelected = viewModel::setConnectionType
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Bluetooth MAC (hidden when WiFi-only mode is selected).
            if (needsBluetooth) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("BLUETOOTH", fontWeight = FontWeight.ExtraBold)
                        Text("MAC address of the ESP32 (e.g. AA:BB:CC:DD:EE:FF).", color = MutedText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = prefs.bluetoothMacAddress,
                            onValueChange = viewModel::setBluetoothMac,
                            label = { Text("Bluetooth MAC address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // WiFi host + port (hidden when Bluetooth-only mode is selected).
            if (needsWifi) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("WIFI HOTSPOT", fontWeight = FontWeight.ExtraBold)
                        Text("IP and port of the ESP32 when connected to the phone hotspot.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = prefs.wifiHost,
                            onValueChange = viewModel::setWifiHost,
                            label = { Text("Host (IP address)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = prefs.wifiPort.toString(),
                            // Strip any non-digits, then only push a valid number to the VM (ignores empty/garbage input).
                            onValueChange = { it.filter { c -> c.isDigit() }.toIntOrNull()?.let(viewModel::setWifiPort) },
                            label = { Text("Port") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Auto-reconnect + connect timeout.
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("AUTO RECONNECT", fontWeight = FontWeight.Bold)
                            Text("Re-establish the link if it drops.", color = MutedText, style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(checked = prefs.isAutoReconnect, onCheckedChange = viewModel::setAutoReconnect)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("CONNECT TIMEOUT: ${prefs.connectionTimeoutMs} ms", fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = prefs.connectionTimeoutMs.toFloat(),
                        onValueChange = { viewModel.setConnectionTimeout(it.toInt()) },
                        valueRange = 500f..10000f,
                        steps = 19
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // Active Connection Control
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("HARDWARE LINK", fontWeight = FontWeight.ExtraBold)
                    Text("Establish a live connection to the robot hardware.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            // Permission handling (Android 12+)
                            val missing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                mutableListOf<String>().apply {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.BLUETOOTH_CONNECT)
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.BLUETOOTH_SCAN)
                                }
                            } else { mutableListOf() }

                            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
                            else viewModel.toggleConnection()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isConnected) SafetyRed else SignalGreen,
                            contentColor = Color.White
                        )
                    ) { 
                        Text(
                            if (isConnected) "DISCONNECT" else "CONNECT", 
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ) 
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape, 
                            color = if (isConnected) SignalGreen else MutedText, 
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isConnected) "Robot is ONLINE" else "Robot is OFFLINE", 
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isConnected) SignalGreen else MutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Save: persist to DataStore, then return to Settings.
            Button(
                onClick = { viewModel.save(onSaved = onBackClick) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text("SAVE CONNECTION", fontWeight = FontWeight.Bold) }
        }
    }
}

// Three-way segmented control for the connection mode. The selected option is highlighted green.
@Composable
private fun ConnectionTypeSelector(selected: ConnectionType, onSelected: (ConnectionType) -> Unit) {
    val options = ConnectionType.values()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { type ->
            Button(
                onClick = { onSelected(type) },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == selected) SignalGreen else MaterialTheme.colorScheme.surface,
                    contentColor = if (type == selected) MaterialTheme.colorScheme.onPrimary else MutedText
                ),
                elevation = null
            ) { Text(type.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
        }
    }
}
