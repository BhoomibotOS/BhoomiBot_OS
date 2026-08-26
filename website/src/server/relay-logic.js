// BhoomiBot RobotRelay v9 - High-Visibility Connector
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
  }

  async fetch(request) {
    const upgradeHeader = request.headers.get("Upgrade");
    if (upgradeHeader !== "websocket") {
      return new Response("Relay Engine v9 Online", { status: 200 });
    }

    const pair = new WebSocketPair();
    const [client, server] = [pair[0], pair[1]];
    await this.handleSession(server);

    return new Response(null, { status: 101, webSocket: client });
  }

  async handleSession(ws) {
    ws.accept();

    ws.addEventListener("message", async (msg) => {
      try {
        const meta = this.sessions.get(ws);

        if (!meta) {
          if (typeof msg.data !== "string") return;
          const data = JSON.parse(msg.data);

          if (data.type === "HELLO") {
            const p = typeof data.payload === 'string' ? JSON.parse(data.payload) : (data.payload || {});
            const newMeta = {
              robotId: data.robotId || "default",
              session: p.session || "",
              role: p.role || "OPERATOR",
            };
            this.sessions.set(ws, newMeta);
            console.log(`[Relay] ${newMeta.role} CONNECTED: ${newMeta.robotId}`);

            // Tell everyone that a new device joined
            this.broadcastStatus(newMeta.robotId, newMeta.session);
          }
          return;
        }

        // Forward video/commands
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            peer.send(msg.data);
          }
        }
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

    for (const [ws, m] of this.sessions.entries()) {
      if (m.robotId === robotId && m.session === session) {
        try { ws.send(msg); } catch(e) {}
      }
    }
  }
}
