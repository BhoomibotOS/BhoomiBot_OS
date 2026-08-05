# Autonomy Implementation Tasks

## AUT-001: Mission Data Models

**Objective:** Define core data structures for mission recording, storage, and playback.

**Description:** Create Kotlin data classes representing missions, command records, waypoints, and mission metadata. These models will be used throughout the autonomy system for recording manual operations and playing them back.

**Existing files to reuse:**
- `app/src/main/java/com/bhoomibot/os/model/DriveCommand.kt` (for DriveCommand enum)
- `app/src/main/java/com/bhoomibot/os/model/RobotStatus.kt` (for robot state reference)
- `app/src/main/java/com/bhoomibot/os/connection/model/RobotCommand.kt` (for command structure reference)
- `app/src/main/java/com/bhoomibot/os/model/RobotModels.kt` (for existing model patterns)

**New files required:**
- `app/src/main/java/com/bhoomibot/os/model/MissionRecord.kt`
- `app/src/main/java/com/bhoomibot/os/model/CommandRecord.kt`
- `app/src/main/java/com/bhoomibot/os/model/Waypoint.kt`
- `app/src/main/java/com/bhoomibot/os/model/MissionMetadata.kt`

**Dependencies:** None (foundational task)

**Expected input:** Manual operation data (drive commands, speed, states, GPS, timestamps)

**Expected output:** Serialized mission data models that can be persisted and replayed

**Acceptance criteria:**
- [ ] MissionRecord contains mission ID, name, waypoints, command records, and metadata
- [ ] CommandRecord captures DriveCommand, speed percent, PTO/hydraulic states, GPS coordinates, heading, and timestamp
- [ ] Waypoint contains latitude, longitude, timestamp, and accuracy
- [ ] All models are serializable/deserializable via JSON
- [ ] Models contain no Android-specific dependencies
- [ ] Models match the structure described in MISSION_SYSTEM.md and RECORDING_ENGINE.md

**Testing method:**
- Unit tests verifying serialization/deserialization of mission data
- Unit tests validating data constraints (speed ranges, percentage bounds)
- Integration test with sample mission data

## AUT-002: Mission Storage

**Objective:** Implement persistence layer for saving and retrieving missions using Android DataStore.

**Description:** Create a MissionStorage class that handles saving MissionRecord objects to persistent storage and retrieving them. Uses Proto DataStore for type-safe storage.

**Existing files to reuse:**
- `app/src/main/java/com/bhoomibot/os/data/DevicePreferences.kt` (DataStore pattern reference)
- `app/src/main/java/com/bhoomibot/os/data/ConnectionPreferencesStore.kt` (Preferences implementation)
- `app/src/main/java/com/bhoomibot/os/data/ControlCalibrationStore.kt` (Storage pattern reference)
- Models from AUT-001

**New files required:**
- `app/src/main/java/com/bhoomibot/os/data/MissionStorage.kt`
- `app/src/main/java/com/bhoomibot/os/data/MissionSerializers.kt` (TypeConverters for MissionRecord)

**Dependencies:** AUT-001 (Mission Data Models)

**Expected input:** MissionRecord objects to save; mission IDs to retrieve

**Expected output:** Saved missions retrievable by ID; list of available missions

**Acceptance criteria:**
- [ ] MissionStorage provides saveMission(mission: MissionRecord) function
- [ ] MissionStorage provides getMission(id: String): MissionRecord? function
- [ ] MissionStorage provides getAllMissions(): List<MissionMetadata> function
- [ ] MissionStorage handles serialization/deserialization via JSON/Protobuf
- [ ] Storage location follows /autonomy/missions/ directory structure
- [ ] Handles edge cases (corrupted data, missing files, storage full)

**Testing method:**
- Unit tests for save/retrieve operations
- Integration tests with actual DataStore
- Edge case testing (corrupt files, storage limits)
- Concurrency testing for simultaneous read/write

## AUT-003: Manual Recording Engine

