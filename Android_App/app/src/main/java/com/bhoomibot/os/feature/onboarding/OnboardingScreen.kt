package com.bhoomibot.os.feature.onboarding

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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SignalGreen

// First-launch screen. Lets the user pick which physical role this phone serves.
// The choice is saved by the caller (AppNavigation/ViewModel) and persists across launches.
@Composable
fun OnboardingScreen(onRoleSelected: (DeviceRole) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Welcome to BhoomiBot", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Text("Choose Device Role", style = MaterialTheme.typography.titleMedium, color = MutedText)
        Spacer(Modifier.height(28.dp))

        // Operator phone card.
        RoleCard(
            icon = Icons.Default.Person,
            title = "Operator Phone",
            description = "The handheld device used by the person driving and monitoring the robot.",
            responsibilities = listOf("Manual driving", "Live video", "Robot status", "Mission control"),
            usage = "Carried by the operator in the field.",
            onClick = { onRoleSelected(DeviceRole.OPERATOR) }
        )
        Spacer(Modifier.height(18.dp))

        // Robot-mounted phone card.
        RoleCard(
            icon = Icons.Default.Agriculture,
            title = "Robot Mounted Phone",
            description = "Mounted on the robot; acts as the primary on-board computer.",
            responsibilities = listOf("AI & vision", "Decision making", "Video streaming", "Diagnostics"),
            usage = "Fixed to the robot chassis.",
            onClick = { onRoleSelected(DeviceRole.ROBOT) }
        )
        Spacer(Modifier.height(24.dp))
    }
}

// One selectable role card with illustration, description, responsibilities and a SELECT button.
@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    responsibilities: List<String>,
    usage: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Illustration bubble.
            Surface(shape = CircleShape, color = SignalGreen.copy(alpha = 0.14f), modifier = Modifier.size(56.dp)) {
                Icon(icon, title, tint = SignalGreen, modifier = Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MutedText)

            Spacer(Modifier.height(14.dp))
            Text("RESPONSIBILITIES", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            responsibilities.forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(SignalGreen))
                    Spacer(Modifier.width(10.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("TYPICAL USAGE", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(usage, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(18.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) {
                Text("SELECT $title", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
