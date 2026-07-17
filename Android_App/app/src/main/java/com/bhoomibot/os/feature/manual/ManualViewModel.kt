package com.bhoomibot.os.feature.manual

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.bhoomibot.os.data.ControlCalibrationStore
import com.bhoomibot.os.repository.provideRobotRepository
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.repository.RobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder for [ManualControlScreen]. Coordinates manual HMI (human-machine interface)
 * intent and talks to the robot only through [RobotRepository].
 *
 * Follows the app's screen pattern: this ViewModel owns the screen state in a [StateFlow]
 * (the single source of truth) and exposes plain action methods; the Composable observes the
 * flow and calls the actions. Keeping state here — outside the Compose layer — means it
 * survives recomposition and configuration changes (e.g. the forced landscape rotation when
 * the camera goes full-screen) without being rebuilt.
 *
 * Per-step increments and the speed ceiling are read live from [ControlCalibrationStore]
 * (edited on the Settings screen), so retuning calibration changes drive/PTO/hydraulic
 * behaviour here immediately.
 */
class ManualViewModel(application: Application) : AndroidViewModel(application) {

    // The repository is supplied by provideRobotRepository (fake by default, real VCU when enabled).
    // NOTE: it must be a plain field, not a default constructor parameter — viewModel() resolves
    // ManualViewModel via AndroidViewModelFactory, which only looks for a (Application) constructor.
    // Kotlin does not synthesize that single-arg constructor for default parameters, so a default
    // second parameter would make instantiation throw NoSuchMethodException and crash the screen.
    private val repository: RobotRepository = provideRobotRepository(application)
    // Tear down the underlying transport (Bluetooth/WiFi) when the ViewModel is cleared.
    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    // The single source of truth for the screen. UI reads this; actions update it.
    private val _uiState = MutableStateFlow(ManualUiState(robotStatus = repository.status()))
    // Read-only stream the Compose screen collects to re-render on every change.
    val uiState: StateFlow<ManualUiState> = _uiState.asStateFlow()

    // Switch between DIGITAL (buttons) and JOYSTICK drive modes.
    fun setDrivingMode(mode: DrivingMode) { _uiState.value = _uiState.value.copy(drivingMode = mode) }
    // Digital drive button handlers: each adds one "drive step" of speed in that direction.
    fun onForward() = changeDigitalSpeed(DriveCommand.FORWARD)
    fun onReverse() = changeDigitalSpeed(DriveCommand.REVERSE)
    fun onLeft() = changeDigitalSpeed(DriveCommand.LEFT)
    fun onRight() = changeDigitalSpeed(DriveCommand.RIGHT)
    // Stop: send STOP and zero the displayed speed.
    fun onStop() { repository.sendDriveCommand(DriveCommand.STOP); _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.STOP) }
    // Emergency stop: send EMERGENCY_STOP (hard halt) and zero the displayed speed.
    fun onEmergencyStop() { repository.sendDriveCommand(DriveCommand.EMERGENCY_STOP); _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.EMERGENCY_STOP) }
    // Joystick drag: magnitude (0–1) scales to m/s; releases to 0 send STOP.
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
    // PTO switch: turn attachment on/off. When switched OFF the PTO speed resets to 0.
    fun onPtoToggle(enabled: Boolean) {
        repository.setPto(enabled)
        _uiState.value = _uiState.value.copy(
            ptoEnabled = enabled,
            ptoSpeedPercent = if (enabled) _uiState.value.ptoSpeedPercent else 0
        )
    }
    // PTO speed slider: only applies while PTO is enabled (otherwise ignored).
    fun onPtoSpeedChanged(speed: Int) {
        if (_uiState.value.ptoEnabled) {
            _uiState.value = _uiState.value.copy(ptoSpeedPercent = snapToStep(speed, ControlCalibrationStore.calibration.value.ptoStepPercent))
        }
    }
    // Work lights switch: turn on/off and reflect in state.
    fun onLightsToggle(enabled: Boolean) { repository.setLights(enabled); _uiState.value = _uiState.value.copy(lightsEnabled = enabled) }
    // Hydraulic switch: turn on/off. When switched OFF the height resets to 0.
    fun onHydraulicToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            hydraulicEnabled = enabled,
            hydraulicHeightPercent = if (enabled) _uiState.value.hydraulicHeightPercent else 0
        )
    }
    // Hydraulic height slider: snaps to the configured step increment.
    fun onHydraulicHeightChanged(height: Int) { _uiState.value = _uiState.value.copy(hydraulicHeightPercent = snapToStep(height, ControlCalibrationStore.calibration.value.hydraulicHeightStepPercent)) }
    // Camera torch/flash toggle.
    fun onCameraLightToggle(enabled: Boolean) { _uiState.value = _uiState.value.copy(cameraLightEnabled = enabled) }
    // Learning/recording-mode toggle: when ON the Robot Phone should capture manual driving
    // telemetry for later autonomous/AI training. The actual recording pipeline is a future module,
    // so today this only tracks the operator's intent in UI state.
    fun onLearningToggle(enabled: Boolean) { _uiState.value = _uiState.value.copy(learningEnabled = enabled) }
    // Open/close the full-screen (landscape) camera view.
    fun setCameraMaximized(maximized: Boolean) { _uiState.value = _uiState.value.copy(isCameraMaximized = maximized) }
    // Turn the live camera feed ON/OFF (the "Live camera" switch).
    fun setCameraEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(cameraEnabled = enabled) }
    // Horn button: no-op today, reserved for a future VCU auxiliary-output command.
    fun onHorn() = Unit // Reserved for a future VCU auxiliary-output command.

    // Helper: add one drive-step of speed for a digital button, capped at the maximum speed.
    private fun changeDigitalSpeed(command: DriveCommand) {
        val calibration = ControlCalibrationStore.calibration.value
        val speed = (_uiState.value.vehicleSpeedPercent + calibration.driveStepMetersPerSecond).coerceAtMost(calibration.maximumSpeedMetersPerSecond)
        repository.updateSpeed(speed); repository.sendDriveCommand(command)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = speed, lastCommand = command)
    }
    // Helper: send a raw drive command and record it as the last command.
    private fun send(command: DriveCommand) { repository.sendDriveCommand(command); _uiState.value = _uiState.value.copy(lastCommand = command) }
    // Helper: round a slider value to the nearest allowed step (so the UI shows clean increments).
    private fun snapToStep(value: Int, step: Int): Int = ((value.coerceIn(0, 100) + step / 2) / step * step).coerceIn(0, 100)
}
