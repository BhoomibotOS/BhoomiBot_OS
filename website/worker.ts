// @ts-nocheck
import handler from "./.open-next/worker.js";
import { RobotRelay } from "./src/server/relay-logic.js";

/**
 * BhoomiBot Custom Cloudflare Worker Entry Point
 * This wrapper intercepts the relay API path for video streaming,
 * serves static assets directly via Cloudflare Assets,
 * and delegates website requests to OpenNext.
 */
export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. Critical Relay Interception
    // Handles video WebSockets directly for ultra-low latency
    if (url.pathname === "/api/relay") {
      try {
        const robotId = url.searchParams.get("robotId") || "BHOOMI-001";
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return await obj.fetch(request);
      } catch (err) {
        return new Response("Relay Route Error: " + err.message, { status: 500 });
      }
    }

    // 2. Serve static assets (images, icons, favicons, fonts, CSS/JS) via env.ASSETS
    if (env.ASSETS) {
      const assetResponse = await env.ASSETS.fetch(request);
      if (assetResponse.status !== 404) {
        return assetResponse;
      }
    }

    // 3. Main Website Logic
    // Delegates everything else to the OpenNext bundle
    return handler.fetch(request, env, ctx);
  },
} satisfies ExportedHandler<CloudflareEnv>;

// Export for Cloudflare Durable Objects binding
export { RobotRelay };
