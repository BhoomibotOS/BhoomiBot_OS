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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // Everything is stacked in a Box so the video fills the screen and the
    // controls/overlays float on top of it, anchored to different corners.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // The live feed (or a waiting placeholder). `frame` is the latest jpeg
        // already decoded into an ImageBitmap by the ViewModel; null until the
        // first frame arrives from the robot.
        val frame = state.frame
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = "Live robot feed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = SignalGreen)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Waiting for the robot's live feed…",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Top bar.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("OPERATOR LIVE", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            ConnectionBadge(state.connectionState, Modifier)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onOpenOptions) { Icon(Icons.Default.Settings, "Options", tint = Color.White) }
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

        // Drive controls (bottom-center). Method references hand each button press
        // straight to the ViewModel, which packages it as a RobotCommand and sends
        // it back to the robot over the live link.
        DriveControls(
            onDrive = viewModel::sendDrive,
            onEmergencyStop = viewModel::sendEmergencyStop,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}
