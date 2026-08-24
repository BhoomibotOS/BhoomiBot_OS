// @ts-nocheck
import { NextResponse } from 'next/server';

export const runtime = 'edge';

export async function GET(request: Request) {
  const upgradeHeader = request.headers.get('Upgrade');

  if (!upgradeHeader || upgradeHeader !== 'websocket') {
    return new Response('Expected Upgrade: websocket', { status: 426 });
  }

  // Get the room ID from the URL (e.g., /api/relay?robotId=BHOOMI-001)
  const { searchParams } = new URL(request.url);
  const robotId = searchParams.get('robotId') || 'default-room';

  // In a real Cloudflare environment, we would access the Durable Object here
  // via the platform object: env.RELAY.get(env.RELAY.idFromName(robotId))

  // For now, we return a 101 Switching Protocols to indicate the server is ready
  // to handle the WebSocket relay once deployed to Cloudflare.

  const [client, server] = new (globalThis as any).WebSocketPair();

  // Basic signaling logic for the Edge Worker
  (server as any).accept();

  server.addEventListener('message', (event: any) => {
    // Broadcast signaling data (SDP/ICE) to the connected peer
    // In the Durable Object, this would broadcast to all clients in the room
    server.send(event.data);
  });

  return new Response(null, {
    status: 101,
    // @ts-ignore
    webSocket: client,
  });
}
