package com.bhoomibot.os.feature.manual

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.feature.live.OperatorLiveViewModel
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Manual drive screen (OPERATOR role) — the "handheld remote" for the field robot.
 *
 * This file is pure UI: it renders [ManualUiState] and forwards user gestures to
 * [ManualViewModel] actions. It never touches the robot transport directly — every command
 * goes ViewModel → RobotRepository (fake/no-op by default). Reached from the dashboard;
 * [onBackClick] returns there.
 *
 * Layout (top → bottom, vertically scrollable): status strip + E-STOP, camera preview card,
 * DIGITAL/JOYSTICK mode selector, speed read-out, the active drive control, PTO, hydraulic
 * lift, and the quick-toggles row (work lights / horn / camera light / learning). Opening the
 * camera full-screen swaps the whole layout for [MaximizedCameraOverlay] (forced landscape).
 *
 * NOTE: three composables below — [PrimaryLightsControl], [CompactStatus] and [ManualBottomBar]
 * — are RESERVED. They compile and are kept for a planned redesign but are not called from the
 * layout yet, so they render nowhere at runtime.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlScreen(onBackClick: () -> Unit, viewModel: ManualViewModel = viewModel(), liveViewModel: OperatorLiveViewModel = viewModel()) {
    // Subscribe to the ViewModel's state; recomposes whenever the state changes.
    val state by viewModel.uiState.collectAsState()
    // The live-link (internet relay) feed: connects as OPERATOR and decodes the
    // robot's video frames. We reuse OperatorLiveViewModel so the manual screen
    // can show the remote feed in place of the local CameraX preview.
    val liveState by liveViewModel.uiState.collectAsState()
    // Flipping "Live camera" both shows/hides the feed locally AND remotely
    // tells the robot to start/stop broadcasting (the link stays connected).
    val onLiveCameraToggle: (Boolean) -> Unit = { enabled ->
        viewModel.setCameraEnabled(enabled)
        liveViewModel.sendLiveCamera(enabled)
    }
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        // Top app bar: center title (status strip) + back arrow to return to the dashboard.
        topBar = { CenterAlignedTopAppBar(title = { TopStatusBar(state, viewModel::onEmergencyStop) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Return to dashboard") } }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // Scrollable main column holding all manual controls, top to bottom.
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Live camera preview card (hidden when the camera is opened full-screen).
            if (!state.isCameraMaximized) {
                LiveCameraPreview(
                    onMaximize = { viewModel.setCameraMaximized(true) },
                    cameraEnabled = state.cameraEnabled,
                    onCameraToggle = { onLiveCameraToggle(!state.cameraEnabled) },
                    liveFrame = liveState.frame,
                    connectionState = liveState.connectionState,
                    onRetry = liveViewModel::retry
                )
            }
            Spacer(Modifier.height(12.dp))
            // DIGITAL / JOYSTICK mode switch.
            DrivingModeSelector(state.drivingMode, viewModel::setDrivingMode)
            Spacer(Modifier.height(12.dp))
            // Current vehicle speed read-out (m/s).
            VehicleSpeed(state.vehicleSpeedPercent)
            Spacer(Modifier.height(4.dp))
            // Show the drive control matching the selected mode (buttons or joystick).
            AnimatedContent(targetState = state.drivingMode, label = "drivingMode") { mode ->
                if (mode == DrivingMode.DIGITAL) DigitalDriveControls(viewModel::onForward, viewModel::onReverse, viewModel::onLeft, viewModel::onRight, viewModel::onStop, modifier = Modifier.fillMaxWidth().offset(y = (-8).dp))
                else if (!state.isCameraMaximized) JoystickControl(viewModel::onJoystickChanged, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(20.dp))
            // PTO (power-take-off) control: enable switch + speed slider.
            PtoControls(state, viewModel::onPtoToggle, viewModel::onPtoSpeedChanged)
            Spacer(Modifier.height(16.dp))
            // Hydraulic lift control: enable switch + height slider.
            HydraulicHeightControl(state.hydraulicEnabled, state.hydraulicHeightPercent, viewModel::onHydraulicToggle, viewModel::onHydraulicHeightChanged)
            Spacer(Modifier.height(16.dp))
            // Quick toggles: work lights, horn, camera light.
            QuickControls(state, viewModel::onLightsToggle, viewModel::onHorn, viewModel::onCameraLightToggle, viewModel::onLearningToggle)
            Spacer(Modifier.height(18.dp))
        }
    }
        // When the camera is opened full-screen, draw the landscape overlay on top of everything.
        if (state.isCameraMaximized) {
            MaximizedCameraOverlay(
                mode = state.drivingMode,
                speedMetersPerSecond = state.vehicleSpeedPercent,
                cameraEnabled = state.cameraEnabled,
                onCameraToggle = { onLiveCameraToggle(!state.cameraEnabled) },
                onJoystickChanged = viewModel::onJoystickChanged,
                onForward = viewModel::onForward,
                onReverse = viewModel::onReverse,
                onLeft = viewModel::onLeft,
                onRight = viewModel::onRight,
                onStop = viewModel::onStop,
                onDismiss = { viewModel.setCameraMaximized(false) },
                liveFrame = liveState.frame,
                connectionState = liveState.connectionState,
                onRetry = liveViewModel::retry
            )
        }
    }
}

// The top status strip: BAT, GPS, VCU, MODE quick-readouts plus the red E-STOP button.
@Composable private fun TopStatusBar(state: ManualUiState, onEmergencyStop: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TopStatus("BAT", "${state.robotStatus.batteryPercent}%", SignalGreen, Modifier.weight(1f))   // Battery %
    TopStatus("GPS", state.robotStatus.gpsStatus, SignalGreen, Modifier.weight(1f))              // GPS state
    TopStatus("VCU", "Connected", SignalGreen, Modifier.weight(1f))                              // Vehicle control unit (hard-coded for now)
    TopStatus("MODE", "Manual", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))        // Current mode label
    // Emergency stop button (red): immediately halts the robot.
    Button(onEmergencyStop, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp)) { Text("E-STOP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
}
// One small readout (label on top, value below) used inside the top status strip.
@Composable private fun TopStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall); Text(value, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1) }

