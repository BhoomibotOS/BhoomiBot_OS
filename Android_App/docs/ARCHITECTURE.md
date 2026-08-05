# BhoomiBot OS Architecture

## Application Architecture
BhoomiBot OS follows a **single-Activity Compose-based architecture** with **ViewModel pattern** and **repository abstraction** for data layers. The application supports two distinct roles:
- **Operator Mode**: Controls the robot remotely via WebSocket
- **Robot Mode**: Runs on the robot itself, controlling hardware locally

## Core Components

### 1. Navigation System
- **MainActivity.kt**: Single Android Activity hosting Compose UI
- **AppNavigation.kt**: Defines navigation graph with routes for all screens
- Navigation uses **Jetpack Navigation Compose** with route constants

### 2. Role-Based Operation
The app determines role at startup via **OnboardingScreen**, persists selection in DevicePreferences:
- **Operator Home**: Entry point for remote control interface
- **Robot Home**: Entry point for on-board robot interface

### 3. Communication Layers

#### A. Local VCU/Esp32 Connection (Robot Hardware Control)
- **ConnectionManager.kt**: Abstracts Bluetooth/WiFi transport to ESP32
- **VcuRobotRepository.kt**: Implements RobotRepository interface for real hardware communication
- **RobotRepository.kt**: Interface defining robot control operations (drive, speed, PTO, lights, etc.)
- Protocol: ASCII-based commands defined in **DriveCommand.kt** and **VcuProtocol.kt**

#### B. Live Link (Internet Relay - WebSocket)
- **LiveLinkRepository.kt**: Role-agnostic interface for WebSocket communication
- **LiveLinkRepositoryImpl.kt**: Implementation using WebSocketLiveLinkClient
- Protocol: JSON envelopes defined in **LiveEnvelope.kt** with message types in **LiveMessageType.kt**
- Data flow:
  - Operator → Relay → Robot: RobotCommand messages
  - Robot → Relay → Operator: Telemetry and video frames

### 4. Data Models
- **RobotCommand.kt**: Drive/auxiliary commands (direction, speed, emergency stop, etc.)
- **LiveEnvelope.kt**: Standard message format for live link communication
- **ConnectionConfig.kt**: Configuration for live link sessions
- **PeerStatus.kt**: Tracks connected peers in live link session
- **TelemetrySnapshot.kt**: Robot status data sent to operator

### 5. UI Features (Feature Modules)
- **Camera**: Camera preview and processing (CameraScreen.kt, BackCameraPreview.kt)
- **Diagnostics**: System health display (DiagnosticsScreen.kt)
- **Manual Control**: Direct robot control interface (ManualControlScreen.kt)
- **Map**: Location/map visualization (MapScreen.kt)
- **Settings**: Configuration interfaces (SettingsViewModel.kt, etc.)

## Data Flow

### Startup Flow
1. MainActivity launches
2. DevicePreferences determines language/theme
3. OnboardingScreen prompts for role selection (Operator/Robot)
4. Based on role, navigates to OperatorHome or RobotHome
5. MainActivity hosts AppNavigation which manages all screen transitions

### Local Control Flow (Robot Mode)
1. UI triggers robot command via ViewModel
2. ViewModel calls RobotRepository.sendDriveCommand() etc.
3. VcuRobotRepository sends command via ConnectionManager (Bluetooth/WiFi)
4. ConnectionManager writes ASCII protocol to ESP32 hardware
5. ESP32 executes motor/peripheral commands

### Remote Control Flow (Operator Mode)
1. UI sends command via LiveLinkRepository.sendCommand()
2. LiveLinkRepositoryImpl packages command in LiveEnvelope
3. WebSocketLiveLinkClient sends JSON to relay server
4. Relay forwards message to target robot
5. Robot receives command via LiveLinkRepositoryImpl.incomingCommands flow
6. Robot ViewModel processes command and updates UI state
7. Robot publishes telemetry via LiveLinkRepository.publishTelemetry()
8. Telemetry flows back to operator via same relay path

### Camera Flow
1. Robot captures camera frame via CameraX
2. Frame encoded as JPEG
3. Robot publishes frame via LiveLinkRepository.publishFrame()
4. Frame flows to relay then to operator
5. Operator receives frame via LiveLinkRepository.frames flow
6. Operator displays frame in OperatorLiveScreen

## Key Design Patterns
- **Repository Pattern**: Abstracts data sources (Bluetooth, WebSocket, local storage)
- **ViewModel Pattern**: Manages UI-related data lifecycle
- **Single Source of Truth**: ConnectionConfig defines live link parameters
- **Event-Driven Communication**: Uses Kotlin Flows/StateFlows for reactive updates
- **Role Agnostic Interfaces**: LiveLinkRepository works for both operator and robot

## Current Features Implemented
- Role selection and persistence
- Bluetooth/WiFi connection to ESP32 hardware
- WebSocket-based live link with relay server
- Robot command transmission (drive, speed, auxiliary controls)
- Telemetry reception and display
- Camera frame streaming
- Basic UI screens for all major features
- Connection state monitoring and error handling

## Limitations / Not Yet Implemented
- Autonomous navigation logic (AutonomousScreen is placeholder)
- Advanced AI/perception features
- Complex mission planning
- Detailed diagnostic telemetry parsing
- Emergency stop hardware integration
- Full camera processing pipeline