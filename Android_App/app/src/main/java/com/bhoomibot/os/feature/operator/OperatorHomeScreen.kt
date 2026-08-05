package com.bhoomibot.os.feature.operator

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bhoomibot.os.model.MockRobotData
import com.bhoomibot.os.navigation.AppRoute
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

// Primary home for the handheld OPERATOR phone.
@Composable
fun OperatorHomeScreen(
    navController: NavController,
    viewModel: OperatorHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val actions = listOf(
        OperatorAction("Manual", Icons.Default.SportsEsports, AppRoute.Manual),
        OperatorAction("Live View", Icons.Default.Videocam, AppRoute.OperatorLive),
        OperatorAction("Agent AI", Icons.Default.AutoAwesome, AppRoute.AgentChat),
        OperatorAction("Skill Library", Icons.Default.Psychology, AppRoute.SkillLibrary),
        OperatorAction("Diagnostics", Icons.Default.Analytics, AppRoute.Diagnostics),
        OperatorAction("Notifications", Icons.Default.Notifications, AppRoute.Notifications),
        OperatorAction("Settings", Icons.Default.Settings, AppRoute.Settings)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // App header: robot badge + product name / tagline.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SignalGreen.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.SmartToy, "Bhoomibot", tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Bhoomibot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Cultivating Inteligence",
                    style = MaterialTheme.typography.labelMedium,
                    color = SignalGreen,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(Modifier.height(28.dp))
        
        // Compact Status Bar (BAT, GPS, VCU)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniStatus("BAT", "${uiState.batteryPercent}%", SignalGreen, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                MiniStatus("GPS", uiState.gpsStatus, SignalGreen, Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                MiniStatus(
                    "VCU", 
                    if (uiState.vcuConnected) "Connected" else "Offline", 
                    if (uiState.vcuConnected) SignalGreen else SafetyRed, 
                    Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        
        Text(
            "OPERATIONS",
            style = MaterialTheme.typography.labelLarge,
            color = MutedText,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(16.dp))
        
        // 2-per-row action grid with a gap between rows.
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                pair.forEach { action ->
                    OperatorActionCard(action, Modifier.weight(1f)) { navController.navigate(action.route) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// One small readout (label on top, value below) used inside the top status bar.
@Composable
private fun MiniStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(
    modifier,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    Text(value, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, maxLines = 1)
}

// A square shortcut card: icon bubble on top, action title underneath. Tapping routes to the feature.
@Composable
private fun OperatorActionCard(action: OperatorAction, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SignalGreen.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(action.icon, action.label, tint = SignalGreen, modifier = Modifier.padding(10.dp))
            }
            Text(
                action.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// One operator action entry (display title + icon + destination route).
private data class OperatorAction(val label: String, val icon: ImageVector, val route: String)
