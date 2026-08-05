# Asynchronous Communication System Documentation

## System Overview
BhoomiBot implements an asynchronous communication architecture that enables decoupled interactions between the operator device and field robot. Key components include:

1. **LiveLinkRepository**: Role-agnostic interface for WebSocket communication
2. **VcuRobotRepository**: Local robot control implementation  
3. **Event Stream Processing**: Real-time telemetry/camera feed handling

## Core Components
- **Bidirectional Data Streams**: 
  ```diff
  + TelemetrySnapshots (Robot → Relay → Operator)
  + Camera Frames (Robot → Relay → Operator)
  - Robot Commands (Operator → Relay → Robot)
  ```

- **Safety Features**:
  - Automatic reconnection via ServerHold
  - Message retries with exponential backoff
  - State synchronization through MCU

## Communication Protocol
- **Message Types**: 
  - `HELLO` - Session initialization
  # LiveLinkRepositoryImpl.kt
  + `TELEMETRY` - Robot status updates
  # `COMMAND` - Operator-initiated actions
  # `VIDEO_FRAME` - Live camera feed transmission
  # `PEER_STATUS` - Connection health indicators

## End-to-End Flow
1. Operator initiates command through Manual Control UI
2. Command routed through LiveLinkRepositoryImpl 
3. Envelope serialized with routing metadata
4. Relay server forwards to target robot
5. Robot receives and executes command
6. State updates sent back to operator

## Technical Constraints
- Single message throughput limited by WebSocket bandwidth
- Payload size capped at 64KB
- Message ordering preserved per stream
- Retries handled client-side