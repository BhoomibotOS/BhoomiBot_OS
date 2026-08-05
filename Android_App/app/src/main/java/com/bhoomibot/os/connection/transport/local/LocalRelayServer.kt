package com.bhoomibot.os.connection.transport.local

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * A local WebSocket relay server that runs on the Robot Phone.
 */
class LocalRelayServer(
    port: Int = 8080,
    private val onStarted: () -> Unit,
    private val onFailed: (Exception) -> Unit
) : WebSocketServer(InetSocketAddress(port)) {

    private val clients = mutableSetOf<WebSocket>()
    
    init {
        isReuseAddr = true
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        synchronized(clients) { clients.add(conn) }
        Log.d("LocalRelay", "New client connected: ${conn.remoteSocketAddress}")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        synchronized(clients) { clients.remove(conn) }
        Log.d("LocalRelay", "Client disconnected: ${conn.remoteSocketAddress}")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        broadcastToOthers(conn, message)
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        // Log.v("LocalRelay", "Binary frame: ${message.remaining()} bytes")
        broadcastToOthers(conn, message)
    }

    private fun broadcastToOthers(sender: WebSocket, data: Any) {
        synchronized(clients) {
            clients.forEach { client ->
                if (client != sender && client.isOpen) {
                    when (data) {
                        is String -> client.send(data)
                        is ByteBuffer -> {
                            // VERY IMPORTANT: Rewind for EACH client so they all read from start
                            data.rewind()
                            client.send(data)
                        }
                    }
                }
            }
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e("LocalRelay", "Server error: ${ex.message}", ex)
        if (conn == null) { 
            onFailed(ex)
        }
    }

    override fun onStart() {
        Log.i("LocalRelay", "Local Relay Server live")
        onStarted()
    }
}
