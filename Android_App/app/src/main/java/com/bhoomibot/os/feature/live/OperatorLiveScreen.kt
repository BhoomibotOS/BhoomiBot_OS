// ===========================================================================
// OperatorLiveScreen.kt
// ---------------------------------------------------------------------------
// The OPERATOR side of the "Internet Live Link" feature. The person holding
// the handheld phone opens this screen to watch the robot's camera feed, read
// its telemetry, see who else is connected, and drive the robot remotely.
//
// This file is UI only: it reads immutable state from OperatorLiveViewModel
// (collected as Compose state) and forwards button presses back to the VM.
// All networking/decoding lives in the ViewModel + LiveLinkRepository.
// The shared pieces (ConnectionBadge, TelemetryOverlay, PeerRow, DriveControls)
// come from LiveComponents.kt.
// ===========================================================================
package com.bhoomibot.os.feature.live

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.feature.connection.PhoneNetworkMode
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

/**
 * Operator-side screen. Shows the live video feed the robot is broadcasting,
 * a telemetry overlay, who is present, and drive controls that send
 * commands back to the robot over the live link.
 */
@Composable
fun OperatorLiveScreen(
    onBackClick: () -> Unit,
    onOpenOptions: () -> Unit,
    viewModel: OperatorLiveViewModel = viewModel()
) {
    // collectAsState() subscribes this composable to the VM's StateFlow; whenever
    // the ViewModel pushes a new OperatorLiveUiState, `state` updates and the UI
    // recomposes. `by` delegates so we can read fields directly as `state.xxx`.
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Surface connection-state changes as on-screen Toasts.
    LaunchedEffect(state.connectionState) {
        val isHotspot = state.networkMode == PhoneNetworkMode.HOTSPOT
        val msg = when (state.connectionState) {
            LiveConnectionState.CONNECTED -> if (isHotspot) "HOTSPOT: Link Active" else "INTERNET: Connected to relay"
            LiveConnectionState.CONNECTING -> if (isHotspot) "HOTSPOT: Searching for Robot..." else "INTERNET: Connecting..."
            LiveConnectionState.RECONNECTING -> "Link lost — retrying..."
            LiveConnectionState.ERROR -> "No connection"
            LiveConnectionState.IDLE -> "No connection"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // Everything is stacked in a Box so the video fills the screen and the
    // controls/overlays float on top of it, anchored to different corners.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // The live feed (or a waiting placeholder). `frame` is the latest jpeg
        // already decoded into an ImageBitmap by the ViewModel; null until the
        // first frame arrives from the robot.
        val frame = state.frame
        if (!state.liveCameraEnabled) {
            // The operator switched the robot's camera off remotely: the robot has
            // stopped broadcasting, so there's nothing to show until it's turned on.
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Videocam, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(12.dp))
                Text("Live camera is off", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Turn it on to watch the robot's feed.", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
            }
        } else if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = "Live robot feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // AI Vision Overlay (Bounding Boxes for Weeds)
            Canvas(Modifier.fillMaxSize()) {
                state.detectedObjects.forEach { obj ->
                    val rect = obj.boundingBox
                    drawRect(
                        color = SafetyRed,
                        topLeft = Offset(rect.left * size.width, rect.top * size.height),
                        size = Size(rect.width() * size.width, rect.height() * size.height),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.connectionState == LiveConnectionState.CONNECTED || state.connectionState == LiveConnectionState.CONNECTING) {
                    CircularProgressIndicator(color = SignalGreen)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (state.networkMode == PhoneNetworkMode.HOTSPOT) "Waiting for Robot Hub..." else "Waiting for Internet Feed...",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                } else {
                    Icon(Icons.Default.Videocam, null, tint = SafetyRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No connection",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Top bar.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text(
                if (state.networkMode == PhoneNetworkMode.HOTSPOT) "HOTSPOT VIEW" else "INTERNET LIVE", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            ConnectionBadge(state.connectionState, Modifier)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onOpenOptions) { Icon(Icons.Default.Settings, "Options", tint = Color.White) }
        }

        // Error banner: if the link failed or an unexpected error occurred, show
        // it with a Retry button instead of leaving the user on a dead screen.
        if (state.error != null || state.connectionState == LiveConnectionState.ERROR) {
            Column(
                Modifier.align(Alignment.TopCenter)
                    .padding(top = 64.dp, start = 12.dp, end = 12.dp)
                    .background(SafetyRed.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    state.error ?: "Connection failed. Check the relay URL, Robot ID and session code, then retry.",
                    color = Color.White, style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::retry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SafetyRed)
                ) { Text("Retry", fontWeight = FontWeight.Bold) }
            }
        }

        // Telemetry overlay (top-right).
        TelemetryOverlay(
            state.telemetry,
            Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 12.dp)
        )

        // Peer presence (bottom-left).
        PeerRow(
            state.peerStatus,
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(8.dp)
        )

        // Diagnostic: this phone's role + Robot ID + session so a role/key
        // mismatch (the usual "connected but no video" cause) is visible.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SessionInfoChip(state.activeRole, state.activeRobotId, state.activeSession, Modifier)
            Spacer(Modifier.height(8.dp))
            // AI Status Bar (AI-004 Feedback)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (state.aiStatus.contains("Ready")) SignalGreen else Color.Gray,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    state.aiStatus,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.aiSteeringOffset != 0f) {
                Text(
                    "Steering Correction: ${if (state.aiSteeringOffset > 0) "RIGHT" else "LEFT"} (${(state.aiSteeringOffset * 100).toInt()}%)",
                    color = SignalGreen,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
