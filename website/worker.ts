// OpenNext generates this module during `opennextjs-cloudflare build`.
// @ts-expect-error Generated at build time.
import handler from "./.open-next/worker.js";
import { RobotRelay } from "./src/server/relay-logic.js";

export default {
  fetch: handler.fetch,
} satisfies ExportedHandler<CloudflareEnv>;

// Wrangler resolves this exported class for the RELAY Durable Object binding.
export { RobotRelay };
