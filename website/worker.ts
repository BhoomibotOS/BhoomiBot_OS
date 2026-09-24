// @ts-nocheck
import handler from "./.open-next/worker.js";
import { RobotRelay } from "./src/server/relay-logic.js";

/**
 * BhoomiBot Custom Cloudflare Worker Entry Point
 * Intercepts video WebSocket relay, Next.js image optimization requests,
 * serves static assets directly via Cloudflare Assets,
 * and delegates SSR requests to OpenNext.
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

    // 2. Next.js Image Optimization fallback handler
    // If Next.js requests /_next/image?url=/robots/xyz.png, serve the source image directly from ASSETS
    if (url.pathname === "/_next/image") {
      const targetImage = url.searchParams.get("url");
      if (targetImage && env.ASSETS) {
        try {
          const imageReqUrl = new URL(targetImage, request.url);
          const imageAssetResponse = await env.ASSETS.fetch(new Request(imageReqUrl, request));
          if (imageAssetResponse.status !== 404) {
            return imageAssetResponse;
          }
        } catch (e) {
          // ignore parsing error
        }
      }
    }

    // 3. Serve static assets directly via Cloudflare Assets binding
    if (env.ASSETS) {
      const assetResponse = await env.ASSETS.fetch(request);
      if (assetResponse.status !== 404) {
        return assetResponse;
      }
    }

    // 4. Main Website Logic (Next.js SSR / API)
    return handler.fetch(request, env, ctx);
  },
} satisfies ExportedHandler<CloudflareEnv>;

// Export for Cloudflare Durable Objects binding
export { RobotRelay };
