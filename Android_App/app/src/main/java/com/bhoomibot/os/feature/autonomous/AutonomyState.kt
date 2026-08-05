package com.bhoomibot.os.feature.autonomous

/**
 * Lifecycle states for the autonomy subsystem.
 * 
 * Follows the state diagram in docs/autonomy/STATE_MACHINE.md.
 */
enum class AutonomyState {
    IDLE,               // System waiting for action
    RECORDING,          // Capturing manual commands + GPS
    MISSION_SAVED,      // Recording finished and persisted
    READY,              // Mission loaded and ready to execute
    EXECUTING,          // Replaying commands
    PAUSED,             // Execution suspended, but position preserved
    COMPLETED,          // Mission finished successfully
    ERROR,              // Failure detected (GPS loss, disconnect, etc.)
    EMERGENCY_STOP      // Forced halt, hardware intervention required
}
