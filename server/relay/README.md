# BhoomiBot Live-Link Relay

A tiny **WebSocket relay** that links a **robot phone** and an **operator phone** over
the internet, so the operator can watch the robot's **live camera feed** and telemetry
and send drive commands back.

It is deliberately dumb: it only forwards messages inside a *session* (keyed by
`robotId + sessionCode`) and never parses the video payload. This matches the
project's **Communication Master** (Wi-Fi + WebSocket + JSON) and is the seed of
the future web dashboard backend.

## How it fits together

```
Robot phone (role=ROBOT)                Operator phone (role=OPERATOR)
  CameraX frames + telemetry  ──WS──►  Relay server  ──WS──►  Live view + overlay
  ◄── COMMAND (drive/estop) ─────────◄────────────────────  send controls
            same session (robotId + sessionCode)
```

- The **robot** joins with `role=ROBOT` and publishes `VIDEO_FRAME` (binary jpeg)
  + `TELEMETRY` (JSON).
- The **operator** joins with `role=OPERATOR`, subscribes to the same session, and
  receives the stream + telemetry, then sends `COMMAND` back.
- The relay broadcasts a `PEER_STATUS` envelope whenever a peer joins/leaves, so each
  side shows whether its counterpart is online.

## Run locally

```bash
cd server/relay
npm install
npm start            # listens on :8080 by default
```

Override the port: `PORT=3000 npm start`.

Health check: open `http://localhost:8080/health` → `{"ok":true,...}`.

## Point the app at it

In the Android app → **Live Link** (Connection Options):

- **Server URL:** `ws://<host>:8080`
  - Same machine as the emulator? use `ws://10.0.2.2:8080`
    (that's the emulator's alias for your dev PC).
  - Two real phones on the same Wi-Fi? use `ws://<your-pc-lan-ip>:8080`.
  - Across the internet? deploy below, then use the hosted `wss://...` URL.
- **Robot ID** + **Session code**: type the **same** values on both phones.
- **Role:** `ROBOT` on the robot phone, `OPERATOR` on the handheld.

> The app is configured to allow cleartext `ws://` for development. For any hosted
> deployment use `wss://` (TLS) — see below.

## Deploy (free) — so two phones work over the real internet

1. Push this `server/relay` folder to a Git repo.
2. **Render** (render.com): New → Web Service → connect repo → 
   - Build: `npm install`; Start: `npm start`; Add env `PORT` (Render sets it).
   - Use the generated `wss://<app>.onrender.com` URL in the app.
3. Or **Railway** (railway.app): deploy the folder; same `npm start` start command.

After deploy, set the app's **Server URL** to the `wss://` URL and the two phones
will connect from anywhere.

## Protocol notes

First text frame from every client must be the `HELLO` envelope:

```json
{ "type": "HELLO", "robotId": "R1", "ts": 0,
  "payload": "{\"role\":\"ROBOT\",\"session\":\"S1\"}" }
```

Subsequent frames are forwarded to the *other* peer(s) in the session:

| Type          | Direction            | Payload                          |
|---------------|---------------------|----------------------------------|
| `VIDEO_FRAME` | ROBOT → OPERATOR    | binary jpeg bytes                 |
| `TELEMETRY`   | ROBOT → OPERATOR    | JSON `TelemetrySnapshot`          |
| `COMMAND`      | OPERATOR → ROBOT    | JSON `RobotCommand`               |
| `PEER_STATUS`  | server → both        | `{robot:bool, operator:bool}`    |
| `ACK`/`ERROR`  | either → other       | per Communication Master envelope  |

Every JSON message uses the master envelope:
`{type, robotId, ts, payload, ack, code, retry}`.