**Objective:** Implement the RecordingEngine that captures manual operations during manual control mode.

**Description:** Create a RecordingEngine that observes ManualViewModel state changes and records CommandRecords when manual controls are used. Implements event-based recording with debouncing to prevent excessive storage usage.

**Existing files to reuse:**
- `app/src/main/java/com/bhoomibot/os/feature/manual/ManualViewModel.kt` (StateFlow to observe)
- `app/src/main/java/com/bhoomibot/os/feature/manual/ManualUiState.kt` (State to observe)
- `app/src/main/java/com/bhoomibot/os/feature/manual/ManualControlScreen.kt` (UI integration point)
- Models from AUT-001
- MissionStorage from AUT-002

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/RecordingEngine.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/RecordingState.kt` (if needed for internal state)

**Dependencies:** AUT-001, AUT-002

**Expected input:** ManualViewModel state changes (drive commands, speed changes, PTO/hydraulic toggles, GPS updates)

**Expected output:** Recorded CommandRecords stored in MissionRecord via MissionStorage

**Acceptance criteria:**
- [ ] RecordingEngine observes ManualViewModel.uiState StateFlow
- [ ] Records CommandRecord on state changes (drive command, speed, PTO, hydraulic, lights, horn)
- [ ] Implements minimum interval (50ms) between recordings to prevent storage bloat
- [ ] Captures GPS coordinates when available via DevicePreferences
- [ ] Captures heading from device orientation when available
- [ ] Associates each CommandRecord with accurate timestamp
- [ ] Provides startRecording(), stopRecording(), and isRecording() functions
- [ ] Automatically saves mission when stopped via MissionStorage
- [ ] Handles permission checks for GPS access

**Testing method:**
- Unit tests verifying recording triggers on state changes
- Unit tests verifying 50ms debouncing behavior
- Integration tests with ManualViewModel mock
- Permission handling tests
- End-to-end test: record manual operation → save → verify mission data

## AUT-004: Mission Library

**Objective:** Implement mission listing and management interface for selecting saved missions.

**Description:** Create a MissionRepository or MissionManager that provides interface for browsing, loading, and managing saved missions. Includes metadata display and mission validation.

**Existing files to reuse:**
- MissionStorage from AUT-002
- Models from AUT-001
- Existing settings/preferences patterns

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/MissionRepository.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/MissionListItem.kt` (UI model for mission listing)

**Dependencies:** AUT-001, AUT-002

**Expected input:** User requests to list, load, or delete missions

**Expected output:** Lists of missions; loaded missions; validated mission data

**Acceptance criteria:**
- [ ] Provides getAvailableMissions(): List<MissionMetadata> function
- [ ] Provides loadMission(id: String): MissionResult function with validation
- [ ] Provides deleteMission(id: String): Boolean function
- [ ] Validates mission integrity on load (command sequences, data consistency)
- [ ] Returns descriptive error messages for corrupted/invalid missions
- [ ] Caches mission metadata for performance
- [ ] Handles empty mission library gracefully

**Testing method:**
- Unit tests for mission listing, loading, deletion
- Integration tests with actual MissionStorage
- Error case testing (corrupt JSON, missing files)
- Performance testing with large mission lists

## AUT-005: Mission Playback Engine

**Objective:** Implement the PlaybackEngine that executes recorded missions with proper timing.

**Description:** Create a PlaybackEngine that reads CommandRecords from a MissionRecord and executes them via RobotRepository with accurate timing intervals between commands. Handles pause/resume/stop functionality.

