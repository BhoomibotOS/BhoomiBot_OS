# End‑to‑End Design for Autonomous Agricultural Robot

**Goal:** Build a field‑robot that can (1) record a field boundary and task demonstration, (2) learn and repeat the demonstrated work pattern autonomously, and (3) operate safely at scale across unlimited fields and tasks.

---

## 1. System Overview

```
+-------------------+        Bluetooth/Wi‑Fi        +-------------------+
|   Android Front‑  | <-------------------------> |   ESP32 Firmware  |
|   end App         |   (Secure, Low‑Latency)    |   (Motor/Steer/  |
|   (Teach / Auto)  |                            |    Sensor Control)|
+-------------------+                            +-------------------+
        ^                                                   |
        |                                                   v
+-------------------+                               +-------------------+
|   Cloud / Local   |                               |   Sensors &       |
|   Storage (SQLite)|                               |   Actuators       |
|   (Fields, Tasks) |                               +-------------------+
+-------------------+
```

### Core Concepts
- **Teach Mode** – Farmer drives the robot once (or a few times) while the Android app records:
  - GPS trajectory
  - IMU orientation
  - Motor speeds
  - Tool state (ON/OFF, intensity)
  - Environmental data (camera images, LiDAR optional)
- **Repeat / Auto Mode** – The stored demonstration becomes a **learned policy** (behavioral model) that can be executed on new fields, generating coverage paths and adapting to geometry.
- **Safety & Oversight** – Geofence enforcement, emergency stop, battery monitoring, remote manual override.

---

## 2. Android Application Architecture

| Layer | Responsibility | Key Components |
|-------|----------------|----------------|
| **UI** | Displays maps, status, controls, and configuration | `MainActivity`, `TeachModeScreen`, `AutoModeScreen`, `MapViewFragment` |
| **ViewModel** | Holds UI state, coordinates data flow, manages persistence | `TeachModeViewModel`, `AutoModeViewModel` |
| **Repository** | Abstracts local SQLite + optional remote storage | `FieldRepository`, `PatternRepository` |
| **Domain** | Business logic – pattern learning, coverage planning, safety checks | `PatternLearner`, `CoveragePlanner`, `SafetyManager` |
| **Connectivity** | Manages Bluetooth/Serial link to ESP32, sends commands & receives telemetry | `Esp32BleConnector`, `Esp32TcpController` |
| **Permissions** | Requests location, storage, Bluetooth, foreground service | Manifest + `PermissionHelper` |

### Data Flow (Teach → Store → Learn → Execute)

1. **Recording** – Sensor streams (GPS, IMU, motor PWM, tool state) are sampled at 10‑20 Hz.
2. **Serialization** – Each sample becomes a `RecordedEvent` protobuf / JSON object, appended to a `FieldLog`.
3. **Pattern Extraction** – `PatternLearner` clusters trajectories, extracts key primitives (straight line, turn, turn‑type, implement depth, speed curve) and creates a **behavior graph**.
4. **Storage** – `FieldRepository` persists:
   - `Field` (name, boundary polygon, GPS centroid)
   - `Task` (type: weeding, spraying, harvesting…)
   - `Pattern` (behavior graph, parameters)
5. **Execution** – `AutoModeViewModel` loads the selected pattern, routes it through `CoveragePlanner` → waypoint generator → `Esp32Controller` → motor commands.
6. **Telemetry** – Real‑time status (position, battery, error flags) streamed back to UI for safety overlay.

---

## 3. ESP32 Firmware Architecture

| Module | Function |
|--------|----------|
| **Bootloader** | Initializes peripherals, sets up BLE/BT Classic, boots into safe mode if firmware update needed. |
| **Connectivity** | Handles incoming commands (JSON) from Android, acknowledges, implements timeout & retransmission. |
| **Motor Control** | PID loops for left/right wheel speed, steering servo control, implements smooth acceleration curves. |
| **Tool Control** | Digital I/O for pump, sprayer, cutter; PWM for variable‑rate actuation; safety interlock logic. |
| **Sensor Fusion** | Reads wheel encoders, IMU, optional ultrasonic/IR distance sensors; provides odometry to main loop. |
| **State Machine** | `IDLE → RECORD → TRANSIT → OPERATE → ERROR` with explicit transitions and watchdog timers. |
| **Safety Watchdog** | Monitors heartbeat from Android; if lost > 2 s → emergency stop, lock motors. |
| **Power Management** | Battery voltage monitor; graceful shutdown if < 3.5 V; low‑power sleep modes between runs. |
| **OTA Update** | Secure bootloader for firmware upgrades via BLE DFU. |

### Communication Protocol (Android ↔ ESP32)

