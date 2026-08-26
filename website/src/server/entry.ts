// @ts-nocheck
import { RobotRelay } from './RobotRelay';
import nextWorker from '../../.vercel/output/static/_worker.js/index.js';

// Re-export the Durable Object class
export { RobotRelay };

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. Manually handle the relay route
    if (url.pathname === '/api/relay') {
      try {
        const robotId = url.searchParams.get('robotId') || 'default';
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return await obj.fetch(request);
      } catch (err) {
        return new Response(`Relay Logic Error: ${err.message}`, { status: 500 });
      }
    }

    // 2. Delegate to Next.js with robust handler detection
    try {
      // In some versions of next-on-pages, the export is the fetch function itself.
      // In others, it is an object with a fetch method.
      const handler = nextWorker.fetch || nextWorker.default?.fetch || nextWorker.default || nextWorker;

      if (typeof handler !== 'function' && typeof handler?.fetch !== 'function') {
        return new Response("Next.js handler not found in bundle. Check build configuration.", { status: 500 });
      }

      // Call the handler, ensuring we pass all three required arguments
      return await (typeof handler === 'function'
        ? handler(request, env, ctx)
        : handler.fetch(request, env, ctx));

    } catch (e) {
      // Catch and display the error directly on the page for debugging
      return new Response(
        `BhoomiBot Runtime Error:\n\n${e.message}\n\nStack:\n${e.stack}`,
        { status: 500, headers: { 'Content-Type': 'text/plain' } }
      );
    }
  }
};
