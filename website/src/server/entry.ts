// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    try {
      // 1. Log incoming request for debugging
      console.log(`[Request] ${request.method} ${url.pathname}`);

      // 2. Handle API Relay Routing
      if (url.pathname === '/api/relay') {
        if (!env.RELAY) {
          throw new Error("Durable Object binding 'RELAY' not found.");
        }
        const robotId = url.searchParams.get('robotId') || 'default';
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return obj.fetch(request);
      }

      // 3. Check if Next.js worker is loaded correctly
      if (!nextWorker || !nextWorker.fetch) {
        throw new Error("Next.js entry point (.vercel/output) failed to load.");
      }

      // 4. Delegate to Next.js
      return await nextWorker.fetch(request, env, ctx);

    } catch (e) {
      console.error(`[Fatal Error] ${e.message}`);
      return new Response(
        JSON.stringify({
          error: "BhoomiBot Worker Exception",
          message: e.message,
          path: url.pathname,
          stack: e.stack
        }),
        {
          status: 500,
          headers: { 'Content-Type': 'application/json' }
        }
      );
    }
  }
};
