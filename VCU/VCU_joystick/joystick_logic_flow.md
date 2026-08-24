# BhoomiBot VCU: Complete Joystick Logic Architecture

This document breaks down the complex logic used to control the BhoomiBot. The system is split into three distinct processes: **The Command Parser**, **The Physics Engine**, and the **OTA Maintenance Mode**.

---

## 1. Process A: The Command Parser (Brain)
This process runs every time a Bluetooth packet arrives. It filters out "junk" data and prepares the robot's intended state.

```mermaid
graph TD
    Start([Bluetooth Packet Arrives]) --> Trim[Trim Whitespace & Clean String]
    Trim --> Type{What Type?}
    
    %% OTA Handling
    Type -- "OTA" --> OTA[Trigger OTA Mode]
    OTA --> OTASafety[Force Stop & Engage Brakes]
    OTASafety --> WiFi[Start WiFi AP & Web Server]
    
    %% Speed Handling
    Type -- "SPD XX" --> SpeedTrap{Is Robot Turning?}
    SpeedTrap -- Yes --> Ignore[Ignore: Prevent Speed Jump during Sync]
    SpeedTrap -- No --> UpdateSpeed[Update Target Speed & Store Time]
    
    %% Direction Handling
    Type -- "L / R" --> Shield{Speed changed < 150ms ago?}
    Shield -- Yes --> Revert[Revert Speed to Previous Level]
    Shield -- No --> CheckDir{Same as last turn?}
    CheckDir -- Yes --> IncInt[Increment Turn Intensity 1-4]
    CheckDir -- No --> ResetInt[Set Intensity = 1]
    
    %% Restore / Stop Handling
    Type -- "F / B / S / E" --> ClearInt[Reset Intensity to 0]
    ClearInt --> SetDir[Update Main Direction]
    
    UpdateSpeed --> End([State Ready for Physics])
    Revert --> CheckDir
    IncInt --> End
    ResetInt --> End
    SetDir --> End
    Ignore --> End
```

---

## 2. Process B: The Physics Engine (Body)
This process runs strictly at **1000Hz (1ms)**. It ensures the motors never move in a way that damages the controllers or gearboxes.

```mermaid
graph TD
    Loop([1ms Timer Trigger]) --> OTACheck{In OTA Mode?}
    
    OTACheck -- Yes --> Web[Handle Web Requests & ElegantOTA]
    Web --> LED[Status LED Fast Blink]
    
    OTACheck -- No --> StageCheck{Direction or Intensity Changed?}
    
    %% Relay & Arc Protection
    StageCheck -- Yes --> ArcStop[Force DAC to 0V]
    ArcStop --> Wait[Wait 15ms: Let Field Collapse]
    Wait --> Flip[Flip Relays 18/19]
    Flip --> Pulse[Pulse Start: Jump instantly to SPD 2]
    Pulse --> Ramp
    
    %% Slew Rate Ramping
    StageCheck -- No --> TargetCalc[Calculate Target DAC based on Stage]
    TargetCalc --> Ramp{Current < Target?}
    Ramp -- Yes --> StepUp[Increase by SLEW_RATE]
    Ramp -- No --> StepDown[Decrease by SLEW_RATE * 2]
    
    %% Sequential Output
    StepUp --> DAC_L[Write Left DAC]
    StepDown --> DAC_L
    DAC_L --> micro[Wait 50µs: Settlement]
    micro --> DAC_R[Write Right DAC]
    DAC_R --> LoopEnd([Loop Complete])
```

---

## 3. The OTA Maintenance Mode
When the `OTA` command is received, the robot undergoes a **Safety Transformation**:

| Step | Action | Reason |
| :--- | :--- | :--- |
| **1** | **Total Stop** | Motors are cut to 0V and brakes are engaged. The robot MUST be stationary for safety. |
| **2** | **End Bluetooth** | Frees up the Radio for high-speed WiFi data transfer. |
| **3** | **Start WiFi AP** | Creates a local network named `BhoomiBot_Update`. |
| **4** | **Boot Web Portal** | Starts a web server at `192.168.4.1/update` for the technician. |

---

## 4. Key Engineering Timings

| Timing | Value | Why? (The Physics) |
| :--- | :--- | :--- |
| **Control Loop** | 1ms | Human reaction time is ~200ms. 1ms ensures the robot feels "Zero Lag." |
| **Arc Protection**| 15ms | Switching a relay under load creates a spark. 0V for 15ms saves the relay contacts. |
| **Transition Shield**| 150ms | Apps often send a speed bump *before* a turn. This 150ms window undoes that mistake. |
| **Slew Rate** | 5 units | Prevents the motor from "snapping" the gearbox teeth. |
| **OTA LED Pulse** | 200ms | Fast blinking provides clear visual confirmation of Maintenance Mode. |

---
*Documented by Meta-Engineer for BhoomiBot VCU Engineering Team.*
