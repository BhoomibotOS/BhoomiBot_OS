package com.bhoomibot.os.feature.connection

/**
 * How the operator and robot phones reach the live-link relay. Drives which URL scheme
 * [ConnectionOptionsUiState.canStart] accepts: INTERNET requires secure `wss://`, LOCAL_WIFI
 * also permits plaintext `ws://`.
 *
 * The [title]/[description] are for display only; the enum's `name` (INTERNET / LOCAL_WIFI) is
 * what gets persisted to DataStore and parsed back in ConnectionOptionsViewModel, so renaming a
 * constant would invalidate previously saved values.
 */
enum class PhoneNetworkMode(
    val title: String,
    val description: String
) {
    INTERNET(
        "Different networks",
        "Recommended — each phone can use its own Wi-Fi or mobile data. Requires a secure wss:// relay URL."
    ),
    LOCAL_WIFI(
        "Same Wi-Fi",
        "Both phones are on one trusted Wi-Fi network. A local ws:// relay URL is allowed."
    )
}
