// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import * as nextWorker from '../../.vercel/output/static/_worker.js/index.js';

export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. SYSTEM HEALTH CHECK
    // If you visit /__health, you should see this message.
    if (url.pathname === '/__health') {
      return new Response(JSON.stringify({
        status: "alive",
        worker: "BhoomiBot Conductor",
        hasRELAY: !!env.RELAY,
        hasNextJS: !!nextWorker
      }), { headers: { 'Content-Type': 'application/json' } });
    }

    try {
      // 2. Handle API Relay Routing
      if (url.pathname === '/api/relay') {
        const robotId = url.searchParams.get('robotId') || 'default';
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return obj.fetch(request);
      }

      // 3. Delegate to Next.js (Trying both default and named export)
      const handler = nextWorker.default || nextWorker;
      if (handler && typeof handler.fetch === 'function') {
        return await handler.fetch(request, env, ctx);
      } else if (typeof handler === 'function') {
        return await handler(request, env, ctx);
      }

      throw new Error("Next.js handler not found in bundle.");

    } catch (e) {
      return new Response(
        JSON.stringify({
          error: "BhoomiBot Execution Error",
          message: e.message,
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