- **Message Format:** JSON with fields: `{type: "CMD", payload: {...}, seq: <int>}` over BLE (GATT) or Wi‑Fi TCP.
- **Command Types:** `START_RECORD`, `STOP_RECORD`, `START_AUTO`, `STOP_AUTO`, `MOVE`, `SET_TOOL_STATE`, `RESET`, `HEARTBEAT`.
- **Telemetry Types:** `{type: "TELEMETERY", payload: {battery: xx, gps: {...}, imu: {...}}}`.
- **Reliability:** Sequence numbers, CRC‑16, ACK/NACK flow control.

---

## 4. Navigation & Coverage Algorithms

1. **Field Boundary Inflate / Offset** – Convert raw GPS polygon into a **geofence** with a safety margin (e.g., 0.5 m). Store as a convex hull or raster mask.
2. **Path Planning** –  
   - **Coverage Generation:** Use **lawn‑mower** or **spiral** patterns that respect the inflate margin.  
   - **Pattern Mapping:** Map extracted primitives to coverage primitives (e.g., “straight‑line @ 0.8 m/s, implement ON for 3 s”).  
   - **Dynamic Adjustments:** If obstacle detected (via ESP32 sensor feedback), branch to “avoid” sub‑graph and resume pattern after clearance.
3. **Return‑to‑Start / Charge** – After task completion, compute shortest‑distance route back to predefined home point or charging station.

---

## 5. Teach‑by‑Demonstration Learning Pipeline

```
RecordedEvent Stream → Feature Extraction → Primitive Detection → 
Behavior Graph → Abstract Policy (JSON) → Coverage Planner → Motor Commands
```

- **Feature Extraction:** Timestamp, lat/lon, speed, heading (from IMU), PWM values, tool state.
- **Primitive Detection:** Sliding‑window clustering to detect *Straight*, *Curve*, *Turn‑Left*, *Turn‑Right*, *StartTool*, *StopTool*. Each primitive is tagged with parameters (speed, curvature, duration).
- **Behavior Graph:** Nodes = primitives; Edges = sequence constraints. Optional **loop‑closure** for repetitive patterns.
- **Policy Abstractization:** Convert graph to a **parametrized task model** (e.g., “repeat pattern X, scaling factor = field‑area / demo‑area”). This model is stored as compact JSON.
- **Policy Execution:** During Auto Mode, the model is instantiated with current field geometry, producing a **continuous stream of waypoints + tool commands**. A simple **finite‑state machine** on the ESP32 follows this stream, with built‑in safety checks.

---

## 6. Data Structures & Database Design (SQLite)

```sql
-- Fields table
CREATE TABLE field (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    centroid_lat REAL,
    centroid_lon REAL,
    boundary_geojson TEXT,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tasks table
CREATE TABLE task (
    id          INTEGER PRIMARY KEY,
    field_id    INTEGER NOT NULL,
    type        TEXT,          -- e.g., "weeding", "spraying"
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(field_id) REFERENCES field(id)
);

-- Patterns table
CREATE TABLE pattern (
    id          INTEGER PRIMARY KEY,
    task_id     INTEGER NOT NULL,
    json_model  TEXT,          -- serialized behavior graph
    scaling_factor REAL,
    FOREIGN KEY(task_id) REFERENCES task(id)
);

-- Recorded events (optional raw log)
CREATE TABLE recording (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp   DATETIME,
    lat         REAL,
    lon         REAL,
    speed       REAL,
    heading_deg REAL,
    pwm_left    INTEGER,
    pwm_right   INTEGER,
    tool_state  INTEGER,
    pattern_id  INTEGER,
    FOREIGN KEY(pattern_id) REFERENCES pattern(id)
);
```

- **Indices**: On `field.name`, `task.type`, `pattern.task_id` for fast lookup.
- **Versioning**: Add `schema_version` column to enforce migration path.

---

## 7. Safety Mechanisms

| Layer | Mechanism |
|-------|-----------|
| **Hardware** | Emergency stop button on ESP32 board; watchdog timer forces motor off if heartbeat lost > 2 s. |
| **Software (Android)** | Real‑time telemetry overlay; if position deviates > 1 m from geofence → immediate stop & alert. |
| **Firmware** | Dual‑check: command validation (speed limits, tool‑state consistency) before actuation; redundant sensor confirmation for obstacle detection. |
| **User Controls** | Manual “Abort” button on UI; “Resume” after safe stop; configurable speed limits per task. |
| **Fail‑Safe Paths** | Pre‑programmed “return‑to‑home” and “stop‑in‑place” routines stored in firmware. |

---

## 8. Recommended Open‑Source Stack & Libraries

