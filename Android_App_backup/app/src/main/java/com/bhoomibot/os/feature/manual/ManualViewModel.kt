package com.bhoomibot.os.feature.manual

import androidx.lifecycle.ViewModel
import com.bhoomibot.os.data.LocalRobotRepository
import com.bhoomibot.os.data.ControlCalibrationStore
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Coordinates manual HMI intent. Replace [RobotRepository] with a VCU transport when available. */
class ManualViewModel(private val repository: RobotRepository = LocalRobotRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow(ManualUiState(robotStatus = repository.status()))
    val uiState: StateFlow<ManualUiState> = _uiState.asStateFlow()

    fun setDrivingMode(mode: DrivingMode) { _uiState.value = _uiState.value.copy(drivingMode = mode) }
    fun onForward() = changeDigitalSpeed(DriveCommand.FORWARD)
    fun onReverse() = changeDigitalSpeed(DriveCommand.REVERSE)
    fun onLeft() = changeDigitalSpeed(DriveCommand.LEFT)
    fun onRight() = changeDigitalSpeed(DriveCommand.RIGHT)
    fun onStop() { repository.sendDriveCommand(DriveCommand.STOP); _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.STOP) }
    fun onEmergencyStop() { repository.sendDriveCommand(DriveCommand.EMERGENCY_STOP); _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.EMERGENCY_STOP) }
    fun onJoystickChanged(magnitude: Float, command: DriveCommand) {
        val speed = (magnitude.coerceIn(0f, 1f) * ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond).toInt()
        repository.updateSpeed(speed); repository.sendDriveCommand(if (speed == 0) DriveCommand.STOP else command)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = speed, lastCommand = if (speed == 0) DriveCommand.STOP else command)
    }
    /** Callback reserved for a future external speed source or control profile. */
    fun onVehicleSpeedChanged(speed: Int) {
        val boundedSpeed = speed.coerceIn(0, ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond)
        repository.updateSpeed(boundedSpeed)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = boundedSpeed)
    }
    fun onPtoToggle(enabled: Boolean) { repository.setPto(enabled); _uiState.value = _uiState.value.copy(ptoEnabled = enabled) }
    fun onPtoSpeedChanged(speed: Int) { _uiState.value = _uiState.value.copy(ptoSpeedPercent = snapToStep(speed, ControlCalibrationStore.calibration.value.ptoStepPercent)) }
    fun onLightsToggle(enabled: Boolean) { repository.setLights(enabled); _uiState.value = _uiState.value.copy(lightsEnabled = enabled) }
    fun onHydraulicToggle(enabled: Boolean) { _uiState.value = _uiState.value.copy(hydraulicEnabled = enabled) }
    fun onHydraulicHeightChanged(height: Int) { _uiState.value = _uiState.value.copy(hydraulicHeightPercent = snapToStep(height, ControlCalibrationStore.calibration.value.hydraulicHeightStepPercent)) }
    fun onCameraLightToggle(enabled: Boolean) { _uiState.value = _uiState.value.copy(cameraLightEnabled = enabled) }
    fun setCameraMaximized(maximized: Boolean) { _uiState.value = _uiState.value.copy(isCameraMaximized = maximized) }
    fun onHorn() = Unit // Reserved for a future VCU auxiliary-output command.

    private fun changeDigitalSpeed(command: DriveCommand) {
        val calibration = ControlCalibrationStore.calibration.value
        val speed = (_uiState.value.vehicleSpeedPercent + calibration.driveStepMetersPerSecond).coerceAtMost(calibration.maximumSpeedMetersPerSecond)
        repository.updateSpeed(speed); repository.sendDriveCommand(command)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = speed, lastCommand = command)
    }
    private fun send(command: DriveCommand) { repository.sendDriveCommand(command); _uiState.value = _uiState.value.copy(lastCommand = command) }
    private fun snapToStep(value: Int, step: Int): Int = ((value.coerceIn(0, 100) + step / 2) / step * step).coerceIn(0, 100)
}
