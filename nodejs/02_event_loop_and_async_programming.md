# Part 2: Event Loop, Microtasks, Timers & Asynchronous Patterns (Q26 - Q50)

---

### Q26: What are the distinct phases of the Node.js Event Loop and in what order do they execute?
**Answer:**
The Libuv Event Loop executes in 6 main sequential phases. Between each phase, Node.js exhausts the **Microtask Queues** (`process.nextTick` followed by Promise microtasks).

```
   ┌───────────────────────────┐
┌─>│          timers           │  - setTimeout(), setInterval()
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │     pending callbacks     │  - I/O callbacks deferred to next loop iteration
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │       idle, prepare       │  - Internal Libuv use only
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │           poll            │  - Retrieve new I/O events; execute I/O callbacks
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │           check           │  - setImmediate() callbacks
│  └─────────────┬─────────────┘
│  ┌─────────────┴─────────────┐
│  │      close callbacks      │  - socket.on('close', ...), handle destructions
└──┴─────────────┬─────────────┘
```

1. **Timers Phase**: Executes callbacks scheduled by `setTimeout()` and `setInterval()` whose threshold elapsed.
2. **Pending Callbacks Phase**: Executes I/O callbacks deferred from the previous loop iteration (e.g., specific OS TCP errors like `ECONNREFUSED`).
3. **Idle, Prepare Phase**: Used strictly internally by Libuv.
4. **Poll Phase**:
   - Calculates how long it should block and wait for I/O.
   - Polls for new I/O events (`epoll_wait` / `kevent`).
   - Executes callbacks for incoming network packets, data streams, and file completion.
5. **Check Phase**: Executes `setImmediate()` callbacks specifically.
6. **Close Callbacks Phase**: Executes cleanup callbacks (e.g., `socket.on('close', ...)`).

---

### Q27: How do Microtasks (`process.nextTick`, `Promise.then`, `queueMicrotask`) interact with Event Loop phases?
**Answer:**
Microtasks do **not** belong to Libuv; they are managed by the V8 engine and Node.js core.

**Priority Order:**
1. **`process.nextTick` Queue (Highest Priority)**: Drained completely before any Promise microtask runs.
2. **Promise / `queueMicrotask` Queue**: Drained immediately after `process.nextTick`.

**Execution Rule (Node.js v11+)**:
Microtasks are processed **immediately after every single callback finishes** in any phase (Timers, Poll, Check, etc.), matching the browser standard HTML5 event loop specification.

```javascript
setTimeout(() => {
  console.log('Timer 1');
  Promise.resolve().then(() => console.log('Promise inside Timer 1'));
  process.nextTick(() => console.log('NextTick inside Timer 1'));
}, 0);

setTimeout(() => {
  console.log('Timer 2');
}, 0);

// Output:
// Timer 1
// NextTick inside Timer 1
// Promise inside Timer 1
// Timer 2
```

---

### Q28: What is Event Loop Starvation and how can recursive `process.nextTick` cause a Denial of Service (DoS)?
**Answer:**
Because the `process.nextTick` queue is processed recursively until empty, scheduling `process.nextTick` inside another `process.nextTick` creates an infinite microtask loop that **completely blocks the Event Loop from ever moving to the Poll or Timers phase**.

```javascript
// 🚨 CRITICAL BUG: Completely freezes the HTTP server!
function recursiveNextTick() {
  process.nextTick(recursiveNextTick);
}
recursiveNextTick();

// The server below will NEVER accept incoming connections:
http.createServer((req, res) => {
  res.end('Hello');
}).listen(3000);
```

**Fix:** Use `setImmediate()` to yield control back to the Event Loop, allowing I/O polling to occur between iterations:
```javascript
// ✅ SAFE: Yields to I/O poll phase between iterations
function safeChunkProcessing() {
  // do chunk of work
  setImmediate(safeChunkProcessing);
}
```

---

### Q29: What determines whether `setTimeout(fn, 0)` or `setImmediate(fn)` executes first when called in the main module?
**Answer:**
When called in the main module (outside an I/O callback), the execution order is **non-deterministic** due to CPU scheduling and process startup jitter.

```javascript
setTimeout(() => console.log('timeout'), 0);
setImmediate(() => console.log('immediate'));
// Output can be:
// timeout -> immediate OR immediate -> timeout
```

**Reason:**
- `setTimeout(fn, 0)` is normalized internally to `setTimeout(fn, 1)`.
- If the Event Loop enters the Timers phase before 1ms of clock time has elapsed, the timer is not ready, so it skips to the Poll/Check phase (`setImmediate` runs first).
- If system jitter causes startup to take >1ms, the timer is ready (`setTimeout` runs first).

