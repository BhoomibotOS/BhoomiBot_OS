package com.bhoomibot.os.feature.connection

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.ui.components.QrScannerDialog
import com.bhoomibot.os.ui.components.RobotPairingDialog
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
private fun HotspotInstructionCard() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("SETTING UP HOTSPOT", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = SignalGreen)
            Spacer(Modifier.height(8.dp))
            InstructionRow("1", "Pull down Android notification bar")
            InstructionRow("2", "Turn ON 'Mobile Hotspot'")
            InstructionRow("3", "Connect Operator Phone to this Wi-Fi")
        }
    }
}

@Composable
private fun InstructionRow(num: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Surface(
            Modifier.size(16.dp),
            shape = CircleShape,
            color = SignalGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(num, color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConnectionOptionsScreen(
    onBackClick: () -> Unit,
    onStart: (DeviceRole) -> Unit,
    viewModel: ConnectionOptionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showQrDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column {
                Text("Connection Options", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("LIVE LINK CONFIGURATION", color = MutedText, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.verticalScroll(rememberScrollState())) {
            // 0. Role Selection (Robot vs Operator)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("DEVICE ROLE", fontWeight = FontWeight.ExtraBold)
                    Text("Is this phone on the Robot or in your hand?", color = MutedText, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeviceRole.values().forEach { role ->
                            Button(
                                onClick = { viewModel.setRole(role) },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (role == state.role) SignalGreen else MaterialTheme.colorScheme.surface,
                                    contentColor = if (role == state.role) Color.Black else MutedText
                                )
                            ) { Text(role.name, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 1. Network Selection (Same Network vs Different Network)

            // 2. Local Hub (Only for Robot + Hotspot Mode)
            if (state.networkMode == PhoneNetworkMode.HOTSPOT && state.role == DeviceRole.ROBOT) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("LOCAL HUB SERVER", fontWeight = FontWeight.ExtraBold, color = SignalGreen)
                        Text("Act as the central router for the operator.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Status: ${if (state.localHubActive) "RUNNING" else "STOPPED"}", fontWeight = FontWeight.Bold)
                                Text("Server IP: ${state.localIpAddress}", color = MutedText, style = MaterialTheme.typography.labelSmall)
                            }
                            Switch(checked = state.localHubActive, onCheckedChange = { viewModel.toggleLocalHub() })
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Shortcut to System Hotspot Settings
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_MAIN).apply {
                                    setClassName("com.android.settings", "com.android.settings.TetherSettings")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ENABLE SYSTEM HOTSPOT", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        if (state.localHubActive) {
                            Spacer(Modifier.height(12.dp))
                            HotspotInstructionCard()
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showQrDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.QrCode, null)
                                Spacer(Modifier.width(8.dp))
                                Text("SHOW PAIRING QR")
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Instruct the Operator to type: ws://${state.localIpAddress}:8080", color = SignalGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 3. Server Configuration (Hidden in Hotspot Mode for Robot)
            if (!(state.networkMode == PhoneNetworkMode.HOTSPOT && state.role == DeviceRole.ROBOT)) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("RELAY CONFIGURATION", fontWeight = FontWeight.ExtraBold)
                                Text(
                                    if (state.networkMode == PhoneNetworkMode.INTERNET) "Public Internet Relay" else "Local Network Path",
                                    color = MutedText, style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (state.networkMode == PhoneNetworkMode.HOTSPOT) {
                                IconButton(onClick = { showScanner = true }) {
                                    Icon(Icons.Default.QrCodeScanner, null, tint = SignalGreen)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = state.serverUrl,
                            onValueChange = viewModel::setServerUrl,
                            label = { Text("Server URL (ws://...)") },
                            placeholder = { Text(if (state.networkMode == PhoneNetworkMode.INTERNET) "wss://bhoomibot-os.onrender.com" else "ws://192.168.1.X:8080") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (state.recentServerUrls.isNotEmpty() && state.networkMode == PhoneNetworkMode.INTERNET) {
                            Spacer(Modifier.height(8.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                state.recentServerUrls.forEach { url ->
                                    SuggestionChip(
                                        onClick = { viewModel.selectRecentServerUrl(url) },
                                        label = { Text(url, fontSize = 10.sp) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 4. Shared Keys
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("SHARED KEYS", fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.robotId,
                            onValueChange = viewModel::setRobotId,
                            label = { Text("Robot ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.sessionCode,
                            onValueChange = viewModel::setSessionCode,
                            label = { Text("Session Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

// 5. Video Quality
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text("VIDEO QUALITY", fontWeight = FontWeight.ExtraBold)

                    if (state.networkMode == PhoneNetworkMode.HOTSPOT) {
                        // Show simple confirmation instead of options
                        Text("HOTSPOT MODE: Ultra HD 1080p active.", color = SignalGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Select stream resolution for Internet.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        // Keep your existing 2x2 Grid of QualityButtons here
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Save & Start
            Button(
                onClick = {
                    Toast.makeText(context, "Config Saved. Connecting...", Toast.LENGTH_SHORT).show()
                    viewModel.save { onStart(state.role) }
                },
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)
            ) {
                Text("SAVE & GO LIVE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showQrDialog) {
        RobotPairingDialog(
            pairingData = viewModel.getPairingData(),
            onDismiss = { showQrDialog = false }
        )
    }

    if (showScanner) {
        QrScannerDialog(
            onResult = { data ->
                viewModel.processScannedData(data)
                showScanner = false
                Toast.makeText(context, "Settings Auto-Filled!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showScanner = false }
        )
    }
}

@Composable
private fun QualityButton(
    quality: VideoQuality,
    selected: VideoQuality,
    modifier: Modifier,
    onClick: (VideoQuality) -> Unit
) {
    Button(
        onClick = { onClick(quality) },
        modifier = modifier.height(44.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (quality == selected) SignalGreen else MaterialTheme.colorScheme.surface,
            contentColor = if (quality == selected) Color.White else MutedText
        ),
        elevation = null
    ) {
        Text(
            text = if (quality == VideoQuality.ULTRA) "ULTRA (Actual)" else quality.label.substringBefore(" "),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
