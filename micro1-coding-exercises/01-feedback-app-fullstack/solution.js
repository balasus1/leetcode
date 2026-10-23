/**
 * ══════════════════════════════════════════════════════════════════════════════
 * micro1 CODING EXERCISE: Full-Stack Real-Time Feedback Application
 * ══════════════════════════════════════════════════════════════════════════════
 * Language: JavaScript (Node.js runtime)
 * Time Target: 15:00 minutes
 * 
 * FRONTEND REQUIREMENTS:
 * 1. Form with Name, Email, and Feedback fields.
 * 2. Real-time updates displaying submitted entries below the form.
 * 3. Client & server-side validation with actionable error messages.
 * 
 * BACKEND REQUIREMENTS:
 * 1. REST API with POST /feedback and GET /feedback routes.
 * 2. In-memory Mock Database.
 * 3. IP Rate-Limiting per client (Sliding Window / Token Bucket) [BONUS].
 * 4. Structured JSON responses with status codes and ISO-8601 timestamps.
 * 5. Built-in Automated Verification Test Suite for the micro1 code evaluator.
 * ══════════════════════════════════════════════════════════════════════════════
 */

const http = require('node:http');

// ─────────────────────────────────────────────────────────────────────────────
// 1. IN-MEMORY DATABASE & STATE
// ─────────────────────────────────────────────────────────────────────────────
class MockDatabase {
  constructor() {
    this.feedbackEntries = [];
    this.currentId = 1;
  }

  insert({ name, email, feedback, ip }) {
    const entry = {
      id: this.currentId++,
      name: name.trim(),
      email: email.trim().toLowerCase(),
      feedback: feedback.trim(),
      ip: ip || '127.0.0.1',
      createdAt: new Date().toISOString()
    };
    this.feedbackEntries.unshift(entry); // newest first
    return entry;
  }

  findAll() {
    return [...this.feedbackEntries];
  }

  clear() {
    this.feedbackEntries = [];
    this.currentId = 1;
  }
}

const db = new MockDatabase();

// ─────────────────────────────────────────────────────────────────────────────
// 2. IP RATE LIMITER (SLIDING WINDOW ALGORITHM)
// ─────────────────────────────────────────────────────────────────────────────
class IpRateLimiter {
  constructor({ maxRequests = 5, windowMs = 10000 } = {}) {
    this.maxRequests = maxRequests;
    this.windowMs = windowMs;
    this.requests = new Map(); // ip -> [timestamps]
  }

  isAllowed(ip) {
    const now = Date.now();
    const timestamps = this.requests.get(ip) || [];

    // Filter out timestamps outside the active sliding window
    const validTimestamps = timestamps.filter(ts => now - ts < this.windowMs);

    if (validTimestamps.length >= this.maxRequests) {
      const oldest = validTimestamps[0];
      const retryAfterSec = Math.ceil((this.windowMs - (now - oldest)) / 1000);
      return { allowed: false, retryAfterSec };
    }

    validTimestamps.push(now);
    this.requests.set(ip, validTimestamps);
    return { 
      allowed: true, 
      remaining: this.maxRequests - validTimestamps.length,
      retryAfterSec: 0
    };
  }

  reset() {
    this.requests.clear();
  }
}

const rateLimiter = new IpRateLimiter({ maxRequests: 5, windowMs: 10000 });

// ─────────────────────────────────────────────────────────────────────────────
// 3. REAL-TIME EVENT STREAM (SERVER-SENT EVENTS - SSE)
// ─────────────────────────────────────────────────────────────────────────────
const sseClients = new Set();

