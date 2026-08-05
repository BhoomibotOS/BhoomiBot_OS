# Navigation

Simple navigation following recorded path.

## Following Recorded Path

### Waypoint Tracking
Uses GPS coordinates stored in `MissionRecord.waypoints`. Each waypoint contains:
- Latitude (Double)
- Longitude (Double)  
- Timestamp (Long milliseconds)
- Accuracy (Float meters)

### Tracking Logic
1. Read waypoints from loaded mission
2. Get current position from `DevicePreferences.getLatitude()/getLongitude()`
3. Calculate distance to next waypoint using haversine formula
4. When within threshold (5 meters), mark waypoint complete
5. Advance to next waypoint
6. Repeat until all waypoints processed

## Waypoint Tracking

### Waypoint Data Structure
```kotlin
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)
```

### Tracking Algorithm
1. Current position from GPS
2. Distance to waypoint = haversine(lat1, lng1, lat2, lng2)
3. If distance < 5m: waypoint reached
4. If waypoint reached: advance to next
5. If last waypoint: mission complete

### Position Correction
- Uses `ControlCalibrationStore` for speed adjustment
- Reads current speed from `ManualUiState.vehicleSpeedPercent`
- Adjusts speed based on distance to waypoint
- No new sensors - relies on existing GPS

## Speed Adjustment

### Simple Speed Control
Speed adjusted based on:
- Distance to next waypoint
- Current speed from `RobotStatus`
- Calibration values from `ControlCalibrationStore`

### Adjustment Logic
1. If distance > 50m: maintain max speed
2. If distance < 50m: reduce speed proportionally
3. If distance < 5m: prepare to stop
4. Speed clamped to valid range (-100 to +100)

### Speed Source
- Uses existing `toVcuPercent()` conversion
- Reads from `ControlCalibrationStore.calibration.value.maximumSpeedMetersPerSecond`
- No new speed control mechanism needed

## Integration Points

### Existing Components Used
- `DevicePreferences` - GPS coordinates
- `ControlCalibrationStore` - Speed limits
- `RobotRepository` - Speed commands
- `RobotStatus` - Current state

### No New Components
- No computer vision
- No AI perception
- No LiDAR
- No SLAM
- No object detection

## Simple Path Following Steps

1. Load waypoints from mission
2. Get current GPS position
3. Calculate distance to next waypoint
4. If close enough: advance to next waypoint
5. Adjust speed based on distance
6. Send speed command via `RobotRepository.updateSpeed()`
7. Send drive command via `RobotRepository.sendDriveCommand()`
8. Repeat until path complete