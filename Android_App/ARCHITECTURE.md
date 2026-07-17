# BhoomiBot OS — Structure-Wise Architecture Guide

> A readable map of the Android app for engineers who are new to the codebase.
> Goal: after reading this you should know **where things live, what each layer does, and how data flows** — without reading every file.

---

## 1. What this app is

**BhoomiBot OS** is a single-Activity **Jetpack Compose** Android app (package `com.bhoomibot.os`) that controls an agricultural field robot. The same APK is installed on **two kinds of phones**:

| Role | What it is | Home screen |
|------|-----------|-------------|
| `OPERATOR` | Handheld phone used by the person driving/monitoring the robot | `OperatorHomeScreen` |
| `ROBOT` | Phone mounted on the robot; acts as the on-board computer | `RobotHomeScreen` |

The role is chosen on the **Onboarding** screen (shown on every launch) and persisted in `DevicePreferences`. The app then routes to the matching home.

**Tech used:** Kotlin 2.2, Compose BOM 2026.02, Material 3, OkHttp (WebSocket), Jetpack DataStore, CameraX, Navigation Compose.

---

## 2. The 30-second mental model

```
                 ┌─────────────────────────────────────────────┐
                 │                 MainActivity                  │  single Activity
                 │   (applies language + theme, hosts NavHost)   │
                 └───────────────────────┬─────────────────────┘
                                         │
                                 ┌───────▼────────┐
                                 │ AppNavigation  │  AppRoute = all routes
                                 └───┬─────────┬───┘
                      ┌──────────────┘         └───────────────┐
                OperatorHome                          RobotHome
                      │                                      │
        Manual / Live / Mission / Camera /      Go Live / AI / Diagnostics /
        Map / Diagnostics / Settings ...         Logs / Developer / Maintenance
```

Every **screen** follows one pattern:

```
Screen.kt      → @Composable         (pure UI; reads state, fires actions)
ScreenVm.kt    → *ViewModel          (AndroidViewModel; holds UiState in a StateFlow)
ScreenState.kt → *UiState            (immutable data class — the single source of truth for the screen)
```

---

## 3. Folder-by-folder layout

All source lives under `app/src/main/java/com/bhoomibot/os/`.

```
os/
├── MainActivity.kt            ← the only Activity; language + theming + nav host
├── navigation/
│   ├── AppNavigation.kt       ← AppRoute (all routes) + NavHost graph  ★ start here
│   ├── MissionPlannerScreen.kt ← operator extra (mock mission builder)
│   └── NotificationsScreen.kt  ← operator extra (mock notification feed)
│
├── feature/                   ← one package per screen (the UI)
│   ├── onboarding/            ← role picker (first launch)
│   ├── operator/              ← OperatorHome (status + action grid)
│   ├── robot/                 ← RobotHome (system vitals) + RobotSection (generic detail)
│   ├── dashboard/             ← legacy operator home (still reachable)
│   ├── manual/                ← ★ Manual drive controls (buttons/joystick/PTO/hydraulic)
│   ├── live/                  ← ★ Internet live link UI (operator + robot)
│   ├── connection/            ← relay link setup (server URL / IDs / quality)
│   ├── settings/              ← control calibration + VCU connection settings
│   ├── camera/                ← CameraX preview composable + placeholder screen
│   ├── common/                ← OperationalScreen (shared "Module ready" placeholder)
│   └── diagnostics/ map/ autonomous/  ← placeholder screens → OperationalScreen
│
├── connection/                ← ★ INTERNET LIVE LINK (relay / WebSocket)
│   ├── transport/             ← LiveLinkClient (interface), WebSocketLiveLinkClient (OkHttp),
│   │                            │  LiveConnectionState (state machine), FrameDecoder (jpeg→bitmap)
│   ├── protocol/              ← LiveEnvelopeSerializer + LivePayloads (org.json codecs)
│   ├── repository/            ← LiveLinkRepository (interface) + Impl
│   ├── model/                 ← envelope/frame/message-type/peer/command/telemetry data classes
│   └── LiveLinkRepositoryProvider.kt  ← single decision point for the repo impl
│
├── vcu/                       ← LOCAL VCU / ESP32 link (Bluetooth / Wi-Fi)
│   ├── ConnectionManager.kt   ← owns the actual BT/WiFi socket
│   ├── VcuProtocol.kt         ← raw serial command tokens (F/B/L/R/S/E, SPDxx, …)
│   ├── ConnectionType.kt      ← BLUETOOTH / WIFI_HOTSPOT / AUTO
│   └── ConnectionPreferences.kt ← connection prefs data class + DataStore keys
│
├── repository/                ← RobotRepository boundary (the "robot" the UI talks to)
│   ├── RobotRepository.kt     ← interface
│   ├── RobotRepositoryProvider.kt  ← ★ USE_REAL_TRANSPORT flag (fake vs real)
│   └── VcuRobotRepository.kt  ← real impl driving the ESP32
│
├── data/                      ← persistence (Jetpack DataStore)
│   ├── DevicePreferences.kt         ← role / theme / language
│   ├── ConnectionPreferencesStore.kt← VCU BT/WiFi prefs
│   ├── LiveLinkPreferencesStore.kt  ← relay URL / IDs / quality
│   ├── ControlCalibrationStore.kt   ← in-memory calibration (Settings writes, Manual reads)
│   └── LocalRobotRepository.kt      ← no-op FAKE robot (default transport)
│
├── model/                     ← domain models (no Android deps)
│   ├── RobotModels.kt         ← DriveCommand + RobotStatus
│   ├── DeviceRole.kt          ← OPERATOR / ROBOT
│   ├── ThemeMode.kt           ← DARK / LIGHT
│   ├── ControlCalibration.kt  ← manual control increments
│   └── MockRobotData.kt       ← static placeholder status (swap for a real repo later)
│
├── viewmodel/
│   └── RobotViewModels.kt     ← DashboardViewModel (reads robot status)
│
└── ui/theme/                  ← Color.kt, Theme.kt, Type.kt (industrial dark theme)
```

