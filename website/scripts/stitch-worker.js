const fs = require('fs');
const path = require('path');

const workerPath = path.resolve(__dirname, '../.vercel/output/static/_worker.js/index.js');
const logicPath = path.resolve(__dirname, '../src/server/relay-logic.js');

try {
  console.log('--- BhoomiBot: Global Context Injection v4 ---');

  if (!fs.existsSync(workerPath)) {
    console.error('Build file not found at: ' + workerPath);
    process.exit(1);
  }

  let content = fs.readFileSync(workerPath, 'utf8');

  // 1. Remove Next.js default export
  let nextWorkerName = '';
  const exportMatch = content.match(/export\s+default\s+([^;]+);/) ||
                      content.match(/export\s*{\s*([^ ]+)\s+as\s+default\s*};/);

  if (exportMatch) {
    nextWorkerName = exportMatch[1].trim();
    content = content.replace(exportMatch[0], `const nextWorker = ${nextWorkerName};`);
    console.log(`Successfully captured Next.js worker: ${nextWorkerName}`);
  } else {
    console.error('Critical Error: Could not find default export.');
    process.exit(1);
  }

  const relayLogic = fs.readFileSync(logicPath, 'utf8');

  // 2. Create the Stitched Worker with Global Env Injection
  // We inject env into globalThis so Next.js API routes can access bindings
  const finalBundle = `
/** BHOOMI BOT SYSTEM WORKER **/
${content}

${relayLogic}

export default {
  async fetch(request, env, ctx) {
    // Inject bindings into global context for Next.js API routes
    globalThis.BHOOMI_ENV = env;

    try {
      // 1. Critical Relay Bypass (Fast path for video)
      const url = new URL(request.url);
      if (url.pathname === '/api/relay') {
        const id = env.RELAY.idFromName(url.searchParams.get('robotId') || 'default');
        return env.RELAY.get(id).fetch(request);
      }

      // 2. Run Next.js
      return await nextWorker.fetch(request, env, ctx);
    } catch (e) {
      return new Response("BhoomiBot Runtime Error: " + e.message, { status: 500 });
    }
  }
};
`;

  fs.writeFileSync(workerPath, finalBundle);
  console.log('Final build v4 deployed to bundle.');

} catch (err) {
  console.error('Stitching Failed:', err);
  process.exit(1);
}
