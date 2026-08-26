export class RobotRelay {
  constructor(state, env) {
    this.state = state;
    this.env = env;
    this.sessions = new Map();
  }
  async fetch(request) {
    if (request.headers.get('Upgrade') === 'websocket') {
      const [client, server] = Object.values(new WebSocketPair());
      server.accept();
      server.addEventListener('message', (msg) => {
        const meta = this.sessions.get(server);
        if (!meta) {
          try {
            const hello = JSON.parse(msg.data);
            if (hello.type === 'HELLO') {
              const p = JSON.parse(hello.payload || '{}');
              this.sessions.set(server, { robotId: hello.robotId, session: p.session });
            }
          } catch(e) {}
          return;
        }
        this.sessions.forEach((peerMeta, peer) => {
          if (peer !== server && peerMeta.robotId === meta.robotId && peerMeta.session === meta.session) {
            peer.send(msg.data);
          }
        });
      });
      server.addEventListener('close', () => this.sessions.delete(server));
      return new Response(null, { status: 101, webSocket: client });
    }
    return new Response("Relay Active", { status: 200 });
  }
}
