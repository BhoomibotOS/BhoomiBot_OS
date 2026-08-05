package com.bhoomibot.os.repository

import android.content.Context
import android.widget.Toast
import com.bhoomibot.os.data.ConnectionPreferencesStore
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.vcu.ConnectionManager
import com.bhoomibot.os.vcu.ConnectionPreferences
import com.bhoomibot.os.vcu.hydraulicCommand
import com.bhoomibot.os.vcu.hornCommand
import com.bhoomibot.os.vcu.lightsCommand
import com.bhoomibot.os.vcu.ptoCommand
import com.bhoomibot.os.vcu.speedCommand
import com.bhoomibot.os.vcu.toProtocol
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Real transport implementation of [RobotRepository].
 * AI-Fix: Uses a single persistent Command Loop and lifecycle-safe listeners.
 */
class VcuRobotRepository(private val context: Context) : RobotRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var manager: ConnectionManager? = null
    private val connectMutex = Mutex()
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _rpmData = MutableStateFlow(Pair(0, 0))
    override val rpmData: StateFlow<Pair<Int, Int>> = _rpmData.asStateFlow()

    private val commandChannel = Channel<String>(Channel.BUFFERED)
    private var telemetryJob: Job? = null

    init {
        // AI-Fix: Initial connection attempt to start telemetry
        repositoryScope.launch {
            try {
                ensureConnected()
            } catch (e: Exception) {
                android.util.Log.e("VCU", "Initial connection failed: ${e.message}")
            }
        }

        // Start the single persistent command processing loop
        repositoryScope.launch {
            for (cmd in commandChannel) {
                try {
                    val m = ensureConnected()
                    if (m != null) {
                        m.send(cmd)
                        _isConnected.value = true
                    } else {
                        _isConnected.value = false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VCU", "Write failed: ${e.message}")
                    _isConnected.value = false
                }
            }
        }
    }

    private suspend fun getManager(): ConnectionManager {
        val prefs: ConnectionPreferences = ConnectionPreferencesStore.preferences(context).first()
        if (manager == null || _currentPrefs != prefs) {
            manager?.disconnect()
            _currentPrefs = prefs
            manager = ConnectionManager(context, prefs)
        }
        return manager!!
    }

    private var _currentPrefs: ConnectionPreferences? = null

    private suspend fun ensureConnected(): ConnectionManager? {
        if (_isConnected.value && manager != null) return manager
        
        return connectMutex.withLock {
            if (!_isConnected.value || manager == null) {
                try {
                    val m = getManager()
                    m.connect()
                    
                    // We must call receive() to actually trigger the blocking connect() in ConnectionManager
                    // Start telemetry before marking as connected
                    startTelemetryListener(m)
                    
                    // Small delay to allow the first receive attempt to succeed or fail
                    delay(500)
                    
                    _isConnected.value = true
                    m
                } catch (e: Exception) {
                    android.util.Log.e("VCU", "Connection error: ${e.message}")
                    _isConnected.value = false
                    null
                }
            } else manager
        }
    }

    private fun startTelemetryListener(m: ConnectionManager) {
        telemetryJob?.cancel()
        telemetryJob = repositoryScope.launch {
            try {
                m.receive().collect { line ->
                    if (line.startsWith("RPM ")) {
                        runCatching {
                            val data = line.substring(4).split(",")
                            if (data.size == 2) {
                                val left = data[0].trim().toIntOrNull() ?: 0
                                val right = data[1].trim().toIntOrNull() ?: 0
                                _rpmData.value = Pair(left, right)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }

    override fun status(): RobotStatus = RobotStatus(isOnline = _isConnected.value)

    override fun sendDriveCommand(command: DriveCommand) {
        commandChannel.trySend(command.toProtocol())
    }

    override fun updateSpeed(percent: Int) {
        commandChannel.trySend(speedCommand(percent))
    }

    override fun setPto(enabled: Boolean) {
        commandChannel.trySend(ptoCommand(enabled))
    }

    override fun setLights(enabled: Boolean) {
        commandChannel.trySend(lightsCommand(enabled))
    }

    override fun setHydraulic(heightPercent: Int) {
        commandChannel.trySend(hydraulicCommand(heightPercent))
    }

    override fun horn() {
        commandChannel.trySend(hornCommand())
    }

    override fun disconnect() {
        repositoryScope.launch { 
            telemetryJob?.cancel()
            manager?.disconnect()
            _isConnected.value = false
        }
    }
}
