# Mission Planner Documentation

## Overview
The Mission Planner is an operator-side planning interface located in the navigation system. It allows the operator to configure and schedule field operations for the robot before deployment.

## Current Implementation

### MissionPlannerScreen.kt
- **Package**: `com.bhoomibot.os.navigation`
- **Status**: Active implementation with mock data
- **Features**:
  - Farm/Region selection (Field A, Field B, Orchard North)
  - Robot selection (BhoomiBot-01, BhoomiBot-02)
  - Mission type selection (Spraying, Seeding, Weeding, Mapping)
  - Attachment selection (Sprayer, Seeder, Weeder, None)
  - Speed control (0-100%)
  - Estimated time calculation
  - Start/Pause/Resume/Cancel controls

## Mock Data Implementation

### Selection Catalogs (in MissionPlannerScreen.kt)
```kotlin
val farms = listOf("Field A", "Field B", "Orchard North")
val robots = listOf("Bhoomibot-01", "Bhoomibot-02")
val missions = listOf("Spraying", "Seeding", "Weeding", "Mapping")
val attachments = listOf("Sprayer", "Seeder", "Weeder", "None")
```

### State Management
```kotlin
var selectedFarm by remember { mutableStateOf(farms.first()) }
var selectedRobot by remember { mutableStateOf(robots.first()) }
var selectedMission by remember { mutableStateOf(missions.first()) }
var selectedAttachment by remember { mutableStateOf(attachments.first()) }
var speed by remember { mutableIntStateOf(50) }
var plannerState by remember { mutableStateOf(PlannerState.IDLE) }
```

## Technical Details

### Estimated Time Calculation
```kotlin
val eta = if (speed <= 0) "—" else run {
    val minutes = (120 * 50) / speed  // Base: 120 min at 50% speed
    val h = minutes / 60
    val m = minutes % 60
    if (h > 0) "$h h ${m}m" else "${m}m"
}
```

### Planner States
```kotlin
enum class PlannerState(val label: String, val color: Color) {
    IDLE("Not started", MutedText),
    RUNNING("Running", SignalGreen),
    PAUSED("Paused", WarningAmber)
}
```

## UI Structure

### Main Layout
1. **Top Bar**: Back navigation + title + "OPERATOR EXRAS" subtitle
2. **Selection Groups**: Farm, Robot, Mission, Attachment
3. **Speed Control Card**: Slider with percentage display
4. **ETA Card**: Estimated time + status side-by-side
5. **Progress Bar**: Visualizes planner state
6. **Transport Controls**: Start/Pause/Resume/Cancel depending on state

### Control Mapping
- **IDLE State**: Only "Start" button available
- **RUNNING State**: "Pause" + "Cancel" buttons
- **PAUSED State**: "Resume" + "Cancel" buttons

## Integration Points

### Navigation
- **Location**: `navigation/MissionPlannerScreen.kt`
- **NavGraph**: Added to AppNavigation routes
- **Context**: Operator-only functionality

### Current Limitations
1. **No Real Backend**: All data is local (remember scope)
2. **Mock Implementation**: No actual mission service integration
3. **No Robot Delegation**: Robot execution is currently not integrated
4. **No Validation**: No runtime validation of mission compatibility

### Future Requirements
1. **Mission Service Integration**:
   - Add `MissionRepository` interface
   - Implement real mission planning logic
   - Connect to robot control systems

2. **Data Persistence**:
   - Save/load mission plans
   - Mission history tracking
   - Cloud sync support

3. **Advanced Features**:
   - Obstacle avoidance integration
   - Weather-aware route optimization
   - Multi-mission batching
   - Resource monitoring

## Dependencies
- Android Compose UI components
- Material3 design system
- Navigation Component integration
- Theme system (MutedText, SignalGreen, etc.)

## Technical Patterns
1. **Operator Extras**: Located alongside Notifications as operator-specific features
2. **Mock-First Development**: Test-driven implementation with planned backend integration
3. **State-Managed UI**: Composable state management using AndroidX Compose primitives

## Testing Considerations
1. State transitions validation (IDLE → RUNNING → PAUSED → IDLE)
2. ETA calculation edge cases (0% speed, fractional minutes)
3. UI responsiveness during state transitions
4. Integration testing with actual mission service

## Future Roadmap
1. **Phase 1**: Integration with real mission planning service
2. **Phase 2**: Automated robot mission execution
3. **Phase 3**: Real-time monitoring and adjustment
4. **Phase 4**: Multi-robot coordination

## Key Differentiators
- Real-time ETA calculation with speed dependency
- Visual state management with color-coded status
- Mock-first approach enabling rapid UI development
- Clean navigation integration for operator-specific features

Note: This planner is currently a standalone operator tool awaiting integration with the actual mission planning backend and robot control systems.