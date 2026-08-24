// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

// Export the Durable Object class for Cloudflare
export { RobotRelay };

// Export the default handler which points to the Next.js app
export default {
  async fetch(request, env, ctx) {
    // This allows the Next.js app to handle all standard requests
    // while the Durable Object handles the specific relay logic
    return nextWorker.fetch(request, env, ctx);
  }
};