| Area | Library / Framework |
|------|----------------------|
| **Android UI** | Jetpack Compose, Mapbox/Android‑Maps‑SDK, Retrofit for REST (if using cloud), WorkManager for background ops |
| **Mapping / GIS** | OsmAnd‑Core, GeoJSON‑kit, Shapely (via Python for preprocessing), JTS Topology Suite |
| **BLE / Serial** | AndroidX‑Bluetooth‑LE, Kotlin Coroutines Flow for stream handling |
| **State Management** | Hilt (DI), Model‑View‑ViewModel, StateFlow |
| **Protobuf / JSON Serialization** | Google’s Protocol Buffers, kotlinx‑serialization |
| **Data Persistence** | Room (SQLite abstraction), H2 for testing |
| **Coverage Planning** | OR‑Tools (routing & vehicle‑routing algorithms), custom “lawn‑mower” generator |
| **ESP32 Firmware** | Arduino‑Core‑ESP32, FreeRTOS primitives, PID library, ESP‑Now / BLE libraries |
| **Testing** | JUnit5 + Robolectric, Espresso, ESP‑HOME tests (unit‑test on host) |
| **Build / CI** | Gradle Kotlin DSL, Fastlane for automated builds, GitHub Actions for CI pipelines |

---

## 9. Development Roadmap (Prototype → Commercial)

| Phase | Duration | Milestones |
|-------|----------|------------|
| **0 – Foundations** | 2 weeks | Repo set‑up, Android skeleton, ESP32 dev board, CI pipeline |
| **1 – Sensor Capture** | 3 weeks | GPS + IMU + motor telemetry logging, basic Android UI, raw recording pipeline |
| **2 – Pattern Extraction** | 4 weeks | Primitive detection algorithm, behavior graph serialization, storage layer |
| **3 – Auto Mode Prototype** | 4 weeks | Coverage planner, ESP32 command interpreter, safety watchdog |
| **4 – Obstacle & Adaptive Navigation** | 5 weeks | Sensor integration (ultrasonic/IR), dynamic replanning, emergency stop |
| **5 – Scaling & Fleet Features** | 6 weeks | Multi‑field DB, task queue, remote monitoring dashboard, OTA updates |
| **6 – Safety & Certification** | 3 weeks | Hazard analysis, ISO 26262 (functional safety) checklist, battery‑fail safe testing |
| **7 – Field Trials** | 8 weeks | Pilot with 2‑3 farms, data collection for model refinement, user feedback loop |
| **8 – Commercial Release** | 4 weeks | Polished UI, documentation, support channels, app store deployment |

*Each phase ends with a **demo‑ready** artifact and a set of **acceptance criteria** (e.g., “recorded path must stay within 0.2 m error of ground truth”).*

---

## 10. Verification & Testing Strategy

1. **Unit Tests** – Business logic (PatternLearner, CoveragePlanner) with JUnit.
2. **Integration Tests** – Android ↔ ESP32 command round‑trip using a mock BLE peripheral.
3. **Simulation** – Headless Unity / JavaFX mock of GPS/IMU streams to stress‑test coverage generation.
4. **Hardware‑in‑the‑Loop (HIL)** – Real robot on a small test field with recorded drives; verify geofence enforcement and emergency stop latency.
5. **User Acceptance** – Pilot farms run a set of tasks; collect error logs, adjust scaling factors.
6. **Performance Metrics** – Battery consumption per hectare, path repeatability error, average speed, system uptime.

---

## 11. Open Questions & Future Enhancements

| Feature | Description |
|---------|-------------|
| **Computer Vision** | On‑device crop‑row detection (YOLO‑v5 Tiny) for row‑following and weed‑targeted spraying. |
| **Weed Identification** | Image classification models running on the phone’s NPU to differentiate crops vs weeds. |
| **Voice Commands** | Wake‑word detection (e.g., “stop”, “slow down”) for hands‑free control. |
| **Fleet Coordination** | Distributed task scheduler using a lightweight broker (MQTT) to allocate fields across multiple robots. |
| **Advanced Battery Management** | Predictive range estimation based on terrain and load, dynamic return‑to‑charge routing. |
| **Edge AI** | Deploy TinyML models directly onto ESP32 for ultra‑low‑latency inference (e.g., obstacle avoidance). |

---

### Next Steps

1. **Approve this design** – If aligned with your vision, we can move to concrete implementation tasks (e.g., setting up the Android project, writing the ESP32 firmware skeleton, or drafting the first data model).
2. **Prioritize** – Which component should we prototype first? (e.g., “record GPS/IMU data” vs “build pattern extraction engine”).
3. **Provide any additional constraints** – Performance targets, supported Android API level, specific ESP32 hardware modules, or regulatory limits.

Let me know how you’d like to proceed!