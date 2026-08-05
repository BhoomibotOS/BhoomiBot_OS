package com.bhoomibot.os.connection.repository

import android.app.Application

/**
 * Singleton provider for the LiveLinkRepository.
 * Ensures that both the Robot and Operator roles share the same network session.
 */
object LiveLinkRepositoryProvider {
    private var instance: LiveLinkRepository? = null

    fun get(application: Application): LiveLinkRepository {
        if (instance == null) {
            instance = LiveLinkRepositoryImpl()
        }
        return instance!!
    }
}
