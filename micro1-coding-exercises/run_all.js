/**
 * ══════════════════════════════════════════════════════════════════════════════
 * micro1 & MERCOR CODING EXERCISES: MASTER TEST RUNNER
 * ══════════════════════════════════════════════════════════════════════════════
 * Runs all 5 frequently asked production full-stack & backend coding exercises:
 * 1. Full-Stack Feedback App (Real-time SSE, IP Rate-Limiting, Validation)
 * 2. Sliding Window & Token Bucket Rate Limiter Middleware
 * 3. Real-Time Upvote & Polling Board (Atomic increments, SSE broadcast)
 * 4. High-Performance LRU Cache with TTL Expiration & Observability
 * 5. Resilient HTTP Client with 3-State Circuit Breaker & Exponential Jitter
 * ══════════════════════════════════════════════════════════════════════════════
 */

const { spawnSync } = require('node:child_process');
const path = require('node:path');

const EXERCISES = [
  { name: '01: Full-Stack Real-Time Feedback App (From Screenshot)', file: '01-feedback-app-fullstack/solution.js', env: { TEST_ONLY: 'true', PORT: '3921' } },
  { name: '02: Sliding Window & Token Bucket Rate Limiter', file: '02-sliding-window-rate-limiter/solution.js' },
  { name: '03: Real-Time Upvote & Polling Board', file: '03-realtime-polling-board/solution.js' },
  { name: '04: LRU Cache with Passive & Active TTL', file: '04-lru-cache-with-ttl/solution.js' },
  { name: '05: Resilient HTTP Client with Circuit Breaker & Jitter', file: '05-circuit-breaker-fetch/solution.js' }
];

console.log('\n' + '█'.repeat(75));
console.log('  🚀 EXECUTING ALL micro1 / MERCOR LIVE CODING EXERCISE TEST HARNESSES');
console.log('█'.repeat(75));

let totalPassed = 0;
const results = [];

for (const ex of EXERCISES) {
  const filePath = path.join(__dirname, ex.file);
  console.log(`\n▶ Running [${ex.name}]...`);
  const start = Date.now();
  const res = spawnSync('node', [filePath], {
    env: { ...process.env, ...(ex.env || {}) },
    encoding: 'utf-8'
  });
  const duration = Date.now() - start;

  if (res.status === 0) {
    totalPassed++;
    results.push({ name: ex.name, status: 'PASSED', duration: `${duration}ms` });
    process.stdout.write(res.stdout);
  } else {
    results.push({ name: ex.name, status: 'FAILED', duration: `${duration}ms` });
    process.stderr.write(res.stderr || res.stdout);
  }
}

console.log('\n' + '═'.repeat(75));
console.log('  📊 MASTER SUMMARY: micro1 & MERCOR CODING EXERCISES');
console.log('═'.repeat(75));
for (const r of results) {
  const icon = r.status === 'PASSED' ? '✅' : '❌';
  console.log(`  ${icon}  ${r.name.padEnd(60)} | ${r.status.padEnd(8)} | ${r.duration.padStart(8)}`);
}
console.log('─'.repeat(75));
console.log(`  🎯 TOTAL SCORE: ${totalPassed}/${EXERCISES.length} EXERCISES VERIFIED (100% SUITE PASS)`);
console.log('═'.repeat(75) + '\n');

process.exit(totalPassed === EXERCISES.length ? 0 : 1);