**Deterministic Case inside I/O Callback:**
Inside an I/O cycle (e.g. `fs.readFile`), `setImmediate` is **guaranteed** to run first because the callback is executed in the Poll phase, and the Check phase immediately follows Poll.

```javascript
import fs from 'node:fs';

fs.readFile('package.json', () => {
  setTimeout(() => console.log('timeout'), 0);
  setImmediate(() => console.log('immediate'));
});

// ALWAYS outputs:
// immediate
// timeout
```

---

### Q30: What is the difference between `unref()` and `ref()` on Timers and Sockets?
**Answer:**
By default, active timers (`setTimeout`, `setInterval`) and open sockets are "referenced" handles that prevent the Node.js process from exiting.
- `timer.unref()`: Tells the Event Loop that this handle should **not** keep the process alive if no other active work remains.
- `timer.ref()`: Restores the default behavior, keeping the process alive.

```javascript
// Background telemetry ping that shouldn't block test suites or process exit
const pingTimer = setInterval(() => {
  sendMetricsHeartbeat();
}, 5000);

// Process will exit cleanly when web server closes, without hanging on this timer
pingTimer.unref();
```

---

### Q31: How does Node.js handle Unhandled Promise Rejections (`unhandledRejection`)?
**Answer:**
In modern Node.js (v15+), unhandled promise rejections have a default mode of `--unhandled-rejections=throw`, which crashes the process with a non-zero exit code (1) to prevent undefined application state.

```javascript
// Global Handler
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  // In production: Log structured error, flush telemetry, and initiate graceful shutdown
  process.exit(1);
});

// Triggering unhandled rejection:
Promise.reject(new Error('Fatal DB Connection Drop'));
```

---

### Q32: What is the `uncaughtExceptionMonitor` event and how does it differ from `uncaughtException`?
**Answer:**
- `uncaughtException`: Emitted when an uncaught JavaScript exception bubbles all the way back to the event loop. If a listener is attached, it prevents the default crash behavior (though running in a corrupted state is dangerous).
- `uncaughtExceptionMonitor`: Emitted **before** `uncaughtException` listeners are invoked. It does not alter the process exit behavior; it is strictly intended for observability, crash loggers, and APM tools (Datadog, New Relic, Sentry).

```javascript
process.on('uncaughtExceptionMonitor', (err, origin) => {
  // Safe place for APM agents to record the crash event
  metricsClient.increment('app.fatal_crashes');
  fs.writeSync(2, `FATAL CRASH [${origin}]: ${err.stack}
`);
});
```

---

### Q33: How does the `EventEmitter` class work under the hood, and how do you prevent Memory Leaks?
**Answer:**
`EventEmitter` stores listeners in an internal dictionary (`_events`) mapping event names to functions or arrays of functions.

**Memory Leak Warning:**
If more than 10 listeners are added to a single event on an emitter, Node.js prints:
`MaxListenersExceededWarning: Possible EventEmitter memory leak detected.`

**Production Best Practices:**
1. Explicitly clean up listeners using `.off()` or `AbortSignal`.
2. Use `EventEmitter.on(emitter, event, { signal })` with `AbortController`.
3. Tune limit if intentional: `emitter.setMaxListeners(50)`.

```javascript
import EventEmitter, { on } from 'node:events';

const emitter = new EventEmitter();
const ac = new AbortController();

// Modern auto-cleanup with AbortSignal
emitter.on('data', (payload) => {
  console.log('Received:', payload);
}, { signal: ac.signal });

// Later, remove all listeners bound to this signal in one call:
ac.abort();
```

---

### Q34: What is the difference between `EventEmitter.captureRejections` and standard error handling?
**Answer:**
By default, async listeners on `EventEmitter` that return a rejected Promise do not trigger the `'error'` event and result in an `unhandledRejection`.
Setting `captureRejections: true` automatically catches rejected Promises returned by event listeners and routes them to the `'error'` event.

```javascript
import EventEmitter from 'node:events';

const ee = new EventEmitter({ captureRejections: true });

ee.on('event', async () => {
  throw new Error('Async failure inside listener');
});

ee.on('error', (err) => {
  console.error('Caught via error event:', err.message);
});

ee.emit('event'); // Automatically triggers 'error' event!
```

---

### Q35: How does `Promise.all()`, `Promise.allSettled()`, `Promise.race()`, and `Promise.any()` behave under concurrency?
**Answer:**

