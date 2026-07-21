/**
 * REAL robot transport (used when `USE_REAL_TRANSPORT = true`).
 *
 * Drives the ESP32/VCU through [com.bhoomibot.os.vcu.ConnectionManager], which owns the actual
 * Bluetooth/Wi-Fi socket. [RobotRepository] is deliberately non-suspend, so every command is sent
 * fire-and-forget on a SupervisorJob + IO scope; a failed send is swallowed via runCatching so the
 * UI never crashes. The connection is opened lazily and reused. See in-file notes for the
 * AndroidViewModel constructor gotcha (the repository is a field, not a default param).
 */
package com.bhoomibot.os.repository

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Real transport implementation of [RobotRepository].
 *
 * It drives the ESP32/VCU through [ConnectionManager], which owns the actual Bluetooth/WiFi
 * socket. [RobotRepository] is deliberately non-suspend, so every command is sent fire-and-forget
 * on a [SupervisorJob] + [Dispatchers.IO] scope. A failed send (e.g. no paired device, or the
 * current firmware not parsing serial) is swallowed via [runCatching] so the UI never crashes.
 *
 * The connection is established lazily on first use and reused for subsequent commands; [disconnect]
 * tears it down (call from the ViewModel's onCleared).
 */
class VcuRobotRepository(private val context: Context) : RobotRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var manager: ConnectionManager? = null
    // Connect exactly once and reuse the socket; @Volatile so concurrent send coroutines see it.
    private val connectMutex = Mutex()
    @Volatile private var isConnected = false

    private suspend fun manager(): ConnectionManager {
        if (manager == null) {
            val prefs: ConnectionPreferences = ConnectionPreferencesStore.preferences(context).first()
            manager = ConnectionManager(context, prefs)
        }
        return manager!!
    }

    // Connect once and reuse the socket for every later send. The Mutex serializes the connect
    // decision so two rapid sends (e.g. speed + direction) can't both call connect() — the old
    // code reconnected on every pair and the second connect() closed the in-flight socket,
    // dropping the direction command.
    private fun sendAsync(cmd: String) {
        scope.launch {
            runCatching {
                val m = manager()
                connectMutex.withLock {
                    if (!isConnected) {
                        m.connect()
                        isConnected = true
                    }
                }
                m.send(cmd)
            }.onFailure { isConnected = false }
        }
    }

    override fun status(): RobotStatus = RobotStatus()

    override fun sendDriveCommand(command: DriveCommand) = sendAsync(command.toProtocol())

    override fun updateSpeed(percent: Int) = sendAsync(speedCommand(percent))

    override fun setPto(enabled: Boolean) = sendAsync(ptoCommand(enabled))

    override fun setLights(enabled: Boolean) = sendAsync(lightsCommand(enabled))

    override fun setHydraulic(heightPercent: Int) = sendAsync(hydraulicCommand(heightPercent))

    override fun horn() = sendAsync(hornCommand())

    override fun disconnect() {
        scope.launch { runCatching { manager()?.disconnect(); isConnected = false } }
    }
}
