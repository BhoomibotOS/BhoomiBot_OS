/**
 * Shared IN-MEMORY calibration source (NOT persisted — resets on app restart).
 *
 * A single global [MutableStateFlow] of [com.bhoomibot.os.model.ControlCalibration]. The Settings
 * screen writes here via [update]; the Manual screen reads [calibration]. Persist with DataStore
 * later once the vehicle setup is finalized.
 */
package com.bhoomibot.os.data

import com.bhoomibot.os.model.ControlCalibration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared in-memory calibration source.
 *  A single global store for the robot's control tuning values (drive step, max speed, PTO step, etc.).
 *  Settings screen writes here; Manual mode reads from here.
 *  Persist it with DataStore when vehicle setup is finalized (so values survive app restarts). */
object ControlCalibrationStore {
    // The live, editable calibration values. Starts with the defaults from ControlCalibration().
    private val mutableCalibration = MutableStateFlow(ControlCalibration())

    // Read-only view of the calibration that the UI observes (so screens react to changes).
    val calibration: StateFlow<ControlCalibration> = mutableCalibration.asStateFlow()

    // Applies a change to the calibration (e.g. copy() with a new drive step) and stores the result.
    fun update(transform: (ControlCalibration) -> ControlCalibration) {
        mutableCalibration.value = transform(mutableCalibration.value)
    }
}
