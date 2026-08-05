package com.bhoomibot.os.feature.operator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class OperatorHomeUiState(
    val batteryPercent: Int = 85,
    val gpsStatus: String = "Connected",
    val vcuConnected: Boolean = false
)

class OperatorHomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RobotRepository = provideRobotRepository(application)
    
    private val _uiState = MutableStateFlow(OperatorHomeUiState())
    val uiState: StateFlow<OperatorHomeUiState> = _uiState.asStateFlow()

    init {
        // Collect hardware connection state
        viewModelScope.launch {
            repository.isConnected.collectLatest { connected ->
                android.util.Log.d("Connection", "VCU Status changed: $connected")
                _uiState.value = _uiState.value.copy(vcuConnected = connected)
            }
        }
        
        // Proactively connect to hardware to show true status on home screen
        viewModelScope.launch {
            repository.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.STOP)
        }
        
        // In a real app, we'd poll status() or collect a flow from repository
        val currentStatus = repository.status()
        _uiState.value = _uiState.value.copy(
            batteryPercent = currentStatus.batteryPercent,
            gpsStatus = currentStatus.gpsStatus
        )
    }
}
