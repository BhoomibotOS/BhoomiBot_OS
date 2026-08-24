interface SessionMeta {
  robotId: string;
  session: string;
  role: 'ROBOT' | 'OPERATOR';
}

export class RobotRelay {
  state: any;
  sessions: Map<WebSocket, SessionMeta> = new Map();

  constructor(state: any) {
    this.state = state;
  }

  async fetch(request: Request) {
    const upgradeHeader = request.headers.get('Upgrade');
    if (!upgradeHeader || upgradeHeader !== 'websocket') {
      return new Response('Expected Upgrade: websocket', { status: 426 });
    }

    const [client, server] = new (globalThis as any).WebSocketPair();
    await this.handleSession(server);

    return new Response(null, {
      status: 101,
      webSocket: client,
    });
  }

  async handleSession(ws: WebSocket) {
    (ws as any).accept();

    ws.addEventListener('message', async (msg) => {
      try {
        const meta = this.sessions.get(ws);

        // Handshake: First message must be HELLO
        if (!meta) {
          if (typeof msg.data !== 'string') {
             ws.close(1008, 'HELLO handshake required (text)');
             return;
          }

          const hello = JSON.parse(msg.data);
          if (hello.type === 'HELLO') {
            const payload = hello.payload ? JSON.parse(hello.payload) : {};
            const robotId = hello.robotId || 'default';
            const session = payload.session || '';
            const role = payload.role === 'ROBOT' ? 'ROBOT' : 'OPERATOR';

            this.sessions.set(ws, { robotId, session, role });
            this.broadcastPeerStatus(robotId, session);
            console.log(`[Relay] ${role} joined: ${robotId}::${session}`);
          } else {
            ws.close(1008, 'HELLO handshake required');
          }
          return;
        }

        // Relay: Forward to all OTHER peers in the same session
        this.relayMessage(ws, meta, msg.data);

      } catch (err) {
        console.error('[Relay Error]', err);
      }
    });

    ws.addEventListener('close', () => {
      const meta = this.sessions.get(ws);
      if (meta) {
        this.sessions.delete(ws);
        this.broadcastPeerStatus(meta.robotId, meta.session);
        console.log(`[Relay] ${meta.role} left: ${meta.robotId}::${meta.session}`);
      }
    });
  }

  relayMessage(sender: WebSocket, meta: SessionMeta, data: any) {
    for (const [ws, otherMeta] of this.sessions.entries()) {
      if (ws !== sender && otherMeta.robotId === meta.robotId && otherMeta.session === meta.session) {
        try {
          ws.send(data);
        } catch (e) {
          this.sessions.delete(ws);
        }
      }
    }
  }

  broadcastPeerStatus(robotId: string, session: string) {
    let hasRobot = false;
    let hasOperator = false;

    for (const meta of this.sessions.values()) {
      if (meta.robotId === robotId && meta.session === session) {
        if (meta.role === 'ROBOT') hasRobot = true;
        if (meta.role === 'OPERATOR') hasOperator = true;
      }
    }

    const statusMsg = JSON.stringify({
      type: 'PEER_STATUS',
      ts: Date.now(),
      payload: JSON.stringify({ robot: hasRobot, operator: hasOperator })
    });

    for (const [ws, meta] of this.sessions.entries()) {
      if (meta.robotId === robotId && meta.session === session) {
        ws.send(statusMsg);
      }
    }
  }
}
