package com.bhoomibot.os.feature.autonomous.simulation

import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.engine.RobotStateVector
import com.bhoomibot.os.feature.autonomous.simulation.hardware.MockPerception
import com.bhoomibot.os.feature.autonomous.simulation.world.FarmWorld
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OBSTACLE DETECTION SIMULATION TEST
 */
class ObstacleDetectionTest {

    @Test
    fun testObstacleAppearsInCamera() = runBlocking {
        // 1. Setup
        val perception = MockPerception()
        FarmWorld.clearWorld()
        
        // Robot at (0,0) looking straight (Yaw=0)
        DigitalTwin.update(RobotStateVector(x = 0.0, y = 0.0, yaw = 0.0))

        // 2. Spawn an obstacle 5 meters directly in front
        FarmWorld.spawnObstacle(x = 5.0, y = 0.0, label = "HUMAN")

        // 3. Check what the robot "sees"
        val observations = perception.observationStream.first()
        
        println("Perception result: ${observations.size} objects detected")
        if (observations.isNotEmpty()) {
            println("Detected: ${observations[0].label} at ${observations[0].estimatedDistance}m")
        }

        // 4. Validate
        assertEquals("Should detect one obstacle", 1, observations.size)
        assertEquals("Should be a HUMAN", "HUMAN", observations[0].label)
        assertTrue("Distance should be approx 5m", observations[0].estimatedDistance!! in 4.9f..5.1f)
    }

    @Test
    fun testObstacleOutOfFieldOfView() = runBlocking {
        // 1. Setup
        val perception = MockPerception()
        FarmWorld.clearWorld()
        DigitalTwin.update(RobotStateVector(x = 0.0, y = 0.0, yaw = 0.0))

        // 2. Spawn an obstacle BEHIND the robot
        FarmWorld.spawnObstacle(x = -5.0, y = 0.0, label = "ROCK")

        // 3. Check
        val observations = perception.observationStream.first()
        
        // 4. Validate (Robot only sees 90 degrees in front)
        assertEquals("Should see zero obstacles behind it", 0, observations.size)
    }
}
