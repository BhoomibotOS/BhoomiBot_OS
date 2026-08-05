# BhoomiBot OS: Comprehensive Technical & Operational Guide

## 1. Executive Summary
BhoomiBot OS is a dual-role Android application designed to control agricultural robots. It acts as both the **Robot's On-board Computer** (decision making, AI, hardware bridging) and the **Operator's Handheld Remote** (live monitoring, manual driving, and teaching).

---

## 2. Core Philosophy: The "Teach & Replay" Workflow
The app is built around the **"A techy in agri"** vision. Instead of complex programming, a farmer "teaches" the robot once, and the AI handles the rest.
1.  **Manual Drive:** Operator drives one pass via Bluetooth.
2.  **Learning:** Robot records the GPS path and hardware actions (PTO, Speed).
3.  **AI Expansion:** AI turns that one pass into a full-field "S-Pattern" (Zig-Zag).
4.  **Autonomous Replay:** Robot repeats the mission, using **TensorFlow** to stay centered in crop rows.

---

## 3. Step-by-Step System Architecture (For Humans & AI)

### Step A: Hardware Layer (The VCU)
*   **What it is:** An ESP32 microcontroller inside the robot chassis.
*   **How it talks:** Via Classic Bluetooth (SPP) or Wi-Fi TCP.
*   **Protocol:** Receives simple ASCII strings like `SPD:50\n` (Speed 50%) or `DIR:F\n` (Direction Forward).
*   **Code Location:** `com.bhoomibot.os.vcu.*`

### Step B: The Bridge (Robot Phone Mode)
*   **What it is:** A phone mounted physically on the robot.
*   **Responsibilities:**
    1.  **Local Hub:** Runs a local WebSocket server (Hotspot Mode) so it can receive commands without internet.
    2.  **Recorder:** Saves missions to its local storage so they are safe even if the operator disconnects.
    3.  **Translator:** Receives high-level "RobotCommand" from the operator and translates them to VCU strings for the motors.
*   **Code Location:** `com.bhoomibot.os.feature.live.RobotLiveViewModel`

### Step C: The Interface (Operator Phone Mode)
*   **What it is:** The phone in the farmer's hand.
*   **Responsibilities:**
    1.  **Remote HMI:** Provides the **Manual Control** buttons and Joystick.
    2.  **Live View:** Decodes and displays the robot's camera feed in real-time.
    3.  **Safety Layer:** Shows the **Red Safety Overlay** if the robot stalls or hits an error.
*   **Code Location:** `com.bhoomibot.os.feature.operator.OperatorHomeScreen`

---

## 4. Operational Modes (GUI Guide)

### 1. Internet Mode (Different Network)
*   **Use Case:** Long-range control (e.g., Robot in the field, Operator at the house).
*   **Requirement:** Both phones need a SIM card/Data.
*   **Path:** `Operator -> Render Cloud Server -> Robot`.

### 2. Hotspot Mode (Same Network)
*   **Use Case:** Precise teaching in remote fields with **No Internet**.
*   **Requirement:** Phones connected to the same Router or Robot's Hotspot.
*   **Path:** `Operator -> Local Hub (Robot Phone)`. This is the **Zero Lag** mode.

---

## 5. Intelligence Features

### AI-001: Field Coverage (Geometric AI)
*   Located in: `PathGenerator.kt`
*   **Logic:** Takes a straight line (lat/lon) and uses vector math to calculate parallel lines offset by the "Implement Width." It automatically flips the direction for every second pass (S-Pattern).

### AI-002: Smart Stall Detection
*   Located in: `SafetyMonitor.kt`
*   **Logic:** If **Speed > 0** but **GPS Displacement < 20cm** for 5 seconds, the robot triggers an **ALERT**. The operator's phone will "Ring" with a high-pitched alarm.

### AI-004: Vision Correction (TensorFlow)
*   Located in: `PerceptionEngine.kt`
*   **Logic:** Processes camera frames using **TensorFlow Lite**. It calculates a `steeringOffset` to keep the robot centered in the green crop row, correcting for any GPS inaccuracy.

---

## 6. Future-Proofing: 16KB Alignment
The app is configured in `build.gradle.kts` for **16KB Page Alignment**. This means it is ready for 2025 high-performance Android hardware (Android 15+) and won't crash when running heavy AI/Native workloads.

---

## 7. Developer Cheat Sheet (For Junior Engineers)
*   **UI Framework:** Jetpack Compose (Modern, declarative).
*   **State Management:** `StateFlow` inside ViewModels. The UI only "reacts" to state.
*   **Persistence:** `Android DataStore` (Not SharedPreferences). Missions are JSON strings.
*   **Navigation:** `AppNavigation.kt` is the master map of all screens.
*   **The "Singleton" Rule:** Hardware connection is a singleton in `RobotRepositoryProvider`. Never create a second connection instance.