---

## 4. The two "connection worlds" (read this twice)

This is the single most confusing part of the codebase. There are **two completely separate ways the phones talk to the robot**, and they must not be confused.

### World A — Local VCU / ESP32 link (on the field)
- **Purpose:** actually drive the robot (move, stop, PTO, lights).
- **Hidden behind:** `RobotRepository` interface → UI calls `sendDriveCommand(...)` and never knows the transport.
- **Default impl:** `LocalRobotRepository` — a **no-op fake**. Every command does nothing; `status()` returns defaults. So the app runs with **no real robot paired**.
- **Real impl:** `VcuRobotRepository` sends raw serial tokens over Bluetooth/Wi-Fi via `ConnectionManager`. Driven by the `USE_REAL_TRANSPORT` flag (see §6).
- **Config:** `ConnectionPreferencesStore` + the `ConnectionSettingsScreen` (BT MAC, Wi-Fi host/port, mode), with a "Test connection" probe.

### World B — Internet live link (relay / WebSocket)
- **Purpose:** operator **watches** the robot's camera + telemetry over the internet.
- **Hidden behind:** `LiveLinkRepository` interface → `LiveLinkRepositoryImpl` over `WebSocketLiveLinkClient` (OkHttp).
- **Wire format (fixed by external "Communication Master" spec):** JSON envelope
  `{ type, robotId, ts, payload, ack, code, retry }`.
  - `type` ∈ HELLO / TELEMETRY / COMMAND / PEER_STATUS / PING / PONG / ERROR / VIDEO_FRAME
  - `payload` = JSON string for control/telemetry/peer-status.
- **Video frames** travel as **raw binary jpeg bytes** on the same socket, **outside** the JSON envelope.
- **"Meeting" condition:** both phones must use the **same relay URL + Robot ID + session code**.
- **Config:** `LiveLinkPreferencesStore` + `ConnectionOptionsScreen` (server URL, IDs, video quality/fps, network mode).
- **State machine:** `IDLE → CONNECTING → CONNECTED → RECONNECTING → ERROR`, with capped exponential backoff (1s,2s,4s… capped at 30s) when `autoReconnect` is on.

> 💡 Mental rule: **World A = make the robot move. World B = see the robot.** Different repos, different stores, different settings screens.

---

## 5. Data flow examples

### Operator sends a drive command (World A — real transport)
```
OperatorLiveScreen.DriveControls
   → OperatorLiveViewModel.sendDrive(DriveCommand.FORWARD)
   → LiveLinkRepository.sendCommand(RobotCommand(...))     [World B! operator→robot]
   → WebSocketLiveLinkClient.send(envelope)  ──WebSocket──▶  ROBOT phone
```
On the robot phone, the inbound COMMAND is surfaced in `RobotLiveViewModel.lastCommand` (which a real VCU bridge would forward to World A).

### Robot broadcasts its camera (World B)
```
RobotLiveScreen (CameraX ImageAnalysis)
   → throttle to fps → resize + compress jpeg
   → RobotLiveViewModel.publishFrame(jpeg)
   → LiveLinkRepository.publishFrame(jpeg)
   → WebSocketLiveLinkClient.sendFrame(jpeg)  ──WebSocket(binary)──▶  OPERATOR phone
   → OperatorLiveViewModel decodes jpeg → ImageBitmap → shown in OperatorLiveScreen
```

