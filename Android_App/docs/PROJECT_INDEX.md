# BhoomiBot Android Application - Project Index

## Folder Structure
```
app/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── bhoomibot/
│       │           └── os/
│       │               ├── MainActivity.kt
│       │               ├── navigation/
│       │               │   └── AppNavigation.kt
│       │               ├── connection/
│       │               │   ├── ConnectionManager.kt
│       │               │   ├── model/
│       │               │   │   ├── ConnectionConfig.kt
│       │               │   │   ├── LiveEnvelope.kt
│       │               │   │   ├── LiveMessageType.kt
│       │               │   │   ├── LivePayloads.kt
│       │               │   │   ├── PeerStatus.kt
│       │               │   │   └── TelemetrySnapshot.kt
│       │               │   └── repository/
│       │               │       ├── LiveLinkRepository.kt
│       │               │       └── LiveLinkRepositoryImpl.kt
│       │               ├── model/
│       │               │   ├── DeviceRole.kt
│       │               │   └── DriveCommand.kt
│       │               ├── repository/
│       │               │   ├── RobotRepository.kt
│       │               │   ├── VcuRobotRepository.kt
│       │               │   └── LocalRobotRepository.kt
│       │               ├── viewmodel/
│       │               │   └── RobotViewModels.kt
│       │               ├── feature/
│       │               │   ├── camera/
│       │               │   │   ├── CameraScreen.kt
│       │               │   │   └── BackCameraPreview.kt
│       │               │   ├── autonomous/
│       │               │   │   └── AutonomousScreen.kt
│       │               │   ├── diagnostics/
│       │               │   │   └── DiagnosticsScreen.kt
│       │               │   ├── map/
│       │               │   │   └── MapScreen.kt
│       │               │   ├── robot/
│       │               │   │   └── RobotSectionScreen.kt
│       │               │   ├── operator/
│       │               │   │   └── OperatorHomeScreen.kt
│       │               │   └── settings/
│       │               │       └── SettingsViewModel.kt
│       │               ├── connection/
│       │               │   ├── ConnectionOptionsScreen.kt
│       │               │   ├── RobotLiveScreen.kt
│       │               │   └── OperatorLiveScreen.kt
│       │               └── common/
│       │                   └── OperationalScreen.kt
│       └── res/
│           ├── layout/
│           ├── values/
│           ├── mipmap-*
│           └── xml/
└── build.gradle.kts
└── gradle.properties
```

## Technology Stack
- **Framework**: Android Jetpack Compose (UI)
- **Language**: Kotlin
- **Networking**: OkHttp, Retrofit (implied from dependencies)
- **Bluetooth**: Android Bluetooth API
- **WebSocket**: Custom implementation for live link
- **Camera**: CameraX
- **Navigation**: Jetpack Navigation Component (Compose-based)
- **Coroutines**: For asynchronous programming
- **State Management**: ViewModel + Compose State

## Module Summary
1. **Navigation**: AppNavigation.kt manages all screen transitions
2. **Communication**: 
   - Bluetooth/Wi-Fi for local robot control
   - WebSocket for remote operator-robot communication
3. **Repositories**:
   - RobotRepository interface with VcuRobotRepository implementation
   - LiveLinkRepository interface with LiveLinkRepositoryImpl implementation
4. **UI Features**:
   - Camera processing and display
   - Diagnostics and system health monitoring
   - Manual control interface
   - Map visualization
5. **Settings**: Connection configuration and device preferences

## Key Files
- **MainActivity.kt**: Entry point of the application
- **AppNavigation.kt**: Navigation graph definition
- **ConnectionManager.kt**: Manages Bluetooth/WiFi connections
- **VcuRobotRepository.kt**: Real implementation of robot communication
- **LiveLinkRepositoryImpl.kt**: WebSocket-based live link implementation
- **RobotCommand.kt**: Defines robot command structure