package com.bhoomibot.os.feature.settings

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryProvider
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.data.ControlCalibrationStore
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.flow.StateFlow

// State holder for [SettingsScreen]. Exposes the calibration values and writes edits back.
//
// Unlike the connection ViewModels, this one is a plain ViewModel (no Application needed): it
// neither loads from DataStore nor persists on Save. It reads/writes ControlCalibrationStore,
// an in-memory singleton shared with ManualViewModel, so slider changes take effect live and
// are simply the current values — they are NOT persisted across app restarts.
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = provideRobotRepository(application)
    private val liveLinkRepository = LiveLinkRepositoryProvider.get(application)

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

    fun triggerOta() {
        // We need to know if we are in DIRECT or VIA_ROBOT mode. 
        // In the original ManualViewModel, this was in the UI state.
        // For simplicity here, we'll try both or we might need to expose the control path in Settings.
        // Actually, triggerOta in RobotRepository should handle the local case.
        
        if (repository.isConnected.value) {
            repository.triggerOta()
            Toast.makeText(getApplication(), "OTA Mode Triggered (Local)", Toast.LENGTH_LONG).show()
        } else if (liveLinkRepository.connectionState.value == LiveConnectionState.CONNECTED) {
            liveLinkRepository.sendCommand(RobotCommand(triggerOta = true))
            Toast.makeText(getApplication(), "OTA Mode Triggered (Remote)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(getApplication(), "No robot connected to trigger OTA", Toast.LENGTH_SHORT).show()
        }
    }
}
