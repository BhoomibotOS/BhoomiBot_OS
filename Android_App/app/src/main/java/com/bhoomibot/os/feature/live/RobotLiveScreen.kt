// ===========================================================================
// RobotLiveScreen.kt
// ---------------------------------------------------------------------------
// The ROBOT side of the "Internet Live Link" feature. This runs on the phone
// mounted ON the robot. It shows the local camera preview and a START/STOP
// BROADCAST button. While broadcasting, CameraX analyzes frames, this file
// throttles + compresses them to jpeg, and hands them to the ViewModel, which
// pushes them to the operator. The operator's OperatorLiveScreen shows them.
//
// Layout of this file:
//   - RobotLiveScreen: the Compose UI (permission handling + overlays).
//   - CameraPreview:   hosts CameraX's PreviewView and binds/unbinds it.
//   - bindCamera:      wires up Preview + ImageAnalysis to the lifecycle.
//   - analyzeFrame / resizeToLongestSide: per-frame throttle + jpeg encode.
// ===========================================================================
package com.bhoomibot.os.feature.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import kotlinx.coroutines.flow.first
import kotlin.runCatching
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Robot-side screen. Holds the camera preview (local), and — once broadcasting —
 * the CameraX analyzer pushes compressed jpeg frames to the live link while the
 * ViewModel streams telemetry. The operator sees the feed on their phone.
 */
@Composable
fun RobotLiveScreen(
    onBackClick: () -> Unit,
    onOpenOptions: () -> Unit,
    viewModel: RobotLiveViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    // Surface connection-state changes as on-screen Toasts (no terminal needed).
    LaunchedEffect(state.connectionState) {
        val msg = when (state.connectionState) {
            LiveConnectionState.CONNECTED -> "ROBOT: Connected to relay (Live)"
            LiveConnectionState.CONNECTING -> "ROBOT: Connecting to relay…"
            LiveConnectionState.RECONNECTING -> "ROBOT: Connection lost — reconnecting…"
            LiveConnectionState.ERROR -> "ROBOT: Connection failed (Offline)"
            LiveConnectionState.IDLE -> "ROBOT: Idle"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    // Surface the exact WebSocket failure reason (the missing piece for debugging).
    LaunchedEffect(state.error) {
        state.error?.let { Toast.makeText(context, "ROBOT error: $it", Toast.LENGTH_LONG).show() }
    }
    // Track camera permission in Compose state so the UI switches between the
    // preview and the "grant permission" prompt. Seeded with the current grant.
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    // The system permission dialog. Its callback updates our state and, if the
    // user just granted access, starts broadcasting immediately.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startBroadcast()
    }

    // Ask once when the screen first appears (keyed on Unit = run only on entry)
    // if we don't already have permission.
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                isActive = state.isBroadcasting,
                fps = state.videoFps,
                quality = state.videoQuality,
                onFrame = viewModel::publishFrame
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to broadcast the live feed.", color = Color.White)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant camera")
                }
            }
        }

        // Top bar.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("ROBOT LIVE", color = Color.White, fontWeight = FontWeight.Bold)
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

        // Bottom: peer presence + start/stop + counters.
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Diagnostic: this phone's role + Robot ID + session so a role/key
            // mismatch (the usual "connected but no video" cause) is visible.
            SessionInfoChip(
                state.activeRole, state.activeRobotId, state.activeSession,
                Modifier.background(Color.Black.copy(alpha = 0.4f)).padding(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            PeerRow(
                state.peerStatus,
                Modifier.background(Color.Black.copy(alpha = 0.4f)).padding(8.dp)
            )
            Spacer(Modifier.height(8.dp))
            val broadcasting = state.isBroadcasting
            // One button toggles broadcasting: red STOP while live, green START
            // otherwise. Disabled until settings are loaded (can't connect without
            // them), but always enabled while broadcasting so STOP is reachable.
            Button(
                onClick = { if (broadcasting) viewModel.stopBroadcast() else viewModel.startBroadcast() },
                enabled = broadcasting || state.isConfigurationReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (broadcasting) SafetyRed else SignalGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(if (broadcasting) Icons.Default.VideocamOff else Icons.Default.Videocam, null)
                Spacer(Modifier.width(8.dp))
                Text(if (broadcasting) "STOP BROADCAST" else "START BROADCAST")
            }
            if (!state.isConfigurationReady) {
                Text("Loading this phone's live-link settings…", color = Color.White.copy(alpha = 0.7f))
            }
            Text(
                "${state.networkLabel}: ${state.videoQuality.label}, ${state.videoFps} fps" +
                    if (state.isConstrainedNetwork) " (data saver active)" else "",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.framesSent} frames sent" +
                    (state.lastCommand?.let { "  •  Operator: ${it.drive}" } ?: ""),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Hosts the [PreviewView] and binds CameraX (preview + frame analyzer) once active. */
@Composable
private fun CameraPreview(
    modifier: Modifier,
    isActive: Boolean,
    fps: Int,
    quality: VideoQuality,
    onFrame: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // The PreviewView is a classic Android View; remember it so it survives
    // recomposition instead of being recreated each frame.
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    // Guards against binding CameraX twice; flipped once bind succeeds.
    var bound by remember { mutableStateOf(false) }
    // rememberUpdatedState keeps the LATEST fps/quality readable from inside the
    // long-lived analyzer lambda WITHOUT re-binding the camera. We pass these as
    // lambdas ({ currentFps }) so each frame reads the current value live.
    val currentFps by rememberUpdatedState(fps)
    val currentQuality by rememberUpdatedState(quality)

    // Bridge the Android View into Compose.
    AndroidView(factory = { previewView }, modifier = modifier)

    // React to broadcasting starting/stopping (keyed on isActive).
    LaunchedEffect(isActive) {
        if (isActive && !bound) {
            bindCamera(context, previewView, lifecycleOwner, { currentFps }, { currentQuality }, onFrame) { bound = true }
        } else if (!isActive && bound) {
            // Stop analysis as soon as the robot stops broadcasting. Otherwise a
            // hidden analyzer keeps compressing frames and consuming the phone's
            // battery/data after the live link has been closed.
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            bound = false
        }
    }
}

// Sets up CameraX with two use cases bound to the composable's lifecycle:
//   - Preview: what the user sees locally on this phone.
//   - ImageAnalysis: gives us each camera frame so we can jpeg it and broadcast.
// getInstance() is async, hence the listener; STRATEGY_KEEP_ONLY_LATEST drops
// backlogged frames so we always process the freshest one (no lag build-up).
// fps/quality are passed as lambdas so the analyzer always reads current values.
private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    fps: () -> Int,
    quality: () -> VideoQuality,
    onFrame: (ByteArray) -> Unit,
    onBound: () -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        try {
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                analyzeFrame(proxy, fps(), quality(), onFrame)
            }
            // Unbind first to release the camera from any prior binding, then bind
            // the back camera with both use cases.
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            onBound()
        } catch (_: Exception) {
            // Camera busy / unavailable — user can retry via the broadcast button.
        }
    }, ContextCompat.getMainExecutor(context))
}

