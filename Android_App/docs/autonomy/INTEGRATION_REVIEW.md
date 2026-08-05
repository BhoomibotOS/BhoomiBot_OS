**Integration Review for BhoomiBot Autonomy Architecture**  
*Document: docs/autonomy/INTEGRATION_REVIEW.md*  

---  

### **1. How will Recording Engine receive manual driving commands?**  
**Answer:**  
The Recording Engine will observe `ManualViewModel` via a StateFlow-based listener to capture:  
- Drive commands (`DriveCommand.enum` values) from UI interactions  
- Speed percentages, PTO/hydraulic states, and GPS coordinates  
This mirrors the existing `ManualViewModel` logic, which already tracks manual inputs.  
  
**Codebase Support:**  
- `ManualViewModel` already processes UI inputs  
- `RecordingEngine` (placeholder) will consume its state changes  
  
**Code Check:**  
- No direct hook detected in `ManualViewModel` yet  
- `RecordingEngine` class in `feature/autonomous` is empty  
  
---  

### **2. How can Playback Engine generate the same commands?**  
**Answer:**  
The Playback Engine will replay `CommandRecord` objects from persisted missions using:  
- `RobotRepository.sendDriveCommand()` (same as manual mode)  
- `updateSpeed()`, `setPto()`, `setHydraulic()` in sequence  
This reuses existing command generation logic.  
  
**Codebase Support:**  
- `MissionRecord.rawCommands` matches `CommandRecord` structure from recording  
- `PlaybackEngine` (placeholder) will execute identical method calls  
  
**Code Check:**  
- `CommandRecord` format matches `DriveCommand` and other state fields  
- `Replay Strategy` in `PLAYBACK_ENGINE.md` aligns with current `RobotRepository` API  
  
---  

### **3. What existing modules can autonomy reuse?**  
**Reusable Components:**  
- `RobotRepository` (interface) and `VcuRobotRepository` (implementation)  
- `DriveCommand` enum (shared vocabulary)  
- `RobotStatus` for state feedback  
- `ConnectionManager` for transport  
- `ControlCalibrationStore` for speed calibration  
- `DevicePreferences` for GPS data  
  
**Codebase Confirmation:**  
- All listed modules exist and are actively used  
- `RobotHeard` and `DriverInterface` in `code` module suggest additional compatibility  
  
---  

### **4. What new modules are required?**  
**Minimum Required Modules:**  
1. **MissionStorage** (to persist/load missions from/to DataStore)  
2. **AutonomyController** (orchestrates recording/playback state machine)  
3. **RecordingEngine** (captures manual operations)  
4. **PlaybackEngine** (executes recorded missions)  
5. **WaypointTracker** (follows recorded paths)  
6. **StateMachine** (manages autonomy lifecycle states)  
  
**Codebase Note:**  
- All these modules are placeholders in `feature/autonomous` (not implemented)  
  
---  

### **5. Identify gaps**  
**Critical Missing Elements:**  
- ** MissionStorage implementation** (DataStore integration is outlined but not coded)  
- **WaypointTracker logic** (path-following algorithm unimplemented)  
- **Command persistence format** (History of how `MissionRecord` is serialized/deserialized)  
- **StateMachine instantiation** (state transitions need initialization)  
- **Testing harness** for replay scenarios (no test cases detected)  
  
**Codebase Gaps:**  
- `LIVE` variables in `RobotViewModels` suggest runtime dependency resolution that may conflict with autonomy state  
  
---  

### **6. Verify autonomy flow**  
**Flow Validity (Based on Documentation/PROJECT-INDEX.md):**  
1. **Manual Mode → Recording Engine:**  
   - ✅ Valid: Existing `ManualViewModel` UI → `RecordingEngine` observer pattern  

2. **Recording Engine → Mission Storage:**  
   - ✅ Valid: `MissionRecord` structure and persistence plan match DataStore capabilities  

3. **Mission Storage → Playback Engine:**  
   - ✅ Valid: `rawCommands` reuse existing `RobotRepository` API  

4. **Playback Engine → Command Pipeline:**  
   - ✅ Valid: Commands re-enter system via same `sendDriveCommand()` path  
  
**Weaknesses Identified:**  
- **Missing implementation** in `feature/autonomous` (all autopilot modules are stubbed)  
- **No error handling** for playback failures (documented but not coded)  
  
---  

**Next Implementation Task:**  
Implement `MissionStorage` (DataStore integration) and `WaypointTracker` as starting points for autonomy.  

---  

**Update Required Documents:**  
- `docs/MASTER_STATUS.md`: Add "Integration Review Complete" under Completed Tasks  
- `docs/AI_SESSION_LOG.md`: Document missing module implementations and next steps  

Proceed?