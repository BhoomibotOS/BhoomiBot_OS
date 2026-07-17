package com.bhoomibot.os.data

import com.bhoomibot.os.model.ControlCalibration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared in-memory calibration source. Persist it with DataStore when vehicle setup is finalized. */
object ControlCalibrationStore {
    private val mutableCalibration = MutableStateFlow(ControlCalibration())
    val calibration: StateFlow<ControlCalibration> = mutableCalibration.asStateFlow()

    fun update(transform: (ControlCalibration) -> ControlCalibration) {
        mutableCalibration.value = transform(mutableCalibration.value)
    }
}
