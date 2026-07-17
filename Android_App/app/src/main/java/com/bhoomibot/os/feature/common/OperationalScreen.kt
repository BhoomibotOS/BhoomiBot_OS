package com.bhoomibot.os.feature.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SignalGreen

/** Consistent shell for non-control operational modules while their data sources are integrated.
 *  Used by Camera, Diagnostics, Map and Autonomous as a temporary "not built yet" placeholder screen.
 *
 *  Unlike the real feature screens, this one is fully stateless — no ViewModel or UiState. It just
 *  renders the [title]/[subtitle] passed in plus a fixed "Module ready" card, so a single Composable
 *  can stand in for several not-yet-built modules. */
@Composable
fun OperationalScreen(title: String, subtitle: String, onBackClick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: a back arrow (top-left) plus the screen title and a small uppercase subtitle.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle.uppercase(), color = MutedText, style = MaterialTheme.typography.labelMedium) }
        }
        Spacer(Modifier.height(24.dp))
        // A single info card telling the operator the module is wired up but waiting for robot data.
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Construction, null, tint = SignalGreen); Spacer(Modifier.padding(8.dp))
                Column { Text("Module ready", fontWeight = FontWeight.Bold); Text("Awaiting vehicle telemetry and service integration.", color = MutedText, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
