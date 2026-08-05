package com.bhoomibot.os.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bhoomibot.os.model.Waypoint
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.MutedText

/**
 * A custom high-tech field map that renders GPS waypoints on a coordinate grid.
 * 
 * Scaled automatically to fit all waypoints in the mission. Supports zoom and pan.
 */
@Composable
fun FieldMap(
    waypoints: List<Waypoint>,
    modifier: Modifier = Modifier,
    robotPosition: Waypoint? = null
) {
    if (waypoints.isEmpty()) {
        Box(modifier.background(Color.Black.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Text("No GPS Data Available", color = MutedText)
        }
        return
    }

    // 1. Calculate Bounds
    val minLat = waypoints.minOf { it.latitude }
    val maxLat = waypoints.maxOf { it.latitude }
    val minLon = waypoints.minOf { it.longitude }
    val maxLon = waypoints.maxOf { it.longitude }

    val latRange = (maxLat - minLat).coerceAtLeast(0.00001)
    val lonRange = (maxLon - minLon).coerceAtLeast(0.00001)

    // Interaction State
    var zoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF0A0F12)) // Deep tech background
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                zoom *= gestureZoom
                offset += pan
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            val width = size.width
            val height = size.height

            // Helper to project GPS to Screen Pixels
            fun project(wp: Waypoint): Offset {
                val x = ((wp.longitude - minLon) / lonRange).toFloat() * width
                val y = (1f - ((wp.latitude - minLat) / latRange).toFloat()) * height
                return (Offset(x, y) * zoom) + offset
            }

            // 2. Draw Grid Lines
            val gridStep = 50f * zoom
            for (x in 0..(width / gridStep).toInt() + 10) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(x * gridStep + offset.x % gridStep, 0f), Offset(x * gridStep + offset.x % gridStep, height))
            }
            for (y in 0..(height / gridStep).toInt() + 10) {
                drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y * gridStep + offset.y % gridStep), Offset(width, y * gridStep + offset.y % gridStep))
            }

            // 3. Draw Path
            if (waypoints.size > 1) {
                val path = Path()
                val start = project(waypoints[0])
                path.moveTo(start.x, start.y)

                for (i in 1 until waypoints.size) {
                    val p = project(waypoints[i])
                    path.lineTo(p.x, p.y)
                }

                drawPath(
                    path = path,
                    color = SignalGreen,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 4. Draw Waypoints (Dots)
            waypoints.forEach { wp ->
                drawCircle(
                    color = SignalGreen.copy(alpha = 0.5f),
                    radius = 4.dp.toPx(),
                    center = project(wp)
                )
            }

            // 5. Draw Robot Current Position
            robotPosition?.let {
                val robotCoord = project(it)
                drawCircle(Color.White, radius = 8.dp.toPx(), center = robotCoord)
                drawCircle(SignalGreen, radius = 6.dp.toPx(), center = robotCoord)
            }
        }

        // Overlay Info
        Column(Modifier.padding(16.dp).align(Alignment.BottomStart)) {
            Text("FIELD MAP VIEW", color = SignalGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${waypoints.size} GPS Points Recorded", color = Color.White, fontSize = 12.sp)
            Text("Lat: ${String.format("%.6f", minLat)}..${String.format("%.6f", maxLat)}", color = MutedText, fontSize = 10.sp)
        }
    }
}
