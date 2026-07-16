package com.bhoomibot.os.viewmodel

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.repository.provideRobotRepository

// Provides the robot status shown on the home Dashboard screen.
// Uses the repository selected by provideRobotRepository (fake by default, real VCU when enabled).
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // See ManualViewModel for why the repository is a field and not a default constructor parameter:
    // viewModel() needs a sole (Application) constructor, which Kotlin won't synthesize for defaults.
    private val repository: RobotRepository = provideRobotRepository(application)
    // The current robot status (battery, mode, GPS, camera, AI). Read once when the screen opens.
    val status: RobotStatus = repository.status()
}
