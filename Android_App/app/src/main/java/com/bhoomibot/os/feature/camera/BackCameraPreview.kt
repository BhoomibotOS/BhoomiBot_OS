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

/** Lifecycle-aware CameraX preview using the device's back camera.
 *  Shows the live camera feed inside any Compose layout. If camera permission is missing it
 *  prompts the user and otherwise displays a "permission required" message.
 *
 *  IMPORTANT: everything that touches CameraX lives behind a [Throwable]-level guard. A camera
 *  failure (no back camera, a CameraX/lifecycle version skew, or an async PreviewView surface
 *  error) can surface as an [Error] rather than an [Exception]; if that escapes it crashes the
 *  whole app (which on a device looks like the app "minimizing"). Containing it here keeps the
 *  Manual screen — and its drive controls — usable even when the camera can't start. */
@Composable
fun BackCameraPreview(modifier: Modifier = Modifier, torchEnabled: Boolean = false) {
    val context = LocalContext.current
    // Tracks whether the app currently has the CAMERA permission (checked once at start).
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    // Registers a permission request popup; updates hasPermission when the user responds.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }
    // When permission isn't granted yet, automatically launch the request dialog (once per change).
    LaunchedEffect(hasPermission) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // No permission → show a friendly message instead of the camera, and stop here.
    if (!hasPermission) {
        Box(modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Text("Camera permission is required for live video.")
        }
        return
    }

    // All CameraX access is isolated + guarded inside CameraPreviewContent so a camera failure
    // (no back camera, a version skew, or an async PreviewView error) is shown as a message
    // instead of crashing the app to the background. (Compose forbids try/catch *around* a
    // composable call, so the guards live *inside* the composable instead.)
    CameraPreviewContent(modifier, torchEnabled)
}

/** All CameraX references live here. Every camera operation is guarded with [Throwable] (not just
 *  [Exception]) because camera failures can surface as link/runtime [Error]s that would otherwise
 *  crash the whole app. The controller is created inside [remember] so a construction failure is
 *  contained and reported instead of escaping. */
@Composable
private fun CameraPreviewContent(modifier: Modifier, torchEnabled: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // The CameraX controller manages the camera session. Created inside remember and guarded so a
    // construction/link error (e.g. a CameraX <-> lifecycle version mismatch) is contained.
    val controller = remember {
        try { LifecycleCameraController(context) }
        catch (e: Throwable) { null }
    }
    // Tracks a binding failure (e.g. no back camera available) so we can show a message
    // instead of letting bindToLifecycle throw and crash the app.
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Controller failed to construct → show a message and stop.
    if (controller == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Text("Camera unavailable: failed to initialize camera")
        }
        return
    }

    // Bind the camera to this screen's lifecycle: open on show, release on leave.
    DisposableEffect(controller, lifecycleOwner) {
        cameraError = null
        try {
            controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            controller.bindToLifecycle(lifecycleOwner)
        } catch (e: Throwable) {
            cameraError = e.localizedMessage ?: "Unable to open camera"
        }
        onDispose { runCatching { controller.unbind() } }
    }
    // Turn the torch/flash on or off whenever torchEnabled changes (ignore if unsupported).
    LaunchedEffect(controller, torchEnabled) {
        runCatching { controller.enableTorch(torchEnabled) }
    }
    // A binding failure (no camera, etc.) shows a friendly message instead of crashing.
    if (cameraError != null) {
        Box(modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Text("Camera unavailable: $cameraError")
        }
        return
    }
    // Hosts the classic Android PreviewView (a normal View) inside Compose.
    AndroidView(
        modifier = modifier,
        factory = { viewContext -> PreviewView(viewContext).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            this.controller = controller
        } },
        update = { it.controller = controller }
    )
}
