package com.bhoomibot.os.feature.connection

/**
 * How the operator and robot phones reach the live-link relay.
 */
enum class PhoneNetworkMode(
    val title: String,
    val description: String
) {
    INTERNET(
        "Internet Mode",
        "Recommended for long range. Uses the public internet relay (requires data/SIM)."
    ),
    HOTSPOT(
        "Hotspot Mode",
        "Zero lag. Uses local Wi-Fi or Router. No internet or SIM card required."
    )
}