**Existing files to reuse:**
- Models from AUT-001
- RobotRepository interface and implementations
- MissionStorage from AUT-002
- Existing command pipeline (VcuRobotRepository, ConnectionManager)

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/PlaybackEngine.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/PlaybackState.kt`

**Dependencies:** AUT-001, AUT-002, AUT-003 (indirectly via stored missions)

**Expected input:** MissionRecord to play back; playback control commands (start, pause, resume, stop)

**Expected output:** Executed commands via RobotRepository with correct timing

**Acceptance criteria:**
- [ ] Provides loadMission(missionId: String): PlaybackResult function
- [ ] Provides startPlayback(), pausePlayback(), resumePlayback(), stopPlayback() functions
- [ ] Executes CommandRecord sequence with accurate timing delays between commands
- [ ] Implements proper command execution: sendDriveCommand(), updateSpeed(), setPto(), setHydraulic(), etc.
- [ ] Handles pause/resume by preserving execution position and recalculating delays
- [ ] Implements stop functionality that sends STOP command and resets position
- [ ] Provides playback state (IDLE, LOADED, PLAYING, PAUSED, STOPPED, ERROR)
- [ ] Handles edge cases (empty mission, corrupted data, playback errors)
- [ ] Implements error recovery strategies as outlined in PLAYBACK_ENGINE.md

**Testing method:**
- Unit tests for command execution sequencing
- Unit tests for timing accuracy between commands
- Integration tests with mocked RobotRepository
- Pause/resume/stop functionality tests
- Error scenario testing (invalid commands, playback interruptions)

## AUT-006: Waypoint Tracking

**Objective:** Implement waypoint following functionality for path-based navigation during playback.

**Description:** Create a WaypointTracker that uses GPS coordinates from MissionRecord.waypoints to guide the robot along a path by adjusting speed and heading based on distance to next waypoint.

**Existing files to reuse:**
- DevicePreferences.kt (for current GPS position)
- ControlCalibrationStore.kt (for speed calibration values)
- RobotRepository (for sending speed/drive commands)
- Models from AUT-001 (Waypoint)

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/WaypointTracker.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/WaypointFollowerConfig.kt` (configuration parameters)

**Dependencies:** AUT-001, AUT-005 (WaypointTracker works with PlaybackEngine)

**Expected input:** Current GPS position; target waypoint list from MissionRecord

**Expected output:** Speed and heading adjustments sent to RobotRepository

**Acceptance criteria:**
- [ ] Calculates distance to next waypoint using Haversine formula
- [ ] Implements waypoint completion detection (within 5m threshold)
- [ ] Adjusts speed based on distance to waypoint (per NAVIGATION.md logic)
- [ ] Uses existing ControlCalibrationStore for speed limits
- [ ] Sends appropriate DriveCommand (FORWARD, LEFT, RIGHT, etc.) based on bearing to waypoint
- [ ] Sends speed commands via updateSpeed() based on distance calculations
- [ ] Handles edge cases (no GPS signal, waypoint list empty, final waypoint reached)
- [ ] Provides current waypoint index and progress percentage
- [ ] Integrates with PlaybackEngine to provide navigation commands during playback

**Testing method:**
- Unit tests for distance calculation (Haversine formula)
- Unit tests for waypoint completion detection
- Integration tests with mocked DevicePreferences and RobotRepository
- Path following simulation tests
- Edge case testing (GPS loss, empty waypoints)

## AUT-007: Autonomy State Machine

**Objective:** Implement the state machine that manages autonomy lifecycle states.

**Description:** Create a StateMachine that manages the autonomy workflow: IDLE → RECORDING → MISSION_SAVED → READY → EXECUTING → PAUSED/COMPLETED/ERROR states with proper transitions.

