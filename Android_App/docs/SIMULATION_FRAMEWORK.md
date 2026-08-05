# BhoomiBot AgentOS: Production-Grade Simulation Framework (SIL)

**Architectural Philosophy:** "Test as you fly, fly as you test." 
This framework implements **Software-in-the-Loop (SIL)** simulation. The core AgentOS (L0-L8) remains identical to the production code, but it is sandwiched between a **Virtual Farm** and **Mock Hardware**.

---

## 1. High-Level Architecture (The Simulation Sandwich)

```text
[ SCENARIO GENERATOR ] ➔ [ AGENT OS (Production Logic) ] ➔ [ MOCK HARDWARE ]
          │                         │                          │
          └─────➔ [ WORLD SIMULATOR / DIGITAL TWIN ] ──────────┘
```

---

## 2. Component Breakdown

### A. The World Simulator (The Physics)
*   **Coordinate System:** Local UTM (Universal Transverse Mercator) for GPS precision.
*   **Entities:** Roads, Crop Rows (Tomatoes, Mangoes), Obstacles (Dynamic Humans, Static Rocks).
*   **Physics Engine:** Calculates `Position(t+1) = Position(t) + Velocity(t) * dt`. Includes wheel slip and friction coefficients for Mud/Water.

### B. The Digital Twin (The State)
*   **State Vector:** `[X, Y, Z, Yaw, Pitch, Roll, Speed, Battery%, MotorTemp, CurrentSkill]`.
*   **Observability:** Exposes every internal variable of the stack for the Dashboard.

### C. Mock Hardware (The IO Drivers)
| Driver | Production Source | Simulation Source |
| :--- | :--- | :--- |
| **Mock GPS** | Android GPS Sensor | World Pos + Gaussian Noise |
| **Mock Camera** | CameraX API | Virtual Scene Render (Bitmaps) |
| **Mock IMU** | Gyroscope/Acc | Physics Engine Forces |
| **Mock ESP32** | Bluetooth Socket | Local Buffer / Loopback |
| **Mock VCU** | Serial ASCII | ASCII Parser + Velocity State |

---

## 3. Folder Structure (Maven Standard)

```text
com.bhoomibot.os.feature.autonomous.simulation
├── engine/              # Physics and Time-Stepping
│   ├── SimEngine.kt     # Master Clock (e.g., 20Hz)
│   └── DigitalTwin.kt   # State Mirror
├── world/               # Virtual Environment
│   ├── FarmMap.kt       # Entity Registry
│   └── Obstacle.kt      # Dynamic agents
├── hardware/            # The Mocks
│   ├── MockGps.kt       # NMEA Simulator
│   ├── MockCamera.kt    # Frame Generator
│   └── MockVcu.kt       # ASCII Protocol Emulator
├── scenario/            # Test Logic
│   ├── Scenario.kt      # Definition (Goal + Events)
│   └── ScenarioRunner.kt# Execution & Scoring
└── visualization/       # Dashboards
    └── SimDashboard.kt  # Real-time state display
```

---

## 4. Master Simulation Interface

Every layer in AgentOS must support a **Provider Injection**.

```kotlin
interface HardwareBridge {
    fun sendCommand(cmd: String)
}

// Production Implementation
class RealVcuBridge : HardwareBridge { ... }

// Simulation Implementation
class MockVcuBridge : HardwareBridge {
    val receivedCommands = mutableListOf<String>()
    override fun sendCommand(cmd: String) {
        receivedCommands.add(cmd)
        // Feed into Physics Engine
    }
}
```

---

## 5. Automated Scenario Example: "The Obstacle Test"
1.  **Init:** Start Sim at Point A (Shed). Goal: Point B (Field).
2.  **Trigger:** At `t=5s`, spawn a `Human` object 2 meters in front of the robot.
3.  **Validation:** 
    *   Did L4 (Perception) see the human?
    *   Did L7 (Control) calculate zero velocity?
    *   Did L8 (Hardware) send "S\n" or "E\n"?
4.  **Result:** PASS/FAIL reported to CI/CD.

---

## 6. Visualization Dashboard (SimView)
A real-time Compose-based dashboard showing:
*   **2D Bird's Eye View:** Robot path vs. Intended Trajectory.
*   **Sensor Feeds:** The "AI's View" (Bounding boxes on mock frames).
*   **Graph Monitor:** Real-time Knowledge Graph node creation.
*   **Health:** Battery drain curve and motor load.

---
*Architect's Note: By validating 95% of logic in SIL, we reduce field testing costs by 80% and prevent expensive physical hardware damage.*
