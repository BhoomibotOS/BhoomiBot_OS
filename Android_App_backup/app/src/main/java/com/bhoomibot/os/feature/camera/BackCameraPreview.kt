package com.bhoomibot.os.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat

/** Lifecycle-aware CameraX preview using the device's back camera. */
@Composable
fun BackCameraPreview(modifier: Modifier = Modifier, torchEnabled: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasPermission = permissions[Manifest.permission.CAMERA] == true || hasPermission
    }
    LaunchedEffect(hasPermission) {
        if (!hasPermission || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    if (!hasPermission) {
        Box(modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Text("Camera permission is required for live video.")
        }
        return
    }

    val controller = remember { LifecycleCameraController(context) }
    DisposableEffect(controller, lifecycleOwner) {
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    LaunchedEffect(controller, torchEnabled) {
        controller.enableTorch(torchEnabled)
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext -> PreviewView(viewContext).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            this.controller = controller
        } },
        update = { it.controller = controller }
    )
}
