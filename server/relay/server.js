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

// === TEMPORARY DEBUG LOGGING — remove before production =========================
// Lets us see, on Render, exactly what connects and what frames flow. Delete this
// whole block (and every dlog(...) call below) once the connection is verified.
// ON by default while we verify the link, but can be switched off from Render's
// environment variables (set RELAY_DEBUG=false) without a redeploy.
const RELAY_DEBUG = (process.env.RELAY_DEBUG ?? 'true') === 'true';
function dlog(...args) {
  if (!RELAY_DEBUG) return;
  console.log('[relay-debug]', new Date().toISOString(), ...args);
}
function clientIp(req) { return req.headers['x-forwarded-for'] || req.socket.remoteAddress; }
function safeType(data) {
  try { const o = JSON.parse(data.toString()); return (o && o.type) || '?'; } catch (_e) { return '?'; }
}
// =================================================================================

// Last line of defence: a single failed write to a dead peer (EPIPE) or any
// other unexpected error must never crash the relay and drop every connected
// phone. Swallow uncaught exceptions/rejections, log them, and keep the process
// (and the other peers) alive. Dead sockets are pruned by the try/catch above.
process.on('uncaughtException', (e) => dlog('UNCAUGHT', e && e.message));
process.on('unhandledRejection', (e) => dlog('UNHANDLED_REJECTION', e && (e && e.message)));

const app = express();
// Log every HTTP request (health checks, root, etc.) with caller IP + User-Agent.
app.use((req, res, next) => {
  dlog('[HTTP REQUEST]', req.method, req.url, 'ip=', clientIp(req), 'ua=', req.headers['user-agent']);
  next();
});
app.get('/', (_req, res) => res.json({ service: 'bhoomibot-live-relay', status: 'ok' }));
app.get('/health', (_req, res) => res.json({ ok: true, sessions: sessions.size }));

const server = http.createServer(app);
const wss = new WebSocketServer({ server });
// Log each WebSocket upgrade request (the moment a client asks to switch protocols).
server.on('upgrade', (req) => {
  dlog('[WS UPGRADE]', req.url, 'ip=', clientIp(req), 'ua=', req.headers['user-agent']);
});

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
    if (c.readyState === c.OPEN) {
      // A peer's TCP connection can die without a clean close (network drop,
      // WiFi<->cellular handoff, or the phone's battery saver killing the
      // socket). readyState may still report OPEN, so the write throws EPIPE.
      // Guard it: a failed send must not crash the relay or drop the other peer.
      try { c.send(msg); }
      catch (e) {
        dlog('PEER_STATUS-SEND-FAIL', 'to=', c.id, 'reason=', e && e.message);
        try { c.terminate(); } catch (_e) { /* already gone */ }
      }
    }
  }
}

wss.on('connection', (ws, req) => {
  ws.id = nextId++;
  ws.meta = null;     // filled in once HELLO arrives
  ws.isAlive = true;
  dlog('[CLIENT CONNECTED]', 'id=', ws.id, 'ip=', clientIp(req), 'ua=', req.headers['user-agent']);
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', (data, isBinary) => {
    dlog('[CLIENT MSG]', 'role=', ws.meta && ws.meta.role, 'robotId=', ws.meta && ws.meta.robotId,
      'sessionId=', ws.meta && ws.meta.session, 'binary=', isBinary, 'bytes=', data.length,
      isBinary ? '' : ('type=' + safeType(data)));
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
        dlog('[CLIENT HELLO]', 'id=', ws.id, 'role=', role, 'robotId=', robotId, 'sessionId=', session);
        return;
      }
      // Not a valid hello: refuse the connection.
      dlog('REJECT', 'id=', ws.id, 'reason=HELLO handshake required');
      ws.close(1008, 'HELLO handshake required');
      return;
    }

    // After hello: relay to every *other* peer in the same session.
    const set = ws.meta ? sessions.get(ws.meta.key) : null;
    if (!set) return;
    for (const c of set) {
      if (c !== ws && c.readyState === c.OPEN) {
        // Same dead-peer hazard as broadcastPeerStatus: a vanished socket can
        // still report OPEN, so the write throws EPIPE. Guard it and prune the
        // dead socket instead of letting the exception propagate.
        try {
          if (isBinary) {
            c.send(data);
            dlog('SEND', 'from=', ws.id, 'to=', c.id, 'binary=true', 'bytes=', data.length);
          } else {
            c.send(data.toString());
            dlog('SEND', 'from=', ws.id, 'to=', c.id, 'binary=false', 'bytes=', data.toString().length);
          }
        } catch (e) {
          dlog('SEND-FAIL', 'to=', c.id, 'reason=', e && e.message);
          try { c.terminate(); } catch (_e) { /* already gone */ }
        }
      }
    }
  });

  ws.on('close', (code, reason) => {
    dlog('[CLIENT DISCONNECTED]', 'id=', ws.id, 'code=', code, 'reason=', reason && reason.toString());
    if (ws.meta && sessions.has(ws.meta.key)) {
      const set = sessions.get(ws.meta.key);
      set.delete(ws);
      if (set.size === 0) sessions.delete(ws.meta.key);
      else broadcastPeerStatus(set);
    }
  });

  ws.on('error', (err) => { dlog('ERROR', 'id=', ws.id, 'msg=', err && err.message); });
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
