package com.bhoomibot.os.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
fun DashboardScreen(
    onNavigateToAgent: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Text("BhoomiBot OS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard("AGENT", Icons.Default.SmartToy, SignalGreen, Modifier.weight(1f), onNavigateToAgent)
            DashboardCard("MANUAL", Icons.Default.PrecisionManufacturing, Color.Cyan, Modifier.weight(1f), onNavigateToManual)
        }
        
        Spacer(Modifier.height(16.dp))
        DashboardCard("SETTINGS", Icons.Default.Settings, Color.Gray, Modifier.fillMaxWidth(), onNavigateToSettings)
    }
}

@Composable
fun DashboardCard(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.Gray, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = modifier.height(120.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
