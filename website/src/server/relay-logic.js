// BhoomiBot RobotRelay - Ported from server.js
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
    return new Response("RobotRelay Active", { status: 200 });
  }

  async handleSession(ws) {
    ws.accept();
    ws.addEventListener('message', async (msg) => {
      try {
        const meta = this.sessions.get(ws);
        if (!meta) {
          if (typeof msg.data !== 'string') return;
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
        for (const [peer, peerMeta] of this.sessions.entries()) {
          if (peer !== ws && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            try { peer.send(msg.data); } catch (e) { this.sessions.delete(peer); }
          }
        }
      } catch (e) { console.error('[Relay Error]', e); }
    });
    ws.addEventListener('close', () => this.sessions.delete(ws));
    ws.addEventListener('error', () => this.sessions.delete(ws));
  }
}
