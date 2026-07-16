package com.bhoomibot.os.feature.camera
import androidx.compose.runtime.Composable
import com.bhoomibot.os.feature.common.OperationalScreen
// Camera / perception feed screen.
// Currently a placeholder (shows "Module ready"). The real live-feed UI lives in Manual mode's
// camera preview for now; this screen will later host a dedicated full camera/AI view.
@Composable fun CameraScreen(onBackClick: () -> Unit) = OperationalScreen("Camera", "Perception feed", onBackClick)
