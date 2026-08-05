package com.bhoomibot.os.feature.autonomous.simulation

import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.engine.SimEngine
import com.bhoomibot.os.feature.autonomous.simulation.hardware.MockRobotRepository
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PRODUCTION-GRADE SIMULATION TEST
 */
class RobotSimulationTest {

    @Test
    fun testRobotMovementInSimulation() = runBlocking {
        // 1. Setup Simulation Environment
        val engine = SimEngine(frequencyHz = 20)
        val repository = MockRobotRepository()
        
        // Reset Twin to Origin
        DigitalTwin.update(com.bhoomibot.os.feature.autonomous.simulation.engine.RobotStateVector())
        
        // 2. Start the world clock
        engine.start()

        // 3. Send physical commands via the L8 abstraction
        repository.updateSpeed(50) // 50% Speed
        repository.sendDriveCommand(DriveCommand.FORWARD)

        // 4. Wait for physics to process (Virtual 1 second)
        delay(1200)

        // 5. Validate the Digital Twin state
        val finalState = DigitalTwin.state.value
        
        println("Simulation Results:")
        println("Position: (${finalState.x}, ${finalState.y})")
        println("Speed: ${finalState.speed} m/s")
        println("Last Command: ${finalState.lastCommand}")

        // Assert that the robot moved forward along the X-axis
        assertTrue("Robot should have moved forward", finalState.x > 0)
        
        engine.stop()
    }
}
