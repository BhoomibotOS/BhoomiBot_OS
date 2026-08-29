export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.sessions = new Map(); // ws -> { robotId, role, session }
  }

  async fetch(request) {
    const url = new URL(request.url);
    const upgradeHeader = request.headers.get("Upgrade");
    if (!upgradeHeader || upgradeHeader !== "websocket") {
      return new Response("Expected Upgrade: websocket", { status: 426 });
    }

    const [client, server] = new WebSocketPair();
    const robotId = url.searchParams.get("robotId") || "BHOOMI-001";

    this.state.acceptWebSocket(server);
    // Initial role is PENDING until HELLO handshake arrives
    this.sessions.set(server, { robotId, role: "PENDING", session: "123" });

    return new Response(null, { status: 101, webSocket: client });
  }

  async webSocketMessage(ws, message) {
    const meta = this.sessions.get(ws);
    if (!meta) return;

    // PERFORMANCE OPTIMIZATION: Instant relay for binary (Video Frames)
    // We check if message is a string. If not, it's a raw JPEG binary frame.
    // We relay it without parsing JSON to save CPU and reduce lag.
    if (typeof message !== "string") {
      this.sessions.forEach((peerMeta, peer) => {
        if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
          try { peer.send(message); } catch (e) { this.sessions.delete(peer); }
        }
      });
      return;
    }

    try {
      const envelope = JSON.parse(message);

      // 1. Handshake Phase
      if (envelope.type === "HELLO") {
        const payload = JSON.parse(envelope.payload || "{}");
        const newMeta = {
          robotId: envelope.robotId || "BHOOMI-001",
          session: payload.session || "123",
          role: payload.role || "OPERATOR",
        };
        this.sessions.set(ws, newMeta);
        console.log(`[Relay] ${newMeta.role} joined session ${newMeta.session} for ${newMeta.robotId}`);
        this.broadcastStatus(newMeta.robotId, newMeta.session);
        return;
      }

      // 2. Text Relay Phase (Commands, Telemetry)
      this.sessions.forEach((peerMeta, peer) => {
        if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
          try { peer.send(message); } catch (e) { this.sessions.delete(peer); }
        }
      });

    } catch (err) {
      console.error("[Relay] JSON Parse Error:", err);
    }
  }

  broadcastStatus(robotId, session) {
    let hasRobot = false;
    let hasOperator = false;

    this.sessions.forEach((meta) => {
      if (meta.robotId === robotId && meta.session === session) {
        if (meta.role === "ROBOT") hasRobot = true;
        if (meta.role === "OPERATOR") hasOperator = true;
      }
    });

    const msg = JSON.stringify({
      type: "PEER_STATUS",
      payload: JSON.stringify({ robot: hasRobot, operator: hasOperator })
    });

    this.sessions.forEach((meta, ws) => {
      if (meta.robotId === robotId && meta.session === session) {
        try { ws.send(msg); } catch (e) {}
      }
    });
  }

  async webSocketClose(ws) {
    const meta = this.sessions.get(ws);
    if (meta) {
      console.log(`[Relay] ${meta.role} left ${meta.robotId}`);
      this.sessions.delete(ws);
      this.broadcastStatus(meta.robotId, meta.session);
    }
  }
}
