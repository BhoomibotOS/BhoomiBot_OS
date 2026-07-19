package com.bhoomibot.os.feature.connection

import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.model.DeviceRole
import java.net.URI

/**
 * Editable state for the live-link connection screen.
 *
 * Validation lives beside the values so the disabled button, field feedback, and persisted
 * preferences agree about which address is safe. Public relay traffic must use `wss://`; a
 * plaintext `ws://` address is permitted only for an intentionally local trusted-Wi-Fi relay.
 */
data class ConnectionOptionsUiState(
    val role: DeviceRole = DeviceRole.OPERATOR,
    val serverUrl: String = "",
    val robotId: String = "",
    val sessionCode: String = "",
    val autoReconnect: Boolean = true,
    val networkMode: PhoneNetworkMode = PhoneNetworkMode.INTERNET,
    val videoFps: Int = 12,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val saved: Boolean = false
) {
    /**
     * The URL actually used to connect: [serverUrl] with its scheme normalized
     * (https:// -> wss://, http:// -> ws://) so users can paste the relay's
     * https address and still get a valid secure WebSocket URL.
     */
    val normalizedServerUrl: String
        get() = normalizeWebSocketUrl(serverUrl)

    /** True only when the address has an allowed WebSocket scheme and a real host. */
    val isServerUrlValid: Boolean
        get() = isUsableWebSocketUrl(
            url = normalizedServerUrl,
            allowInsecureLocalUrl = networkMode == PhoneNetworkMode.LOCAL_WIFI
        )

    /** Keeps the field label aligned with the selected network mode. */
    val serverUrlLabel: String
        get() = if (networkMode == PhoneNetworkMode.INTERNET) {
            "Server URL (wss://...)"
        } else {
            "Server URL (ws://... or wss://...)"
        }

    /** Explains exactly why a non-empty server address cannot start the live link. */
    val serverUrlError: String?
        get() = when {
            serverUrl.isBlank() -> null
            isServerUrlValid -> null
            networkMode == PhoneNetworkMode.INTERNET ->
                "Enter the relay's wss:// URL (https://bhoomibot-os.onrender.com works too — it's converted to wss://)"
            else ->
                "Enter ws://192.168.1.10:8080, wss://…, or an http(s):// URL (converted automatically)"
        }

    /**
     * Whether "Save & start" is allowed. Both phones must share a server URL, a Robot ID and a
     * session code to meet on the relay, and the fps must be in range.
     *
     * Scheme rule: on [PhoneNetworkMode.INTERNET] only a secure `wss://` URL is accepted (traffic
     * crosses the public internet); on [PhoneNetworkMode.LOCAL_WIFI] a plaintext `ws://` URL is
     * also allowed because both phones are on one trusted network.
     */
    val canStart: Boolean
        get() = isServerUrlValid &&
            robotId.isNotBlank() &&
            sessionCode.isNotBlank() && videoFps in 1..30
}

/** Validates address shape only; the live screen still reports unreachable relay failures. */
private fun isUsableWebSocketUrl(url: String, allowInsecureLocalUrl: Boolean): Boolean {
    val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
    val permittedScheme = uri.scheme.equals("wss", ignoreCase = true) ||
        (allowInsecureLocalUrl && uri.scheme.equals("ws", ignoreCase = true))
    return permittedScheme && !uri.host.isNullOrBlank()
}

/**
 * Accepts a user-typed relay URL and returns one the WebSocket client can use.
 * The common mistake is pasting the site's https:// address (e.g.
 * https://bhoomibot-os.onrender.com) — a WebSocket needs wss://. We normalize
 * https->wss and http->ws so the typed value still works. Anything already
 * wss:// / ws:// is returned unchanged.
 */
private fun normalizeWebSocketUrl(url: String): String {
    val trimmed = url.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.startsWith("https://") -> "wss://" + trimmed.substring("https://".length)
        lower.startsWith("http://") -> "ws://" + trimmed.substring("http://".length)
        else -> trimmed
    }
}
