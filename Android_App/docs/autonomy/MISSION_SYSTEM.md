# Mission System

## Mission Recording

### Current Operation Capture Flow
When in RECORDING state, the system captures the following data from ManualViewModel/UI interactions:

- **Drive Commands**: Classifies drive control (FORWARD, REVERSE, LEFT, RIGHT, STOP, EMERGENCY_STOP) pressed/released
- **Speed Percentage**: Integer -100 to +100 representing signed speed
- **PTO State**: Boolean for enabled/disabled, integer 0-100 for speed when enabled
- **Hydraulic State**: Boolean for enabled/disabled, integer 0-100 height
- **GPS Coordinates**: Captured via `DevicePreferences.getLatitude()` and `getLongitude()` (from LiveLink or device)
- **Timestamp**: Millisecond precision from `System.currentTimeMillis()`
- **Command Duration**: Derived from consecutive command intervals

### Observer Pattern
ManualViewModel contains an internal `RecordingEngine` that observes `ManualUiState` changes and records:
1. When drive mode changes (DIGITAL/JOYSTICK)
2. When speed changes via controller input
3. When PTO or hydraulic toggles/dials change
4. When hydraulic height changes
5. When camera light or horn triggers

All captured states are queued with timestamps in a `RecorderState` object.

### Sensor Data Integration
- Uses existing `RobotStatus.gpsStatus` for location
- Leverages `ControlCalibrationStore` for speed calibration
- References existing `CommandPipeline` for underlying command flow

## Mission Storage

### Data Structure
```kotlin
// Represents a complete recorded operation
data class MissionRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val waypoints: List<Waypoint>,
    val rawCommands: List<CommandRecord>,
    // Metadata for operators
    val operatorId: String
    val createdTimestamp: Long
)
```

### Persistence Layer
- Uses Android DataStore with TypeConverters for `MissionRecord` serialization
- Stores as JSON objects with:
  - `id` (String)
  - `name` (String) 
  - `waypoints` (JSON Array of GPS coordinates)
  - `commands` (JSON Array of `CommandRecord`)
  - `metadata` (JSON with naming and IDs)
- Directory structure: `/autonomy/missions/` with incremental file numbering

### Access Control
- Only accessible when app in AUTONOMOUS_ROLE
- Requires runtime permission check `AUTONOMY_PERMISSION`
- Managed through existing `SettingsViewModel` security layer

## Mission Replay

### Command Execution Flow
Replaying a `MissionRecord` follows the exact same manual command generation path:
1. `PlaybackEngine` reads `rawCommands` array sequentially
2. For each `CommandRecord`:
   - Calls `RobotRepository.sendDriveCommand()` with same `DriveCommand` enum
   - Calls `updateSpeed()` with same speed percent
   - Calls `setPto()` with same PTO state
   - Calls `setHydraulic()` with same hydraulic height
3. Replays timing intervals between commands using `SystemClock.sleep()`
4. Maintains original command ordering and dependencies

### Waypoint Tracking
- Uses GPS coordinates from stored `waypoints` list
- Integrates with existing `LocationManager` service
- Employs `PositionController` for heading correction
- Uses `SpeedAdjuster` for velocity matching

### Mission Lifecycle
1. **RECORDING**: Capture manual operation
2. **SAVED**: Persist to storage
3. **LOADED**: Parse mission metadata
4. **EXECUTING**: Run playback engine
5. **PAUSED**: Support user interruption
6. **COMPLETED**: Mission finished successfully
7. **ERROR**: Handle execution failures

## Mission Lifecycle States

| State | Description | Transition |
|-------|-------------|------------|
| `IDLE` | No active mission | Initial state or after completion |
| `RECORDING` | Actively capturing commands | Button press initiates | 
| `MISSION_SAVED` | Mission persisted to storage | Recording stop → save completed |
| `READY` | Mission loaded and configured | From MISSION_SAVED when ready to play |
| `EXECUTING` | Running playback sequence | After READY → start | 
| `PAUSED` | Temporarily halted execution | User interrupt | 
| `COMPLETED` | Successfully finished mission | Reached end of commands |
| `ERROR` | Execution failure detected | During execution | 
| `EMERGENCY_STOP` | Critical stop condition | Manual interrupt |