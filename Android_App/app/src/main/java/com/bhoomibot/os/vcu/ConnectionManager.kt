package com.bhoomibot.os.vcu

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Factory & manager that hides details of BT / Wi‑Fi from UI.
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
                } catch (e: Exception) {
                    WifiConn(prefs.wifiHost, prefs.wifiPort)
                }
        }
    }

    suspend fun send(cmd: String) {
        conn?.send(cmd) ?: throw IllegalStateException("Not connected")
    }

    /** Returns a stream of lines received from the VCU using a thread-safe callbackFlow */
    fun receive(): Flow<String> {
        return conn?.receive() ?: callbackFlow { close() }
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
    private val mutex = Mutex()

    @Throws(IOException::class)
    private suspend fun ensureSocket() {
        if (socket?.isConnected == true) return
        
        mutex.withLock {
            if (socket?.isConnected == true) return@withLock
            
            runCatching { socket?.close() }
            
            val device: BluetoothDevice = adapter?.getRemoteDevice(mac)
                ?: throw IOException("Bluetooth device not found: $mac")
            
            socket = device.createRfcommSocketToServiceRecord(
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            )
            
            // AI-Fix: Always cancel discovery before connecting as it heavily slows down connection
            try {
                if (adapter?.isDiscovering == true) {
                    adapter.cancelDiscovery()
                }
            } catch (e: SecurityException) {
                android.util.Log.w("VCU", "Could not cancel discovery: ${e.message}")
            }
            
            socket?.connect()
            android.util.Log.i("VCU", "Bluetooth Connected to $mac")
        }
    }

    override suspend fun send(cmd: String) {
        withContext(Dispatchers.IO) {
            try {
                ensureSocket()
                val out: OutputStream = socket!!.outputStream
                out.write((cmd + "\n").toByteArray())
                out.flush()
            } catch (e: Exception) {
                android.util.Log.e("VCU", "BT Send Error: ${e.message}")
                throw e
            }
        }
    }

    override fun receive(): Flow<String> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val job = scope.launch {
            try {
                ensureSocket()
                val reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                while (isActive) {
                    val line = reader.readLine()
                    if (line != null) {
                        trySend(line)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VCU", "BT Receive Error: ${e.message}")
            } finally {
                close()
            }
        }
        awaitClose { 
            job.cancel() 
            // We don't close the socket here because send() might still need it.
            // Socket is closed in Connection.close()
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching { socket?.close() }
                socket = null
            }
        }
    }
}

private class WifiConn(host: String, port: Int) : Connection {
    private val targetHost: String = host
    private val targetPort: Int = port
    private var socket: Socket? = null
    private val mutex = Mutex()

    private suspend fun ensureSocket() {
        if (socket?.isConnected == true) return
        
        mutex.withLock {
            if (socket?.isConnected == true) return@withLock
            
            runCatching { socket?.close() }
            socket = Socket()
            socket?.connect(InetSocketAddress(targetHost, targetPort), 5000)
            android.util.Log.i("VCU", "WiFi Connected to $targetHost:$targetPort")
        }
    }

    override suspend fun send(cmd: String) {
        withContext(Dispatchers.IO) {
            try {
                ensureSocket()
                socket?.outputStream?.write((cmd + "\n").toByteArray())
                socket?.outputStream?.flush()
            } catch (e: Exception) {
                android.util.Log.e("VCU", "WiFi Send Error: ${e.message}")
                throw e
            }
        }
    }

    override fun receive(): Flow<String> = callbackFlow {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val job = scope.launch {
            try {
                ensureSocket()
                val reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                while (isActive) {
                    val line = reader.readLine()
                    if (line != null) {
                        trySend(line)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VCU", "WiFi Receive Error: ${e.message}")
            } finally {
                close()
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching { socket?.close() }
                socket = null
            }
        }
    }
}

/** Runtime abstraction for a persistent connection to the ESP32 */
interface Connection {
    suspend fun send(cmd: String)
    fun receive(): Flow<String>
    suspend fun close()
}
