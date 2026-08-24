// @ts-nocheck
import { getRequestContext } from '@cloudflare/next-on-pages';

export const runtime = 'edge';

export async function GET(request: Request) {
  try {
    const { env } = getRequestContext();
    const { searchParams } = new URL(request.url);
    const robotId = searchParams.get('robotId') || 'default';

    // Get the Durable Object ID based on the Robot ID
    const id = env.RELAY.idFromName(robotId);
    const obj = env.RELAY.get(id);

    // Forward the WebSocket request to the Durable Object
    return obj.fetch(request);
  } catch (e) {
    return new Response(`Relay Connection Error: ${e.message}`, { status: 500 });
  }
}
