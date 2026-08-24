// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. Handle the Relay WebSocket route first
    if (url.pathname === '/api/relay') {
      try {
        const robotId = url.searchParams.get('robotId') || 'default';
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return obj.fetch(request);
      } catch (err) {
        return new Response(`Relay Logic Error: ${err.message}`, { status: 500 });
      }
    }

    // 2. Delegate everything else to Next.js with robust error catching
    try {
      if (!nextWorker) {
        return new Response("Next.js Bundle not found. Check build output path.", { status: 500 });
      }

      // We use .call to ensure the Next.js worker keeps its internal 'this' context
      return await nextWorker.fetch.call(nextWorker, request, env, ctx);

    } catch (e) {
      console.error(`[NextJS Crash] ${e.message}`);

      // Return the error stack trace to the browser so we can read it
      return new Response(
        `BhoomiBot Runtime Crash:\n\nMessage: ${e.message}\n\nStack: ${e.stack}`,
        {
          status: 500,
          headers: { 'Content-Type': 'text/plain' }
        }
      );
    }
  }
};
