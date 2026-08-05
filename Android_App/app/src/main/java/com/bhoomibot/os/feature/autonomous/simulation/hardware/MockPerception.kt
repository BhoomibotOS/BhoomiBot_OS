package com.bhoomibot.os.feature.autonomous.simulation.hardware

import android.graphics.RectF
import com.bhoomibot.os.feature.autonomous.core.model.Observation
import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.world.FarmWorld
import kotlinx.coroutines.flow.map
import kotlin.math.*

/**
 * MOCK PERCEPTION: Simulates the Vision System (YOLO/DINO).
 * 
 * Instead of pixels, it uses math to "see" virtual entities in the FarmWorld
 * if they are within the robot's Field of View (FOV).
 */
class MockPerception {

    private val FOV_DEGREES = 90.0
    private val MAX_RANGE_METERS = 10.0

    val observationStream = DigitalTwin.state.map { twin ->
        val currentEntities = FarmWorld.entities.value
        val observations = mutableListOf<Observation>()

        for (entity in currentEntities) {
            // 1. Calculate relative distance and angle
            val dx = entity.x - twin.x
            val dy = entity.y - twin.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance <= MAX_RANGE_METERS) {
                // Angle to object in degrees (World coordinates)
                val angleToObj = Math.toDegrees(atan2(dy, dx))
                
                // Relative angle to robot's heading
                var relAngle = angleToObj - twin.yaw
                
                // Normalize to -180..180
                while (relAngle > 180) relAngle -= 360
                while (relAngle < -180) relAngle += 360

                if (abs(relAngle) <= FOV_DEGREES / 2) {
                    // Object is in FOV!
                    observations.add(Observation(
                        label = entity.label,
                        confidence = 0.95f,
                        boundingBox = RectF(0.4f, 0.4f, 0.6f, 0.6f), // Dummy box
                        estimatedDistance = distance.toFloat(),
                        timestamp = twin.timestamp
                    ))
                }
            }
        }
        observations
    }
}
