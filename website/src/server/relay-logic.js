// BhoomiBot RobotRelay v12 - Ultimate Synchronization
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
  }

  async fetch(request) {
    if (request.headers.get("Upgrade") !== "websocket") {
      return new Response("Relay v12 Online", { status: 200 });
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
        if (!meta) {
          const envelope = JSON.parse(msg.data);
          if (envelope.type === "HELLO") {
            const p = typeof envelope.payload === 'string' ? JSON.parse(envelope.payload) : (envelope.payload || {});
            this.sessions.set(ws, {
              robotId: envelope.robotId || "BHOOMI-001",
              session: p.session || "123",
              role: p.role || "OPERATOR",
            });
            console.log(`[Sync] ${p.role} entered BHOOMI-001`);
            this.broadcastStatus("BHOOMI-001", p.session || "123");
          }
          return;
        }

        // Forward everything (Video/Commands)
        this.sessions.forEach((peerMeta, peer) => {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            peer.send(msg.data);
          }
        });
      } catch (e) {}
    });

    const cleanup = () => {
      const m = this.sessions.get(ws);
      if (m) {
        this.sessions.delete(ws);
        this.broadcastStatus(m.robotId, m.session);
      }
    };
    ws.addEventListener("close", cleanup);
    ws.addEventListener("error", cleanup);
  }

  broadcastStatus(robotId, session) {
    const hasRobot = Array.from(this.sessions.values()).some(m => m.role === "ROBOT" && m.robotId === robotId);
    const statusMsg = JSON.stringify({ type: "PEER_STATUS", payload: JSON.stringify({ robot: hasRobot }) });
    this.sessions.forEach((meta, ws) => {
      if (meta.robotId === robotId && meta.session === session) {
        try { ws.send(statusMsg); } catch(e) {}
      }
    });
  }
}