// Normal (non-full-screen) camera card. Tapping the card opens full-screen; the "Live camera"
// switch turns the feed ON/OFF. Shows the robot's remote live feed (internet relay) when the
// link is healthy, a "check your connection" hint while it isn't, and "CAMERA OFF" when disabled.
/** Normal field preview. Previous height was 250dp; restore it if 360dp is unsuitable. */
@Composable private fun LiveCameraPreview(onMaximize: () -> Unit, cameraEnabled: Boolean, onCameraToggle: () -> Unit, liveFrame: ImageBitmap?, connectionState: LiveConnectionState, onRetry: () -> Unit) = Card(onClick = onMaximize, modifier = Modifier.fillMaxWidth().height(360.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B22))) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (cameraEnabled) {
            // When the relay link is healthy and a frame has arrived, show the robot's remote
            // feed IN PLACE OF the local CameraX preview. Otherwise ask the user to check the link.
            if (liveFrame != null) {
                Image(
                    bitmap = liveFrame,
                    contentDescription = "Live robot feed",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Button(onMaximize, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) { Text("FULL SCREEN", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall) }
            } else {
                LiveConnectionHint(connectionState, Modifier.align(Alignment.Center), onRetry)
            }
        } else {
            // Camera disabled: just show a label.
            Text("CAMERA OFF", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        // Top-left: "Live camera" label + the ON/OFF switch that toggles the feed.
        Row(Modifier.align(Alignment.TopStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Live camera", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Switch(cameraEnabled, onCheckedChange = { onCameraToggle() })
        }
        Text(if (liveFrame != null) "Live relay feed" else "Waiting for connection…", modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), color = MutedText, style = MaterialTheme.typography.labelSmall)
    }
}

// Shown in the camera slot when "Live camera" is ON but the relay link isn't yet delivering
// frames. Guides the operator to check their connection based on the current socket state.
@Composable private fun LiveConnectionHint(state: LiveConnectionState, modifier: Modifier = Modifier, onRetry: () -> Unit) = Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    val (title, detail) = when (state) {
        LiveConnectionState.ERROR -> "Check your connection" to "Couldn't reach the relay. Verify the relay URL, Robot ID and session code in Live Link settings, then retry."
        LiveConnectionState.RECONNECTING -> "Reconnecting…" to "The link dropped — trying to re-establish the connection."
        LiveConnectionState.CONNECTING -> "Connecting…" to "Connecting to the relay. Make sure the robot is broadcasting."
        LiveConnectionState.CONNECTED -> "Waiting for feed…" to "Connected, but no video yet. Confirm the robot is live and the keys match."
        LiveConnectionState.IDLE -> "Not connected" to "No live feed yet. Check the Live Link settings and your connection."
    }
    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(detail, color = MutedText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    if (state == LiveConnectionState.ERROR) {
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) { Text("Retry", fontWeight = FontWeight.Bold) }
    }
}

