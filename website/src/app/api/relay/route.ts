// @ts-nocheck
export const runtime = 'edge';

export async function GET(request: Request) {
  try {
    // We use the global BHOOMI_ENV we will inject in the worker wrapper
    const env = (globalThis as any).BHOOMI_ENV;

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
    return new Response(`Relay Logic Error: ${e.message}`, { status: 500 });
  }
}
