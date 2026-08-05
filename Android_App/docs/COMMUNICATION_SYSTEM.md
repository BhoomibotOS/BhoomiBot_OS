# Communication System Documentation

## Overview
BhoomiBot implements a dual-path communication architecture supporting both local (robot-to-device) and remote (internet relay) communication modes.

## Communication Worlds

### 1. Local VCU/Esp32 Communication (Robot Mode)
**Purpose**: Direct hardware control of motors, PTO, lights, hydraulics

**Components**:
- `ConnectionManager.kt` - Socket management (Bluetooth/WiFi)
- `VcuRobotRepository.kt` - Command translation
- `VcuProtocol.kt` - ASCII protocol definitions
- `ConnectionPreferences.kt` - Connection settings

### 2. Internet Live Link (Operator Mode)
**Purpose**: Remote operator control via relay server

**Components**:
- `LiveLinkClient.kt` - Transport abstraction
- `WebSocketLiveLinkClient.kt` - WebSocket implementation
- `LiveLinkRepositoryImpl.kt` - Message handling
- `LiveEnvelope.kt` - Message format
- `ConnectionConfig.kt` - Session parameters

## Local Communication Flow (Robot Mode)

### ASCII Protocol (VcuProtocol.kt)
```
Drive Commands:
- F = FORWARD
- B = REVERSE  
- L = LEFT
- R = RIGHT
- S = STOP
- E = EMERGENCY_STOP

Aux Commands:
- SPD[-100..+100] = Speed percentage
- PTO0/PTO1 = Power take-off
- LGT0/LGT1 = Work lights
- HYD[0..100] = Hydraulic height
- HRN = Horn pulse
```

### ConnectionManager.kt Flow
1. User configures connection type (BT/WiFi/AUTO)
2. `connect()` opens appropriate socket
3. `send(cmd)` writes ASCII command with newline
4. `disconnect()` closes socket

### VcuRobotRepository.kt
- Wraps ConnectionManager in coroutines
- Fire-and-forget command dispatch
- Auto-reconnect on failure
- Mutex-protected connection state

## Remote Communication Flow (Live Link)

### Message Types (LiveMessageType.kt)
| Type | Direction | Purpose |
|------|-----------|---------|
| HELLO | Bidirectional | Session handshake |
| VIDEO_FRAME | Robot→Operator | JPEG camera frame |
| TELEMETRY | Robot→Operator | Robot status snapshot |
| COMMAND | Operator→Robot | Drive/aux commands |
| PEER_STATUS | Bidirectional | Connection health |
| PING/PONG | Bidirectional | Keepalive |
| ACK | Bidirectional | Message acknowledgment |

### LiveEnvelope Structure
```json
{
  "type": "COMMAND",
  "robotId": "BhoomiBot-01",
  "ts": 1719123456789,
  "payload": "{\"drive\":\"FORWARD\",...}",
  "ack": false,
  "code": 0,
  "retry": 0
}
```

### LiveLinkRepositoryImpl.kt Flow
1. **Connect**:
   - Store config (serverUrl, robotId, sessionCode, role)
   - Launch message collector coroutine
   - Open WebSocket connection

2. **Message Handling**:
   - Demultiplex by message type
   - Decode payload to appropriate model
   - Emit to typed flows (telemetry, commands, frames)

3. **Publish**:
   - Telemetry: `LiveEnvelope(type=TELEMETRY, payload=encodeTelemetry())`
   - Commands: `LiveEnvelope(type=COMMAND, payload=encodeCommand())`
   - Frames: `sendFrame(jpegBytes)` (binary, no envelope)

## Control Flow Comparison

### Manual Driving (Local)
```
UI → ManualViewModel → RobotRepository.sendDriveCommand() 
→ VcuRobotRepository → ConnectionManager.send() 
→ ESP32 Serial → Motors
```

### Remote Driving (Live Link)
```
UI → OperatorLiveViewModel → LiveLinkRepository.sendCommand()
→ LiveEnvelope → WebSocketLiveLinkClient → Relay Server
→ WebSocket to Robot → LiveLinkRepositoryImpl
→ Incoming Commands Flow → Robot ViewModel → Local Repository
→ VcuRobotRepository → ESP32 Serial → Motors
```

## Data Models

### RobotCommand.kt (Operator→Robot)
```kotlin
data class RobotCommand(
    val drive: DriveCommand = STOP,
    val speedPercent: Int = 0,
    val emergencyStop: Boolean = false,
    val pto: Boolean? = null,
    val lights: Boolean? = null,
    val liveCamera: Boolean? = null
)
```

### TelemetrySnapshot.kt (Robot→Operator)
Contains battery, GPS, camera, AI status information

### LiveFrame.kt
Container for JPEG-encoded camera frames

## Error Handling

### Connection States (LiveConnectionState.kt)
- IDLE - No connection
- CONNECTING - Establishing link
- CONNECTED - Active connection
- RECONNECTING - Link recovery
- ERROR - Connection failed

### Retry Logic
- Automatic reconnect on disconnect
- Exponential backoff for failed sends
- Configurable auto-reconnect in ConnectionConfig

## Configuration Persistence

### ConnectionPreferences.kt
- Connection type (BT/WiFi/AUTO)
- Bluetooth MAC address
- WiFi host/port
- Auto-reconnect flag
- Last connected timestamp
- Connection timeout

### LiveLinkPreferencesStore.kt
- Server URL
- Robot ID
- Session code
- Video settings
- Auto-reconnect preference

## Key Design Decisions

### 1. Repository Pattern
Both local and remote communicate via repository interfaces:
- `RobotRepository` (local VCU)
- `LiveLinkRepository` (remote WebSocket)

This allows UI to be transport-agnostic.

### 2. Fire-and-Forget Commands
Commands from UI are sent asynchronously without blocking:
- VcuRobotRepository uses `SupervisorJob` + `Dispatchers.IO`
- Failed sends are swallowed (no UI crashes)

### 3. Message Demultiplexing
Single inbound stream split by type:
- Reduces connection overhead
- Allows parallel processing
- Type-safe delivery to appropriate flows

### 4. Binary vs JSON Frames
Video frames transmitted as raw bytes:
- More efficient than base64 encoding
- Separate from text message stream
- Requires binary WebSocket frame support

## Testing Support
- `FakeLiveLinkClient` for unit tests
- `MockRobotData` for simulated status
- `runTest` scope injection for deterministic testing
- MockWebServer integration for relay testing