// (Reserved) Work-lights card control — not currently wired into the screen layout.
@Composable private fun PrimaryLightsControl(enabled: Boolean, onToggle: (Boolean) -> Unit) = Card(onClick = { onToggle(!enabled) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (enabled) SignalGreen.copy(alpha = .16f) else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lightbulb, null, tint = if (enabled) SignalGreen else MutedText); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("WORK LIGHTS", fontWeight = FontWeight.Bold); Text(if (enabled) "ON • Field illumination active" else "OFF • Tap to enable", color = MutedText, style = MaterialTheme.typography.labelSmall) }; Switch(enabled, onToggle) } }

// Full-screen landscape camera overlay. Forced to landscape while shown; restores previous
// orientation on close. Hosts the camera feed, "Live camera" switch, current speed, an
// EXIT VIEW button, and drive controls (joystick or buttons) over the video.
@Composable private fun MaximizedCameraOverlay(mode: DrivingMode, speedMetersPerSecond: Int, cameraEnabled: Boolean, onCameraToggle: () -> Unit, onJoystickChanged: (Float, DriveCommand) -> Unit, onForward: () -> Unit, onReverse: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onStop: () -> Unit, onDismiss: () -> Unit, liveFrame: ImageBitmap?, connectionState: LiveConnectionState, onRetry: () -> Unit) {
    // Force the screen to landscape for a proper wide camera view.
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF071117)) {
        Box(Modifier.fillMaxSize()) {
            // Live feed (remote relay) when healthy, a "check connection" hint while not, else "CAMERA OFF".
            if (cameraEnabled && liveFrame != null) {
                Image(bitmap = liveFrame, contentDescription = "Live robot feed", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else if (cameraEnabled) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LiveConnectionHint(connectionState, Modifier, onRetry) }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("CAMERA OFF", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
            }
            // Top-left: "Live camera" label + ON/OFF switch.
            Row(Modifier.align(Alignment.TopStart).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Live camera", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp))
                Switch(cameraEnabled, onCheckedChange = { onCameraToggle() })
            }
            // Top-center: current speed in m/s (green when slow, red when fast).
            Text("$speedMetersPerSecond m/s", modifier = Modifier.align(Alignment.TopCenter).padding(24.dp), color = speedColor(speedMetersPerSecond), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            // Top-right: red EXIT VIEW button to leave full-screen.
            Button(onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White)) { Text("EXIT VIEW", fontWeight = FontWeight.Bold) }
            // Bottom-right: joystick (compact) when in JOYSTICK mode, else the digital buttons.
            if (mode == DrivingMode.JOYSTICK) {
                Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) { JoystickControl(onChanged = onJoystickChanged, compact = true, modifier = Modifier.padding(12.dp)) }
            } else {
                DigitalDriveControls(onForward, onReverse, onLeft, onRight, onStop, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
            }
        }
    }
}

