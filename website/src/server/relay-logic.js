// BhoomiBot RobotRelay v5 - Standard WebSocket Implementation
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
  }

  async fetch(request) {
    console.log("[Relay] Fetch request received");
    const upgradeHeader = request.headers.get('Upgrade');

    if (upgradeHeader === 'websocket') {
      console.log("[Relay] Upgrading to WebSocket...");
      const pair = new WebSocketPair();
      const client = pair[0];
      const server = pair[1];

      await this.handleSession(server);

      return new Response(null, {
        status: 101,
        webSocket: client,
      });
    }

    return new Response("BhoomiBot Relay is Online and Waiting for WebSockets.", { status: 200 });
  }

  async handleSession(ws) {
    ws.accept();
    console.log("[Relay] WebSocket Accepted");

    ws.addEventListener('message', async (msg) => {
      try {
        const meta = this.sessions.get(ws);

        // Handle Handshake
        if (!meta) {
          console.log("[Relay] Processing Handshake:", msg.data);
          const hello = JSON.parse(msg.data);
          if (hello.type === 'HELLO') {
            const p = JSON.parse(hello.payload || '{}');
            const sessionMeta = {
              robotId: hello.robotId || 'default',
              session: p.session || '',
              role: p.role || 'OPERATOR'
            };
            this.sessions.set(ws, sessionMeta);
            console.log(`[Relay] Registered: ${sessionMeta.role} for ${sessionMeta.robotId}`);
          }
          return;
        }

        // Relay packets to other peers in the same room
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            peer.send(msg.data);
          }
        }
      } catch (e) {
        console.error("[Relay Internal Error]", e.message);
      }
    });

    ws.addEventListener('close', () => {
      console.log("[Relay] Client Disconnected");
      this.sessions.delete(ws);
    });

    ws.addEventListener('error', (e) => {
      console.error("[Relay Socket Error]", e);
      this.sessions.delete(ws);
    });
  }
}
