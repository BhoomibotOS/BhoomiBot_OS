package com.bhoomibot.os.repository

import android.app.Application
import com.bhoomibot.os.data.LocalRobotRepository

/**
 * Singleton decision point for which [RobotRepository] the app uses.
 * 
 * Ensures that all ViewModels share the same hardware connection instance.
 */
const val USE_REAL_TRANSPORT = true

object RobotRepositoryProvider {
    private var instance: RobotRepository? = null

    fun get(application: Application): RobotRepository {
        if (instance == null) {
            instance = if (USE_REAL_TRANSPORT) {
                VcuRobotRepository(application.applicationContext)
            } else {
                LocalRobotRepository()
            }
        }
        return instance!!
    }
}

/** Legacy helper for existing ViewModels */
fun provideRobotRepository(application: Application): RobotRepository = RobotRepositoryProvider.get(application)