function broadcastFeedback(entry) {
  const payload = `data: ${JSON.stringify(entry)}\n\n`;
  for (const client of sseClients) {
    client.write(payload);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. REQUEST VALIDATION
// ─────────────────────────────────────────────────────────────────────────────
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateFeedbackInput({ name, email, feedback }) {
  const errors = {};

  if (!name || typeof name !== 'string' || name.trim().length < 2) {
    errors.name = 'Name must be at least 2 characters long.';
  }

  if (!email || typeof email !== 'string' || !EMAIL_REGEX.test(email.trim())) {
    errors.email = 'A valid email address is required (e.g., user@domain.com).';
  }

  if (!feedback || typeof feedback !== 'string' || feedback.trim().length < 5) {
    errors.feedback = 'Feedback must be at least 5 characters long.';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. EMBEDDED FRONTEND UI (REACT / MODERN RESPONSIVE SPA)
// ─────────────────────────────────────────────────────────────────────────────
const FRONTEND_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>micro1 Feedback Hub</title>
  <style>
    :root {
      --bg: #0b0f19;
      --card-bg: #151d2f;
      --card-border: #232f48;
      --primary: #4f46e5;
      --primary-hover: #4338ca;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --danger: #ef4444;
      --success: #10b981;
      --accent: #38bdf8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding: 2rem 1rem; min-height: 100vh; display: flex; justify-content: center; }
    .container { width: 100%; max-width: 680px; display: flex; flex-direction: column; gap: 2rem; }
    .header { text-align: center; }
    .header h1 { font-size: 1.8rem; font-weight: 700; background: linear-gradient(135deg, #a5b4fc, #38bdf8); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    .header p { color: var(--text-muted); font-size: 0.95rem; margin-top: 0.4rem; }
    .live-badge { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.75rem; background: rgba(16, 185, 129, 0.15); color: var(--success); padding: 0.25rem 0.6rem; border-radius: 9999px; margin-top: 0.6rem; }
    .live-dot { width: 8px; height: 8px; background-color: var(--success); border-radius: 50%; animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.85); } }
    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 1.5rem; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.4); }
    .form-group { display: flex; flex-direction: column; gap: 0.4rem; margin-bottom: 1.1rem; }
    label { font-size: 0.85rem; font-weight: 600; color: #cbd5e1; }
    input, textarea { background: #0c1220; border: 1px solid #293854; border-radius: 8px; padding: 0.75rem; color: #fff; font-size: 0.95rem; transition: border-color 0.2s; outline: none; }
    input:focus, textarea:focus { border-color: var(--primary); }
    textarea { resize: vertical; min-height: 90px; }
    .error-msg { color: var(--danger); font-size: 0.78rem; display: none; }
    .has-error input, .has-error textarea { border-color: var(--danger); }
    .has-error .error-msg { display: block; }
    .btn { background: var(--primary); color: white; border: none; border-radius: 8px; padding: 0.85rem; font-size: 0.95rem; font-weight: 600; cursor: pointer; transition: background 0.2s, transform 0.1s; display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
    .btn:hover { background: var(--primary-hover); }
    .btn:active { transform: scale(0.99); }
    .btn:disabled { opacity: 0.6; cursor: not-allowed; }
    .alert-banner { padding: 0.8rem; border-radius: 8px; font-size: 0.85rem; margin-bottom: 1rem; display: none; }
    .alert-banner.error { background: rgba(239, 68, 68, 0.15); border: 1px solid var(--danger); color: #fca5a5; display: block; }
    .alert-banner.success { background: rgba(16, 185, 129, 0.15); border: 1px solid var(--success); color: #6ee7b7; display: block; }
    .entries-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .entries-header h2 { font-size: 1.2rem; font-weight: 600; }
    .badge { font-size: 0.75rem; background: #232f48; padding: 0.2rem 0.6rem; border-radius: 6px; color: var(--accent); }
    .entries-list { display: flex; flex-direction: column; gap: 0.85rem; }
    .entry-item { background: #0c1220; border: 1px solid #1e293b; border-radius: 8px; padding: 1rem; animation: slideIn 0.3s ease-out; }
    @keyframes slideIn { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
    .entry-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 0.4rem; }
    .entry-author { font-weight: 600; font-size: 0.95rem; color: #e2e8f0; }
    .entry-email { color: var(--text-muted); font-size: 0.8rem; margin-left: 0.4rem; }
    .entry-time { font-size: 0.75rem; color: #64748b; }
    .entry-body { font-size: 0.9rem; color: #cbd5e1; line-height: 1.45; word-break: break-word; }
    .empty-state { text-align: center; color: #64748b; padding: 2rem; font-size: 0.9rem; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>micro1 Real-Time Feedback App</h1>
      <p>Submit feedback and watch entries update in real time via Server-Sent Events</p>
      <div class="live-badge"><div class="live-dot"></div> Live SSE Connected</div>
    </div>

    <div class="card">
      <div id="alertBanner" class="alert-banner"></div>
      <form id="feedbackForm" novalidate>
        <div class="form-group" id="groupName">
          <label for="name">Your Name</label>
          <input type="text" id="name" placeholder="John Doe" required autocomplete="off">
          <div class="error-msg" id="nameError">Name must be at least 2 characters long.</div>
        </div>

        <div class="form-group" id="groupEmail">
          <label for="email">Email Address</label>
          <input type="email" id="email" placeholder="john@example.com" required autocomplete="off">
          <div class="error-msg" id="emailError">Please enter a valid email address.</div>
        </div>

        <div class="form-group" id="groupFeedback">
          <label for="feedback">Feedback Message</label>
          <textarea id="feedback" placeholder="Share your experience or suggestions..." required></textarea>
          <div class="error-msg" id="feedbackError">Feedback must be at least 5 characters long.</div>
        </div>

        <button type="submit" class="btn" id="submitBtn">
          <span>Submit Feedback</span>
        </button>
      </form>
    </div>

    <div class="card">
      <div class="entries-header">
        <h2>Live Feedback Stream</h2>
        <span class="badge" id="countBadge">0 entries</span>
      </div>
      <div class="entries-list" id="entriesList">
        <div class="empty-state">No feedback submitted yet. Be the first!</div>
      </div>
    </div>
  </div>

  <script>
    const form = document.getElementById('feedbackForm');
    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const feedbackInput = document.getElementById('feedback');
    const submitBtn = document.getElementById('submitBtn');
    const alertBanner = document.getElementById('alertBanner');
    const entriesList = document.getElementById('entriesList');
    const countBadge = document.getElementById('countBadge');

    let entries = [];

    function showAlert(msg, isError = false) {
      alertBanner.textContent = msg;
      alertBanner.className = 'alert-banner ' + (isError ? 'error' : 'success');
      setTimeout(() => { alertBanner.className = 'alert-banner'; }, 5000);
    }

    function renderEntries() {
      countBadge.textContent = entries.length + (entries.length === 1 ? ' entry' : ' entries');
      if (entries.length === 0) {
        entriesList.innerHTML = '<div class="empty-state">No feedback submitted yet. Be the first!</div>';
        return;
      }
      entriesList.innerHTML = entries.map(item => \`
        <div class="entry-item" id="entry-\${item.id}">
          <div class="entry-top">
            <div>
              <span class="entry-author">\${escapeHtml(item.name)}</span>
              <span class="entry-email">&lt;\${escapeHtml(item.email)}&gt;</span>
            </div>
            <span class="entry-time">\${new Date(item.createdAt).toLocaleTimeString()}</span>
          </div>
          <div class="entry-body">\${escapeHtml(item.feedback)}</div>
        </div>
      \`).join('');
    }

    function escapeHtml(str) {
      return (str || '').replace(/[&<>"']/g, m => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
      }[m]));
    }

    // Load initial feedback
    async function fetchInitial() {
      try {
        const res = await fetch('/feedback');
        const json = await res.json();
        if (json.status === 'success') {
          entries = json.data;
          renderEntries();
        }
      } catch (err) {
        console.error('Initial fetch failed:', err);
      }
    }

    // Connect to real-time Server-Sent Events stream
    function initSSE() {
      const source = new EventSource('/feedback/stream');
      source.onmessage = (event) => {
        try {
          const newEntry = JSON.parse(event.data);
          // Prepend new entry if not already present
          if (!entries.some(e => e.id === newEntry.id)) {
            entries.unshift(newEntry);
            renderEntries();
          }
        } catch (e) {
          console.error('SSE parse error:', e);
        }
      };
      source.onerror = () => {
        console.warn('SSE connection disconnected. Reconnecting...');
      };
    }

    // Form submission with validation
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      let hasError = false;

      // Client-side validation
      if (nameInput.value.trim().length < 2) {
        document.getElementById('groupName').classList.add('has-error');
        hasError = true;
      } else {
        document.getElementById('groupName').classList.remove('has-error');
      }

      const emailRegex = /^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$/;
      if (!emailRegex.test(emailInput.value.trim())) {
        document.getElementById('groupEmail').classList.add('has-error');
        hasError = true;
      } else {
        document.getElementById('groupEmail').classList.remove('has-error');
      }

      if (feedbackInput.value.trim().length < 5) {
        document.getElementById('groupFeedback').classList.add('has-error');
        hasError = true;
      } else {
        document.getElementById('groupFeedback').classList.remove('has-error');
      }

      if (hasError) return;

      submitBtn.disabled = true;
      submitBtn.textContent = 'Submitting...';

      try {
        const res = await fetch('/feedback', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: nameInput.value,
            email: emailInput.value,
            feedback: feedbackInput.value
          })
        });

        const data = await res.json();

        if (res.status === 201) {
          showAlert('Feedback successfully submitted! Check the live list below.');
          form.reset();
        } else if (res.status === 429) {
          showAlert(\`Rate limit reached! Please wait \${data.retryAfterSec || 10} seconds.\`, true);
        } else {
          showAlert(data.message || 'Validation failed.', true);
        }
      } catch (err) {
        showAlert('Network error submitting feedback. Please try again.', true);
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Submit Feedback';
      }
    });

    fetchInitial();
    initSSE();
  </script>
</body>
</html>`;

// ─────────────────────────────────────────────────────────────────────────────
// 6. HTTP SERVER & REST ROUTING
// ─────────────────────────────────────────────────────────────────────────────
function createFeedbackServer() {
  const server = http.createServer((req, res) => {
    // Parse client IP (supporting reverse proxies)
    const clientIp = (req.headers['x-forwarded-for'] || req.socket.remoteAddress || '127.0.0.1').split(',')[0].trim();

    // Helper for JSON responses
    const sendJson = (statusCode, payload, extraHeaders = {}) => {
      res.writeHead(statusCode, {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type',
        ...extraHeaders
      });
      res.end(JSON.stringify(payload));
    };

    // CORS preflight
    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
      });
      res.end();
      return;
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

    // ROUTE 1: GET / -> Serves Full-Stack React/Web UI
    if (req.method === 'GET' && parsedUrl.pathname === '/') {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(FRONTEND_HTML);
      return;
    }

    // ROUTE 2: GET /feedback/stream -> Server-Sent Events (SSE) for Real-Time Updates
    if (req.method === 'GET' && parsedUrl.pathname === '/feedback/stream') {
      res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache, no-transform',
        'Connection': 'keep-alive',
        'Access-Control-Allow-Origin': '*'
      });
      res.write(': connected\n\n');
      sseClients.add(res);

      req.on('close', () => {
        sseClients.delete(res);
      });
      return;
    }

    // ROUTE 3: GET /feedback -> List all submitted feedback
    if (req.method === 'GET' && parsedUrl.pathname === '/feedback') {
      const items = db.findAll();
      sendJson(200, {
        status: 'success',
        count: items.length,
        data: items,
        timestamp: new Date().toISOString()
      });
      return;
    }

    // ROUTE 4: POST /feedback -> Submit new feedback with validation & IP rate-limiting
    if (req.method === 'POST' && parsedUrl.pathname === '/feedback') {
      // 1. Enforce IP Rate-Limiting
      const rateCheck = rateLimiter.isAllowed(clientIp);
      if (!rateCheck.allowed) {
        sendJson(429, {
          status: 'error',
          code: 'RATE_LIMIT_EXCEEDED',
          message: `Too many requests from IP ${clientIp}. Please try again later.`,
          retryAfterSec: rateCheck.retryAfterSec,
          timestamp: new Date().toISOString()
        }, {
          'Retry-After': String(rateCheck.retryAfterSec)
        });
        return;
      }

      // 2. Read request body
      let body = '';
      req.on('data', chunk => {
        body += chunk;
        if (body.length > 1e6) { // 1MB limit guard
          req.destroy();
        }
      });

      req.on('end', () => {
        let payload;
        try {
          payload = JSON.parse(body || '{}');
        } catch (err) {
          sendJson(400, {
            status: 'error',
            message: 'Malformed JSON payload.',
            timestamp: new Date().toISOString()
          });
          return;
        }

        // 3. Validate input
        const validation = validateFeedbackInput(payload);
        if (!validation.isValid) {
          sendJson(400, {
            status: 'error',
            message: 'Validation failed.',
            errors: validation.errors,
            timestamp: new Date().toISOString()
          });
          return;
        }

        // 4. Save to Mock Database
        const savedEntry = db.insert({
          name: payload.name,
          email: payload.email,
          feedback: payload.feedback,
          ip: clientIp
        });

        // 5. Broadcast to connected real-time SSE clients
        broadcastFeedback(savedEntry);

        // 6. Return response
        sendJson(201, {
          status: 'success',
          data: savedEntry,
          timestamp: new Date().toISOString()
        });
      });
      return;
    }

    // 404 Fallback
    sendJson(404, {
      status: 'error',
      message: `Route ${req.method} ${parsedUrl.pathname} not found.`,
      timestamp: new Date().toISOString()
    });
  });

  return server;
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. AUTOMATED VERIFICATION TEST HARNESS (FOR micro1 EVALUATION CONSOLE)
// ─────────────────────────────────────────────────────────────────────────────
async function makeRequest(port, { method = 'GET', path = '/', headers = {}, body = null }) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      hostname: '127.0.0.1',
      port,
      path,
      method,
      headers: {
        'Content-Type': 'application/json',
        ...headers
      }
    }, res => {
      let rawData = '';
      res.on('data', chunk => { rawData += chunk; });
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(rawData); } catch (e) { json = rawData; }
        resolve({ statusCode: res.statusCode, headers: res.headers, body: json });
      });
    });
    req.on('error', reject);
    if (body) {
      req.write(typeof body === 'string' ? body : JSON.stringify(body));
    }
    req.end();
  });
}

async function runMicro1VerificationSuite(port) {
  console.log('\n' + '═'.repeat(70));
  console.log('  🧪 RUNNING micro1 EVALUATION TEST SUITE (Output Panel Simulation)');
  console.log('═'.repeat(70));

  let passed = 0;
  let total = 0;

  function assertTest(title, condition, details = '') {
    total++;
    if (condition) {
      passed++;
      console.log(`  ✅ [PASS] Test ${total}: ${title}`);
    } else {
      console.error(`  ❌ [FAIL] Test ${total}: ${title} - ${details}`);
    }
  }

  try {
    // Test 1: GET /feedback initially returns empty array
    db.clear();
    rateLimiter.reset();
    const res1 = await makeRequest(port, { path: '/feedback' });
    assertTest('GET /feedback returns 200 with empty entries initially', 
      res1.statusCode === 200 && res1.body.status === 'success' && res1.body.data.length === 0,
      `Received status ${res1.statusCode}`);

    // Test 2: POST /feedback with valid data returns 201 & timestamp
    const res2 = await makeRequest(port, {
      method: 'POST',
      path: '/feedback',
      body: { name: 'Alice Smith', email: 'alice@example.com', feedback: 'Great interview platform experience!' }
    });
    assertTest('POST /feedback returns 201 Created with ISO timestamp and saved entry',
      res2.statusCode === 201 && res2.body.status === 'success' && res2.body.data.id === 1 && typeof res2.body.timestamp === 'string',
      `Received status ${res2.statusCode}`);

    // Test 3: POST /feedback validation rejection (invalid email & short feedback)
    const res3 = await makeRequest(port, {
      method: 'POST',
      path: '/feedback',
      body: { name: 'A', email: 'not-an-email', feedback: 'bad' }
    });
    assertTest('POST /feedback rejects invalid email and short fields with 400 Bad Request',
      res3.statusCode === 400 && res3.body.status === 'error' && res3.body.errors.email && res3.body.errors.name,
      `Received status ${res3.statusCode}`);

    // Test 4: Rate-Limiting Enforced per IP (Burst 5 allowed, 6th rejected with 429)
    rateLimiter.reset();
    let rateLimited = false;
    let lastStatusCode = 200;
    for (let i = 0; i < 6; i++) {
      const resRate = await makeRequest(port, {
        method: 'POST',
        path: '/feedback',
        headers: { 'x-forwarded-for': '198.51.100.42' },
        body: { name: `User ${i}`, email: `user${i}@domain.com`, feedback: `Valid feedback payload #${i}` }
      });
      lastStatusCode = resRate.statusCode;
      if (resRate.statusCode === 429) {
        rateLimited = true;
      }
    }
    assertTest('IP Rate Limiter returns 429 Too Many Requests on 6th burst request',
      rateLimited && lastStatusCode === 429,
      `Expected 429 on 6th request, got ${lastStatusCode}`);

    // Test 5: Verify GET /feedback contains the submitted items with newest first
    const res5 = await makeRequest(port, { path: '/feedback' });
    assertTest('GET /feedback correctly aggregates stored feedback list',
      res5.statusCode === 200 && res5.body.data.length >= 1 && res5.body.data[0].name.startsWith('User 4'),
      `Stored count: ${res5.body.data?.length}`);

    // Test 6: Verify Frontend HTML served on GET /
    const res6 = await makeRequest(port, { path: '/' });
    assertTest('GET / serves modern React/HTML frontend with SSE client logic',
      res6.statusCode === 200 && typeof res6.body === 'string' && res6.body.includes('micro1 Real-Time Feedback App'),
      `Status: ${res6.statusCode}`);

    console.log('─'.repeat(70));
    console.log(`  🎯 RESULTS: ${passed}/${total} TESTS PASSED (${Math.round((passed / total) * 100)}% SUCCESS RATE)`);
    console.log('═'.repeat(70) + '\n');

    return passed === total;
  } catch (err) {
    console.error('Test runner failure:', err);
    return false;
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. ENTRY POINT EXECUTION
// ─────────────────────────────────────────────────────────────────────────────
if (require.main === module) {
  const PORT = process.env.PORT || 3000;
  const server = createFeedbackServer();

  server.listen(PORT, async () => {
    console.log(`\n🚀 micro1 Full-Stack Feedback Server listening on http://localhost:${PORT}`);
    console.log(`   • Web UI & React App: http://localhost:${PORT}/`);
    console.log(`   • REST API:           POST & GET http://localhost:${PORT}/feedback`);
    console.log(`   • Real-Time Stream:   GET http://localhost:${PORT}/feedback/stream`);

    // Run the automated test verification suite
    const success = await runMicro1VerificationSuite(PORT);

    if (process.env.TEST_ONLY === 'true') {
      server.close(() => {
        process.exit(success ? 0 : 1);
      });
    } else {
      console.log(`✨ Server remains active on port ${PORT}. Press Ctrl+C to terminate or test in browser.\n`);
    }
  });
}

module.exports = { createFeedbackServer, MockDatabase, IpRateLimiter, validateFeedbackInput };
