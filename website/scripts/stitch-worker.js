const fs = require('fs');
const path = require('path');

const workerPath = path.resolve(__dirname, '../.vercel/output/static/_worker.js/index.js');
const logicPath = path.resolve(__dirname, '../src/server/relay-logic.js');

try {
  console.log('--- Stitching RobotRelay to Next.js Worker ---');

  if (!fs.existsSync(workerPath)) {
    console.error('Error: Built Next.js worker not found at ' + workerPath);
    process.exit(1);
  }

  const workerContent = fs.readFileSync(workerPath, 'utf8');
  const logicContent = fs.readFileSync(logicPath, 'utf8');

  // Check if logic is already appended
  if (workerContent.includes('class RobotRelay')) {
    console.log('RobotRelay already exists in worker bundle. Skipping.');
  } else {
    // Append logic to the end of the file
    fs.appendFileSync(workerPath, '\n\n' + logicContent);
    console.log('Successfully stitched RobotRelay to index.js');
  }

  // Create .assetsignore in the same directory to prevent wrangler upload errors
  const ignorePath = path.resolve(__dirname, '../.vercel/output/static/.assetsignore');
  fs.writeFileSync(ignorePath, '_worker.js\n');
  console.log('Created .assetsignore for Wrangler.');

} catch (err) {
  console.error('Stitching Failed:', err.message);
  process.exit(1);
}
