# Camera System Documentation

## Overview
The Camera system in BhoomiBot implements a CameraX-based video pipeline that streams live footage from the robot phone to the operator phone via the Live Link relay.

## Key Components

### 1. BackCameraPreview.kt (Preview Surface)
- **Package**: `com.bhoomibot.os.feature.camera`
- **Purpose**: Renders the local camera feed on the robot phone
- **CameraX Integration**: Uses CameraX API for lifecycle-aware streaming
- **Orientation**: Likely portrait/landscape switchable

### 2. CameraScreen.kt (UI Screen)
- **Package**: `com.bhoomibot.os.feature.camera`
- **Purpose**: Full-screen camera view on the operator phone
- **Current Status**: Placeholder - shows "Module ready"
- **Design**: Uses `OperationalScreen` scaffold

### 3. Frame Handling (via LiveLink)
- **Publish Path**: Robot → JPEG encode → publishFrame() → WebSocket → Operator
- **Display Path**: Operator receives `LiveFrame` → renders in `OperatorLiveScreen` or `ManualControlScreen`

## Data Flow

### Capture → Encode → Transmit
```kotlin
// Robot side (inferred from LiveLinkRepository interface)
cameraCallback(frame) -> jpegByteArray -> publishFrame(jpegByteArray)
// -> LiveEnvelope with type VIDEO_FRAME -> WebSocket -> Operator
```

### Receive → Decode → Display
```kotlin
// Operator side
frames Flow<LiveFrame> -> operator receives JPEG bytes
// -> decode to ImageBitmap -> display in LiveCameraPreview
```

## Configuration

### From ConnectionConfig.kt
```kotlin
videoFps: Int = 12           // Frames per second
videoQuality: VideoQuality = VideoQuality.MEDIUM  // Resolution/quality
```

### Video Quality Levels (from ConnectionConfig.kt)
| Quality | Resolution | Description |
|---------|------------|-------------|
| LOW | 360p | 480px longest side, 45% JPEG quality |
| MEDIUM | 540p | 720px longest side, 58% JPEG quality |
| HIGH | 720p | 1280px longest side, 72% JPEG quality |

## Protocol Integration
- **Frame Type**: `LiveMessageType.VIDEO_FRAME`
- **Transmission**: Binary JPEG bytes (not JSON-encapsulated)
- **Envelope**: Binary frames skip the JSON envelope; travel as raw bytes separate from text

## Integration Points
- **ManualControlScreen**: Embeds camera in control UI
- **OperatorLiveScreen**: Full-screen remote camera view
- **MaximizedCameraOverlay**: Forces landscape for wide angle view
- **LiveCameraPreview**: Card UI with ON/OFF toggle

## Camera Controls in Manual UI
- **Live Camera Toggle**: ON/OFF switch in manual screen
- **Maximize**: Fullscreen landscape mode
- **Retry**: Reconnect when link drops

## Current Limitations
- Placeholder CameraScreen (not yet fully implemented)
- No AI processing on frames (autonomous features are future)
- No recording/clip saving on robot side
- No resolution change at runtime

## Future Extension Points
- AI object detection (perception AI pipeline)
- Recording and clip export
- Night mode / IR camera support
- Multi-camera support

## Dependencies
- CameraX (camera-camera2, camera-lifecycle, camera-view)
- Compose UI for preview surface
- OkHttp for WebSocket binary frame transmission

## Key File Locations
| File | Purpose |
|------|---------|
| `feature/camera/BackCameraPreview.kt` | Local camera preview implementation |
| `feature/camera/CameraScreen.kt` | Placeholder full camera screen |
| `connection/model/LiveFrame.kt` | Frame container model |
| `connection/transport/WebSocketLiveLinkClient.kt` | Frame transmission |
| `connection/repository/LiveLinkRepositoryImpl.kt` | Frame publish/consume |
| `feature/live/RobotLiveScreen.kt` | Robot-side live link (publishes frames) |
| `feature/manual/ManualControlScreen.kt` | Manual mode with camera preview |