**Existing files to reuse:**
- STATE_MACHINE.md documentation for state definitions
- Existing ViewModel patterns for state management
- Kotlin StateFlow or similar reactive patterns

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomyStateMachine.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomyState.kt` (enum class for states)

**Dependencies:** AUT-003, AUT-002, AUT-005 (interacts with recording engine, storage, playback engine)

**Expected input:** User actions (start recording, stop recording, load mission, start playback, pause, resume, stop)

**Expected output:** State transitions; notifications to UI and other components

**Acceptance criteria:**
- [ ] Implements all states from STATE_MACHINE.md: IDLE, RECORDING, MISSION_SAVED, READY, EXECUTING, PAUSED, COMPLETED, ERROR, EMERGENCY_STOP
- [ ] Implements all valid state transitions per state diagram
- [ ] Provides currentState() method for observers
- [ ] Handles invalid transition attempts gracefully
- [ ] Integrates with RecordingEngine (start/stop recording transitions)
- [ ] Integrates with MissionStorage (save/load operations)
- [ ] Integrates with PlaybackEngine (play/pause/stop operations)
- [ ] Handles emergency stop transition from any state
- [ ] Provides state change callbacks for UI updates

**Testing method:**
- Unit tests for all state transitions
- Integration tests with RecordingEngine and PlaybackEngine mocks
- Invalid transition handling tests
- Emergency stop functionality tests
- State persistence testing (if applicable)

## AUT-008: Autonomy Controller

**Objective:** Implement the main coordinator that ties together all autonomy components.

**Description:** Create an AutonomyController that acts as the main interface between UI components and the autonomy subsystem, coordinating the state machine, recording engine, playback engine, and mission storage.

**Existing files to reuse:**
- ViewModel patterns from existing features
- All previously developed autonomy components (AUT-001 through AUT-007)

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomyViewModel.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomyUiState.kt`

**Dependencies:** All previous AUT tasks (001-007)

**Expected input:** UI actions from autonomy-related screens; system events (GPS changes, connection status)

**Expected output:** Coordinated autonomy functionality; UI state updates

**Acceptance criteria:**
- [ ] Provides init() and cleanup() methods for lifecycle management
- [ ] Coordinates RecordingEngine for manual recording operations
- [ ] Coordinates MissionStorage for save/load operations
- [ ] Coordinates PlaybackEngine for mission playback operations
- [ ] Coordinates WaypointTracker for navigation during playback
- [ ] Manages AutonomyStateMachine for state transitions
- [ ] Exposes UI state via StateFlow or LiveData for Compose consumption
- [ ] Handles permission requests and validation
- [ ] Provides methods for all UI actions: startRecording, stopRecording, loadMission, startPlayback, pausePlayback, resumePlayback, stopPlayback
- [ ] Reports errors and status updates to UI layer

**Testing method:**
- Unit tests for each coordination function
- Integration tests with all subsystem mocks
- Lifecycle testing (creation, active use, destruction)
- Error propagation testing
- UI state update verification

## AUT-009: Safety Integration

**Objective:** Implement safety mechanisms and emergency handling for autonomous operations.

**Description:** Add safety overlays and emergency stop functionality that can interrupt autonomous operations at any time, ensuring safe operation of the robot.

