// BhoomiBot RobotRelay v11 - High-Performance Cloud Bridge
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map(); // ws -> { robotId, session, role }
  }

  async fetch(request) {
    const upgradeHeader = request.headers.get("Upgrade");
    if (upgradeHeader !== "websocket") {
      const active = Array.from(this.sessions.values()).map(m => `${m.role}:${m.robotId}`).join(", ");
      return new Response("Relay Active. Connected: " + (active || "None"), { status: 200 });
    }

    const [client, server] = Object.values(new WebSocketPair());
    server.accept();
    this.handleSession(server);

    return new Response(null, { status: 101, webSocket: client });
  }

  handleSession(ws) {
    ws.addEventListener("message", async (msg) => {
      try {
        const meta = this.sessions.get(ws);

        // 1. Handshake Phase
        if (!meta) {
          if (typeof msg.data !== "string") return;
          const envelope = JSON.parse(msg.data);

          if (envelope.type === "HELLO") {
            const payload = typeof envelope.payload === 'string' ? JSON.parse(envelope.payload) : (envelope.payload || {});
            const newMeta = {
              robotId: envelope.robotId || "BHOOMI-001",
              session: payload.session || "123",
              role: payload.role || "OPERATOR",
            };
            this.sessions.set(ws, newMeta);
            console.log(`[Relay] Handshake: ${newMeta.role} (${newMeta.robotId})`);
            this.broadcastStatus(newMeta.robotId, newMeta.session);
          }
          return;
        }

        // 2. Relay Phase (Binary Frames or JSON Commands)
        this.sessions.forEach((peerMeta, peer) => {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            try {
              peer.send(msg.data);
            } catch (e) {
              this.sessions.delete(peer);
            }
          }
        });
      } catch (err) {
        console.error("Relay Error:", err.message);
      }
    });

    const cleanup = () => {
      const meta = this.sessions.get(ws);
      if (meta) {
        this.sessions.delete(ws);
        this.broadcastStatus(meta.robotId, meta.session);
      }
    };

    ws.addEventListener("close", cleanup);
    ws.addEventListener("error", cleanup);
  }

  broadcastStatus(robotId, session) {
    let hasRobot = false;
    for (const m of this.sessions.values()) {
      if (m.robotId === robotId && m.session === session && m.role === "ROBOT") hasRobot = true;
    }

    const msg = JSON.stringify({
      type: "PEER_STATUS",
      payload: JSON.stringify({ robot: hasRobot })
    });

    this.sessions.forEach((meta, ws) => {
      if (meta.robotId === robotId && meta.session === session) {
        try { ws.send(msg); } catch (e) {}
      }
    });
  }
}
