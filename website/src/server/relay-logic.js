// BhoomiBot RobotRelay v7 - High-Reliability Implementation
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map(); // ws -> meta
  }

  async fetch(request) {
    if (request.headers.get("Upgrade") !== "websocket") {
      return new Response("Relay Engine Active", { status: 200 });
    }

    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];

    await this.handleSession(server);

    return new Response(null, {
      status: 101,
      webSocket: client,
    });
  }

  async handleSession(ws) {
    ws.accept();

    ws.addEventListener("message", async (msg) => {
      try {
        const meta = this.sessions.get(ws);

        // 1. Handshake Phase
        if (!meta) {
          if (typeof msg.data !== "string") return;
          const hello = JSON.parse(msg.data);
          if (hello.type === "HELLO") {
            const p = JSON.parse(hello.payload || "{}");
            const newMeta = {
              robotId: hello.robotId || "default",
              session: p.session || "",
              role: p.role || "OPERATOR",
            };
            this.sessions.set(ws, newMeta);
            console.log(`[Relay] Handshake: ${newMeta.role} joined ${newMeta.robotId}`);
            this.broadcastPeerStatus(newMeta.robotId, newMeta.session);
          }
          return;
        }

        // 2. Relay Phase (Binary Video or JSON Commands)
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (
            peer !== ws &&
            peerMeta.robotId === meta.robotId &&
            peerMeta.session === meta.session
          ) {
            peer.send(msg.data);
          }
        }
      } catch (err) {
        console.error("DO Error:", err.message);
      }
    });

    const cleanup = () => {
      const meta = this.sessions.get(ws);
      if (meta) {
        this.sessions.delete(ws);
        this.broadcastPeerStatus(meta.robotId, meta.session);
        console.log(`[Relay] ${meta.role} left ${meta.robotId}`);
      }
    };

    ws.addEventListener("close", cleanup);
    ws.addEventListener("error", cleanup);
  }

  broadcastPeerStatus(robotId, session) {
    let hasRobot = false;
    let hasOperator = false;

    for (const meta of this.sessions.values()) {
      if (meta.robotId === robotId && meta.session === session) {
        if (meta.role === "ROBOT") hasRobot = true;
        if (meta.role === "OPERATOR") hasOperator = true;
      }
    }

    const statusMsg = JSON.stringify({
      type: "PEER_STATUS",
      ts: Date.now(),
      payload: JSON.stringify({ robot: hasRobot, operator: hasOperator }),
    });

    for (const [ws, meta] of this.sessions.entries()) {
      if (meta.robotId === robotId && meta.session === session) {
        try {
          ws.send(statusMsg);
        } catch (e) {
          this.sessions.delete(ws);
        }
      }
    }
  }
}