**Existing files to reuse:**
- Emergency stop handling patterns from manual mode
- Existing safety indicators in UI components
- RobotRepository emergency stop capabilities
- Connection monitoring patterns

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/SafetyMonitor.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/EmergencyHandler.kt`

**Dependencies:** AUT-005, AUT-008 (monitoring playback and autonomy controller)

**Expected input:** Emergency stop commands; safety sensor inputs (if available); connection loss events

**Expected output:** Immediate cessation of autonomous operation; emergency stop commands to robot; user notifications

**Acceptance criteria:**
- [ ] Provides emergency stop functionality accessible from all autonomy states
- [ ] Immediately stops playback and sends EMERGENCY_STOP command via RobotRepository
- [ ] Resets autonomy state machine to EMERGENCY_STOP state regardless of current state
- [ ] Requires explicit user action to recover from emergency stop
- [ ] Monitors connection status and initiates safe stop on disconnect
- [ ] Provides safety status indicators to UI
- [ ] Implements timeout-based safety checks for stalled operations
- [ ] Logs safety events for post-operation review
- [ ] Complies with safety requirements outlined in PLAYBACK_ENGINE.md error handling

**Testing method:**
- Unit tests for emergency stop triggering
- Integration tests with mocked RobotRepository verifying EMERGENCY_STOP command
- Connection loss simulation tests
- State transition validation during emergency situations
- Recovery process testing after emergency stop

## AUT-010: Autonomy UI Integration

**Objective:** Integrate autonomy features into the existing BhoomiBot UI framework.

**Description:** Create or modify UI screens to expose autonomy functionality to users, following the existing Material Design 3 patterns used in the application.

**Existing files to reuse:**
- Existing feature screen patterns (OperatorHomeScreen.kt, RobotHomeScreen.kt, etc.)
- OperationalScreen.kt base component
- Navigation patterns from AppNavigation.kt
- Existing UI component libraries

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomousScreen.kt` (enhance existing placeholder)
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/MissionLibraryScreen.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/RecordingScreen.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/PlaybackScreen.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/AutonomySettingsScreen.kt`

**Dependencies:** AUT-008 (AutonomyViewModel for UI state)

**Expected input:** User interactions with autonomy UI components

**Expected output:** Visual feedback of autonomy state; controls for autonomy operations

**Acceptance criteria:**
- [ ] AutonomousScreen shows current autonomy state and primary controls
- [ ] MissionLibraryScreen displays list of available missions with metadata
- [ ] RecordingScreen provides recording controls and status feedback
- [ ] PlaybackScreen provides playback controls, progress indication, and navigation data
- [ ] All screens follow existing Material Design 3 patterns
- [ ] Navigation integrates with existing AppNavigation.kt
- [ ] UI properly responds to AutonomyViewModel state changes
- [ ] Error states are clearly communicated to user
- [ ] Loading states are shown during async operations
- [ ] Emergency stop is accessible from all autonomy screens
- [ ] Screens respect device orientation and accessibility guidelines

**Testing method:**
- UI component unit tests
- Integration tests with AutonomyViewModel mocks
- Navigation flow testing
- State change response testing
- Accessibility testing (basic)
- Screen orientation handling tests

## AUT-011: End-to-End Testing

**Objective:** Validate complete autonomy workflow from recording to playback.

... (existing AUT-011 content)

# AI Expansion & Intelligence Tasks

## AI-001: Field Coverage Pattern Generator

**Objective:** Expand a template mission into a full field coverage pattern.

**Description:** Implement an AI path generator that takes a single recorded "pass" (e.g., a straight line) and generates parallel offset passes to cover an entire rectangular field.

**New files required:**
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/ai/PathGenerator.kt`
- `app/src/main/java/com/bhoomibot/os/feature/autonomous/ai/CoveragePattern.kt`

**Acceptance criteria:**
- [ ] Accepts a `MissionRecord` as a template
- [ ] Allows configuration of "Implement Width" (offset distance)
- [ ] Allows configuration of "Number of Passes" or "Field Width"
- [ ] Generates a new `MissionRecord` containing the expanded path (S-pattern/Zig-zag)
- [ ] Maintains recorded settings (PTO speed, vehicle speed) for generated passes

## AI-002: Remote Assistance System (Ring Operator)

**Objective:** Notify the operator and request a decision when a problem is encountered.

**Description:** Implement a "Call for Help" system. When the robot enters an `ERROR` state during autonomous execution, it sends a high-priority alert to the operator's phone.

**Acceptance criteria:**
- [ ] Robot sends an `ASSISTANCE_REQUIRED` LiveLink message on error
- [ ] Operator app plays a distinct "Ring" sound when alert is received
- [ ] Operator UI shows a high-priority decision dialog (Retry / Manual Takeover / E-Stop)
- [ ] Logs the specific problem (GPS loss, Obstacle, low battery) to the operator

## AI-003: Smart Obstacle Handling

**Objective:** Use available telemetry to detect stalls or blockages.

**Description:** Enhance the `SafetyMonitor` to detect when the robot is commanded to move but GPS position isn't changing (likely stuck or blocked), triggering the Remote Assistance System.

**Acceptance criteria:**
- [ ] Detects "Stall" state: Speed > 0 but GPS displacement < threshold over 3 seconds
- [ ] Automatically pauses playback and transitions to `ERROR` state
- [ ] Triggers AI-002 assistance request
