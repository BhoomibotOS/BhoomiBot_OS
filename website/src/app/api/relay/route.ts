// @ts-nocheck
import { getRequestContext } from '@cloudflare/next-on-pages';

export const runtime = 'edge';

export async function GET(request: Request) {
  try {
    const { env } = getRequestContext();

    // Safety check for binding
    if (!env || !env.RELAY) {
      return new Response("RELAY binding missing in Next.js context", { status: 500 });
    }

    const { searchParams } = new URL(request.url);
    const robotId = searchParams.get('robotId') || 'default';

    // Route directly to the Durable Object
    const id = env.RELAY.idFromName(robotId);
    const obj = env.RELAY.get(id);

    return obj.fetch(request);
  } catch (e) {
    return new Response(`Relay Error: ${e.message}`, { status: 500 });
  }
}