| Method | Resolution Criteria | Rejection Criteria | Use Case |
|---|---|---|---|
| **`Promise.all`** | Resolves when **ALL** resolve (returns array) | Rejects immediately on **FIRST** rejection (Fail-fast) | Dependent concurrent tasks (fetch user + profile + permissions) |
| **`Promise.allSettled`** | Resolves when **ALL** settle (returns `{status, value/reason}`) | **Never rejects** | Bulk independent operations (sending 1,000 batch emails) |
| **`Promise.race`** | Settles as soon as the **FIRST** promise settles (resolve OR reject) | Settles as soon as first rejects | Timeout wrappers (`Promise.race([fetch(), timeout(5000)])`) |
| **`Promise.any`** | Resolves as soon as the **FIRST** promise resolves | Rejects only when **ALL** reject (`AggregateError`) | Redundant fallback mirrors (querying 3 replica DBs) |

```javascript
// High-Scale Resilient Fetch with Promise.any
const replicas = ['https://db-replica-1/data', 'https://db-replica-2/data'];
const data = await Promise.any(replicas.map(url => fetch(url).then(r => r.json())));
```

---

### Q36: How do you build an Async Concurrency Limiter (Semaphore/Worker Pool) without external libraries?
**Answer:**
Unbounded `Promise.all(urls.map(fetch))` firing 100,000 requests simultaneously will exhaust sockets, memory, and remote rate limits.

```javascript
// Production-grade async pool / concurrency limiter
async function asyncPool(concurrencyLimit, items, asyncFn) {
  const results = [];
  const executing = new Set();

  for (const [index, item] of items.entries()) {
    const p = Promise.resolve().then(() => asyncFn(item, index));
    results.push(p);
    executing.add(p);

    const clean = () => executing.delete(p);
    p.then(clean, clean);

    if (executing.size >= concurrencyLimit) {
      await Promise.race(executing); // Wait for the fastest slot to free up
    }
  }

  return Promise.all(results);
}

// Example usage: Process 10,000 tasks with exactly 5 concurrent workers
const tasks = Array.from({ length: 100 }, (_, i) => i);
const results = await asyncPool(5, tasks, async (id) => {
  await new Promise(r => setTimeout(r, 100));
  return `Done ${id}`;
});
```

---

### Q37: How do you implement robust timeout and cancellation using `AbortController` and `AbortSignal`?
**Answer:**
Modern Node.js standardizes request cancellation across `fetch`, `fs`, `child_process`, `crypto`, and `http` using `AbortController`.

```javascript
import http from 'node:http';

async function fetchWithTimeout(url, timeoutMs = 3000) {
  // AbortSignal.timeout creates an auto-aborting signal
  const signal = AbortSignal.timeout(timeoutMs);

  try {
    const response = await fetch(url, { signal });
    return await response.json();
  } catch (err) {
    if (err.name === 'TimeoutError') {
      console.error(`Request timed out after ${timeoutMs}ms`);
    } else if (err.name === 'AbortError') {
      console.error('Request was aborted by caller');
    }
    throw err;
  }
}
```

---

### Q38: What is `events.once()` and how does it convert EventEmitters to Promises?
**Answer:**
`events.once(emitter, eventName, [options])` returns a Promise that resolves the next time the given event is emitted, with automatic error propagation and cancellation support via `AbortSignal`.

```javascript
import { once, EventEmitter } from 'node:events';

const server = new EventEmitter();

async function waitForBoot() {
  const ac = new AbortController();
  setTimeout(() => ac.abort(), 5000); // 5s timeout

  try {
    const [status] = await once(server, 'ready', { signal: ac.signal });
    console.log('Server booted with status:', status);
  } catch (err) {
    console.error('Boot timed out or failed:', err);
  }
}
```

---

### Q39: What is `util.promisify` and how do custom promisified functions work (`util.promisify.custom`)?
**Answer:**
`util.promisify` converts standard Node.js error-first callback functions `(err, value) => void` into Promise-returning functions.

```javascript
import util from 'node:util';
import fs from 'node:fs';

// Standard promisify
const stat = util.promisify(fs.stat);
const stats = await stat('./package.json');

// Custom Symbol.for('nodejs.util.promisify.custom')
function legacyComplexFunction(a, b, callback) {
  // returns 2 values: callback(err, res1, res2)
  setTimeout(() => callback(null, a * 2, b * 2), 10);
}

legacyComplexFunction[util.promisify.custom] = (a, b) => {
  return new Promise((resolve) => {
    legacyComplexFunction(a, b, (err, r1, r2) => resolve({ r1, r2 }));
  });
};

const promisified = util.promisify(legacyComplexFunction);
const result = await promisified(2, 4); // { r1: 4, r2: 8 }
```

