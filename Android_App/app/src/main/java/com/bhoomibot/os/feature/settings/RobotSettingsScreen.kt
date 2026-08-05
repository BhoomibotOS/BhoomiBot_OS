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
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SignalGreen

/**
 * Robot-phone settings screen. Hosts the connection configuration the robot needs:
 *   - CONNECTION: the local VCU/ESP32 link (Bluetooth / Wi-Fi hotspot), and
 *   - LIVE LINK: the internet relay (relay URL, Robot ID, session code, video
 *     quality) — the "connection options" that used to live on the Go Live card,
 *     now surfaced here under Live Link.
 *
 * Pure UI: both cards just navigate onward; the real config happens on the
 * destination screens. [onBackClick] returns to the caller.
 */
@Composable
fun RobotSettingsScreen(
    onBackClick: () -> Unit,
    onConnectionClick: () -> Unit = {},
    onLiveLinkClick: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: back arrow + title.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("ROBOT", color = MutedText, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(20.dp))
        // Local VCU/ESP32 link configuration.
        Card(onClick = onConnectionClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = SignalGreen, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text("CONNECTION", fontWeight = FontWeight.ExtraBold)
                    Text("Bluetooth / WiFi settings for the VCU.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(">", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Internet live-link configuration (relay URL, Robot ID, session code, video quality).
        Card(onClick = onLiveLinkClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Videocam, null, tint = SignalGreen, modifier = Modifier.padding(end = 12.dp))
                Column(Modifier.weight(1f)) {
                    Text("LIVE LINK", fontWeight = FontWeight.ExtraBold)
                    Text("Relay URL, Robot ID and session code for the internet live feed.", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
                Text(">", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