// Walks up the Context chain to find the hosting Activity (used to force landscape orientation).
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// (Reserved) Compact status card — not currently wired into the screen layout.
@Composable private fun CompactStatus(state: ManualUiState) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        MiniStatus("BATTERY", "${state.robotStatus.batteryPercent}%", SignalGreen, Modifier.weight(1f)); MiniStatus("GPS", state.robotStatus.gpsStatus, SignalGreen, Modifier.weight(1f)); MiniStatus("VCU", "Connected", SignalGreen, Modifier.weight(1f)); MiniStatus("MODE", "Manual", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}
// One small readout used inside CompactStatus.
@Composable private fun MiniStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(modifier) { Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall); Text(value, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1) }

// Segmented control: pick DIGITAL (tap buttons) or JOYSTICK (drag) drive mode.
@Composable private fun DrivingModeSelector(selected: DrivingMode, onSelected: (DrivingMode) -> Unit) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(Modifier.padding(4.dp)) { ModeOption("DIGITAL", selected == DrivingMode.DIGITAL, Modifier.weight(1f)) { onSelected(DrivingMode.DIGITAL) }; ModeOption("JOYSTICK", selected == DrivingMode.JOYSTICK, Modifier.weight(1f)) { onSelected(DrivingMode.JOYSTICK) } }
}
// One selectable segment (button) inside the mode selector; highlighted green when selected.
@Composable private fun ModeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) = Button(onClick, modifier.height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) SignalGreen else Color.Transparent, contentColor = if (selected) Color(0xFF062112) else MutedText), elevation = null) { Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }

// Speed read-out line. Green when ≤3 m/s (safe), red above that.
@Composable private fun VehicleSpeed(speed: Int) = Text("VEHICLE SPEED: $speed m/s", color = speedColor(speed), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
private fun speedColor(speedMetersPerSecond: Int): Color = if (speedMetersPerSecond <= 3) SignalGreen else SafetyRed

// The 5 digital drive buttons: FORWARD / LEFT / STOP / RIGHT / REVERSE in a cross layout.
@Composable private fun DigitalDriveControls(onForward: () -> Unit, onReverse: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    DriveButton("FORWARD", Icons.Default.ArrowUpward, SignalGreen, onForward); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { DriveButton("LEFT", Icons.Default.KeyboardArrowLeft, MaterialTheme.colorScheme.secondary, onLeft); Button(onStop, Modifier.size(96.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)) { Icon(Icons.Default.StopCircle, "Stop", Modifier.size(42.dp)) }; DriveButton("RIGHT", Icons.Default.KeyboardArrowRight, MaterialTheme.colorScheme.secondary, onRight) }; Spacer(Modifier.height(10.dp)); DriveButton("REVERSE", Icons.Default.ArrowDownward, Color(0xFFFF9F43), onReverse)
}
// A single round drive button (icon-only) used by DigitalDriveControls.
@Composable private fun DriveButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) = Button(onClick, Modifier.size(88.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = color)) { Icon(icon, label, modifier = Modifier.size(38.dp)) }

