// @ts-nocheck
export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
  }

  async fetch(request) {
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader === 'websocket') {
      const pair = new WebSocketPair();
      const [client, server] = [pair[0], pair[1]];
      await this.handleSession(server);
      return new Response(null, { status: 101, webSocket: client });
    }
    return new Response("Durable Object Active", { status: 200 });
  }

  async handleSession(ws) {
    ws.accept();
    ws.addEventListener('message', async (msg) => {
      try {
        const meta = this.sessions.get(ws);
        if (!meta) {
          const hello = JSON.parse(msg.data);
          if (hello.type === 'HELLO') {
            const p = JSON.parse(hello.payload || '{}');
            this.sessions.set(ws, {
              robotId: hello.robotId || 'default',
              session: p.session || '',
              role: p.role || 'OPERATOR'
            });
          }
          return;
        }
        // Relay to others
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            peer.send(msg.data);
          }
        }
      } catch (e) {}
    });

    ws.addEventListener('close', () => {
      this.sessions.delete(ws);
    });
  }
}