---

### Q40: What is the "Callback Hell" and the "Pyramid of Doom", and how is it mitigated across Node.js evolution?
**Answer:**
Callback hell occurs when deeply nested asynchronous callbacks create unreadable, error-prone, tightly coupled code with difficult error propagation.

**Evolution:**
1. **Callbacks (Node.js v0.10 - v4)**: Deep nesting, manual error checks at every step (`if (err) return cb(err)`).
2. **Promises / Bluebird (Node.js v4 - v7)**: Flat chains (`.then().catch()`), centralized error handling.
3. **Async / Await (Node.js v7.6+)**: Synchronous-looking syntax, native `try / catch`, sequential flow.
4. **Top-Level Await (Node.js v14.8+ ESM)**: Eliminates async boilerplate wrappers in module entrypoints.

---

### Q41: How do Async Iterators (`for await...of`) work in Node.js streams and generators?
**Answer:**
Async iterators implement the `[Symbol.asyncIterator]` protocol, yielding Promises that resolve to `{ value, done }`.

```javascript
import fs from 'node:fs';
import readline from 'node:readline';

// Processing a 50GB log file line-by-line with O(1) memory usage
async function processLargeLog(filePath) {
  const fileStream = fs.createReadStream(filePath);
  const rl = readline.createInterface({
    input: fileStream,
    crlfDelay: Infinity
  });

  let errorCount = 0;
  for await (const line of rl) {
    if (line.includes('[ERROR]')) {
      errorCount++;
    }
  }
  console.log(`Total Errors found: ${errorCount}`);
}
```

---

### Q42: What is the cost of `async`/`await` vs raw Promises in terms of memory and stack traces?
**Answer:**
- In early V8 versions, `async/await` allocated extra microtask promise objects.
- In modern V8 (v7.2+ / Node 12+), **zero-cost async stack traces** were implemented:
  - V8 reconstructs async stack traces across `await` points using the `PromiseReaction` job context with almost zero performance penalty.
  - `async/await` is now often faster and uses less memory than long `.then()` callback closures which retain surrounding scope variables in memory.

---

### Q43: What happens when an error is thrown inside a `setTimeout` callback?
**Answer:**
Because the `setTimeout` callback executes in a separate tick of the Event Loop (Timers phase), a `try / catch` block surrounding the `setTimeout` call **cannot catch** errors thrown inside the callback.

```javascript
// ❌ WRONG: try/catch does NOT catch errors in async timer callbacks!
try {
  setTimeout(() => {
    throw new Error('Boom!'); // Crashes process with uncaughtException!
  }, 100);
} catch (err) {
  console.log('Will never be called');
}

// ✅ CORRECT: Handle errors inside the callback or use promisified timer
import { setTimeout as sleep } from 'node:timers/promises';

try {
  await sleep(100);
  throw new Error('Boom!'); // Caught cleanly by try/catch!
} catch (err) {
  console.log('Caught cleanly:', err.message);
}
```

---

### Q44: What are `queueMicrotask()` and `process.nextTick()` and how do they differ?
**Answer:**
- `queueMicrotask(fn)`: Standardized web API (available in Browsers, Deno, Bun, Node.js). Enqueues a microtask onto the standard V8 microtask queue (same queue as `Promise.resolve().then(fn)`).
- `process.nextTick(fn)`: Node.js proprietary API. Enqueues a callback onto the **NextTick Queue**, which executes **before** the standard microtask queue.

---

### Q45: What is the "Zalgo" problem and why must asynchronous APIs always be consistently asynchronous?
**Answer:**
"Releasing Zalgo" refers to an API that is **sometimes synchronous and sometimes asynchronous** depending on conditions (e.g., cache hits vs. cache misses). This introduces non-deterministic race conditions.

```javascript
// 🚨 DANGEROUS: Inconsistent API (Releases Zalgo)
const cache = new Map();
function getUserData(userId, callback) {
  if (cache.has(userId)) {
    callback(null, cache.get(userId)); // SYNCHRONOUS execution!
  } else {
    db.query('SELECT * FROM users WHERE id = ?', [userId], (err, user) => {
      cache.set(userId, user);
      callback(err, user); // ASYNCHRONOUS execution!
    });
  }
}

// ✅ FIXED: Guarantee consistent asynchrony using process.nextTick or queueMicrotask
function getSafeUserData(userId, callback) {
  if (cache.has(userId)) {
    process.nextTick(() => callback(null, cache.get(userId)));
  } else {
    db.query('SELECT * FROM users WHERE id = ?', [userId], (err, user) => {
      cache.set(userId, user);
      callback(err, user);
    });
  }
}
```

