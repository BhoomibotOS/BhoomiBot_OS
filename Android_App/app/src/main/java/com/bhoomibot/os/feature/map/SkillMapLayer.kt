package com.bhoomibot.os.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bhoomibot.os.feature.autonomous.core.model.RobotPose
import com.bhoomibot.os.feature.autonomous.skills.models.ActionType
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import com.bhoomibot.os.ui.theme.SignalGreen
import com.bhoomibot.os.ui.theme.SafetyRed

/**
 * FEATURE: Skill Map Layer with Ghost Robot
 */
@Composable
fun SkillMapLayer(
    skill: DemonstratedSkill,
    ghostPose: RobotPose?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val points = skill.steps
            .filter { it.latitude != null && it.longitude != null }
            .sortedBy { it.sequence }

        if (points.isEmpty()) return@Canvas

        // Robust Scaling Logic: Map GPS to Screen Pixels
        val minLat = points.minOf { it.latitude!! }
        val maxLat = points.maxOf { it.latitude!! }
        val minLon = points.minOf { it.longitude!! }
        val maxLon = points.maxOf { it.longitude!! }
        
        val latRange = (maxLat - minLat).coerceAtLeast(0.0000001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0000001)
        
        fun project(lat: Double, lon: Double): Offset {
            val padding = 50f
            val x = ((lon - minLon) / lonRange * (size.width - 2 * padding) + padding).toFloat()
            val y = ((1.0 - (lat - minLat) / latRange) * (size.height - 2 * padding) + padding).toFloat()
            return Offset(x, y)
        }

        // 1. Draw the Path (Dashed) - Only if we have multiple points
        if (points.size >= 2) {
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = SignalGreen.copy(alpha = 0.3f),
                    start = project(points[i].latitude!!, points[i].longitude!!),
                    end = project(points[i+1].latitude!!, points[i+1].longitude!!),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            }
        }

        // 2. Draw the Markers
        points.forEach { step ->
            val color = when (step.actionType) {
                ActionType.ATTACH -> SignalGreen
                ActionType.DETACH -> SafetyRed
                ActionType.WAIT -> Color.Yellow
                else -> Color.Gray
            }
            drawCircle(color, radius = 5.dp.toPx(), center = project(step.latitude!!, step.longitude!!))
        }

        // 3. Draw the Ghost Robot (If active)
        ghostPose?.let { pose ->
            val robotPos = project(pose.latitude, pose.longitude)
            
            // Pulse Effect
            drawCircle(
                color = SignalGreen.copy(alpha = 0.2f),
                radius = 20.dp.toPx(),
                center = robotPos
            )
            
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = robotPos,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Heading Indicator
            val headingRad = Math.toRadians(pose.heading - 90.0).toFloat()
            val lineLength = 15.dp.toPx()
            drawLine(
                color = SignalGreen,
                start = robotPos,
                end = Offset(
                    robotPos.x + Math.cos(headingRad.toDouble()).toFloat() * lineLength,
                    robotPos.y + Math.sin(headingRad.toDouble()).toFloat() * lineLength
                ),
                strokeWidth = 3.dp.toPx()
            )

            drawCircle(
                color = SignalGreen,
                radius = 4.dp.toPx(),
                center = robotPos
            )
        }
    }
}
