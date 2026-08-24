// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

// Export the Durable Object class so Cloudflare can instantiate it
export { RobotRelay };

// Export the Next.js worker directly as the default entry point
// This ensures Next.js handles all requests (including /api/relay)
// with its own optimized routing logic.
export default nextWorker;
