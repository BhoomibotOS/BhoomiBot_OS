// BhoomiBot RobotRelay v6 - Ultra-Standard Implementation
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
            this.sessions.set(ws, {
              robotId: hello.robotId || "default",
              session: p.session || "",
              role: p.role || "OPERATOR",
            });
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

    ws.addEventListener("close", () => this.sessions.delete(ws));
    ws.addEventListener("error", () => this.sessions.delete(ws));
  }
}
