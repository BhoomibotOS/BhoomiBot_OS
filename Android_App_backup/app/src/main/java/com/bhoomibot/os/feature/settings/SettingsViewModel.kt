package com.bhoomibot.os.feature.settings

import androidx.lifecycle.ViewModel
import com.bhoomibot.os.data.ControlCalibrationStore
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    val calibration: StateFlow<com.bhoomibot.os.model.ControlCalibration> = ControlCalibrationStore.calibration
    fun setDriveStep(value: Int) = ControlCalibrationStore.update { it.copy(driveStepMetersPerSecond = value) }
    fun setMaximumSpeed(value: Int) = ControlCalibrationStore.update { calibration ->
        calibration.copy(maximumSpeedMetersPerSecond = value.coerceAtLeast(calibration.driveStepMetersPerSecond))
    }
    fun setPtoStep(value: Int) = ControlCalibrationStore.update { it.copy(ptoStepPercent = value) }
    fun setHydraulicStep(value: Int) = ControlCalibrationStore.update { it.copy(hydraulicHeightStepPercent = value) }
}
