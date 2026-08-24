// @ts-nocheck
export class RobotRelay {
  state: any;
  env: any;
  sessions: Map<WebSocket, any> = new Map();

  constructor(state: any, env: any) {
    this.state = state;
    this.env = env;
  }

  async fetch(request: Request) {
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader === 'websocket') {
      const [client, server] = new (globalThis as any).WebSocketPair();
      await this.handleSession(server);
      return new Response(null, { status: 101, webSocket: client });
    }
    return new Response("RobotRelay DO is active.", { status: 200 });
  }

  async handleSession(ws: WebSocket) {
    ws.accept();
    ws.addEventListener('message', async (msg) => {
      const meta = this.sessions.get(ws);
      if (!meta) {
        try {
          const hello = JSON.parse(msg.data);
          if (hello.type === 'HELLO') {
            const payload = JSON.parse(hello.payload || '{}');
            this.sessions.set(ws, {
              robotId: hello.robotId || 'default',
              session: payload.session || '',
              role: payload.role || 'OPERATOR'
            });
            this.broadcastPeerStatus();
          }
        } catch(e) {}
        return;
      }
      this.relayMessage(ws, meta, msg.data);
    });

    ws.addEventListener('close', () => {
      this.sessions.delete(ws);
      this.broadcastPeerStatus();
    });
  }

  relayMessage(sender: WebSocket, meta: any, data: any) {
    for (const [ws, otherMeta] of this.sessions.entries()) {
      if (ws !== sender && otherMeta.robotId === meta.robotId && otherMeta.session === meta.session) {
        try { ws.send(data); } catch (e) { this.sessions.delete(ws); }
      }
    }
  }

  broadcastPeerStatus() {
    // Basic status broadcast logic
    const statusMsg = JSON.stringify({ type: 'PEER_STATUS', ts: Date.now() });
    for (const [ws] of this.sessions.entries()) {
      try { ws.send(statusMsg); } catch(e) {}
    }
  }
}