---

### Q46: How does Node.js handle Timer Drift and Timer Coalescing?
**Answer:**
- `setTimeout(fn, 1000)` does **not** guarantee exact execution at 1000.000ms. It guarantees execution **after at least** 1000ms.
- **Timer Drift**: Occurs when CPU-heavy synchronous execution or long I/O callbacks delay the Event Loop from reaching the Timers phase.
- **Timer Coalescing**: Libuv groups timers with identical expiry times into a single binary min-heap / timer wheel to minimize timer registration syscalls.

---

### Q47: How do you implement a robust Circuit Breaker pattern in Node.js?
**Answer:**
The Circuit Breaker pattern prevents cascading failures when downstream microservices fail.

```javascript
class CircuitBreaker {
  constructor(action, { failureThreshold = 5, cooldownPeriodMs = 10000 }) {
    this.action = action;
    this.failureThreshold = failureThreshold;
    this.cooldownPeriodMs = cooldownPeriodMs;
    this.failureCount = 0;
    this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
    this.nextAttempt = Date.now();
  }

  async exec(...args) {
    if (this.state === 'OPEN') {
      if (Date.now() > this.nextAttempt) {
        this.state = 'HALF_OPEN';
      } else {
        throw new Error('CircuitBreaker is OPEN - Request short-circuited');
      }
    }

    try {
      const result = await this.action(...args);
      this.onSuccess();
      return result;
    } catch (err) {
      this.onFailure();
      throw err;
    }
  }

  onSuccess() {
    this.failureCount = 0;
    this.state = 'CLOSED';
  }

  onFailure() {
    this.failureCount++;
    if (this.failureCount >= this.failureThreshold || this.state === 'HALF_OPEN') {
      this.state = 'OPEN';
      this.nextAttempt = Date.now() + this.cooldownPeriodMs;
      console.warn(`Circuit Breaker tripped to OPEN until ${new Date(this.nextAttempt).toISOString()}`);
    }
  }
}
```

---

### Q48: What is the difference between `Promise.resolve().then(...)` and `new Promise(executor)` execution?
**Answer:**
The `executor` function passed to `new Promise((resolve, reject) => { ... })` runs **synchronously and immediately** during construction. Only the `.then()` / `.catch()` callbacks are deferred to the Microtask queue.

```javascript
console.log('1. Start');

new Promise((resolve) => {
  console.log('2. Inside Promise Executor (Synchronous!)');
  resolve();
}).then(() => {
  console.log('4. Inside Promise.then (Microtask)');
});

console.log('3. End');

// Output order: 1 -> 2 -> 3 -> 4
```

---

### Q49: How do you debug Event Loop Latency and Lag in a production Node.js service?
**Answer:**
Event loop delay (lag) is the time difference between when a timer is scheduled to run and when it actually executes.

```javascript
import { monitorEventLoopDelay } from 'node:perf_hooks';

// Resolution in nanoseconds
const histogram = monitorEventLoopDelay({ resolution: 20 });
histogram.enable();

setInterval(() => {
  const p50 = (histogram.percentile(50) / 1_000_000).toFixed(2);
  const p99 = (histogram.percentile(99) / 1_000_000).toFixed(2);
  const max = (histogram.max / 1_000_000).toFixed(2);

  console.log(`[EventLoopLag] p50: ${p50}ms | p99: ${p99}ms | max: ${max}ms`);
  histogram.reset();
}, 5000);
```
If `p99 > 50ms`, the application is executing blocking synchronous CPU tasks or processing large JSON payloads on the main thread.

---

### Q50: What are Top-Level Await gotchas and how does it affect dependency graph execution?
**Answer:**
- Top-Level Await allows `await` at the top level of ES modules.
- **Gotcha**: If module `A` imports module `B`, and `B` contains a top-level `await` that halts (e.g. slow database connection), module `A` and all downstream dependents are **blocked from executing** until `B` resolves.
- **Circular Deadlock**: Circular imports containing top-level `await` can cause unresolvable dependency deadlocks.

```javascript
// db.mjs
export const connection = await createDbPool(); // Blocks all importing modules until connected

// server.mjs
import { connection } from './db.mjs'; // Will not execute until createDbPool() finishes
```
