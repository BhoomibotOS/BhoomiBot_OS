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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.feature.camera.BackCameraPreview
import com.bhoomibot.os.ui.theme.MutedText
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlScreen(onBackClick: () -> Unit, viewModel: ManualViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { TopStatusBar(state, viewModel::onEmergencyStop) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Return to dashboard") } }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (!state.isCameraMaximized) {
                LiveCameraPreview(onMaximize = { viewModel.setCameraMaximized(true) }, torchEnabled = state.cameraLightEnabled)
            }
            Spacer(Modifier.height(12.dp))
            DrivingModeSelector(state.drivingMode, viewModel::setDrivingMode)
            Spacer(Modifier.height(12.dp))
            VehicleSpeed(state.vehicleSpeedPercent)
            Spacer(Modifier.height(4.dp))
            AnimatedContent(targetState = state.drivingMode, label = "drivingMode") { mode ->
                if (mode == DrivingMode.DIGITAL) DigitalDriveControls(viewModel::onForward, viewModel::onReverse, viewModel::onLeft, viewModel::onRight, viewModel::onStop, modifier = Modifier.fillMaxWidth().offset(y = (-8).dp))
                else if (!state.isCameraMaximized) JoystickControl(viewModel::onJoystickChanged, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(20.dp))
            PtoControls(state, viewModel::onPtoToggle, viewModel::onPtoSpeedChanged)
            Spacer(Modifier.height(16.dp))
            HydraulicHeightControl(state.hydraulicEnabled, state.hydraulicHeightPercent, viewModel::onHydraulicToggle, viewModel::onHydraulicHeightChanged)
            Spacer(Modifier.height(16.dp))
            QuickControls(state, viewModel::onLightsToggle, viewModel::onHorn, viewModel::onCameraLightToggle)
            Spacer(Modifier.height(18.dp))
        }
    }
        if (state.isCameraMaximized) {
            MaximizedCameraOverlay(state.drivingMode, state.vehicleSpeedPercent, state.cameraLightEnabled, viewModel::onJoystickChanged, viewModel::onForward, viewModel::onReverse, viewModel::onLeft, viewModel::onRight, viewModel::onStop, onDismiss = { viewModel.setCameraMaximized(false) })
        }
    }
}

@Composable private fun TopStatusBar(state: ManualUiState, onEmergencyStop: () -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TopStatus("BAT", "${state.robotStatus.batteryPercent}%", SignalGreen, Modifier.weight(1f))
    TopStatus("GPS", state.robotStatus.gpsStatus, SignalGreen, Modifier.weight(1f))
    TopStatus("VCU", "Connected", SignalGreen, Modifier.weight(1f))
    TopStatus("MODE", "Manual", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    Button(onEmergencyStop, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp)) { Text("E-STOP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
}
@Composable private fun TopStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall); Text(value, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1) }

/** Normal field preview. Previous height was 250dp; restore it if 360dp is unsuitable. */
@Composable private fun LiveCameraPreview(onMaximize: () -> Unit, torchEnabled: Boolean) = Card(onClick = onMaximize, modifier = Modifier.fillMaxWidth().height(360.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B22))) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BackCameraPreview(Modifier.fillMaxSize(), torchEnabled)
        Text("LIVE CAMERA • 360° READY", modifier = Modifier.align(Alignment.TopStart).padding(16.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Button(onMaximize, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SignalGreen, contentColor = Color(0xFF062112))) { Text("FULL SCREEN", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall) }
        Text("CameraX preview ready", modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), color = MutedText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun PrimaryLightsControl(enabled: Boolean, onToggle: (Boolean) -> Unit) = Card(onClick = { onToggle(!enabled) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (enabled) SignalGreen.copy(alpha = .16f) else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lightbulb, null, tint = if (enabled) SignalGreen else MutedText); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("WORK LIGHTS", fontWeight = FontWeight.Bold); Text(if (enabled) "ON • Field illumination active" else "OFF • Tap to enable", color = MutedText, style = MaterialTheme.typography.labelSmall) }; Switch(enabled, onToggle) } }

@Composable private fun MaximizedCameraOverlay(mode: DrivingMode, speedMetersPerSecond: Int, torchEnabled: Boolean, onJoystickChanged: (Float, DriveCommand) -> Unit, onForward: () -> Unit, onReverse: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onStop: () -> Unit, onDismiss: () -> Unit) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF071117)) {
        Box(Modifier.fillMaxSize()) {
            BackCameraPreview(Modifier.fillMaxSize(), torchEnabled)
            Text("LIVE 360° CAMERA", modifier = Modifier.align(Alignment.TopStart).padding(24.dp), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("$speedMetersPerSecond m/s", modifier = Modifier.align(Alignment.TopCenter).padding(24.dp), color = speedColor(speedMetersPerSecond), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Button(onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed, contentColor = Color.White)) { Text("EXIT VIEW", fontWeight = FontWeight.Bold) }
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

@Composable private fun CompactStatus(state: ManualUiState) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        MiniStatus("BATTERY", "${state.robotStatus.batteryPercent}%", SignalGreen, Modifier.weight(1f)); MiniStatus("GPS", state.robotStatus.gpsStatus, SignalGreen, Modifier.weight(1f)); MiniStatus("VCU", "Connected", SignalGreen, Modifier.weight(1f)); MiniStatus("MODE", "Manual", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}
@Composable private fun MiniStatus(label: String, value: String, color: Color, modifier: Modifier) = Column(modifier) { Text(label, color = MutedText, style = MaterialTheme.typography.labelSmall); Text(value, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1) }

@Composable private fun DrivingModeSelector(selected: DrivingMode, onSelected: (DrivingMode) -> Unit) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(Modifier.padding(4.dp)) { ModeOption("DIGITAL", selected == DrivingMode.DIGITAL, Modifier.weight(1f)) { onSelected(DrivingMode.DIGITAL) }; ModeOption("JOYSTICK", selected == DrivingMode.JOYSTICK, Modifier.weight(1f)) { onSelected(DrivingMode.JOYSTICK) } }
}
@Composable private fun ModeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) = Button(onClick, modifier.height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) SignalGreen else Color.Transparent, contentColor = if (selected) Color(0xFF062112) else MutedText), elevation = null) { Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }

