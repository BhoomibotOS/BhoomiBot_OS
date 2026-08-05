# Autonomy Architecture

## Where Autonomy Runs

The autonomy layer runs on the **Robot phone** (DeviceRole.ROBOT) as a new feature module under `com.bhoomibot.os.feature.autonomous`. It sits above the existing `RobotRepository` abstraction, replacing manual input as the command source.

## How It Connects to Existing Systems

### Command Generation
Autonomy generates the same `DriveCommand` enum values (FORWARD, REVERSE, LEFT, RIGHT, STOP, EMERGENCY_STOP) that `ManualViewModel` already uses. These commands flow through the existing `RobotRepository` interface to `VcuRobotRepository` (real transport) or `LocalRobotRepository` (no-op).

### Data Sources
- **Position**: Uses existing GPS data available in `RobotStatus.gpsStatus`
- **State**: Reads `RobotStatus` for battery, mode, and connection state
- **Control**: Calls `RobotRepository.updateSpeed()`, `sendDriveCommand()`, `setPto()`, `setHydraulic()`
- **Storage**: Persists missions using existing DataStore infrastructure

### Integration Points
1. `RobotRepository` - Command output (same interface as Manual mode)
2. `RobotStatus` - State input (existing data model)
3. `DriveCommand` - Command vocabulary (existing enum)
4. `ControlCalibrationStore` - Speed/step parameters (existing calibration)
5. `VcuProtocol` - ASCII protocol (unchanged)

## Module Boundaries

### New Components (Autonomy Layer)
- `AutonomyController` - Orchestrates recording/playback
- `MissionStorage` - Saves/loads missions
- `RecordingEngine` - Captures manual operations
- `PlaybackEngine` - Executes recorded missions
- `WaypointTracker` - Follows recorded paths

### Existing Components (Unchanged)
- `RobotRepository` / `VcuRobotRepository` / `LocalRobotRepository`
- `ConnectionManager` (Bluetooth/WiFi to ESP32)
- `VcuProtocol` (ASCII command protocol)
- `RobotStatus` / `DriveCommand` (data models)
- `ControlCalibrationStore` (calibration values)

## Data Flow

```
Manual Input OR Autonomy Layer
    ↓
RobotRepository (interface)
    ↓
VcuRobotRepository → ConnectionManager → ESP32/VCU → Robot
    ↑
Telemetry ← RobotStatus
```

### Recording Mode
```
Manual Driving
    ↓
RecordingEngine captures:
  - DriveCommand (FORWARD, STOP, etc.)
  - Speed percentage
  - PTO state
  - Hydraulic state
  - GPS position
  - Timestamp
    ↓
MissionStorage (persists to DataStore)
```

### Playback Mode
```
MissionStorage loads mission
    ↓
PlaybackEngine replays:
  - DriveCommand sequence
  - Speed adjustments
  - PTO/hydraulic states
  - Timing intervals
    ↓
RobotRepository (same path as manual)
```

## Integration Points (Summary)

| Existing Component | Used For | Unchanged |
|---|---|---|
| `RobotRepository` | Command dispatch | Yes |
| `DriveCommand` | Movement vocabulary | Yes |
| `RobotStatus` | State monitoring | Yes |
| `VcuProtocol` | ESP32 communication | Yes |
| `ConnectionManager` | Bluetooth/WiFi | Yes |
| `ControlCalibrationStore` | Speed calibration | Yes |

## Constraints

- Does NOT modify the VCU/ESP32 protocol
- Does NOT replace Bluetooth or WiFi transport
- Does NOT introduce new sensors
- Does NOT create new robot control protocols
- Uses existing `RobotRepository` interface for all commands