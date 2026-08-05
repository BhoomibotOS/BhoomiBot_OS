package com.bhoomibot.os.feature.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.service.RobotCameraManager
import com.bhoomibot.os.ui.theme.SafetyRed
import com.bhoomibot.os.ui.theme.SignalGreen

@Composable
fun RobotLiveScreen(
    onBackClick: () -> Unit,
    onOpenOptions: () -> Unit,
    viewModel: RobotLiveViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(state.connectionState) {
        val msg = when (state.connectionState) {
            LiveConnectionState.CONNECTED -> "ROBOT: Connected to relay (Live)"
            LiveConnectionState.CONNECTING -> "ROBOT: Connecting..."
            else -> "ROBOT: Offline"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.startBroadcast()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            // High-Tech Preview: The UI simply attaches a View to the singleton manager.
            // This manager is driven by the Background Service.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // Attach the surface to the background camera loop
                        RobotCameraManager.setPreviewView(this)
                    }
                },
                onRelease = {
                    RobotCameraManager.setPreviewView(null)
                }
            )
            
            // AI Vision Overlay (Robot Side)
            Canvas(Modifier.fillMaxSize()) {
                state.detectedObjects.forEach { obj ->
                    val rect = obj.boundingBox
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(rect.left * size.width, rect.top * size.height),
                        size = Size(rect.width() * size.width, rect.height() * size.height),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Top bar
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Text("ROBOT LIVE", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            ConnectionBadge(state.connectionState, Modifier)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onOpenOptions) { Icon(Icons.Default.Settings, "Options", tint = Color.White) }
        }

        // Bottom Controls
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val broadcasting = state.isBroadcasting
            
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.flipCamera() }) {
                        Icon(
                            Icons.Default.Cached, 
                            null, 
                            tint = SignalGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { if (broadcasting) viewModel.stopBroadcast() else viewModel.startBroadcast() },
                        enabled = broadcasting || state.isConfigurationReady,
                        colors = ButtonDefaults.buttonColors(containerColor = if (broadcasting) SafetyRed else SignalGreen)
                    ) {
                        Icon(if (broadcasting) Icons.Default.VideocamOff else Icons.Default.Videocam, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (broadcasting) "STOP BROADCAST" else "START BROADCAST")
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.framesSent} frames processed",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
            
            // AI Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = if (state.aiStatus.contains("Ready")) SignalGreen else Color.Gray, modifier = Modifier.size(8.dp)) {}
                Spacer(Modifier.width(8.dp))
                Text(state.aiStatus, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
