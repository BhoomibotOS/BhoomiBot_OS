package com.bhoomibot.os.feature.manual

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryProvider
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.data.ControlCalibrationStore
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.feature.autonomous.AutonomyState
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.feature.autonomous.skills.learning.TeachingManager
import com.bhoomibot.os.repository.provideRobotRepository
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.service.BhoomiBotService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ManualViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RobotRepository = provideRobotRepository(application)
    private val liveLinkRepository = LiveLinkRepositoryProvider.get(application)
    private val recordingEngine = AutonomyManager.getRecordingEngine(application)
    private val locationTracker = LocationTracker(application)
    private val stateMachine = AutonomyManager.stateMachine

    private val _uiState = MutableStateFlow(ManualUiState(robotStatus = repository.status()))
    val uiState: StateFlow<ManualUiState> = _uiState.asStateFlow()

    init {
        // AI-Fix: Background Service is only for the Robot Phone.
        // Removing it from Operator to prevent connection flickering.

        // 1. Initial Local Phone Battery Read (Operator side)
        val initialStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            application.registerReceiver(null, filter)
        }
        val initLevel: Int = initialStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val initScale: Int = initialStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val initPercent = if (initLevel != -1 && initScale != -1) (initLevel * 100 / initScale.toFloat()).toInt() else 0
        
        // Update state flow with real phone battery before any collectors start
        _uiState.update { it.copy(localPhoneBattery = initPercent) }

        // 2. Periodic Local Phone Battery Update (Operator side)
        viewModelScope.launch {
            while (isActive) {
                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                    application.registerReceiver(null, filter)
                }
                val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val phoneBatteryPercent = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
                _uiState.update { it.copy(localPhoneBattery = phoneBatteryPercent) }
                delay(10000) // Update every 10 seconds
            }
        }

        viewModelScope.launch {
            repository.isConnected.collectLatest { connected ->
                if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) {
                    _uiState.value = _uiState.value.copy(bluetoothConnected = connected)
                }
            }
        }
        
        viewModelScope.launch {
            liveLinkRepository.telemetry.collectLatest { tel ->
                if (_uiState.value.controlPath == ControlPath.VIA_ROBOT) {
                    _uiState.value = _uiState.value.copy(
                        bluetoothConnected = tel.isOnline,
                        robotStatus = _uiState.value.robotStatus.copy(
                            batteryPercent = tel.batteryPercent,
                            vcuBattery = tel.vcuBattery,
                            gpsStatus = tel.gpsStatus
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            locationTracker.startTracking()
            locationTracker.currentLocation.collect { loc ->
                val gpsLabel = if (loc != null) {
                    "${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)}"
                } else {
                    "No Signal"
                }
                _uiState.value = _uiState.value.copy(
                    robotStatus = _uiState.value.robotStatus.copy(gpsStatus = gpsLabel)
                )
            }
        }
        
        viewModelScope.launch {
            repository.rpmData.collectLatest { rpm ->
                _uiState.value = _uiState.value.copy(
                    leftRpm = rpm.first,
                    rightRpm = rpm.second
                )
            }
        }

        viewModelScope.launch {
            repository.vcuBattery.collectLatest { battery ->
                if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) {
                    _uiState.value = _uiState.value.copy(
                        robotStatus = _uiState.value.robotStatus.copy(vcuBattery = battery)
                    )
                }
            }
        }
        
        viewModelScope.launch {
            repository.sendDriveCommand(DriveCommand.STOP)
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
    }

    fun setControlPath(path: ControlPath) {
        val currentLocalStatus = repository.isConnected.value
        val currentRemoteStatus = liveLinkRepository.telemetry.value.isOnline
        
        _uiState.value = _uiState.value.copy(
            controlPath = path, 
            bluetoothConnected = if (path == ControlPath.DIRECT_VCU) currentLocalStatus else currentRemoteStatus
        )

        if (path == ControlPath.DIRECT_VCU) {
            viewModelScope.launch { repository.sendDriveCommand(DriveCommand.STOP) }
        }
    }

    fun onAddMarker(type: String) {
        TeachingManager.addActionMarker(getApplication(), type)
        Toast.makeText(getApplication(), "Marker Added: $type", Toast.LENGTH_SHORT).show()
    }

    fun onFinishTeaching() {
        TeachingManager.finishTeaching(getApplication())
        _uiState.value = _uiState.value.copy(learningEnabled = false, isRecording = false)
        // AI-Fix: Stay on this screen for a second to show success, or could navigate back
        Toast.makeText(getApplication(), "Skill Learned Successfully!", Toast.LENGTH_LONG).show()
    }

    fun setDrivingMode(mode: DrivingMode) {
        val nextState = _uiState.value.copy(drivingMode = mode)
        updateAndRecord(nextState)
    }

    fun onForward() { if (checkConnection()) applySignedSpeed(+1) }
    fun onReverse() { if (checkConnection()) applySignedSpeed(-1) }
    fun onLeft() { if (checkConnection()) applySteer(DriveCommand.LEFT) }
    fun onRight() { if (checkConnection()) applySteer(DriveCommand.RIGHT) }
    
    fun onStop() {
        sendToHardware(DriveCommand.STOP, 0)
        val nextState = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.STOP)
        updateAndRecord(nextState)
    }
    
    fun onEmergencyStop() {
        sendToHardware(DriveCommand.EMERGENCY_STOP, 0, isEmergency = true)
        val nextState = _uiState.value.copy(vehicleSpeedPercent = 0, lastCommand = DriveCommand.EMERGENCY_STOP)
        updateAndRecord(nextState)
    }

    fun onJoystickChanged(magnitude: Float, command: DriveCommand) {
        if (!checkConnection() && magnitude > 0) return
        val max = ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond
        val raw = (magnitude.coerceIn(0f, 1f) * max).toInt()
        val signed = if (command == DriveCommand.REVERSE) -raw else raw
        sendToHardware(if (raw == 0) DriveCommand.STOP else command, toVcuPercent(signed))
        val nextState = _uiState.value.copy(vehicleSpeedPercent = signed, lastCommand = if (raw == 0) DriveCommand.STOP else command)
        updateAndRecord(nextState)
    }

    private fun sendToHardware(drive: DriveCommand, speed: Int, isEmergency: Boolean = false) {
        if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) {
            repository.updateSpeed(speed)
            repository.sendDriveCommand(drive)
        } else {
            liveLinkRepository.sendCommand(RobotCommand(drive = drive, speedPercent = speed, emergencyStop = isEmergency))
        }
    }

    private fun checkConnection(): Boolean {
        if (_uiState.value.controlPath == ControlPath.VIA_ROBOT) {
            if (liveLinkRepository.connectionState.value != LiveConnectionState.CONNECTED) {
                Toast.makeText(getApplication(), "Hotspot link not connected", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (!_uiState.value.bluetoothConnected) {
                Toast.makeText(getApplication(), "Local Bluetooth disconnected", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    fun onPtoToggle(enabled: Boolean) {
        if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) repository.setPto(enabled)
        else liveLinkRepository.sendCommand(RobotCommand(pto = enabled))
        val nextState = _uiState.value.copy(ptoEnabled = enabled, ptoSpeedPercent = if (enabled) _uiState.value.ptoSpeedPercent else 0)
        updateAndRecord(nextState)
    }
    
    fun onPtoSpeedChanged(speed: Int) {
        if (_uiState.value.ptoEnabled) {
            val snapped = snapToStep(speed, ControlCalibrationStore.calibration.value.ptoStepPercent)
            val nextState = _uiState.value.copy(ptoSpeedPercent = snapped)
            updateAndRecord(nextState)
        }
    }
    
    fun onLightsToggle(enabled: Boolean) {
        if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) repository.setLights(enabled)
        else liveLinkRepository.sendCommand(RobotCommand(lights = enabled))
        val nextState = _uiState.value.copy(lightsEnabled = enabled)
        updateAndRecord(nextState)
    }

    fun onHydraulicToggle(enabled: Boolean) {
        val height = if (enabled) _uiState.value.hydraulicHeightPercent else 0
        if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) repository.setHydraulic(height)
        val nextState = _uiState.value.copy(hydraulicEnabled = enabled, hydraulicHeightPercent = height)
        updateAndRecord(nextState)
    }
    
    fun onHydraulicHeightChanged(height: Int) {
        val snapped = snapToStep(height, ControlCalibrationStore.calibration.value.hydraulicHeightStepPercent)
        if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) repository.setHydraulic(snapped)
        val nextState = _uiState.value.copy(hydraulicHeightPercent = snapped)
        updateAndRecord(nextState)
    }

    fun onCameraLightToggle(enabled: Boolean) {
        val nextState = _uiState.value.copy(cameraLightEnabled = enabled)
        updateAndRecord(nextState)
    }

    fun onLearningToggle(enabled: Boolean) {
        if (enabled && liveLinkRepository.connectionState.value != LiveConnectionState.CONNECTED) {
            Toast.makeText(getApplication(), "Connect to Robot first", Toast.LENGTH_SHORT).show()
            return
        }
        if (enabled) stateMachine.transitionTo(AutonomyState.RECORDING)
        else stateMachine.transitionTo(AutonomyState.IDLE)
        liveLinkRepository.sendCommand(RobotCommand(learningMode = enabled))
        _uiState.value = _uiState.value.copy(learningEnabled = enabled, isRecording = enabled)
    }

    fun setCameraMaximized(maximized: Boolean) { _uiState.value = _uiState.value.copy(isCameraMaximized = maximized) }
    fun setCameraEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(cameraEnabled = enabled) }

    fun setUseRearCamera(useRear: Boolean) {
        _uiState.value = _uiState.value.copy(useRearCamera = useRear)
        liveLinkRepository.sendCommand(RobotCommand(
            liveCamera = _uiState.value.cameraEnabled,
            useRearCamera = useRear
        ))
    }

    fun onHorn() { if (_uiState.value.controlPath == ControlPath.DIRECT_VCU) repository.horn() }

    private fun applySignedSpeed(direction: Int) {
        val calib = ControlCalibrationStore.calibration.value
        val next = (_uiState.value.vehicleSpeedPercent + direction * calib.driveStepMetersPerSecond)
            .coerceIn(-calib.maximumSpeedMetersPerSecond, calib.maximumSpeedMetersPerSecond)
        val command = when {
            next > 0 -> DriveCommand.FORWARD
            next < 0 -> DriveCommand.REVERSE
            else -> DriveCommand.STOP
        }
        sendToHardware(command, toVcuPercent(next))
        val nextState = _uiState.value.copy(vehicleSpeedPercent = next, lastCommand = command)
        updateAndRecord(nextState)
    }

    private fun applySteer(command: DriveCommand) {
        val speed = _uiState.value.vehicleSpeedPercent
        sendToHardware(command, toVcuPercent(speed))
        val nextState = _uiState.value.copy(lastCommand = command)
        updateAndRecord(nextState)
    }

    private fun updateAndRecord(nextState: ManualUiState) {
        _uiState.value = nextState
        recordingEngine.recordState(nextState)
    }

    private fun toVcuPercent(signedMetersPerSecond: Int): Int {
        val max = ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond
        if (max == 0) return 0
        return (signedMetersPerSecond * 100 / max).coerceIn(-100, 100)
    }
    private fun snapToStep(value: Int, step: Int): Int = ((value.coerceIn(0, 100) + step / 2) / step * step).coerceIn(0, 100)
}