// Timestamp (ms) of the last frame we actually processed. File-level (static)
// because there's a single camera pipeline; used purely for fps throttling.
private var lastAnalyzeTs = 0L

/** Throttles to [fps], converts the frame to jpeg, and hands it to [onFrame]. */
private fun analyzeFrame(proxy: ImageProxy, fps: Int, quality: VideoQuality, onFrame: (ByteArray) -> Unit) {
    // Throttle to the target fps: interval is the minimum gap between frames
    // (e.g. 12 fps -> ~83 ms). max(fps, 1) avoids divide-by-zero. Frames that
    // arrive too soon are dropped — but the proxy MUST be closed either way, or
    // CameraX stalls waiting for the buffer to be released.
    val now = System.currentTimeMillis()
    val interval = 1000L / max(fps, 1)
    if (now - lastAnalyzeTs < interval) { proxy.close(); return }
    lastAnalyzeTs = now

    // Convert the camera buffer to a Bitmap; bail (still closing) if it fails.
    val raw = runCatching { proxy.toBitmap() }.getOrNull() ?: run { proxy.close(); return }
    proxy.close()

    // The raw ImageProxy is in the SENSOR's orientation, which is rotated
    // relative to what the on-device PreviewView shows (CameraX rotates the
    // preview for us). Bake that rotation in here so the frame the operator
    // receives already matches the robot's own preview — the operator then
    // draws it verbatim and never needs to rotate it.
    val rotation = proxy.imageInfo.rotationDegrees
    val bitmap = if (rotation != 0) {
        val m = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        raw.recycle()
        rotated
    } else raw

    // Shrink to the quality tier's max dimension, then jpeg-compress at its
    // quality level. Smaller/lower-quality = less data over the link.
    val resized = resizeToLongestSide(bitmap, quality.longestSide)
    val stream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, stream)
    onFrame(stream.toByteArray())
}

// Downscales so the longer edge is at most [longest], preserving aspect ratio.
// Returns the original untouched if it's already small enough. max(1, ...)
// guards against a computed dimension rounding down to 0.
private fun resizeToLongestSide(bmp: Bitmap, longest: Int): Bitmap {
    val w = bmp.width
    val h = bmp.height
    val long = max(w, h)
    if (long <= longest) return bmp
    val scale = longest.toFloat() / long
    return Bitmap.createScaledBitmap(bmp, max(1, (w * scale).toInt()), max(1, (h * scale).toInt()), true)
}
