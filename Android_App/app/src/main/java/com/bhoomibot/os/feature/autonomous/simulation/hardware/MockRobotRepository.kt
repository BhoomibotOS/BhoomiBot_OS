package com.bhoomibot.os.feature.autonomous.simulation.hardware

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.vcu.toProtocol
import com.bhoomibot.os.vcu.speedCommand
import com.bhoomibot.os.vcu.ptoCommand
import com.bhoomibot.os.vcu.lightsCommand
import com.bhoomibot.os.vcu.hydraulicCommand
import com.bhoomibot.os.vcu.hornCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MOCK ROBOT REPOSITORY: Direct connection for simulation.
 * 
 * JUNIOR ENGINEER NOTE: This replaces the real Bluetooth repository when 
 * we are in simulator mode. It routes commands to the Mock VCU instead
 * of the physical ESP32.
 */
class MockRobotRepository(private val mockVcu: MockVcu = MockVcu()) : RobotRepository {

    private val _isConnected = MutableStateFlow(true) // Sim is always "connected"
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _rpmData = MutableStateFlow(Pair(0, 0))
    override val rpmData: StateFlow<Pair<Int, Int>> = _rpmData.asStateFlow()

    override fun status(): RobotStatus = RobotStatus(isOnline = true)

    override fun sendDriveCommand(command: DriveCommand) {
        mockVcu.onCommandReceived(command.toProtocol())
    }

    override fun updateSpeed(percent: Int) {
        mockVcu.onCommandReceived(speedCommand(percent))
    }

    override fun setPto(enabled: Boolean) {
        mockVcu.onCommandReceived(ptoCommand(enabled))
    }

    override fun setLights(enabled: Boolean) {
        mockVcu.onCommandReceived(lightsCommand(enabled))
    }

    override fun setHydraulic(heightPercent: Int) {
        mockVcu.onCommandReceived(hydraulicCommand(heightPercent))
    }

    override fun horn() {
        mockVcu.onCommandReceived(hornCommand())
    }

    override fun triggerOta() {
        // No-op for mock simulation
    }

    override fun disconnect() {
        _isConnected.value = false
    }
}
