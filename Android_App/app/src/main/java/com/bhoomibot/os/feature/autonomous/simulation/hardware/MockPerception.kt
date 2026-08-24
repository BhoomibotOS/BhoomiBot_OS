package com.bhoomibot.os.feature.autonomous.simulation.hardware

import com.bhoomibot.sdk.Observation
import com.bhoomibot.sdk.BoundingBox
import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.world.FarmWorld
import kotlinx.coroutines.flow.map
import kotlin.math.*

/**
 * MOCK PERCEPTION: Simulates the Vision System.
 */
class MockPerception {

    private val FOV_DEGREES = 90.0
    private val MAX_RANGE_METERS = 10.0

    val observationStream = DigitalTwin.state.map { twin ->
        val currentEntities = FarmWorld.entities.value
        val observations = mutableListOf<Observation>()

        for (entity in currentEntities) {
            val dx = entity.x - twin.x
            val dy = entity.y - twin.y
            val distance = sqrt(dx * dx + dy * dy)

            if (distance <= MAX_RANGE_METERS) {
                val angleToObj = Math.toDegrees(atan2(dy, dx))
                var relAngle = angleToObj - twin.yaw
                
                while (relAngle > 180) relAngle -= 360
                while (relAngle < -180) relAngle += 360

                if (abs(relAngle) <= FOV_DEGREES / 2) {
                    observations.add(Observation(
                        label = entity.label,
                        confidence = 0.95f,
                        box = BoundingBox(0.4f, 0.4f, 0.6f, 0.6f),
                        timestamp = twin.timestamp
                    ))
                }
            }
        }
        observations
    }
}
