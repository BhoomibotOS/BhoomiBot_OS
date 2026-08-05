# Playback Engine

## Loading Saved Missions

### Mission Loading Flow
1. User selects mission from mission library
2. `PlaybackEngine.loadMission(missionId)` called
3. `MissionStorage` retrieves `MissionRecord` from DataStore
4. Mission deserialized from JSON with `TypeConverter`
5. `rawCommands` list validated for integrity
6. `PlaybackEngine` prepares sequential command queue

### Validation Checks on Load
- Ensures mission ID matches stored mission
- Verifies `rawCommands` array is non-empty
- Validates all `DriveCommand` values are recognized
- Checks speed percentages within range (-100 to +100)
- Confirms PTO/hydraulic percentages within (0 to 100)
- Verifies timestamps are monotonically increasing
- Returns `LoadMissionResult` with success/failure status

## Replaying Commands

### Playback Sequence
`PlaybackEngine` iterates through `MissionRecord.rawCommands` sequentially:

1. For each `CommandRecord` at index `i`:
   a. Calculate delay = `CommandRecord[i].timestamp - CommandRecord[i-1].timestamp`
   b. Wait delay milliseconds (`SystemClock.sleep()`)
   c. Execute command:
      - `sendDriveCommand(CommandRecord.drive)`
      - `updateSpeed(CommandRecord.speedPercent)`
      - `setPto(CommandRecord.ptoEnabled)` with optional speed
      - `setHydraulic(CommandRecord.hydraulicHeightPercent)` with optional enable
      - `setLights(CommandRecord.lightsEnabled)`
      - `horn()` if `CommandRecord.hornTriggered` is true
   d. Update internal state tracking

### Timing Control
- Uses command timestamps for precise replay
- Delays between commands match original recording intervals
- Skips zero-duration commands (same timestamp as previous)
- Maintains command sequence integrity

### Pause
- User presses pause button during playback
- `PlaybackEngine.pause()` sets state to PAUSED
- Stops command execution loop
- Preserves current command index for resume
- Timer paused at current point

### Resume
- User presses resume after pause
- `PlaybackEngine.resume()` continues from paused index
- Recalculates delay based on elapsed pause time
- Resumes command execution exactly where paused
- Maintains timing integrity from pause point onward

### Stop
- User presses stop or emergency stop
- `PlaybackEngine.stop()` immediately halts execution
- Sends STOP command to RobotRepository
- Resets command index to beginning
- Clears any pending timer callbacks
- Returns to IDLE state

## Error Recovery

### Common Error Scenarios
1. **GPS Signal Lost During Playback**
   - Continue execution without position updates
   - Log warning for operator review
   - Do not halt mission execution

2. **Robot Repository Unavailable**
   - Check `RobotRepository` connection status
   - Attempt automatic reconnect
   - Queue commands for retry when reconnected
   - Log error if unavailable for >5 seconds

3. **Invalid Command Detected**
   - Skip invalid command with warning log
   - Continue to next command in sequence
   - Do not halt entire mission

4. **Speed Command Out of Range**
   - Clamp speed to valid range (-100 to +100)
   - Log warning with clamped value
   - Continue execution with clamped speed

### Error State
When error occurs during playback:
- `PlaybackEngine` transitions to ERROR state
- Last command index saved for debugging
- Error message available for UI display
- Robot sent EMERGENCY_STOP for safety
- Operator must explicitly clear error to continue

### Retry Strategy
- Transient communication errors: automatic retry up to 3 times
- Persistent hardware errors: halt playback and report
- Network errors (live link): queue commands for transmission