'use strict';

/**
 * BhoomiBot live-link relay.
 *
 * Two phones (a ROBOT-mounted phone and an OPERATOR phone) connect here over the
 * internet. The server only forwards messages inside a *session* keyed by
 * `robotId :: sessionCode`, so it never has to understand the video payload.
 *
 * Protocol (see Android Communication Master: Wi-Fi + WebSocket + JSON):
 *   - First TEXT frame from a client must be a HELLO envelope:
 *       { type:"HELLO", robotId:"R1", ts:..., payload:"{\"role\":\"ROBOT\",\"session\":\"S1\"}" }
 *   - Subsequent TEXT frames (TELEMETRY / COMMAND / ACK / ERROR) and BINARY
 *     frames (VIDEO_FRAME, jpeg bytes) are relayed to the *other* peer(s)
 *     in the same session.
 *   - On join/leave the server broadcasts a PEER_STATUS envelope so each side
 *     knows whether its counterpart is online.
 *
 * No auth beyond the shared session code — fine for a field demo. Put this behind
 * a real auth proxy before any production/hosted deployment.
 */

const http = require('http');
const express = require('express');
const { WebSocketServer } = require('ws');

const PORT = Number(process.env.PORT) || 8080;

const app = express();
app.get('/', (_req, res) => res.json({ service: 'bhoomibot-live-relay', status: 'ok' }));
app.get('/health', (_req, res) => res.json({ ok: true, sessions: sessions.size }));

const server = http.createServer(app);
const wss = new WebSocketServer({ server });

/** sessionKey -> Set<ws> */
const sessions = new Map();
let nextId = 1;

const sessionKey = (robotId, session) => `${robotId}::${session}`;

function peerCounts(set) {
  let robot = 0;
  let operator = 0;
  for (const c of set) {
    if (c.meta && c.meta.role === 'ROBOT') robot++;
    else if (c.meta && c.meta.role === 'OPERATOR') operator++;
  }
  return { robot, operator };
}

function broadcastPeerStatus(set) {
  if (!set) return;
  const counts = peerCounts(set);
  const msg = JSON.stringify({
    type: 'PEER_STATUS',
    robotId: '',
    ts: Date.now(),
    payload: JSON.stringify({ robot: counts.robot > 0, operator: counts.operator > 0 }),
    ack: false,
    code: 0,
    retry: 0
  });
  for (const c of set) {
    if (c.readyState === c.OPEN) c.send(msg);
  }
}

wss.on('connection', (ws) => {
  ws.id = nextId++;
  ws.meta = null;     // filled in once HELLO arrives
  ws.isAlive = true;
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', (data, isBinary) => {
    // The very first text frame must be the HELLO handshake.
    if (!ws.meta && !isBinary) {
      let hello = null;
      try { hello = JSON.parse(data.toString()); } catch (_e) { /* fallthrough to drop */ }
      if (hello && hello.type === 'HELLO') {
        let role = 'OPERATOR';
        let session = '';
        try {
          const p = hello.payload ? JSON.parse(hello.payload) : {};
          role = p.role === 'ROBOT' ? 'ROBOT' : 'OPERATOR';
          session = p.session || '';
        } catch (_e) { /* defaults above */ }
        const robotId = (hello.robotId || 'default').toString();
        const key = sessionKey(robotId, session);
        ws.meta = { robotId, session, role, key };
        if (!sessions.has(key)) sessions.set(key, new Set());
        sessions.get(key).add(ws);
        broadcastPeerStatus(sessions.get(key));
        return;
      }
      // Not a valid hello: refuse the connection.
      ws.close(1008, 'HELLO handshake required');
      return;
    }

    // After hello: relay to every *other* peer in the same session.
    const set = ws.meta ? sessions.get(ws.meta.key) : null;
    if (!set) return;
    for (const c of set) {
      if (c !== ws && c.readyState === c.OPEN) {
        if (isBinary) c.send(data);
        else c.send(data.toString());
      }
    }
  });

  ws.on('close', () => {
    if (ws.meta && sessions.has(ws.meta.key)) {
      const set = sessions.get(ws.meta.key);
      set.delete(ws);
      if (set.size === 0) sessions.delete(ws.meta.key);
      else broadcastPeerStatus(set);
    }
  });

  ws.on('error', () => { /* ignore; 'close' handles cleanup */ });
});

// Heartbeat: drop sockets that stop answering pings (covers dead mobile links).
const heartbeat = setInterval(() => {
  for (const ws of wss.clients) {
    if (ws.isAlive === false) { ws.terminate(); continue; }
    ws.isAlive = false;
    try { ws.ping(); } catch (_e) { /* will be terminated next tick */ }
  }
}, 30000);
wss.on('close', () => clearInterval(heartbeat));

server.listen(PORT, () => {
  console.log(`BhoomiBot live relay listening on :${PORT}`);
  console.log(`Open the app, set the server URL to ws://<this-host>:${PORT}, and share a Robot ID + session code between the two phones.`);
});