// Virtual joystick: drag the green knob to drive. Direction is chosen by the larger axis,
// magnitude (0–1) maps to speed. Releasing snaps back to center and sends STOP.
@Composable
private fun JoystickControl(
    onChanged: (Float, DriveCommand) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var stick by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    // Smaller sizing when shown compact (inside the full-screen overlay).
    val controlSize = if (compact) 150.dp else 190.dp
    val maxDistance = with(density) { (if (compact) 52.dp else 68.dp).toPx() }
    val joystickTrack = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(controlSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        // On release: recenter and stop.
                        onDragEnd = {
                            stick = Offset.Zero
                            onChanged(0f, DriveCommand.STOP)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val next = stick + dragAmount
                        // Clamp the knob to the circular track.
                        stick = if (next.getDistance() > maxDistance) next / next.getDistance() * maxDistance else next
                        // Pick direction from the dominant axis (horizontal = left/right, vertical = fwd/rev).
                        val command = when {
                            abs(stick.x) > abs(stick.y) && stick.x > 0 -> DriveCommand.RIGHT
                            abs(stick.x) > abs(stick.y) -> DriveCommand.LEFT
                            stick.y > 0 -> DriveCommand.REVERSE
                            else -> DriveCommand.FORWARD
                        }
                        onChanged(stick.getDistance() / maxDistance, command)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Track circle + ring.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(joystickTrack)
                drawCircle(SignalGreen.copy(alpha = .45f), style = Stroke(2.dp.toPx()))
            }
            // The draggable green knob.
            Surface(
                modifier = Modifier.offset { IntOffset(stick.x.roundToInt(), stick.y.roundToInt()) }.size(76.dp),
                shape = CircleShape,
                color = SignalGreen
            ) {
                Icon(Icons.Default.SettingsInputComponent, "Joystick", tint = Color(0xFF062112), modifier = Modifier.padding(19.dp))
            }
        }
        if (!compact) {
            Spacer(Modifier.height(12.dp))
            Text("DRAG TO DRIVE", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// PTO card: a power icon + "PTO xx%" label (green when on, grey when off) + enable switch,
// and a speed slider that is only interactive while PTO is enabled.
@Composable private fun PtoControls(state: ManualUiState, onToggle: (Boolean) -> Unit, onSpeed: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = if (state.ptoEnabled) SignalGreen else MutedText, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("PTO  ${state.ptoSpeedPercent}%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (state.ptoEnabled) SignalGreen else MutedText); Switch(state.ptoEnabled, onToggle) }; Slider(state.ptoSpeedPercent.toFloat(), { onSpeed(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f, enabled = state.ptoEnabled) } }

// Hydraulic card: power icon + "HYDRAULIC xx%" label + enable switch + height slider
// (slider only interactive while hydraulic is enabled).
@Composable private fun HydraulicHeightControl(enabled: Boolean, height: Int, onToggle: (Boolean) -> Unit, onHeightChanged: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = if (enabled) SignalGreen else MutedText, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("HYDRAULIC  $height%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Switch(enabled, onToggle) }; Slider(height.toFloat(), { onHeightChanged(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f, enabled = enabled) } }

// Quick-access row: WORK LIGHTS (toggle), HORN (action), CAMERA LIGHT (toggle), LEARNING (toggle).
// LEARNING switch marks the operator's intent to record manual driving for future AI training;
// the recording pipeline itself is implemented later.
@Composable private fun QuickControls(state: ManualUiState, onLights: (Boolean) -> Unit, onHorn: () -> Unit, onCameraLight: (Boolean) -> Unit, onLearning: (Boolean) -> Unit) = Column { Text("QUICK CONTROLS", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickToggle("Work Lights", Icons.Default.Lightbulb, state.lightsEnabled, Modifier.weight(1f), onLights); QuickAction("Horn", Icons.Default.VolumeUp, Modifier.weight(1f), onHorn); QuickToggle("Camera Light", Icons.Default.CameraAlt, state.cameraLightEnabled, Modifier.weight(1f), onCameraLight); QuickToggle("Learning", Icons.Default.Psychology, state.learningEnabled, Modifier.weight(1f), onLearning) } }
// A tappable square toggle (icon + label) that flips its checked state.
@Composable private fun QuickToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) = Card(onClick = { onChange(!checked) }, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (checked) SignalGreen.copy(alpha = .18f) else MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = if (checked) SignalGreen else MutedText); Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } }
// A tappable square action button (icon + label) that runs onClick.
@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) = Card(onClick = onClick, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = MutedText); Text(label, style = MaterialTheme.typography.labelSmall) } }

// (Reserved) Bottom bar with a big E-STOP and a Bluetooth/ESP32 connection indicator.
// Not currently wired into the screen layout.
@Composable private fun ManualBottomBar(onEmergencyStop: () -> Unit, connected: Boolean) = Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 6.dp)) { Button(onEmergencyStop, Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)) { Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp)); Text("  EMERGENCY STOP", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium) }; Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(7.dp), shape = CircleShape, color = if (connected) SignalGreen else MutedText) {}; Spacer(Modifier.width(6.dp)); Text(if (connected) "BLUETOOTH • ESP32 CONNECTED" else "BLUETOOTH • DISCONNECTED", color = MutedText, style = MaterialTheme.typography.labelSmall) } }
