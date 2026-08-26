// BhoomiBot RobotRelay v8 - Universal High-Stability Implementation
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map(); // Store connected WebSockets and their metadata
  }

  async fetch(request) {
    // Only handle WebSocket upgrades
    if (request.headers.get("Upgrade") !== "websocket") {
      return new Response("BhoomiBot Relay Engine is Running.", { status: 200 });
    }

    // Standard Cloudflare WebSocket pairing
    const pair = new WebSocketPair();
    const [client, server] = [pair[0], pair[1]];

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

        // 1. Handshake Phase: Register the device
        if (!meta) {
          if (typeof msg.data !== "string") return;
          const data = JSON.parse(msg.data);

          if (data.type === "HELLO") {
            const payload = typeof data.payload === 'string' ? JSON.parse(data.payload) : (data.payload || {});
            this.sessions.set(ws, {
              robotId: data.robotId || "default",
              session: payload.session || "",
              role: payload.role || "OPERATOR",
            });
            console.log(`[Relay] Handshake Success: ${data.robotId}`);
          }
          return;
        }

        // 2. Relay Phase: Forward the packet (Binary Video or JSON Command)
        // We broadcast to everyone in the same room (robotId + session)
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            try {
              peer.send(msg.data);
            } catch (e) {
              this.sessions.delete(peer);
            }
          }
        }
      } catch (err) {
        console.error("Relay Message Error:", err.message);
      }
    });

    const closeHandler = () => {
      this.sessions.delete(ws);
    };

    ws.addEventListener("close", closeHandler);
    ws.addEventListener("error", closeHandler);
  }
}
