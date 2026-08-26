// @ts-nocheck
import handler from "./.open-next/worker.js";
import { RobotRelay } from "./src/server/relay-logic.js";

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Ensure the relay route is extremely permissive for the Robot App
    if (url.pathname.startsWith("/api/relay")) {
      try {
        const robotId = url.searchParams.get("robotId") || "default";
        const id = env.RELAY.idFromName(robotId);
        const obj = env.RELAY.get(id);

        // Pass through to Durable Object
        return await obj.fetch(request);
      } catch (err) {
        return new Response("Relay Error: " + err.message, { status: 500 });
      }
    }

    return handler.fetch(request, env, ctx);
  },
} satisfies ExportedHandler<CloudflareEnv>;

export { RobotRelay };
