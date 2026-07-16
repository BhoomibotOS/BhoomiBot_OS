package com.bhoomibot.os.feature.settings

import androidx.lifecycle.ViewModel
import com.bhoomibot.os.data.ControlCalibrationStore
import kotlinx.coroutines.flow.StateFlow

// Holds the calibration values shown/edited on the Settings screen and writes changes back to the store.
class SettingsViewModel : ViewModel() {
    // Live calibration values the Settings UI displays. Updates automatically when changed.
    val calibration: StateFlow<com.bhoomibot.os.model.ControlCalibration> = ControlCalibrationStore.calibration

    // Called when the "Drive speed" slider changes: sets how much speed each tap adds (m/s per tap).
    fun setDriveStep(value: Int) = ControlCalibrationStore.update { it.copy(driveStepMetersPerSecond = value) }

    // Called when the "Maximum speed" slider changes: sets the speed ceiling.
    // Guarantees the ceiling is never below the per-tap step (otherwise taps couldn't increase speed).
    fun setMaximumSpeed(value: Int) = ControlCalibrationStore.update { calibration ->
        calibration.copy(maximumSpeedMetersPerSecond = value.coerceAtLeast(calibration.driveStepMetersPerSecond))
    }

    // Called when the "PTO speed" slider changes: sets the PTO step increment (%).
    fun setPtoStep(value: Int) = ControlCalibrationStore.update { it.copy(ptoStepPercent = value) }

    // Called when the "Hydraulic height" slider changes: sets the hydraulic step increment (%).
    fun setHydraulicStep(value: Int) = ControlCalibrationStore.update { it.copy(hydraulicHeightStepPercent = value) }
}
