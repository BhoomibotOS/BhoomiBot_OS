// @ts-nocheck
import handler from "./.open-next/worker.js";
import { RobotRelay } from "./src/server/relay-logic.js";

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. Intercept Relay Route
    // This handles the video WebSocket directly at the edge for max performance
    if (url.pathname === "/api/relay") {
      try {
        const robotId = url.searchParams.get("robotId") || "default";
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);
        return await obj.fetch(request);
      } catch (err) {
        return new Response("Relay Route Initialization Error: " + err.message, { status: 500 });
      }
    }

    // 2. Delegate everything else to OpenNext (the Next.js app)
    return handler.fetch(request, env, ctx);
  },
} satisfies ExportedHandler<CloudflareEnv>;

export { RobotRelay };
