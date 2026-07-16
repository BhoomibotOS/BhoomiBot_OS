package com.bhoomibot.os.vcu

/**
 * Connection type enum for ESP32/VCU communication
 * Allows operator to choose between Bluetooth and WiFi (hotspot) connections
 */
enum class ConnectionType {
    /** Classic Bluetooth connection using DabbleESP32 protocol */
    BLUETOOTH {
        override val displayName: String = "Bluetooth"
        override val description: String = "Classic Bluetooth (Dabble ESP32 protocol)"
        override val icon: String = "bluetooth"
    },

    /** WiFi connection via operator phone hotspot */
    WIFI_HOTSPOT {
        override val displayName: String = "WiFi Hotspot"
        override val description: String = "Connect via phone hotspot (TCP/UDP)"
        override val icon: String = "wifi"
    },

    /** Auto-detect: tries Bluetooth first, falls back to WiFi */
    AUTO {
        override val displayName: String = "Auto Detect"
        override val description: String = "Try Bluetooth first, then WiFi"
        override val icon: String = "swap_horiz"
    };

    abstract val displayName: String
    abstract val description: String
    abstract val icon: String
}