const fs = require('fs');
const path = require('path');

const workerPath = path.resolve(__dirname, '../.vercel/output/static/_worker.js/index.js');
const logicPath = path.resolve(__dirname, '../src/server/relay-logic.js');

try {
  console.log('--- BhoomiBot: Precision Worker Stitching v2 ---');

  if (!fs.existsSync(workerPath)) {
    console.error('Build file not found at: ' + workerPath);
    process.exit(1);
  }

  let content = fs.readFileSync(workerPath, 'utf8');

  // 1. Identify and capture the Next.js worker object name
  // This handles both 'export default Ss' and 'export { Ss as default }'
  let nextWorkerName = '';
  const exportMatch = content.match(/export\s+default\s+([^;]+);/) ||
                      content.match(/export\s*{\s*([^ ]+)\s+as\s+default\s*};/);

  if (exportMatch) {
    nextWorkerName = exportMatch[1].trim();
    // Remove the original export line entirely
    content = content.replace(exportMatch[0], `const nextWorker = ${nextWorkerName};`);
    console.log(`Successfully intercepted Next.js worker: ${nextWorkerName}`);
  } else {
    console.error('Critical Error: Could not find default export in Next.js bundle.');
    process.exit(1);
  }

  // 2. Load our Durable Object logic
  const relayLogic = fs.readFileSync(logicPath, 'utf8');

  // 3. Assemble the final worker
  const finalBundle = `
/** BHOOMI BOT GENERATED WORKER **/
${content}

${relayLogic}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Direct Relay Routing (Bypass Next.js for high-speed video)
    if (url.pathname === '/api/relay') {
      const id = env.RELAY.idFromName(url.searchParams.get('robotId') || 'default');
      return env.RELAY.get(id).fetch(request);
    }

    // Delegate to Next.js
    try {
      return await nextWorker.fetch(request, env, ctx);
    } catch (e) {
      return new Response("BhoomiBot Hub Error: " + e.message, { status: 500 });
    }
  }
};
`;

  fs.writeFileSync(workerPath, finalBundle);
  console.log('Final bundle stitched and exported successfully.');

} catch (err) {
  console.error('Stitching Failed:', err);
  process.exit(1);
}
