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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.feature.live.OperatorLiveViewModel
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlScreen(onBackClick: () -> Unit, viewModel: ManualViewModel = viewModel(), liveViewModel: OperatorLiveViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val liveState by liveViewModel.uiState.collectAsState()
    val onLiveCameraToggle: (Boolean) -> Unit = { enabled ->
        viewModel.setCameraEnabled(enabled)
        liveViewModel.sendLiveCamera(enabled)
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { TopStatusBar(state, viewModel::onEmergencyStop) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") } }) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                // Main Controls (Left Side)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    ControlPathSelector(state.controlPath, viewModel::setControlPath)
                    Spacer(Modifier.height(12.dp))
                    
                    if (!state.isCameraMaximized) {
                        LiveCameraPreview(
                            onMaximize = { viewModel.setCameraMaximized(true) },
                            cameraEnabled = state.cameraEnabled,
                            useRearCamera = state.useRearCamera,
                            onCameraToggle = { onLiveCameraToggle(!state.cameraEnabled) },
                            onFlipCamera = { viewModel.setUseRearCamera(!state.useRearCamera) },
                            liveFrame = liveState.frame,
                            connectionState = liveState.connectionState,
                            onRetry = liveViewModel::retry
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    DrivingModeSelector(state.drivingMode, viewModel::setDrivingMode)
                    Spacer(Modifier.height(12.dp))
                    VehicleSpeed(state.vehicleSpeedPercent)
                    Spacer(Modifier.height(2.dp))
                    RpmDisplay(state.leftRpm, state.rightRpm)
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(targetState = state.drivingMode, label = "drivingMode") { mode ->
                        if (mode == DrivingMode.DIGITAL) DigitalDriveControls(viewModel::onForward, viewModel::onReverse, viewModel::onLeft, viewModel::onRight, viewModel::onStop)
                        else if (!state.isCameraMaximized) JoystickControl(viewModel::onJoystickChanged, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(16.dp))
                    PtoControls(state, viewModel::onPtoToggle, viewModel::onPtoSpeedChanged)
                    Spacer(Modifier.height(16.dp))
                    HydraulicHeightControl(state.hydraulicEnabled, state.hydraulicHeightPercent, viewModel::onHydraulicToggle, viewModel::onHydraulicHeightChanged)
                    Spacer(Modifier.height(16.dp))
                    QuickControls(state, viewModel::onLightsToggle, viewModel::onHorn, viewModel::onCameraLightToggle, viewModel::onLearningToggle)
                    Spacer(Modifier.height(18.dp))
                }

                // Action Markers Panel (Right Side - Visible during Recording)
                if (state.isRecording) {
                    ActionMarkerPanel(
                        onAddMarker = viewModel::onAddMarker,
                        onFinish = viewModel::onFinishTeaching
                    )
                }
            }
        }

        if (state.isCameraMaximized) {
            MaximizedCameraOverlay(
                mode = state.drivingMode,
                speedMetersPerSecond = state.vehicleSpeedPercent,
                leftRpm = state.leftRpm,
                rightRpm = state.rightRpm,
                cameraEnabled = state.cameraEnabled,
                useRearCamera = state.useRearCamera,
                onCameraToggle = { onLiveCameraToggle(!state.cameraEnabled) },
                onFlipCamera = { viewModel.setUseRearCamera(!state.useRearCamera) },
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

@Composable
private fun ActionMarkerPanel(
    onAddMarker: (String) -> Unit,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.width(80.dp).fillMaxHeight(),
        color = Color.Black.copy(alpha = 0.6f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("MARKERS", style = MaterialTheme.typography.labelSmall, color = SignalGreen, fontWeight = FontWeight.Bold)
            
            MarkerButton("PICKUP", Icons.Default.Inbox, SignalGreen) { onAddMarker("PICKUP") }
            MarkerButton("DROP", Icons.Default.Outbox, SafetyRed) { onAddMarker("DROP") }
            MarkerButton("WAIT", Icons.Default.HourglassEmpty, Color.Yellow) { onAddMarker("WAIT") }
            
            Spacer(Modifier.weight(1f))
            
            IconButton(
                onClick = onFinish,
                modifier = Modifier.size(56.dp).background(SignalGreen, CircleShape)
            ) {
                Icon(Icons.Default.Check, "Finish", tint = Color.Black)
            }
        }
    }
}

@Composable
private fun MarkerButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Icon(icon, label, tint = color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable private fun TopStatusBar(state: ManualUiState, onEmergencyStop: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    // 1. Phone Battery (Local Handheld if Direct, Remote Robot Phone if Via Robot)
    val phoneBattery = if (state.controlPath == ControlPath.DIRECT_VCU) state.localPhoneBattery else state.robotStatus.batteryPercent
    TopStatus("PH", "$phoneBattery%", SignalGreen, Modifier.weight(1f))
    
    // 2. Robot Battery (Actual Hardware/Chassis Battery via VCU ADC)
    TopStatus("RB", "${state.robotStatus.vcuBattery}%", SignalGreen, Modifier.weight(1f))

    TopStatus("GPS", state.robotStatus.gpsStatus, if (state.robotStatus.gpsStatus == "No Signal") SafetyRed else SignalGreen, Modifier.weight(1f))
    TopStatus("VCU", if (state.bluetoothConnected) "Connected" else "Offline", if (state.bluetoothConnected) SignalGreen else SafetyRed, Modifier.weight(1f))
    TopStatus("MODE", if (state.isRecording) "TEACHING" else "Manual", if (state.isRecording) SafetyRed else MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    Button(onEmergencyStop, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White)) { Text("E-STOP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
}

@Composable private fun TopStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall); Text(value, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1) }

@Composable
private fun ControlPathSelector(selected: ControlPath, onSelected: (ControlPath) -> Unit) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PathOption("DIRECT VCU", selected == ControlPath.DIRECT_VCU, Modifier.weight(1f)) { onSelected(ControlPath.DIRECT_VCU) }
        PathOption("VIA ROBOT", selected == ControlPath.VIA_ROBOT, Modifier.weight(1f)) { onSelected(ControlPath.VIA_ROBOT) }
    }
}

@Composable
private fun PathOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) = Button(onClick = onClick, modifier = modifier.height(36.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) SignalGreen else Color.Transparent, contentColor = if (selected) Color.Black else MutedText), elevation = null) { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }

@Composable private fun LiveCameraPreview(
    onMaximize: () -> Unit, 
    cameraEnabled: Boolean, 
    useRearCamera: Boolean,
    onCameraToggle: () -> Unit, 
    onFlipCamera: () -> Unit,
    liveFrame: ImageBitmap?, 
    connectionState: LiveConnectionState, 
    onRetry: () -> Unit
) = Card(onClick = onMaximize, modifier = Modifier.fillMaxWidth().height(360.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B22))) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (cameraEnabled) {
            if (liveFrame != null) {
                Image(bitmap = liveFrame, contentDescription = "Live robot feed", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Button(onMaximize, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) { Text("FULL SCREEN", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall) }
            } else { LiveConnectionHint(connectionState, Modifier.align(Alignment.Center), onRetry) }
        } else { Text("CAMERA OFF", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Row(Modifier.align(Alignment.TopStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onFlipCamera) {
                Icon(
                    Icons.Default.Cached,
                    contentDescription = "Flip Camera",
                    tint = if (useRearCamera) Color.White else SignalGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("Live camera", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Switch(cameraEnabled, onCheckedChange = { onCameraToggle() })
        }
    }
}

@Composable private fun LiveConnectionHint(state: LiveConnectionState, modifier: Modifier = Modifier, onRetry: () -> Unit) = Column(modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    val (title, detail) = when (state) {
        LiveConnectionState.ERROR -> "Check connection" to "Couldn't reach relay."
        LiveConnectionState.RECONNECTING -> "Reconnecting…" to "Trying to re-establish."
        LiveConnectionState.CONNECTING -> "Connecting…" to "Connecting to relay."
        LiveConnectionState.CONNECTED -> "Waiting for feed…" to "No video yet."
        LiveConnectionState.IDLE -> "Not connected" to "Check settings."
    }
    Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(detail, color = MutedText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    if (state == LiveConnectionState.ERROR) {
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) { Text("Retry", fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun MaximizedCameraOverlay(
    mode: DrivingMode, 
    speedMetersPerSecond: Int, 
    leftRpm: Int, 
    rightRpm: Int, 
    cameraEnabled: Boolean, 
    useRearCamera: Boolean,
    onCameraToggle: () -> Unit, 
    onFlipCamera: () -> Unit,
    onJoystickChanged: (Float, DriveCommand) -> Unit, 
    onForward: () -> Unit, 
    onReverse: () -> Unit, 
    onLeft: () -> Unit, 
    onRight: () -> Unit, 
    onStop: () -> Unit, 
    onDismiss: () -> Unit, 
    liveFrame: ImageBitmap?, 
    connectionState: LiveConnectionState, 
    onRetry: () -> Unit
) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val prev = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = prev ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF071117)) {
        Box(Modifier.fillMaxSize()) {
            if (cameraEnabled && liveFrame != null) Image(bitmap = liveFrame, contentDescription = "Live", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else if (cameraEnabled) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LiveConnectionHint(connectionState, Modifier, onRetry) }
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("CAMERA OFF", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold) }
            Row(Modifier.align(Alignment.TopStart).padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFlipCamera) {
                    Icon(
                        Icons.Default.Cached,
                        contentDescription = "Flip",
                        tint = if (useRearCamera) Color.White else SignalGreen
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text("Live camera", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(12.dp)); Switch(cameraEnabled, onCheckedChange = { onCameraToggle() })
            }
            Text("$speedMetersPerSecond m/s", modifier = Modifier.align(Alignment.TopCenter).padding(24.dp), color = if (speedMetersPerSecond <= 3) SignalGreen else SafetyRed, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            
            // AI-Fix: Added RPM to maximized view
            RpmDisplay(
                left = leftRpm, 
                right = rightRpm,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
            )

            Button(onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White)) { Text("EXIT", fontWeight = FontWeight.Bold) }
            if (mode == DrivingMode.JOYSTICK) {
                Surface(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) { JoystickControl(onChanged = onJoystickChanged, compact = true, modifier = Modifier.padding(12.dp)) }
            } else {
                DigitalDriveControls(onForward, onReverse, onLeft, onRight, onStop, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable private fun DrivingModeSelector(selected: DrivingMode, onSelected: (DrivingMode) -> Unit) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(Modifier.padding(4.dp)) { ModeOption("DIGITAL", selected == DrivingMode.DIGITAL, Modifier.weight(1f)) { onSelected(DrivingMode.DIGITAL) }; ModeOption("JOYSTICK", selected == DrivingMode.JOYSTICK, Modifier.weight(1f)) { onSelected(DrivingMode.JOYSTICK) } }
}
@Composable private fun ModeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) = Button(onClick, modifier.height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) SignalGreen else Color.Transparent, contentColor = if (selected) Color(0xFF062112) else MutedText), elevation = null) { Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }

@Composable private fun VehicleSpeed(speed: Int) = Text("VEHICLE SPEED: $speed m/s", color = if (speed <= 3) SignalGreen else SafetyRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

@Composable
private fun RpmDisplay(left: Int, right: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RpmValue("L", left)
        RpmValue("R", right)
    }
}

@Composable
private fun RpmValue(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", color = MutedText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text("$value RPM", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable private fun DigitalDriveControls(onForward: () -> Unit, onReverse: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    DriveButton("FORWARD", Icons.Default.ArrowUpward, SignalGreen, onForward); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { DriveButton("LEFT", Icons.Default.KeyboardArrowLeft, MaterialTheme.colorScheme.secondary, onLeft); Button(onStop, Modifier.size(96.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)) { Icon(Icons.Default.StopCircle, "Stop", Modifier.size(42.dp)) }; DriveButton("RIGHT", Icons.Default.KeyboardArrowRight, MaterialTheme.colorScheme.secondary, onRight) }; Spacer(Modifier.height(10.dp)); DriveButton("REVERSE", Icons.Default.ArrowDownward, Color(0xFFFF9F43), onReverse)
}
@Composable private fun DriveButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) = Button(onClick, Modifier.size(88.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = color)) { Icon(icon, label, modifier = Modifier.size(38.dp)) }

@Composable
private fun JoystickControl(onChanged: (Float, DriveCommand) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    var stick by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val controlSize = if (compact) 150.dp else 190.dp
    val maxDist = with(density) { (if (compact) 52.dp else 68.dp).toPx() }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(controlSize).pointerInput(Unit) {
            detectDragGestures(onDragEnd = { stick = Offset.Zero; onChanged(0f, DriveCommand.STOP) }) { change, dragAmount ->
                change.consume(); val next = stick + dragAmount
                stick = if (next.getDistance() > maxDist) next / next.getDistance() * maxDist else next
                val command = when {
                    abs(stick.x) > abs(stick.y) && stick.x > 0 -> DriveCommand.RIGHT
                    abs(stick.x) > abs(stick.y) -> DriveCommand.LEFT
                    stick.y > 0 -> DriveCommand.REVERSE
                    else -> DriveCommand.FORWARD
                }
                onChanged(stick.getDistance() / maxDist, command)
            }
        }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawCircle(Color.Gray.copy(alpha = 0.2f)); drawCircle(SignalGreen.copy(alpha = .45f), style = Stroke(2.dp.toPx())) }
            Surface(modifier = Modifier.offset { IntOffset(stick.x.roundToInt(), stick.y.roundToInt()) }.size(76.dp), shape = CircleShape, color = SignalGreen) { Icon(Icons.Default.SettingsInputComponent, null, tint = Color.Black, modifier = Modifier.padding(19.dp)) }
        }
    }
}

@Composable private fun PtoControls(state: ManualUiState, onToggle: (Boolean) -> Unit, onSpeed: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = if (state.ptoEnabled) SignalGreen else MutedText, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("PTO  ${state.ptoSpeedPercent}%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (state.ptoEnabled) SignalGreen else MutedText); Switch(state.ptoEnabled, onToggle) }; Slider(state.ptoSpeedPercent.toFloat(), { onSpeed(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f, enabled = state.ptoEnabled) } }

@Composable private fun HydraulicHeightControl(enabled: Boolean, height: Int, onToggle: (Boolean) -> Unit, onHeightChanged: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = if (enabled) SignalGreen else MutedText, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("HYDRAULIC  $height%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Switch(enabled, onToggle) }; Slider(height.toFloat(), { onHeightChanged(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f, enabled = enabled) } }

@Composable private fun QuickControls(state: ManualUiState, onLights: (Boolean) -> Unit, onHorn: () -> Unit, onCameraLight: (Boolean) -> Unit, onLearning: (Boolean) -> Unit) = Column { Text("QUICK CONTROLS", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickToggle("Work Lights", Icons.Default.Lightbulb, state.lightsEnabled, Modifier.weight(1f), onLights); QuickAction("Horn", Icons.Default.VolumeUp, Modifier.weight(1f), onHorn); QuickToggle("Camera Light", Icons.Default.CameraAlt, state.cameraLightEnabled, Modifier.weight(1f), onCameraLight); QuickToggle("Learning", Icons.Default.Psychology, state.learningEnabled, Modifier.weight(1f), onLearning) } }
@Composable private fun QuickToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) = Card(onClick = { onChange(!checked) }, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (checked) SignalGreen.copy(alpha = .18f) else MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = if (checked) SignalGreen else MutedText); Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } }
@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) = Card(onClick = onClick, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = MutedText); Text(label, style = MaterialTheme.typography.labelSmall) } }
