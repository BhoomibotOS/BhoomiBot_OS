import { getCloudflareContext } from '@opennextjs/cloudflare';

export async function GET(request: Request) {
  try {
    const { env } = getCloudflareContext();

    if (!env || !env.RELAY) {
      return new Response(JSON.stringify({
        error: "Relay system initializing",
        details: "Durable Object binding not found in global context."
      }), { status: 503, headers: {'Content-Type': 'application/json'} });
    }

    const { searchParams } = new URL(request.url);
    const robotId = searchParams.get('robotId') || 'default';

    const id = env.RELAY.idFromName(robotId);
    const obj = env.RELAY.get(id);

    return obj.fetch(request);
  } catch (e) {
    const message = e instanceof Error ? e.message : "Unknown relay error";
    return new Response(`Relay Logic Error: ${message}`, { status: 500 });
  }
}
