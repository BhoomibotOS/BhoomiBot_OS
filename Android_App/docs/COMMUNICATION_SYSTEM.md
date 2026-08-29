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

### 2. Internet Live Link (Cloudflare Relay)
**Purpose**: Remote operator control via a Cloudflare Workers relay server.

**Components**:
- `LiveLinkClient.kt` - Transport abstraction
- `WebSocketLiveLinkClient.kt` - WebSocket implementation (handles Cloudflare URL formatting)
- `LiveLinkRepositoryImpl.kt` - Message handling
- `LiveEnvelope.kt` - Message format
- `ConnectionConfig.kt` - Session parameters

---

## The Command Path (Step-by-Step)

### Phase 1: Packing (Operator Phone)
When an operator presses a button (e.g., UP arrow) on the **Manual Screen**:
1.  The `ManualViewModel` calculates the intent (e.g., "FORWARD at 40% speed").
2.  It packs these into a single `RobotCommand` JSON object.
3.  The `LiveLinkRepository` wraps this in a `LiveEnvelope` and sends it to Cloudflare.

### Phase 2: Relaying (Cloudflare Worker)
1.  The Cloudflare Worker receives the JSON message.
2.  It identifies the `robotId` from the URL or the envelope.
3.  It finds the corresponding "Session" (Durable Object) and broadcasts the message to the Robot Phone.

### Phase 3: Unpacking & Alignment (Robot Phone)
1.  The `BhoomiBotService` (running in the background on the Robot phone) receives the message.
2.  **CRITICAL SEQUENCE ALIGNMENT**: The service unpacks the JSON and talks to the VCU via Bluetooth in a specific order:
    *   **First**: It sends the **Speed** command (`SPD40`).
    *   **Second**: It sends the **Direction** command (`F`).
3.  This sequence matches the **Direct VCU Mode** behavior, ensuring the robot starts moving instantly and doesn't "jump" speed levels when straightening up.

---

## ASCII Protocol (VcuProtocol.kt)
The VCU firmware (ESP32) only understands short ASCII tokens. Every command ends with a newline (`\n`).

| Intent | ASCII Token | Description |
| :--- | :--- | :--- |
| **Forward** | `F` | Set motors to forward polarity |
| **Reverse** | `B` | Set motors to reverse polarity |
| **Left** | `L` | Spin motors for left turn |
| **Right** | `R` | Spin motors for right turn |
| **Stop** | `S` | Electronic brake (Stop) |
| **Emergency**| `E` | Immediate power cut |
| **Speed** | `SPD[0..100]` | PWM duty cycle (magnitude of movement) |
| **PTO** | `PTO0 / PTO1` | Power Take-Off Toggle |
| **Lights** | `LGT0 / LGT1` | Work Lights Toggle |

---

## Data Models

### RobotCommand.kt (Operator → Robot)
```kotlin
data class RobotCommand(
    val drive: DriveCommand = STOP,    // Directional intent (F, B, L, R, S)
    val speedPercent: Int = 0,         // Motor power (0 to 100)
    val emergencyStop: Boolean = false,
    val pto: Boolean? = null,          // Power Take-Off
    val lights: Boolean? = null,       // Headlights
    val liveCamera: Boolean? = null,   // Request broadcast Start/Stop
    val useRearCamera: Boolean? = null // Flip camera lens
)
```

---

## Key Design Decisions (Cloudflare Era)

### 1. Atomic JSON Transmission
Instead of sending many small serial-like messages over the internet, we send **one JSON object** per user interaction. This ensures the Robot receives the "Complete Intent" (Speed + Direction) at once, preventing "jittery" movement caused by internet lag.

### 2. Sequential Alignment (The VCU Rule)
The VCU (ESP32) is sensitive to the order of commands. To mimic a physical joystick:
1.  We set the "throttle" (Speed) first.
2.  We engage the "gear" (Direction) second.
This prevents the VCU from receiving a direction command while the speed is still 0, which would cause the robot to stay still.

### 3. Base64-JSON Video for Secure Relays
Because Cloudflare and other secure proxies can be strict about raw binary data, we wrap video frames in **Base64** strings inside the standard JSON envelope when using the internet mode. In local Hotspot mode, we use raw binary for maximum speed.
