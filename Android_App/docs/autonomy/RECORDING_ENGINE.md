# Recording Engine

## What Data is Captured During Manual Driving

### Core Data Types
1. **Drive Commands** (CommandRecord.drive)
   - FORWARDBACKWARD, LEFTRIGHT, STOP, EMERGENCY_STOP
   - Captured as `DriveCommand` enum values
   - Source: ManualViewModel action calls

2. **Speed** (CommandRecord.speedPercent)
   - Signed integer: -100 (full reverse) to +100 (full forward)
   - Captured when `updateSpeed()` is called
   - Source: ManualViewModel speed calculations

3. **PTO State** (CommandRecord.ptoEnabled, CommandRecord.ptoSpeedPercent)
   - Boolean enabled/disabled state
   - 0-100 speed percentage when enabled
   - Source: ManualViewModel onPtoToggle()/onPtoSpeedChanged()

4. **Hydraulic State** (CommandRecord.hydraulicEnabled, CommandRecord.hydraulicHeightPercent)
   - Boolean enabled/disabled state  
   - 0-100 height percentage
   - Source: ManualViewModel onHydraulicToggle()/onHydraulicHeightChanged()

5. **Light Controls** (CommandRecord.lightsEnabled, CommandRecord.cameraLightEnabled)
   - Boolean states for work lights and camera light
   - Source: ManualViewModel onLightsToggle()/onCameraLightToggle()

6. **Auxiliary Controls** (CommandRecord.hornTriggered)
   - Boolean for horn activation
   - Source: ManualViewModel onHorn()

## GPS Recording

### Location Source
- Uses existing `DevicePreferences.getLatitude()` and `getLongitude()`
- Falls back to `LocationManager.getLastKnownLocation()` when available
- Records coordinates when drive commands are executed
- GPS accuracy field from `Location.getAccuracy()`

### Position Data Structure
```kotlin
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)
```

## Heading

### Orientation Capture
- Uses `SensorManager.getOrientation()` for device heading
- Calculates relative heading from GPS movement
- Stores heading as degrees (0-360) relative to magnetic north
- Captures heading whenever drive command changes

### Heading Data Structure
```kotlin
data class CommandRecord(
    // ... other fields
    val heading: Float,  // degrees 0-360
    val timestamp: Long
)
```

## Speed

### Speed Capture
- Signed speed in meters/second from `ManualUiState.vehicleSpeedPercent`
- Converted to VCU percentage via `toVcuPercent()` helper
- Records both raw speed and VCU percentage for replay accuracy
- Captures speed changes from joystick or manual buttons

### Timing

### Timestamp Field
- Uses `System.currentTimeMillis()` for wall-clock time
- Records precise timing between commands
- Enables accurate replay timing
- Stores as Long milliseconds since epoch

### Sampling Strategy

### Event-Based Recording
- Records only when state changes occur
- Triggers: button press, slider movement, toggle action
- Does NOT sample continuously (avoids storage bloat)
- Timestamps captured at event trigger moment

### Minimum Interval
- Commands sampled at most every 50ms
- Prevents duplicate rapid-fire recordings
- Ensures smooth replay timing

### Data Capture Sequence
1. User initiates input (button/slider/toggle)
2. `ManualViewModel` processes input and updates state
3. `RecordingEngine` observes state change via StateFlow
4. `RecordingEngine` creates `CommandRecord` with:
   - Current DriveCommand enum value
   - Current speed percentage
   - PTO/hydraulic states
   - GPS position
   - Device heading
   - System timestamp
5. `CommandRecord` appended to `MissionRecord.rawCommands`

### Storage Format
```kotlin
data class CommandRecord(
    val drive: DriveCommand,
    val speedPercent: Int,
    val ptoEnabled: Boolean,
    val ptoSpeedPercent: Int,
    val hydraulicEnabled: Boolean,
    val hydraulicHeightPercent: Int,
    val lightsEnabled: Boolean,
    val cameraLightEnabled: Boolean,
    val hornTriggered: Boolean,
    val latitude: Double,
    val longitude: Double,
    val heading: Float,
    val timestamp: Long
)
```

### Quality Assurance
- Validates GPS fix before recording position
- Rejects commands with invalid speed values (<-100 or >100)
- Ensures PTO/hydraulic percentages within 0-100 range
- Checks timestamp monotonicity for ordering