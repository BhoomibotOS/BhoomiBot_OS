package com.bhoomibot.os.connection.transport.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Manages the lifecycle of the [LocalRelayServer].
 */
object LocalHubManager {

    private var server: LocalRelayServer? = null
    
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _localIpAddress = MutableStateFlow("Detecting...")
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    fun startServer(context: Context, port: Int = 8080) {
        if (server != null) return
        
        try {
            val currentIp = getLocalIpAddress()
            _localIpAddress.value = currentIp
            _isServerRunning.value = true
            
            server = LocalRelayServer(
                port = port,
                onStarted = {
                    android.util.Log.i("LocalHub", "Server is live at $currentIp")
                },
                onFailed = { e ->
                    android.util.Log.e("LocalHub", "Server failed to start", e)
                    _isServerRunning.value = false
                    server = null
                }
            )
            server?.start()
            
        } catch (e: Exception) {
            android.util.Log.e("LocalHub", "Fatal error starting server", e)
            _isServerRunning.value = false
            server = null
        }
    }

    fun stopServer() {
        try {
            server?.stop()
        } catch (e: Exception) {
            android.util.Log.e("LocalHub", "Error stopping server", e)
        } finally {
            server = null
            _isServerRunning.value = false
            _localIpAddress.value = "Detecting..."
        }
    }

    /** 
     * Forced IP detection: Explicitly checks for mobile hotspot gateways.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "192.168.43.1"
            val interfaceList = interfaces.toList()

            // 1. Check for active AP/Hotspot interfaces (ap0, wlan1, etc)
            interfaceList.filter { it.isUp && (it.name.contains("ap") || it.name.contains("p2p") || it.name.contains("softap")) }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull()?.let { return it.hostAddress ?: "192.168.43.1" }

            // 2. Check for standard Wi-Fi (wlan0)
            interfaceList.filter { it.isUp && it.name.contains("wlan") }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull()?.let { return it.hostAddress ?: "192.168.43.1" }

            // 3. Fallback to any active non-loopback
            interfaceList.filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull()?.let { return it.hostAddress ?: "192.168.43.1" }

        } catch (ex: Exception) {
            android.util.Log.e("LocalHub", "IP Lookup failed", ex)
        }
        return "192.168.43.1" // Common Hotspot IP
    }
}
