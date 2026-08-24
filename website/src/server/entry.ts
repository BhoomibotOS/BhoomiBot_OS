// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    try {
      // 1. Handle API Relay Routing manually for better DO stability
      const url = new URL(request.url);
      if (url.pathname === '/api/relay') {
        const robotId = url.searchParams.get('robotId') || 'default';
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return obj.fetch(request);
      }

      // 2. Delegate everything else to the Next.js app
      return await nextWorker.fetch(request, env, ctx);
    } catch (e) {
      return new Response(`BhoomiBot Runtime Error: ${e.message}`, { status: 500 });
    }
  }
};
