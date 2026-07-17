package com.bhoomibot.os.viewmodel

import androidx.lifecycle.ViewModel
import com.bhoomibot.os.data.LocalRobotRepository
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository

class DashboardViewModel(private val repository: RobotRepository = LocalRobotRepository()) : ViewModel() {
    val status: RobotStatus = repository.status()
}
