# State Machine

## States

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> RECORDING : Start Recording
    RECORDING --> MISSION_SAVED : Save Successfully
    MISSION_SAVED --> READY : Load Mission
    READY --> EXECUTING : Start Execution
    EXECUTING --> PAUSED : Pause Button Pressed
    PAUSED --> EXECUTING : Resume Playback
    EXECUTING --> COMPLETED : Mission Complete
    EXECUTING --> ERROR : Execution Failure
    COMPLETED --> [*] : Finished
    ERROR --> EMERGENCY_STOP : Emergency Stop Button
    PAUSED --> ERROR : Timing Failure
    EMERGENCY_STOP --> [*] : System Stopped

    %% Manual transitions
    RECORDING --> ERROR : Invalid GPS
    EXECUTING --> ERROR : Robot Disconnected