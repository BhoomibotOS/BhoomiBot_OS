package com.bhoomibot.os.vcu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Factory & manager that hides details of BT / Wi‑Fi from UI.
 * Call `connect()` once. It will create the required socket
 * and expose `send()` / `disconnect()`.
 */
class ConnectionManager(
    private val context: Context,
    private val prefs: ConnectionPreferences
) {

    private var conn: Connection? = null

    suspend fun connect() {
        disconnect()
        conn = when (prefs.connectionType) {
            ConnectionType.BLUETOOTH -> BluetoothConn(prefs.bluetoothMacAddress)
            ConnectionType.WIFI_HOTSPOT -> WifiConn(prefs.wifiHost, prefs.wifiPort)
            ConnectionType.AUTO ->
                try {
                    BluetoothConn(prefs.bluetoothMacAddress)
                } catch (e: IOException) {
                    WifiConn(prefs.wifiHost, prefs.wifiPort)
                }
        }
    }

    suspend fun send(cmd: String) {
        conn?.send(cmd) ?: throw IllegalStateException("Not connected")
    }

    suspend fun disconnect() {
        conn?.close()
        conn = null
    }
}

/** ---------- Concrete implementations ---------- */

private class BluetoothConn(private val mac: String) : Connection {
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null

    @Throws(IOException::class)
    private suspend fun ensureSocket() {
        if (socket != null) return
        val device: BluetoothDevice = adapter?.getRemoteDevice(mac)
            ?: throw IOException("Bluetooth device not found: $mac")
        // SPP UUID – matches the classic Bluetooth Serial Port Profile
        socket = device.createRfcommSocketToServiceRecord(
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        )
        socket?.connect()
    }

    override suspend fun send(cmd: String) {
        withContext(Dispatchers.IO) {
            ensureSocket()
            val out: OutputStream = socket!!.outputStream
            out.write((cmd + "\n").toByteArray())
            out.flush()
        }
    }

    override suspend fun close() {
        socket?.close()
        socket = null
    }
}

private class WifiConn(host: String, port: Int) : Connection {
    private val targetHost: String = host
    private val targetPort: Int = port
    private var socket: Socket? = null

    private suspend fun ensureSocket() {
        if (socket == null) {
            socket = Socket()
            socket?.connect(InetSocketAddress(targetHost, targetPort), 5000)
        }
    }

    override suspend fun send(cmd: String) {
        withContext(Dispatchers.IO) {
            ensureSocket()
            socket?.outputStream?.write((cmd + "\n").toByteArray())
            socket?.outputStream?.flush()
        }
    }

    override suspend fun close() {
        socket?.close()
        socket = null
    }
}

/** Runtime abstraction for a persistent connection to the ESP32 */
interface Connection {
    suspend fun send(cmd: String)
    suspend fun close()
}