### Operator changes manual calibration
```
SettingsScreen sliders
   → SettingsViewModel.setDriveStep(...)
   → ControlCalibrationStore.update { ... }     (in-memory StateFlow)
   → ManualViewModel reads ControlCalibrationStore.calibration on each drive tap
```

---

## 6. The flag you must respect: `USE_REAL_TRANSPORT`

`repository/RobotRepositoryProvider.kt`:
```kotlin
const val USE_REAL_TRANSPORT = false
fun provideRobotRepository(application) =
    if (USE_REAL_TRANSPORT) VcuRobotRepository(...) else LocalRobotRepository()
```

- `false` (default): app uses the **safe no-op fake**. Every command is ignored; no crash if no robot is paired.
- `true`: app uses the **real** `VcuRobotRepository` (Bluetooth/Wi-Fi through `ConnectionManager`).

⚠️ **Do NOT flip this to `true` unless the ESP32 firmware parses the raw serial tokens in `vcu/VcuProtocol.kt`.** The current firmware (`VCU_till_fixed_and_var_code_with_bluetooth.ino`) speaks the Dabble BLE gamepad protocol, so a raw-serial transport would not be understood. Update the firmware and `VcuProtocol` tokens **together**.

---

## 7. Persistence (three DataStores + one in-memory store)

All use Jetpack **DataStore** (coroutine-first, not SharedPreferences):

| Store | Holds | Written by | Read by |
|-------|-------|-----------|---------|
| `DevicePreferences` | role, theme, language | Onboarding / Settings / MainActivity | Navigation, theme |
| `ConnectionPreferencesStore` | VCU BT MAC, Wi-Fi host/port, mode, timeouts | `ConnectionSettingsViewModel` | `VcuRobotRepository`, `ConnectionSettingsScreen` |
| `LiveLinkPreferencesStore` | relay URL, Robot ID, session code, quality/fps | `ConnectionOptionsViewModel` | `Operator/RobotLiveViewModel` |
| `ControlCalibrationStore` | drive step, max speed, PTO/hydraulic steps | `SettingsViewModel` | `ManualViewModel` |

`ControlCalibrationStore` is **in-memory only** (not persisted yet) — values reset on app restart.

---

## 8. Theme

`ui/theme/` — industrial **dark theme by default** (high contrast for sunlight), light optional (chosen in Settings, applied via `MainActivity` → `BhoomibotTheme(darkTheme)`).
- `Color.kt` — custom palette: `SignalGreen` (ON/active), `SafetyRed` (E-STOP/error), `MutedText` (labels), `SteelBlue` (secondary), `WarningAmber`, `Hmi*` surfaces/background.
- `Theme.kt` — maps Material 3 roles to the custom colors; exposes `BhoomiBotTheme`.
- `Type.kt` — only `bodyLarge` customized; rest are M3 defaults.

---

## 9. Gotchas a junior engineer will hit

1. **ViewModel constructor.** `*ViewModel` extends `AndroidViewModel` and takes **only `Application`**. The repository is a plain `private val` field, **not** a default constructor param — because `viewModel()` uses `AndroidViewModelFactory`, which only finds a `(Application)` constructor; a second defaulted param makes the screen crash with `NoSuchMethodException`. (See `ManualViewModel` / `OperatorLiveViewModel`.)
2. **Two connection worlds** — never merge them (§4).
3. **`USE_REAL_TRANSPORT`** — keep `false` until firmware is ready (§6).
4. **CameraX is guarded with `Throwable`** (not just `Exception`) in `BackCameraPreview.kt`, because camera failures can surface as `Error`s that would otherwise crash the whole app. Keep that guard.
5. **Reserved composables** — in `ManualControlScreen.kt`, `PrimaryLightsControl`, `CompactStatus`, and `ManualBottomBar` exist but are **not wired into the layout yet** (future use).
6. **Placeholder screens** — `CameraScreen`, `DiagnosticsScreen`, `MapScreen`, `AutonomousScreen`, `RobotSectionScreen` all render the shared `OperationalScreen` "Module ready" shell until real data sources are integrated.

---

## 10. Where to start reading

1. `navigation/AppNavigation.kt` — see every screen and route in one place.
2. `feature/operator/OperatorHomeScreen.kt` or `feature/robot/RobotHomeScreen.kt` — see the home pattern.
3. `feature/manual/ManualControlScreen.kt` + `ManualViewModel.kt` + `ManualUiState.kt` — the full Composable+ViewModel+UiState trio.
4. `connection/` — the relay link (the most architecturally interesting code).
5. `repository/RobotRepositoryProvider.kt` — the fake/real transport switch.

---

*Generated as a structural onboarding guide. Comments were added throughout the source for line-level clarity; this document explains the "why" and the big picture.*
