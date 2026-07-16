package com.bhoomibot.os.navigation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.WarningAmber

// Notifications screen (operator extra). Shows a scrollable list of system/robot notifications
// with severity-colored accents and unread indicators. Tapping marks a notification read; a
// "Clear all" button empties the list. Mock data only — replace with a push/event repository later.
@Composable
fun NotificationsScreen(onBackClick: () -> Unit) {
    // Local mock feed. mutableStateListOf re-composes the list on add/remove/mark-read.
    val notifications = remember {
        mutableStateListOf(
            AppNotification("Battery low", "Robot battery dropped below 20%. Return to charge.", "12:42", Severity.WARNING, read = false),
            AppNotification("Mission complete", "Field A — Pass 2 finished successfully.", "11:05", Severity.INFO, read = false),
            AppNotification("Motor fault", "Right motor temperature above threshold.", "09:18", Severity.ERROR, read = false),
            AppNotification("Firmware up to date", "Robot firmware is on the latest stable build.", "Yesterday", Severity.INFO, read = true),
            AppNotification("GPS signal lost", "Re-acquired fix after 8 seconds.", "Yesterday", Severity.WARNING, read = true)
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar: back arrow + title + clear-all.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${notifications.count { !it.read }} unread", color = MutedText, style = MaterialTheme.typography.labelSmall)
            }
            if (notifications.isNotEmpty()) {
                TextButton(onClick = { notifications.clear() }) { Text("Clear all", color = SignalGreen, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            // Empty state.
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = SignalGreen, modifier = Modifier.padding(16.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("All caught up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("No new notifications", color = MutedText, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notifications, key = { it.title + it.time }) { notification ->
                    NotificationCard(notification) {
                        val idx = notifications.indexOf(notification)
                        if (idx >= 0) notifications[idx] = notification.copy(read = true)
                    }
                }
            }
        }
    }
}

// One notification row: colored left accent, severity icon, title + message + time, unread dot.
@Composable
private fun NotificationCard(notification: AppNotification, onOpen: () -> Unit) {
    val accent = notification.severity.color
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Left severity accent bar.
            Surface(color = accent, modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp))) {}
            Spacer(Modifier.width(12.dp))
            // Severity icon bubble.
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.16f), modifier = Modifier.size(38.dp)) {
                Icon(notification.severity.icon, null, tint = accent, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (notification.read) MutedText else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(notification.message, style = MaterialTheme.typography.bodySmall, color = MutedText)
                Spacer(Modifier.height(4.dp))
                Text(notification.time, style = MaterialTheme.typography.labelSmall, color = MutedText)
            }
            // Unread indicator dot.
            if (!notification.read) {
                Spacer(Modifier.width(8.dp))
                Surface(shape = CircleShape, color = accent, modifier = Modifier.size(9.dp)) {}
            }
        }
    }
}

// A single notification entry.
private data class AppNotification(
    val title: String,
    val message: String,
    val time: String,
    val severity: Severity,
    val read: Boolean
)

// Notification severity levels with their icon + accent color.
private enum class Severity(val icon: ImageVector, val color: Color) {
    INFO(Icons.Default.Info, SignalGreen),
    WARNING(Icons.Default.Warning, WarningAmber),
    ERROR(Icons.Default.Error, SafetyRed)
}
