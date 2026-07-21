/**
 * Persisted configuration for the INTERNET live link (relay URL, Robot ID, session code, video
 * quality/fps, network mode).
 *
 * Own DataStore, separate from the VCU [ConnectionPreferencesStore], so the two connection worlds
 * evolve independently. Read via [preferences]; written via [save].
 */
package com.bhoomibot.os.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.feature.connection.PhoneNetworkMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persisted configuration for the internet live link (relay URL, Robot ID, session
 * code, video quality). Kept in its OWN DataStore, separate from the VCU
 * [ConnectionPreferencesStore], so the two connection worlds evolve independently.
 * Read via [preferences]; written via [save].
 */
private val Context.liveLinkDataStore by preferencesDataStore(name = "bhoomibot_livelink_prefs")

data class LiveLinkPreferences(
    val serverUrl: String = "",
    val robotId: String = "",
    val sessionCode: String = "",
    val autoReconnect: Boolean = true,
    val networkMode: String = PhoneNetworkMode.INTERNET.name,
    val videoFps: Int = 12,
    val videoQuality: String = VideoQuality.MEDIUM.name,
    val recentServerUrls: List<String> = emptyList()
)

object LiveLinkPreferencesStore {
    private val SERVER_URL = stringPreferencesKey("live_server_url")
    private val ROBOT_ID = stringPreferencesKey("live_robot_id")
    private val SESSION_CODE = stringPreferencesKey("live_session_code")
    private val AUTO_RECONNECT = booleanPreferencesKey("live_auto_reconnect")
    private val NETWORK_MODE = stringPreferencesKey("live_network_mode")
    private val VIDEO_FPS = intPreferencesKey("live_video_fps")
    private val VIDEO_QUALITY = stringPreferencesKey("live_video_quality")
    private val RECENT_SERVER_URLS = stringPreferencesKey("live_recent_server_urls")

    fun preferences(context: Context): Flow<LiveLinkPreferences> =
        context.liveLinkDataStore.data.map { p ->
            LiveLinkPreferences(
                serverUrl = p[SERVER_URL] ?: "",
                robotId = p[ROBOT_ID] ?: "",
                sessionCode = p[SESSION_CODE] ?: "",
                autoReconnect = p[AUTO_RECONNECT] ?: true,
                networkMode = p[NETWORK_MODE] ?: PhoneNetworkMode.INTERNET.name,
                videoFps = p[VIDEO_FPS] ?: 12,
                videoQuality = p[VIDEO_QUALITY] ?: VideoQuality.MEDIUM.name,
                recentServerUrls = p[RECENT_SERVER_URLS]?.split('|')?.filter { it.isNotBlank() }
                    ?: emptyList()
            )
        }

    suspend fun save(context: Context, prefs: LiveLinkPreferences) {
        context.liveLinkDataStore.edit { p ->
            p[SERVER_URL] = prefs.serverUrl
            p[ROBOT_ID] = prefs.robotId
            p[SESSION_CODE] = prefs.sessionCode
            p[AUTO_RECONNECT] = prefs.autoReconnect
            p[NETWORK_MODE] = prefs.networkMode
            p[VIDEO_FPS] = prefs.videoFps
            p[VIDEO_QUALITY] = prefs.videoQuality
        }
    }

    /**
     * Remembers a server URL the user successfully entered, most-recent-first,
     * deduped, capped at [MAX_RECENT]. The Connection Options screen shows these
     * as quick-pick chips so the user doesn't have to retype the Render URL.
     */
    suspend fun addRecentServerUrl(context: Context, url: String) {
        if (url.isBlank()) return
        context.liveLinkDataStore.edit { p ->
            val current = p[RECENT_SERVER_URLS]?.split('|')?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(url) + current.filter { it != url }).take(MAX_RECENT)
            p[RECENT_SERVER_URLS] = updated.joinToString("|")
        }
    }

    /** Clears the remembered server-URL history (e.g. from a settings menu). */
    suspend fun clearRecentServerUrls(context: Context) {
        context.liveLinkDataStore.edit { it.remove(RECENT_SERVER_URLS) }
    }

    private const val MAX_RECENT = 5
}
