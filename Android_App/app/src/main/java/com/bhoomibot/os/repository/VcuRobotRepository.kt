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
import com.bhoomibot.os.vcu.lightsCommand
import com.bhoomibot.os.vcu.ptoCommand
import com.bhoomibot.os.vcu.speedCommand
import com.bhoomibot.os.vcu.toProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var connectJob: Job? = null

    private suspend fun manager(): ConnectionManager {
        if (manager == null) {
            val prefs: ConnectionPreferences = ConnectionPreferencesStore.preferences(context).first()
            manager = ConnectionManager(context, prefs)
        }
        return manager!!
    }

    // Lazily connect once; reuse the existing connection for later commands.
    private fun sendAsync(cmd: String) {
        scope.launch {
            runCatching {
                val m = manager()
                if (connectJob?.isCompleted != false) connectJob = launch { m.connect() }
                connectJob?.join()
                m.send(cmd)
            }
        }
    }

    override fun status(): RobotStatus = RobotStatus()

    override fun sendDriveCommand(command: DriveCommand) = sendAsync(command.toProtocol())

    override fun updateSpeed(percent: Int) = sendAsync(speedCommand(percent))

    override fun setPto(enabled: Boolean) = sendAsync(ptoCommand(enabled))

    override fun setLights(enabled: Boolean) = sendAsync(lightsCommand(enabled))

    override fun disconnect() {
        scope.launch { runCatching { manager()?.disconnect() } }
    }
}
