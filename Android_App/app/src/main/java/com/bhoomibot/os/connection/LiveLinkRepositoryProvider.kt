package com.bhoomibot.os.connection

import android.app.Application
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryProvider

/**
 * Single decision point for the live-link repository.
 * AI-Fix: Now correctly returns the singleton instance to ensure the 
 * Background Service and the UI share the same network connection.
 */
fun provideLiveLinkRepository(application: Application): LiveLinkRepository =
    LiveLinkRepositoryProvider.get(application)