@Composable private fun VehicleSpeed(speed: Int) = Text("VEHICLE SPEED: $speed m/s", color = speedColor(speed), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
private fun speedColor(speedMetersPerSecond: Int): Color = if (speedMetersPerSecond <= 3) SignalGreen else SafetyRed

@Composable private fun DigitalDriveControls(onForward: () -> Unit, onReverse: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) = Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    DriveButton("FORWARD", Icons.Default.ArrowUpward, SignalGreen, onForward); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { DriveButton("LEFT", Icons.Default.KeyboardArrowLeft, MaterialTheme.colorScheme.secondary, onLeft); Button(onStop, Modifier.size(96.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)) { Icon(Icons.Default.StopCircle, "Stop", Modifier.size(42.dp)) }; DriveButton("RIGHT", Icons.Default.KeyboardArrowRight, MaterialTheme.colorScheme.secondary, onRight) }; Spacer(Modifier.height(10.dp)); DriveButton("REVERSE", Icons.Default.ArrowDownward, Color(0xFFFF9F43), onReverse)
}
@Composable private fun DriveButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) = Button(onClick, Modifier.size(88.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = color)) { Icon(icon, label, modifier = Modifier.size(38.dp)) }

@Composable
private fun JoystickControl(
    onChanged: (Float, DriveCommand) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    var stick by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val controlSize = if (compact) 150.dp else 190.dp
    val maxDistance = with(density) { (if (compact) 52.dp else 68.dp).toPx() }
    val joystickTrack = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(controlSize)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            stick = Offset.Zero
                            onChanged(0f, DriveCommand.STOP)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val next = stick + dragAmount
                        stick = if (next.getDistance() > maxDistance) next / next.getDistance() * maxDistance else next
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
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(joystickTrack)
                drawCircle(SignalGreen.copy(alpha = .45f), style = Stroke(2.dp.toPx()))
            }
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

@Composable private fun PtoControls(state: ManualUiState, onToggle: (Boolean) -> Unit, onSpeed: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = SignalGreen, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("PTO  ${state.ptoSpeedPercent}%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Switch(state.ptoEnabled, onToggle) }; Slider(state.ptoSpeedPercent.toFloat(), { onSpeed(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f) } }

@Composable private fun HydraulicHeightControl(enabled: Boolean, height: Int, onToggle: (Boolean) -> Unit, onHeightChanged: (Int) -> Unit) = Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Power, null, tint = if (enabled) SignalGreen else MutedText, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("HYDRAULIC  $height%", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Switch(enabled, onToggle) }; Slider(height.toFloat(), { onHeightChanged(it.toInt()) }, modifier = Modifier.height(24.dp), valueRange = 0f..100f, enabled = enabled) } }

@Composable private fun QuickControls(state: ManualUiState, onLights: (Boolean) -> Unit, onHorn: () -> Unit, onCameraLight: (Boolean) -> Unit) = Column { Text("QUICK CONTROLS", color = MutedText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickToggle("Work Lights", Icons.Default.Lightbulb, state.lightsEnabled, Modifier.weight(1f), onLights); QuickAction("Horn", Icons.Default.VolumeUp, Modifier.weight(1f), onHorn); QuickToggle("Camera Light", Icons.Default.CameraAlt, state.cameraLightEnabled, Modifier.weight(1f), onCameraLight) } }
@Composable private fun QuickToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) = Card(onClick = { onChange(!checked) }, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (checked) SignalGreen.copy(alpha = .18f) else MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = if (checked) SignalGreen else MutedText); Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) } }
@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) = Card(onClick = onClick, modifier = modifier.height(88.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, label, tint = MutedText); Text(label, style = MaterialTheme.typography.labelSmall) } }

@Composable private fun ManualBottomBar(onEmergencyStop: () -> Unit, connected: Boolean) = Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 6.dp)) { Button(onEmergencyStop, Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SafetyRed)) { Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp)); Text("  EMERGENCY STOP", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium) }; Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(7.dp), shape = CircleShape, color = if (connected) SignalGreen else MutedText) {}; Spacer(Modifier.width(6.dp)); Text(if (connected) "BLUETOOTH • ESP32 CONNECTED" else "BLUETOOTH • DISCONNECTED", color = MutedText, style = MaterialTheme.typography.labelSmall) } }
