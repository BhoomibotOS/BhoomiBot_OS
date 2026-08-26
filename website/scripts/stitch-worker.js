const fs = require('fs');
const path = require('path');

const workerPath = path.resolve(__dirname, '../.vercel/output/static/_worker.js/index.js');
const logicPath = path.resolve(__dirname, '../src/server/relay-logic.js');

try {
  console.log('--- BhoomiBot: High-Resiliency Stitching v3 ---');

  if (!fs.existsSync(workerPath)) {
    console.error('Build file not found at: ' + workerPath);
    process.exit(1);
  }

  let content = fs.readFileSync(workerPath, 'utf8');

  // 1. Intercept the export (Supports: export default X; OR export { X as default };)
  let nextWorkerName = '';
  const exportMatch = content.match(/export\s+default\s+([^;]+);/) ||
                      content.match(/export\s*{\s*([^ ]+)\s+as\s+default\s*};/);

  if (exportMatch) {
    nextWorkerName = exportMatch[1].trim();
    content = content.replace(exportMatch[0], `const nextWorker = ${nextWorkerName};`);
    console.log(`Successfully intercepted Next.js worker variable: ${nextWorkerName}`);
  } else {
    console.error('Critical Error: Could not find default export in Next.js bundle.');
    process.exit(1);
  }

  const relayLogic = fs.readFileSync(logicPath, 'utf8');

  // 2. The new Universal Wrapper
  // It checks if nextWorker is a function or has a .fetch method
  const finalBundle = `
/** BHOOMI BOT STITCHED WORKER **/
${content}

${relayLogic}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // 1. System Diagnostic
    if (url.pathname === '/__status') {
      return new Response(JSON.stringify({
        online: true,
        worker: "BhoomiBot Hub",
        relayReady: !!env.RELAY
      }), { headers: { 'content-type': 'application/json' } });
    }

    // 2. Direct Video Relay Routing
    if (url.pathname === '/api/relay') {
      const id = env.RELAY.idFromName(url.searchParams.get('robotId') || 'default');
      return env.RELAY.get(id).fetch(request);
    }

    // 3. Resilient Next.js Invocation
    try {
      if (typeof nextWorker === 'function') {
        return await nextWorker(request, env, ctx);
      } else if (nextWorker && typeof nextWorker.fetch === 'function') {
        return await nextWorker.fetch(request, env, ctx);
      }
      throw new Error("Next.js handler is neither a function nor an object with a fetch method.");
    } catch (e) {
      console.error("[NextJS Crash]", e.message);
      return new Response("BhoomiBot Internal Error:\\n" + e.message + "\\n\\n" + e.stack, { status: 500 });
    }
  }
};
`;

  fs.writeFileSync(workerPath, finalBundle);
  console.log('Final bundle v3 stitched successfully.');

} catch (err) {
  console.error('Stitching Failed:', err);
  process.exit(1);
}
