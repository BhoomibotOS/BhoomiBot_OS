const fs = require('fs');
const path = require('path');

const workerPath = path.resolve(__dirname, '../.vercel/output/static/_worker.js/index.js');
const logicPath = path.resolve(__dirname, '../src/server/relay-logic.js');

try {
  console.log('--- BhoomiBot: Advanced Worker Stitching ---');

  if (!fs.existsSync(workerPath)) {
    console.error('Build file not found at: ' + workerPath);
    process.exit(1);
  }

  let content = fs.readFileSync(workerPath, 'utf8');

  // 1. Remove the existing default export so we can wrap it
  content = content.replace(/export\s+default\s+([^;]+);/, 'const nextWorker = $1;');

  // 2. Add our Durable Object logic
  const relayLogic = fs.readFileSync(logicPath, 'utf8');

  // 3. Create the wrapped worker
  const wrappedWorker = `
${content}

${relayLogic}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // Diagnostic endpoint
    if (url.pathname === '/__debug') {
      return new Response(JSON.stringify({
        ready: !!nextWorker,
        hasRelay: !!env.RELAY,
        time: new Date().toISOString()
      }), { headers: { 'content-type': 'application/json' } });
    }

    // Direct Relay Routing (Bypass Next.js for speed)
    if (url.pathname === '/api/relay') {
      const id = env.RELAY.idFromName(url.searchParams.get('robotId') || 'default');
      return env.RELAY.get(id).fetch(request);
    }

    try {
      return await nextWorker.fetch(request, env, ctx);
    } catch (e) {
      return new Response("BhoomiBot Runtime Error: " + e.message + "\\n\\nStack: " + e.stack, { status: 500 });
    }
  }
};
`;

  fs.writeFileSync(workerPath, wrappedWorker);
  console.log('Successfully stitched and wrapped Next.js with RobotRelay.');

  // Ensure .assetsignore exists
  fs.writeFileSync(path.resolve(__dirname, '../.vercel/output/static/.assetsignore'), '_worker.js\n');

} catch (err) {
  console.error('Stitching Failed:', err);
  process.exit(1);
}
