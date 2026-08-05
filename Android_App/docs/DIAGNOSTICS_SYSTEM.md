# Diagnostics System Documentation

## Overview
The diagnostics system provides system health monitoring for the BhoomiBot field robot. Currently implemented as a placeholder with future expansion planned.

## Current Implementation

### DiagnosticsScreen.kt
- **Package**: `com.bhoomibot.os.feature.diagnostics`
- **Status**: Placeholder using `OperationalScreen`
- **Display**: "Module ready" - "System health"
- **Navigation**: Returns to calling screen via `onBackClick`

### Data Source
System health data originates from:
1. **RobotRepository.status()** → `RobotStatus` model
2. **LiveLinkRepository.telemetry** → `TelemetrySnapshot`
3. **Connection state** → `LiveConnectionState`

### RobotStatus.kt (Available Data)
```kotlin
data class RobotStatus(
    val isOnline: Boolean = true,
    val batteryPercent: Int = 85,
    val mode: String = "Manual",
    val mission: String = "Idle",
    val gpsStatus: String = "Connected",
    val cameraStatus: String = "Ready",
    val aiStatus: String = "Standing by"
)
```

## Planned Features (Inferred from Architecture)

### Diagnostic Categories
1. **Power System**
   - Battery level/health
   - Charging status
   - Voltage/current monitoring

2. **Communication**
   - Local (BT/WiFi) connection quality
   - Live Link relay status
   - Packet loss/latency metrics

3. **Sensors**
   - GPS fix quality/satellites
   - IMU calibration
   - Camera feed status

4. **Actuators**
   - Motor temperatures
   - Motor current draw
   - PTO/hydraulic pressure

5. **Software**
   - Firmware version
   - App version
   - Memory/storage usage

## Integration Points

### Current Dashboard
- **DashboardViewModel** reads `RobotRepository.status()`
- Displayed on Operator/Robot home screens
- Real-time battery, GPS, mode indicators

### Manual Control Screen
- Top status bar shows:
  - BAT: Battery percentage
  - GPS: GPS status
  - VCU: Vehicle Control Unit (hardcoded "Connected")
  - MODE: Current mode label

### Live Link Monitoring
- **LiveConnectionState** enum tracks:
  - IDLE, CONNECTING, CONNECTED, RECONNECTING, ERROR
- Displayed in `LiveConnectionHint` composable
- Connection error messages shown to operator

## Future Implementation Requirements

### Data Collection
- Regular telemetry snapshots from ESP32
- Historical logging for trend analysis
- Threshold-based alerting

### UI Components
- Dedicated DiagnosticsScreen with:
  - Real-time gauge widgets
  - Historical charts
  - Fault code display
  - Export/log download

### Alert System
- Severity levels (INFO/WARNING/ERROR)
- Push notifications for critical faults
- Auto-navigate to diagnostics on critical alerts

## Dependencies
- RobotRepository for local status
- LiveLinkRepository for remote telemetry
- CameraX for camera health
- GPS/location services for GPS health
- DataStore for historical data persistence

## Testing Considerations
- Mock telemetry for UI testing
- Fault injection for alert testing
- Battery simulation for power diagnostics
- Connection disruption testing