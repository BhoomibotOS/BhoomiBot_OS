// @ts-nocheck
import { RobotRelay } from './RobotRelay';
// This path is where @cloudflare/next-on-pages generates the worker
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    // 1. Manually handle the relay route for maximum speed/reliability
    const url = new URL(request.url);
    if (url.pathname === '/api/relay') {
      const robotId = url.searchParams.get('robotId') || 'default';
      const id = env.RELAY.idFromName(robotId);
      const obj = env.RELAY.get(id);
      return obj.fetch(request);
    }

    // 2. Delegate everything else to the Next.js app
    return nextWorker.fetch(request, env, ctx);
  }
};
