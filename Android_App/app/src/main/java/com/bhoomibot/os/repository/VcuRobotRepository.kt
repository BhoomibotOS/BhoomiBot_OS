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
import com.bhoomibot.os.vcu.otaMaintenanceCommand
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

    private val _vcuBattery = MutableStateFlow(0)
    override val vcuBattery: StateFlow<Int> = _vcuBattery.asStateFlow()

    private val commandChannel = Channel<String>(Channel.BUFFERED)
    private var telemetryJob: Job? = null

    init {
        // PROACTIVE CONNECTION MAINTAINER
        // Keeps the VCU link alive and updates status in real-time, even when idle.
        repositoryScope.launch {
            while (isActive) {
                if (!_isConnected.value) {
                    try {
                        ensureConnected()
                    } catch (e: Exception) {
                        // Silent retry every 5s
                    }
                }
                delay(5000)
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
        // SMART CHECK: If we are already connected and healthy, just return.
        // This prevents the "Flicker" caused by reconnecting on every command.
        if (_isConnected.value && manager != null) {
            return manager
        }

        return connectMutex.withLock {
            // Double-check inside the lock to prevent race conditions
            if (_isConnected.value && manager != null) return@withLock manager
            
            val m = getManager()
            try {
                m.connect()
                if (telemetryJob?.isActive != true) {
                    startTelemetryListener(m)
                }
                m
            } catch (e: Exception) {
                android.util.Log.e("VCU", "Hardware initialization failed: ${e.message}")
                null
            }
        }
    }

    private fun startTelemetryListener(m: ConnectionManager) {
        telemetryJob?.cancel()
        telemetryJob = repositoryScope.launch {
            try {
                // receive() blocks until the Bluetooth/WiFi socket is physically open.
                m.receive().collect { line ->
                    // TRUTHFUL STATUS: Only set Connected to true when we actually see 
                    // a valid line of data arriving from the VCU hardware.
                    if (!_isConnected.value) {
                        _isConnected.value = true
                        android.util.Log.i("VCU", "Hardware Link Verified: Data arriving.")
                    }

                    if (line.startsWith("RPM ")) {
                        runCatching {
                            val data = line.substring(4).split(",")
                            if (data.size == 2) {
                                val left = data[0].trim().toIntOrNull() ?: 0
                                val right = data[1].trim().toIntOrNull() ?: 0
                                _rpmData.value = Pair(left, right)
                            }
                        }
                    } else if (line.startsWith("VBAT ")) {
                        runCatching {
                            val battery = line.substring(5).trim().toIntOrNull() ?: 0
                            _vcuBattery.value = battery.coerceIn(0, 100)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VCU", "Telemetry link failed: ${e.message}")
            } finally {
                _isConnected.value = false
                android.util.Log.w("VCU", "Hardware Link Offline.")
            }
        }
    }

    override fun status(): RobotStatus = RobotStatus(
        isOnline = _isConnected.value,
        vcuBattery = _vcuBattery.value
    )

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

    override fun triggerOta() {
        commandChannel.trySend(otaMaintenanceCommand())
    }

    override fun disconnect() {
        repositoryScope.launch { 
            telemetryJob?.cancel()
            manager?.disconnect()
            _isConnected.value = false
        }
    }
}
