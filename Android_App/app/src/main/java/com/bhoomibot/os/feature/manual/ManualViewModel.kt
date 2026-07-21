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
    // Digital drive buttons implement a SIGNED-speed, joystick-toy model:
    //   UP   = increase speed toward +max (forward)
    //   DOWN = decrease speed through 0 toward -max (reverse; PIN27 goes HIGH past 0)
    //   LEFT / RIGHT = steer (slow the inner wheel) at the CURRENT speed
    //   STOP = speed 0
    // vehicleSpeedPercent is reused as the signed speed: -max..+max, 0 = stopped.
    fun onForward() = applySignedSpeed(+1)
    fun onReverse() = applySignedSpeed(-1)
    fun onLeft() = applySteer(DriveCommand.LEFT)
    fun onRight() = applySteer(DriveCommand.RIGHT)
    // Stop: speed 0.
    fun onStop() {
        repository.updateSpeed(0)
        repository.sendDriveCommand(DriveCommand.STOP)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.STOP)
    }
    // Emergency stop: send EMERGENCY_STOP (hard halt) and zero the displayed speed.
    fun onEmergencyStop() {
        repository.sendDriveCommand(DriveCommand.EMERGENCY_STOP)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.EMERGENCY_STOP)
    }
    // Joystick drag: magnitude (0–1) scales to speed; sign follows the stick (up = +/forward,
    // down = -/reverse) so the read-out matches the digital model. Release to 0 sends STOP.
    fun onJoystickChanged(magnitude: Float, command: DriveCommand) {
        val max = ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond
        val raw = (magnitude.coerceIn(0f, 1f) * max).toInt()
        val signed = if (command == DriveCommand.REVERSE) -raw else raw
        repository.updateSpeed(toVcuPercent(signed))
        repository.sendDriveCommand(if (raw == 0) DriveCommand.STOP else command)
        _uiState.value = _uiState.value.copy(
            vehicleSpeedPercent = signed,
            lastCommand = if (raw == 0) DriveCommand.STOP else command
        )
    }
    /** Callback reserved for a future external speed source or control profile. */
    fun onVehicleSpeedChanged(speed: Int) {
        val max = ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond
        val bounded = speed.coerceIn(-max, max)
        repository.updateSpeed(toVcuPercent(bounded))
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = bounded)
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
    // Hydraulic switch: turn on/off. When switched OFF the height resets to 0 and the lift retracts.
    fun onHydraulicToggle(enabled: Boolean) {
        val height = if (enabled) _uiState.value.hydraulicHeightPercent else 0
        repository.setHydraulic(height)
        _uiState.value = _uiState.value.copy(
            hydraulicEnabled = enabled,
            hydraulicHeightPercent = height
        )
    }
    // Hydraulic height slider: snaps to the configured step increment and pushes it to the VCU.
    fun onHydraulicHeightChanged(height: Int) {
        val snapped = snapToStep(height, ControlCalibrationStore.calibration.value.hydraulicHeightStepPercent)
        repository.setHydraulic(snapped)
        _uiState.value = _uiState.value.copy(hydraulicHeightPercent = snapped)
    }
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
    // Horn button: pulse the horn once (one-shot, like GamePad.isSelectPressed()).
    fun onHorn() = repository.horn()

    // Helper: nudge the signed speed by one step (+1 = up/forward, -1 = down/reverse), clamped
    // to +/-max. The direction token sent to the VCU follows the resulting sign.
    private fun applySignedSpeed(direction: Int) {
        val calib = ControlCalibrationStore.calibration.value
        val next = (_uiState.value.vehicleSpeedPercent + direction * calib.driveStepMetersPerSecond)
            .coerceIn(-calib.maximumSpeedMetersPerSecond, calib.maximumSpeedMetersPerSecond)
        val command = when {
            next > 0 -> DriveCommand.FORWARD
            next < 0 -> DriveCommand.REVERSE
            else -> DriveCommand.STOP
        }
        repository.updateSpeed(toVcuPercent(next))
        repository.sendDriveCommand(command)
        _uiState.value = _uiState.value.copy(vehicleSpeedPercent = next, lastCommand = command)
    }
    // Helper: steering does NOT change speed — it re-asserts the current signed speed while setting
    // the turn direction, so the robot arcs at whatever speed it is already travelling.
    private fun applySteer(command: DriveCommand) {
        val speed = _uiState.value.vehicleSpeedPercent
        repository.updateSpeed(toVcuPercent(speed))
        repository.sendDriveCommand(command)
        _uiState.value = _uiState.value.copy(lastCommand = command)
    }
    // The UI tracks speed in m/s, but the VCU's SPD contract is a 0..100 percentage. Scale the
    // signed m/s value into the VCU's signed percentage so "full speed" maps to full PWM (255)
    // rather than just the low end of the range.
    private fun toVcuPercent(signedMetersPerSecond: Int): Int {
        val max = ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond
        if (max == 0) return 0
        return (signedMetersPerSecond * 100 / max).coerceIn(-100, 100)
    }
    // Helper: send a raw drive command and record it as the last command.
    private fun send(command: DriveCommand) { repository.sendDriveCommand(command); _uiState.value = _uiState.value.copy(lastCommand = command) }
    // Helper: round a slider value to the nearest allowed step (so the UI shows clean increments).
    private fun snapToStep(value: Int, step: Int): Int = ((value.coerceIn(0, 100) + step / 2) / step * step).coerceIn(0, 100)
}
