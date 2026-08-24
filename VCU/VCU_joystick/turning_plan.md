# BhoomiBot VCU: Turning Phase Architecture

This document outlines the 4-stage turning logic and straight-line transition protocols for the BhoomiBot 4-wheel skid-steer chassis.

## 1. GPIO Pin Assignments
| Component | GPIO | Signal Type | Logic |
| :--- | :--- | :--- | :--- |
| **Right Motor Throttle** | 25 | Analog (DAC) | 0.8V to 3.3V |
| **Left Motor Throttle** | 26 | Analog (DAC) | 0.8V to 3.3V |
| **Right Reverse Relay** | 18 | Digital Out | HIGH = Reverse |
| **Left Reverse Relay** | 19 | Digital Out | HIGH = Reverse |
| **Shared Reverse (Legacy)** | 27 | Digital Out | HIGH = Both Reverse |

## 2. 4-Stage Turning Hierarchy
The `g_turnIntensity` variable tracks consecutive taps of the 'L' or 'R' buttons.

### **Stage 1: Differential Steering**
- **Trigger:** 1st Tap.
- **Logic:** Outer wheel = Target Speed. Inner wheel = **50% of Target Speed (Forward)**.
- **Purpose:** Fine adjustments while moving.

### **Stage 2: Gentle Pivot**
- **Trigger:** 2nd Tap.
- **Logic:** Outer wheel = Forward. Inner wheel = **MIN_PWM Reverse (~1.22V)**.
- **Purpose:** Initiating rotation on soft surfaces.

### **Stage 3: Power Pivot**
- **Trigger:** 3rd Tap.
- **Logic:** Outer wheel = Forward. Inner wheel = **~1.5V Reverse**.
- **Purpose:** Turning in thick grass, mud, or heavy loads.

### **Stage 4: Ultra Zero-Turn**
- **Trigger:** 4th Tap.
- **Logic:** Outer wheel = Forward. Inner wheel = **Full Speed Reverse**.
- **Purpose:** Rapid 360-degree rotation on center point.

## 3. Transition & Safety Protocols

### **The "Straight-Line" Reset**
- If the operator presses **Forward ('F')** or **Backward ('B')**, the `g_turnIntensity` immediately resets to **0**.
- Both relays are synchronized to the main command direction.

### **Arc Protection (Relay Safety)**
- When switching a motor from Forward to Reverse (or vice versa):
    1. Set DAC to **0V** for that motor.
    2. Wait **30ms** for the magnetic field to collapse and sparks to dissipate.
    3. Flip the Relay.
    4. Ramp DAC to new target.

## 4. Control Logic Flow
1. **BT Serial Reads:** `pump()` and `handleCommand()` update `g_dir` and `g_speed`.
2. **Intensity Stacking:** If new command matches current turn state, `g_turnIntensity++`.
3. **Execution Loop:** `applyDrive()` runs every 10ms to handle Slew-Rate ramping and relay transitions.
