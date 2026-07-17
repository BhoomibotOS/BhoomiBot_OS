// ============================================================================
// LiveLinkRepositoryProvider.kt
// ----------------------------------------------------------------------------
// Tiny manual dependency-injection seam. This app wires dependencies by hand
// (no Hilt/Dagger), so each subsystem has one provide* function that decides
// which implementation to hand out. Screens/ViewModels call this instead of
// constructing LiveLinkRepositoryImpl directly, keeping the choice in one place.
// ============================================================================
package com.bhoomibot.os.connection

import android.app.Application
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryImpl

/**
 * Single decision point for the live-link repository (mirrors
 * [com.bhoomibot.os.repository.provideRobotRepository]).
 *
 * Today it always returns the real [LiveLinkRepositoryImpl] (WebSocket relay).
 * Swapping to a fake or a WebRTC transport later means changing only this line.
 */
fun provideLiveLinkRepository(application: Application): LiveLinkRepository =
    LiveLinkRepositoryImpl()
