// ===========================================================================
// LiveComponents.kt
// ---------------------------------------------------------------------------
// Shared, stateless composables used by BOTH live screens (operator + robot).
// Each takes plain data in and draws it — none of them touch a ViewModel, so
// they're easy to reuse and preview:
//   - ConnectionBadge: pill showing the socket state (Idle/Live/Offline...).
//   - PeerRow / PeerDot: who is present in the session (robot + operator).
//   - TelemetryOverlay / Line: read-out drawn over the operator's video.
//   - DriveControls / DriveButton: the operator's drive pad + E-STOP.
// Colors come from the app theme (SignalGreen = good, SafetyRed = stop/error).
// ===========================================================================
package com.bhoomibot.os.feature.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

/** Small pill showing the live-link socket state. */
@Composable
fun ConnectionBadge(state: LiveConnectionState, modifier: Modifier = Modifier) {
    // Map each socket state to its label + accent color. Destructuring `(a, b)`
    // pulls both out of the Pair the `when` produces.
    val (label, color) = when (state) {
        LiveConnectionState.IDLE -> "Idle" to MutedText
        LiveConnectionState.CONNECTING -> "Connecting…" to MutedText
        LiveConnectionState.CONNECTED -> "Live" to SignalGreen
        LiveConnectionState.RECONNECTING -> "Reconnecting…" to SafetyRed
        LiveConnectionState.ERROR -> "Offline" to SafetyRed
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
            Spacer(Modifier.width(8.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// A single "<name> online/offline" indicator: colored dot + label. Green when
// present, muted grey when not. Private helper for PeerRow.
@Composable
private fun PeerDot(label: String, online: Boolean, modifier: Modifier = Modifier) {
    val color = if (online) SignalGreen else MutedText
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(8.dp)) {}
        Spacer(Modifier.width(6.dp))
        Text(
            "$label ${if (online) "online" else "offline"}",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Shows which counterpart is present in the session. */
@Composable
fun PeerRow(peer: PeerStatus, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PeerDot("Robot", peer.robotOnline)
        PeerDot("Operator", peer.operatorOnline)
    }
}

/**
 * Diagnostic pill showing THIS phone's active role + meet-keys (Robot ID +
 * session code). When two phones fail to exchange video despite both showing
 * "Connected", the cause is almost always a role or key mismatch — this makes
 * it visible at a glance so the user can confirm both phones agree.
 */
@Composable
fun SessionInfoChip(
    role: DeviceRole?,
    robotId: String,
    session: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.45f)
    ) {
        Text(
            "${role?.name ?: "?"} · $robotId · $session",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/** Semi-transparent telemetry read-out drawn over the live video. */
@Composable
fun TelemetryOverlay(t: TelemetrySnapshot, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "BATTERY ${t.batteryPercent}%",
                color = SignalGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Line("Mode", t.mode)
            Line("Mission", t.mission)
            Line("GPS", t.gpsStatus)
            Line("Camera", t.cameraStatus)
            Line("AI", t.aiStatus)
        }
    }
}

// One "label ........ value" row inside the telemetry overlay. Value is single
// line and ellipsized so long text can't break the layout.
@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodySmall)
        Text(
            value, color = Color.White, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** Digital drive pad + emergency stop (mirrors ManualControlScreen's intent). */
@Composable
fun DriveControls(
    onDrive: (DriveCommand, Int) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Two rows: forward + E-STOP on top, then left/right/reverse. Each direction
    // sends its DriveCommand at a fixed 50% speed; the red ■ is the emergency stop.
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DriveButton("▲", { onDrive(DriveCommand.FORWARD, 50) })
            DriveButton("■", onEmergencyStop, SafetyRed)
        }
        Spacer(Modifier.width(0.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DriveButton("◀", { onDrive(DriveCommand.LEFT, 50) })
            DriveButton("▶", { onDrive(DriveCommand.RIGHT, 50) })
            DriveButton("▼", { onDrive(DriveCommand.REVERSE, 50) })
        }
    }
}

// One square touch button. Defaults to SignalGreen; E-STOP passes SafetyRed.
@Composable
private fun DriveButton(label: String, onClick: () -> Unit, color: Color = SignalGreen) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.18f),
        modifier = Modifier.size(64.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
