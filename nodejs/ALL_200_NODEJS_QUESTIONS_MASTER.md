# Node.js Master Production Engineering & Interview Guide (225 Questions)

> An exhaustive, production-grade knowledge base covering Node.js core internals (V8, Libuv), Event Loop phases, Streams & Buffers, Concurrency (Workers, Child Processes, Clustering), Memory Management & GC, High-Scale Networking, Databases & Advanced Caching Architectures (Write-Through, Write-Behind, SWR, CDC Zero-Miss, Bloom Filters, XFetch, LRU/LFU/ARC, Redis, Kafka), Security, Observability (OpenTelemetry, Pino), and modern Node.js features (Node 18 - 22+).

---

## Table of Contents

- **[Part 1: Node.js Core Architecture, V8 Engine & Libuv Internals (Q1 - Q25)](./01_core_architecture_and_internals.md)** (25 Questions)
- **[Part 2: Event Loop, Microtasks, Timers & Asynchronous Patterns (Q26 - Q50)](./02_event_loop_and_async_programming.md)** (25 Questions)
- **[Part 3: Buffers, Streams, File I/O & Backpressure (Q51 - Q75)](./03_streams_buffers_and_io.md)** (25 Questions)
- **[Part 4: Concurrency, Worker Threads, Child Processes & Cluster (Q76 - Q100)](./04_concurrency_workers_and_clustering.md)** (25 Questions)
- **[Part 5: Memory Management, V8 Garbage Collection & Leak Profiling (Q101 - Q125)](./05_memory_management_and_gc.md)** (25 Questions)
- **[Part 6: High-Scale Networking, HTTP/2, HTTP/3 & WebSockets (Q126 - Q145)](./06_networking_web_and_realtime.md)** (20 Questions)
- **[Part 7: Databases, Caching, Storage & Messaging Architecture (Q146 - Q165)](./07_databases_caching_and_messaging.md)** (20 Questions)
- **[Part 8: Security, Cryptography, Authentication & Hardening (Q166 - Q185)](./08_security_auth_and_cryptography.md)** (20 Questions)
- **[Part 9: Production Scale, Observability, Modern Node.js (v18 - v22+) & Best Practices (Q186 - Q210)](./09_production_scale_observability_and_modern_features.md)** (25 Questions)
- **[Part 10: Advanced Caching Architectures, Write Strategies & Eviction Policies](./10_caching_strategies_and_recency_mechanisms.md)** (15 Questions)

---

# Part 1: Node.js Core Architecture, V8 Engine & Libuv Internals (Q1 - Q25)

---

### Q1: What is the high-level architecture of Node.js, and how do its core components interact?
**Answer:**
Node.js is an open-source, cross-platform JavaScript runtime built on Google Chrome's **V8 JavaScript Engine**, **Libuv**, and core C/C++ libraries.

```
+-------------------------------------------------------------+
|                      JavaScript Code                        |
|   (Application Code, NPM Packages, Node.js Core JS APIs)    |
+-------------------------------------------------------------+
|               Node.js Bindings (Node-API / C++)             |
+------------------------------+------------------------------+
|       V8 JavaScript Engine   |            Libuv             |
|  - JIT Compilation (Ignition |  - Event Loop                |
|    & TurboFan)               |  - Thread Pool (POSIX/Win32) |
|  - Memory & Garbage Collector|  - Non-blocking I/O Polling  |
+------------------------------+------------------------------+
|     External C/C++ Libraries (OpenSSL, zlib, c-ares, llhttp)|
+-------------------------------------------------------------+
|                      Operating System                       |
|           (epoll / kqueue / IOCP / event ports)             |
+-------------------------------------------------------------+
```

1. **V8 Engine**: Parses, optimizes, and executes JavaScript into native machine code using Ignition (interpreter) and TurboFan (optimizing compiler).
2. **Libuv**: An asynchronous I/O library written in C. It provides the event loop, thread pool (default 4 threads for file I/O, DNS lookup, crypto), and platform abstraction layer over OS event demultiplexers (`epoll` on Linux, `kqueue` on macOS/BSD, `IOCP` on Windows).
3. **C++ Bindings & Addons**: Exposes low-level OS operations, crypto primitives (OpenSSL), decompression (zlib), and parser (llhttp) to the JS layer.
4. **Node.js Core Modules**: Standard libraries (`fs`, `http`, `stream`, `cluster`, `crypto`, `worker_threads`) written in JS and C++.

---

### Q2: How does the V8 Engine execute JavaScript code, and what are Ignition and TurboFan?
**Answer:**
V8 uses a dual-engine compilation pipeline:
1. **Parser & AST Generator**: Converts source code into an Abstract Syntax Tree (AST) and Scope analysis.
2. **Ignition (Bytecode Interpreter)**: Compiles the AST into compact bytecode. Bytecode starts executing immediately with low startup latency and minimal memory overhead.
3. **Profiler & Feedback Vector (Inline Caches)**: As bytecode runs, V8 gathers runtime type feedback (e.g., whether a function is consistently called with integers or objects of the same shape).
4. **TurboFan (Optimizing JIT Compiler)**: Frequently executed "hot" functions are fed into TurboFan along with type feedback. TurboFan emits highly optimized native machine code.
5. **Deoptimization (Deopt)**: If runtime assumptions are violated (e.g., passing a string to a function that TurboFan compiled assuming numbers), V8 bails out, discards the machine code, and falls back to Ignition bytecode.

```javascript
// Monomorphic callsite - V8 optimizes aggressively
function calculateTotal(price, tax) {
  return price * (1 + tax);
}

for (let i = 0; i < 1_000_000; i++) {
  calculateTotal(100, 0.08); // TurboFan optimizes with SMI/Double machine code
}

// Polymorphic / Megamorphic deoptimization
calculateTotal("100", "0.08"); // Forces deoptimization back to Ignition bytecode!
```

---

### Q3: What is Libuv, and why is it foundational to Node.js?
**Answer:**
Libuv is a multi-platform support library focusing on asynchronous I/O. It was originally developed specifically for Node.js to bridge the gap between different OS non-blocking primitives:
- **Linux**: `epoll`
- **macOS/FreeBSD**: `kqueue`
- **Windows**: `IOCP` (Input/Output Completion Ports)
- **Solaris/Illumos**: `event ports`

**Key Responsibilities:**
1. **Event Loop Orchestration**: Manages the cyclical execution of timers, I/O callbacks, idle/prepare, poll, check, and close callbacks.
2. **Thread Pool**: Maintains a dedicated worker pool for operations that cannot be performed asynchronously via OS kernel notification (e.g., standard filesystem I/O, blocking DNS lookups, compression, and CPU-intensive crypto tasks).
3. **Cross-Platform Abstractions**: Uniform APIs for TCP/UDP sockets, TTY, pipes, child processes, signals, and high-resolution timers (`uv_hrtime`).

---

### Q4: Which operations run on the Libuv Thread Pool vs. the OS Kernel (epoll/kqueue)?
**Answer:**

| Category | Operation | Handled By | Mechanism |
|---|---|---|---|
| **Network I/O** | TCP / UDP / HTTP / HTTPS client & server | **OS Kernel** | `epoll` / `kqueue` / `IOCP` (zero thread pool threads) |
| **Pipes & Sockets** | Unix Domain Sockets, Windows Named Pipes | **OS Kernel** | Non-blocking OS handles |
| **File I/O** | `fs.readFile`, `fs.writeFile`, `fs.stat`, etc. | **Libuv Thread Pool** | Synchronous OS syscalls executed on worker threads |
| **DNS Resolution** | `dns.lookup()` | **Libuv Thread Pool** | Calls blocking `getaddrinfo(3)` C function |
| **DNS Resolution** | `dns.resolve()`, `dns.resolve4()` | **OS Kernel (c-ares)** | Non-blocking network sockets via `c-ares` library |
| **Cryptography** | `crypto.pbkdf2`, `crypto.scrypt`, `crypto.randomBytes` | **Libuv Thread Pool** | OpenSSL CPU-bound computation on thread pool |
| **Compression** | `zlib.deflate`, `zlib.gzip`, `zlib.brotliCompress` | **Libuv Thread Pool** | Synchronous zlib compression jobs dispatched to pool |

---

### Q5: How do you configure and tune the Libuv Thread Pool (`UV_THREADPOOL_SIZE`), and what are the gotchas?
**Answer:**
By default, the Libuv thread pool size is **4**. In heavy I/O or crypto workloads, 4 threads can easily become saturated, blocking file operations and crypto hashing.

```bash
# Set before launching node (must be set in the shell environment before V8/Libuv initializes)
UV_THREADPOOL_SIZE=64 node server.js
```

**Code Gotcha:**
```javascript
// ❌ WRONG: Setting it in JavaScript after startup has NO EFFECT!
process.env.UV_THREADPOOL_SIZE = 64; // Libuv has already initialized its pool of 4!

// ✅ CORRECT: Must be set in environment before execution or passed via CLI:
// node --env-file=.env server.js (where UV_THREADPOOL_SIZE=32)
```

**Capacity & Tuning Rules:**
- Max value: 1024 (as of modern Libuv).
- Sizing rule: Set according to available CPU cores and concurrent filesystem/crypto throughput. If you have 16 CPU cores doing heavy crypto or disk reads, set `UV_THREADPOOL_SIZE=16` to `32`.

---

### Q6: What is the Node.js C++ Addon architecture, and what is Node-API (formerly N-API)?
**Answer:**
Node.js allows writing native C/C++ modules for raw performance, hardware interaction, or leveraging existing C++ libraries.
- **Legacy Addons (V8 C++ APIs directly)**: Bound directly to the internal V8 and Node.js C++ headers. Any Node.js or V8 major upgrade broke ABI (Application Binary Interface) compatibility, requiring recompilation.
- **Node-API (N-API)**: An ABI-stable C API for building native addons. Addons compiled against Node-API run on newer major versions of Node.js without recompilation.
- **node-addon-api**: C++ header-only wrapper around Node-API providing idiomatic C++ classes (`Napi::Object`, `Napi::Function`, `Napi::Promise`).

```cpp
// Example C++ Addon with node-addon-api
#include <napi.h>

Napi::Number FastAdd(const Napi::CallbackInfo& info) {
    Napi::Env env = info.Env();
    double a = info[0].As<Napi::Number>().DoubleValue();
    double b = info[1].As<Napi::Number>().DoubleValue();
    return Napi::Number::New(env, a + b);
}

Napi::Object Init(Napi::Env env, Napi::Object exports) {
    exports.Set("fastAdd", Napi::Function::New(env, FastAdd));
    return exports;
}

NODE_API_MODULE(addon, Init)
```

---

### Q7: How does Node.js handle Single-Threaded execution while achieving high concurrency?
**Answer:**
Node.js executes **user JavaScript in a single thread** (the Main Thread / Event Loop thread), but the runtime underneath is **heavily multi-threaded**.

1. **Non-Blocking System Calls**: When an incoming HTTP request or TCP packet arrives, the kernel notifies Node.js via OS event notification mechanisms (`epoll` on Linux). The main thread does not sleep waiting for data.
2. **Event Demultiplexing**: The Event Loop continuously collects I/O completion events from the kernel and invokes the corresponding JS callback functions.
3. **Delegation of Expensive Operations**: Disk I/O and crypto are dispatched to Libuv worker threads; CPU-intensive JS tasks can be delegated to `worker_threads`.
4. **No Thread-Switching Overhead**: Handles 100,000+ idle/concurrent sockets with minimal memory overhead compared to thread-per-request models (Apache, Tomcat) which allocate 1-2MB stack per thread.

---

### Q8: What are Hidden Classes (Shapes) and Inline Caching in V8?
**Answer:**
Because JavaScript is a dynamically typed language where object properties can be added or deleted at runtime, accessing properties like `obj.x` would normally require an expensive dictionary lookup.

1. **Hidden Classes (Shapes / Maps)**:
   - V8 assigns a hidden class (Map) to every object.
   - Objects initialized with the same properties in the exact same order share the same Hidden Class.
   - Adding a property creates a transition to a new Hidden Class with a specific memory offset for that property.

2. **Inline Caching (IC)**:
   - V8 caches the memory offset of property lookups at call sites.
   - **Monomorphic**: Always sees objects of 1 shape (Fastest, direct offset access).
   - **Polymorphic**: Sees 2-4 shapes (Branch table check).
   - **Megamorphic**: Sees >4 shapes (Degrades to slow hash table lookup).

```javascript
// ✅ BEST PRACTICE: Keep shapes monomorphic (initialize all keys in constructor)
class User {
  constructor(id, name) {
    this.id = id;
    this.name = name;
  }
}

// ❌ ANTI-PATTERN: Dynamically adding keys in different orders produces distinct hidden classes!
const u1 = {}; u1.a = 1; u1.b = 2; // Shape A -> Shape B
const u2 = {}; u2.b = 2; u2.a = 1; // Shape A -> Shape C (Different Shape!)
```

---

### Q9: What is the differences between CommonJS (CJS) and ECMAScript Modules (ESM) in Node.js?
**Answer:**

| Feature | CommonJS (`require`) | ES Modules (`import / export`) |
|---|---|---|
| **Loading Mechanism** | Synchronous, blocking disk read | Asynchronous resolution, parsing, linking, execution |
| **Top-Level Await** | No (requires IIFE) | Supported natively (`await fetch(...)`) |
| **Tree Shaking** | Difficult (dynamic `require()` allowed) | Native (static AST analysis) |
| **`__dirname` & `__filename`** | Available globally | Not available; use `import.meta.url` |
| **Cyclic Dependencies** | Returns partially exported object | Returns live bindings to exported variables |
| **Default in Node.js** | Default for `.js` files without `"type": "module"` | Default for `.mjs` or `"type": "module"` in `package.json` |

```javascript
// ESM Equivalent of __dirname and __filename
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
```

---

### Q10: How does Module Resolution and Caching work in Node.js `require()`?
**Answer:**
When `require(X)` is called:
1. **Core Module Check**: If `X` is a core module (`fs`, `path`), return immediately.
2. **File / Directory Path Resolution**:
   - If `X` begins with `./`, `/`, or `../`:
     - Try `X`, `X.js`, `X.json`, `X.node`.
     - If `X` is directory, check `package.json` `"main"` field, else `index.js`.
3. **`node_modules` Lookup**: Traverses up the directory tree recursively (`./node_modules`, `../node_modules`, `../../node_modules`) until root.
4. **Module Wrapper**: Code is wrapped in an IIFE:
   ```javascript
   (function(exports, require, module, __filename, __dirname) {
       // Module code here
   });
   ```
5. **Compilation & Caching**: The executed module's `module.exports` is cached in `require.cache[resolvedPath]`. Subsequent `require(X)` calls return the cached object directly without re-evaluating the file.

```javascript
// Busting require cache (useful in dynamic plugin reloading)
delete require.cache[require.resolve('./config.js')];
const freshConfig = require('./config.js');
```

---

### Q11: What is the Node.js Process Execution Model and Lifecycle?
**Answer:**
A Node.js process starts, executes the entry file, processes asynchronous tasks, and terminates when the Event Loop has no more active handles or requests.

```
Process Start -> Compile Entrypoint -> Execute Synchronous Code -> 
Event Loop Begins -> [Timers, Microtasks, I/O, Poll, Check] -> 
Are active handles/requests remaining?
   ├── YES -> Loop again
   └── NO  -> 'beforeExit' event -> 'exit' event -> Process Termination
```

- **Active Handles**: TCP servers, open sockets, timers (`setInterval`), file watchers.
- **Active Requests**: In-flight HTTP requests, pending filesystem reads.
- **`ref()` and `unref()`**: Control whether a handle keeps the process alive.

```javascript
const timer = setInterval(() => {
  console.log('Background heartbeat');
}, 1000);

// Allow process to exit naturally even if this timer is active
timer.unref();
```

---

### Q12: What is the purpose of `process.binding()`, `process._linkedBinding()`, and internal Node.js C++ bindings?
**Answer:**
- `process.binding()` was the legacy internal mechanism used by core Node.js JavaScript modules to access corresponding C++ classes (e.g., `process.binding('fs')`, `process.binding('crypto')`).
- Modern Node.js deprecated `process.binding()` in favor of **internalBinding** (accessible only within internal core JS modules) and `process._linkedBinding()` for custom embedded binaries.
- For userland applications, **Node-API (N-API)** is the standard and supported way to bind C++ code to Node.js.

---

### Q13: How does V8 handle Numbers (SMI vs. HeapNumber) and why does it matter for performance?
**Answer:**
JavaScript defines all numbers as 64-bit IEEE 754 floating-point values. However, allocating 64-bit heap objects for loop counters or IDs would destroy performance.

1. **SMI (Small Integer)**:
   - 31-bit signed integer (on 32-bit platforms) or 32-bit signed integer (on 64-bit platforms).
   - Stored **directly inside the pointer value** using pointer tagging (least significant bit set to `0`).
   - Requires zero heap allocation and zero GC overhead.
2. **HeapNumber**:
   - Double precision 64-bit float or integer outside the SMI range (e.g. `> 2^31 - 1`).
   - Allocated on the V8 heap as a separate boxed object.
3. **BigInt**:
   - Arbitrary precision integer allocated on the heap.

```javascript
// High performance: Operates purely on SMIs (no GC pressure)
let sum = 0;
for (let i = 0; i < 1_000_000; i++) {
  sum += (i & 0xFF); // SMI operations
}
```

---

### Q14: What is the difference between `fs.readFile`, `fs.createReadStream`, and `fs.readFileSync`?
**Answer:**

```javascript
import fs from 'node:fs';
import fsPromises from 'node:fs/promises';

// 1. fs.readFileSync: Blocking synchronous execution (Freezes Event Loop!)
// Use ONLY during initialization/bootstrap (e.g. reading config files on startup)
const config = fs.readFileSync('./config.json', 'utf8');

// 2. fs.readFile (or fsPromises.readFile): Asynchronous, loads ENTIRE file into memory buffer.
// High memory usage if file is large (1GB file = 1GB+ V8 heap consumed!)
const data = await fsPromises.readFile('./large_video.mp4');

// 3. fs.createReadStream: Streaming chunk by chunk (Default 64KB chunks).
// Constant O(1) memory consumption regardless of file size (100GB file processed in 64KB chunks).
const readStream = fs.createReadStream('./large_video.mp4');
readStream.on('data', (chunk) => {
  // process chunk
});
```

---

### Q15: What is the role of `llhttp` in Node.js?
**Answer:**
- `llhttp` is a fast, zero-copy HTTP/1.1 and HTTP/1.0 parser written in C (generated from TypeScript via `llparse`).
- It replaced the older `http-parser` in Node.js v12+.
- It parses HTTP request/response headers, chunked transfer encodings, and upgrades (WebSockets) with strict RFC compliance, defenses against HTTP Request Smuggling, and higher throughput.

---

### Q16: How does Node.js implement the global scope (`globalThis`, `global`) vs. the Browser `window`?
**Answer:**
- In the browser, the top-level scope is `window`, which is also the global execution context where `var` declarations attach.
- In Node.js:
  - Top-level variables in a file are **scoped to that module** due to the Module Wrapper function `(function(exports, require, module, __filename, __dirname) { ... })`.
  - The true global object is `global` (and standardized across JS environments as `globalThis`).
  - Core globals: `process`, `Buffer`, `console`, `setTimeout`, `setImmediate`, `queueMicrotask`, `fetch`, `AbortController`, `crypto`.

---

### Q17: What are Node.js conditional exports in `package.json`?
**Answer:**
Conditional exports allow a package to expose different entry points depending on how the package is imported or required:

```json
{
  "name": "my-enterprise-package",
  "type": "module",
  "exports": {
    ".": {
      "import": "./dist/index.mjs",
      "require": "./dist/index.cjs",
      "types": "./dist/index.d.ts",
      "default": "./dist/index.mjs"
    },
    "./feature": {
      "import": "./dist/feature.mjs",
      "require": "./dist/feature.cjs"
    }
  }
}
```
Prevents consumers from accessing private internal files (`import 'my-package/dist/internal.js'` throws an error) and solves dual CommonJS/ESM publishing.

---

### Q18: What is Node.js Sea (Single Executable Applications)?
**Answer:**
Introduced in Node.js v20+, **Single Executable Applications (SEA)** allows bundling a JavaScript application and the Node.js runtime into a single standalone binary executable for distribution without requiring Node.js to be installed on the target machine.

```json
// sea-config.json
{
  "main": "dist/app.js",
  "output": "sea-prep.blob",
  "disableExperimentalSEAWarning": true
}
```
```bash
# Build SEA binary on macOS/Linux
node --experimental-sea-config sea-config.json
cp $(command -v node) my-standalone-app
# Inject blob using postject
npx postject my-standalone-app NODE_SEA_BLOB sea-prep.blob     --sentinel-fuse NODE_SEA_FUSE_fce680ab2cc467b6e072b8b5df1996b2
```

---

### Q19: How does Node.js manage Signals (SIGINT, SIGTERM, SIGHUP, SIGKILL)?
**Answer:**
POSIX signals are used for OS process communication:
- **`SIGINT`**: Sent by terminal on `Ctrl+C`.
- **`SIGTERM`**: Sent by orchestrators (Kubernetes, Docker, systemd) to request graceful shutdown.
- **`SIGKILL`**: Force termination by kernel. Cannot be intercepted or caught.
- **`SIGHUP`**: Terminal closed or configuration reload trigger.

```javascript
// Graceful shutdown pattern for production
function setupGracefulShutdown(server, dbPool) {
  const shutdown = async (signal) => {
    console.log(`Received ${signal}. Initiating graceful shutdown...`);
    
    // Stop accepting new HTTP connections
    server.close(async () => {
      console.log('HTTP server closed.');
      try {
        await dbPool.end();
        console.log('Database connections closed.');
        process.exit(0);
      } catch (err) {
        console.error('Error during shutdown:', err);
        process.exit(1);
      }
    });

    // Force shutdown after 10s if connections fail to drain
    setTimeout(() => {
      console.error('Forced shutdown timeout exceeded.');
      process.exit(1);
    }, 10000).unref();
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}
```

---

### Q20: What are `process.hrtime` and `process.hrtime.bigint()`, and why shouldn't `Date.now()` be used for micro-benchmarking?
**Answer:**
- `Date.now()` uses the system wall-clock time, which is subject to NTP clock drift, clock corrections, and low resolution (1-15ms depending on OS).
- `process.hrtime.bigint()` provides a **monotonic clock** in nanoseconds (1 billionth of a second). It never goes backwards and is independent of system time changes.

```javascript
const start = process.hrtime.bigint();

// Execute performance-critical task
for (let i = 0; i < 1_000_000; i++) {
  Math.sqrt(i);
}

const end = process.hrtime.bigint();
const durationNs = end - start;
const durationMs = Number(durationNs) / 1_000_000;

console.log(`Execution time: ${durationMs.toFixed(3)} ms`);
```

---

### Q21: What is the difference between `setImmediate()` and `process.nextTick()`?
**Answer:**
- **`process.nextTick()`**: Schedules a callback on the **Microtask / NextTick Queue**. Executes immediately after the currently running JavaScript operation finishes, **before** the Event Loop proceeds to the next phase or executes any I/O.
- **`setImmediate()`**: Schedules a callback on the **Check Phase** of the Event Loop (Macrotask). Executes **after** I/O polling.

```javascript
setImmediate(() => console.log('1. setImmediate (Check Phase)'));
process.nextTick(() => console.log('2. process.nextTick (Microtask)'));
Promise.resolve().then(() => console.log('3. Promise.then (Microtask)'));
console.log('4. Synchronous');

// Output order:
// 4. Synchronous
// 2. process.nextTick (Microtask)
// 3. Promise.then (Microtask)
// 1. setImmediate (Check Phase)
```

---

### Q22: What are `AsyncLocalStorage` and `AsyncResource` in `node:async_hooks`?
**Answer:**
`AsyncLocalStorage` provides **continuation-local storage** (similar to thread-local storage in Java/C#) across asynchronous execution chains (Promises, callbacks, timers).

```javascript
import { AsyncLocalStorage } from 'node:async_hooks';
import http from 'node:http';
import { randomUUID } from 'node:crypto';

const asyncLocalStorage = new AsyncLocalStorage();

// Centralized logger that automatically logs the requestId of the active request
function log(message) {
  const store = asyncLocalStorage.getStore();
  const reqId = store?.requestId ?? 'NO-CONTEXT';
  console.log(`[${new Date().toISOString()}] [${reqId}] ${message}`);
}

const server = http.createServer((req, res) => {
  const context = { requestId: req.headers['x-request-id'] || randomUUID() };
  
  // Wrap entire request lifecycle in AsyncLocalStorage context
  asyncLocalStorage.run(context, async () => {
    log('Incoming HTTP request received');
    await doDatabaseQuery();
    log('Database query finished');
    res.end('OK');
  });
});

async function doDatabaseQuery() {
  await new Promise(r => setTimeout(r, 50));
  log('Querying users table');
}

server.listen(3000);
```

---

### Q23: What is the V8 Code Cache and how does Node.js utilize it to accelerate startup?
**Answer:**
Parsing and compiling large JavaScript codebases on every application restart incurs high CPU and latency penalties.
- **Code Cache**: V8 can serialize the compiled bytecode and AST metadata of a script to disk.
- On subsequent startups, Node.js loads the bytecode blob directly from disk, bypassing parsing and compilation.
- Used in tools like `esbuild`, `webpack`, and Node.js core via `--require` caches or `NODE_V8_COVERAGE`.

```javascript
import vm from 'node:vm';
import fs from 'node:fs';

const code = fs.readFileSync('large_bundle.js', 'utf8');
const script = new vm.Script(code, { produceCachedData: true });
const cachedData = script.cachedData; // Save this Buffer to disk

// Next startup:
const cachedScript = new vm.Script(code, { cachedData });
cachedScript.runInThisContext();
```

---

### Q24: What is the Node.js Permission Model (`--experimental-permission`)?
**Answer:**
Introduced in Node.js v20+, the Permission Model provides fine-grained security sandboxing by restricting access to system resources during runtime without external containers.

```bash
# Restrict filesystem access to /tmp, block child process execution and worker threads
node --experimental-permission      --allow-fs-read=/tmp      --allow-fs-write=/tmp      --deny-fs-write=/etc      --deny-child-process      --deny-worker app.js
```

Inside JavaScript:
```javascript
if (process.permission.has('fs.write', '/tmp/output.log')) {
  // Safe to write
}
```

---

### Q25: How does Node.js implement OpenSSL integration and what is FIPS mode?
**Answer:**
- Node.js statically compiles OpenSSL into its core binary to power `node:crypto`, `node:tls`, and `node:https`.
- **FIPS Mode (Federal Information Processing Standards)**: Enterprise/government compliance mode that enforces only cryptographically validated algorithms (e.g., AES, SHA-256, RSA-2048+) and disables unapproved algorithms like MD5 or DES.
- Enable FIPS mode:
  ```bash
  node --enable-fips app.js
  ```
- Or dynamically:
  ```javascript
  import crypto from 'node:crypto';
  crypto.setFips(true);
  console.log('FIPS Enabled:', crypto.getFips());
  ```


---

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


---

# Part 3: Buffers, Streams, File I/O & Backpressure (Q51 - Q75)

---

### Q51: What is a `Buffer` in Node.js, where is its memory allocated, and how does it relate to `Uint8Array`?
**Answer:**
A `Buffer` is a core Node.js data structure designed to represent raw binary data sequences.
- **Memory Allocation**: Buffers are allocated in **raw C++ memory outside the V8 V8 JavaScript Garbage-Collected Heap** (via `ArrayBuffer` backing stores or Libuv raw allocations).
- **TypedArray Inheritance**: In modern Node.js, `Buffer.prototype` inherits directly from JavaScript's native `Uint8Array`. Any `Buffer` instance is also a valid `Uint8Array`.
- **Buffer Pool**: For small allocations (<4KB / `Buffer.poolSize = 8192`), Node.js pre-allocates an 8KB internal slab to avoid frequent system malloc/free overhead.

```javascript
const buf = Buffer.from('Node.js High Performance', 'utf8');
console.log(buf instanceof Uint8Array); // true
console.log(buf.byteLength); // 24 bytes
console.log(buf.buffer); // Underlying raw ArrayBuffer
```

---

### Q52: What is the difference between `Buffer.alloc()`, `Buffer.allocUnsafe()`, and `Buffer.from()`?
**Answer:**

| Method | Behavior | Security / Performance |
|---|---|---|
| **`Buffer.alloc(size)`** | Allocates zero-filled (initialized) memory | **Safe**. Slower because every byte is wiped with zeros. |
| **`Buffer.allocUnsafe(size)`** | Allocates uninitialized memory segment | **Fast, but potentially dangerous**. Contains old, un-wiped memory data (could expose passwords, API tokens, or secrets). |
| **`Buffer.allocUnsafeSlow(size)`** | Allocates uninitialized memory bypassing the 8KB pool | Used when keeping small buffers alive for long durations without pinning the 8KB slab. |
| **`Buffer.from(data)`** | Copies or wraps string, array, or ArrayBuffer | Safe copy/view constructor. |

```javascript
// 🚨 DANGEROUS: If sent to network without overwriting, leaks old memory contents!
const unsafe = Buffer.allocUnsafe(1024);

// ✅ SAFE: Initialized with zeros
const safe = Buffer.alloc(1024);
```

---

### Q53: What are the four core types of Node.js Streams and how do they function?
**Answer:**

```
+-----------------------------------------------------------------+
| Readable Stream  (fs.createReadStream, req, process.stdin)     |
| ---> Produces data chunks (read-only)                          |
+-----------------------------------------------------------------+
| Writable Stream  (fs.createWriteStream, res, process.stdout)    |
| ---> Consumes data chunks (write-only)                         |
+-----------------------------------------------------------------+
| Duplex Stream    (net.Socket, tls.TLSSocket)                    |
| ---> Both Readable and Writable independently (two channels)   |
+-----------------------------------------------------------------+
| Transform Stream (zlib.createGzip, crypto.createCipheriv)       |
| ---> Duplex stream where output is computed from input (filter)|
+-----------------------------------------------------------------+
```

---

### Q54: What is Backpressure in Node.js Streams and why is it critical in production systems?
**Answer:**
Backpressure occurs when data is read from a fast producer (e.g., SSD reading at 500 MB/s) much faster than the consumer can write it (e.g., slow 3G mobile client downloading at 50 KB/s).
- **Without Backpressure Handling**: Chunks accumulate in the Writable stream's internal memory buffer (`_writableState.buffered`), leading to **RAM exhaustion, swapping, and Out-Of-Memory (OOM) crashes**.
- **With Backpressure Handling**: The Writable stream signals `write() === false`. The Readable stream pauses reading from the OS socket/file until the consumer emits the `'drain'` event.

```javascript
import fs from 'node:fs';

function writeMillionRows(writer, data, encoding, callback) {
  let i = 1_000_000;
  function write() {
    let ok = true;
    do {
      i--;
      if (i === 0) {
        writer.write(data, encoding, callback);
      } else {
        // ok is false if buffer exceeds highWaterMark
        ok = writer.write(data, encoding);
      }
    } while (i > 0 && ok);

    if (i > 0) {
      // 🛑 Buffer full! Wait for 'drain' before writing more
      writer.once('drain', write);
    }
  }
  write();
}
```

---

### Q55: Why should `stream.pipeline` or `stream/promises` pipeline be used instead of `.pipe()`?
**Answer:**
The classic `.pipe()` method has a critical flaw: **it does not automatically destroy and clean up all streams in the pipeline if one of the intermediate streams emits an error or closes abruptly**, causing file descriptor leaks and memory leaks.

- `stream.pipeline` (and `stream/promises.pipeline`):
  1. Forwards errors properly.
  2. Guarantees that **all** streams in the pipeline are destroyed and closed when an error occurs or when the pipeline finishes.

```javascript
import { pipeline } from 'node:stream/promises';
import fs from 'node:fs';
import zlib from 'node:zlib';

async function compressFile(sourcePath, destPath) {
  try {
    await pipeline(
      fs.createReadStream(sourcePath),
      zlib.createGzip(),
      fs.createWriteStream(destPath)
    );
    console.log('Compression successful with zero leaks');
  } catch (err) {
    console.error('Pipeline failed cleanly:', err.message);
  }
}
```

---

### Q56: What is `highWaterMark` and how does it differ for binary streams vs. objectMode streams?
**Answer:**
- **`highWaterMark`**: The threshold (buffer limit) after which the stream stops reading from its underlying source or returns `false` on `write()`.
- **Default Binary Streams**: `16 * 1024` bytes (**16 KB**) for standard streams, and **64 KB** for `fs.createReadStream`.
- **`objectMode: true` Streams**: `highWaterMark` represents the **number of objects** (default is **16 objects**), not bytes!

```javascript
import { Transform } from 'node:stream';

// Object Mode Transform stream
const objectTransformer = new Transform({
  objectMode: true,
  highWaterMark: 32, // Buffers up to 32 JS objects
  transform(chunk, encoding, callback) {
    chunk.processedAt = Date.now();
    this.push(chunk);
    callback();
  }
});
```

---

### Q57: How do you implement a custom `Transform` stream with error handling?
**Answer:**

```javascript
import { Transform } from 'node:stream';

class CsvToJsonTransform extends Transform {
  constructor(options = {}) {
    super({ ...options, objectMode: true });
    this.headers = null;
  }

  _transform(lineChunk, encoding, callback) {
    const line = lineChunk.toString().trim();
    if (!line) return callback();

    const parts = line.split(',');
    if (!this.headers) {
      this.headers = parts;
      return callback(); // First line was header
    }

    try {
      const record = {};
      this.headers.forEach((h, idx) => {
        record[h] = parts[idx] ?? null;
      });
      this.push(record);
      callback(); // Success
    } catch (err) {
      callback(err); // Propagate error through pipeline
    }
  }

  _flush(callback) {
    console.log('Stream finished flushing data');
    callback();
  }
}
```

---

### Q58: What is the difference between Paused (Non-Flowing) and Flowing modes in Readable Streams?
**Answer:**
- **Flowing Mode**: Data is read from the underlying system automatically and provided to the application as fast as possible via `'data'` events.
- **Paused Mode**: Data must be explicitly fetched using `stream.read()`. Streams start in paused mode.

```javascript
const readable = fs.createReadStream('data.txt');

// 1. Switches to FLOWING mode:
readable.on('data', (chunk) => {
  console.log('Chunk received:', chunk.length);
});

// 2. PAUSED mode (Manual pull):
readable.on('readable', () => {
  let chunk;
  while ((chunk = readable.read()) !== null) {
    console.log('Pulled chunk:', chunk.length);
  }
});
```

---

### Q59: What are WHATWG Web Streams (`ReadableStream`, `WritableStream`, `TransformStream`) in Node.js?
**Answer:**
Starting in Node.js v18+, Node.js provides standard **WHATWG Web Streams API** (compatible with Browsers, Cloudflare Workers, and Deno) alongside legacy Node.js streams.

```javascript
import { ReadableStream, TransformStream } from 'node:stream/web';

const webReadable = new ReadableStream({
  start(controller) {
    controller.enqueue('First chunk');
    controller.enqueue('Second chunk');
    controller.close();
  }
});

// Interop: Convert Node stream to Web stream and vice versa
import { Readable } from 'node:stream';
const nodeStream = fs.createReadStream('file.txt');
const webStream = Readable.toWeb(nodeStream);
const backToNode = Readable.fromWeb(webStream);
```

---

### Q60: How does `stream.finished()` work and why is it preferred over listening to `'end'` or `'close'`?
**Answer:**
Listening to `'end'` or `'finish'` alone is unreliable because:
- Streams that emit an `'error'` or are destroyed abruptly may emit `'close'` without ever emitting `'end'` or `'finish'`.
- `stream.finished(stream, callback)` (or `stream/promises.finished`) listens to all completion, error, and abort events, guaranteeing a single callback invocation.

```javascript
import { finished } from 'node:stream/promises';
import fs from 'node:fs';

const rs = fs.createReadStream('input.txt');
await finished(rs);
console.log('Stream completely and safely closed.');
```

---

### Q61: What is `Buffer.concat()` and what is its performance implication in high-throughput applications?
**Answer:**
`Buffer.concat(list, [totalLength])` allocates a new `Buffer` and copies the byte contents of each buffer in `list` into the new buffer.

**Performance Trap:**
Iteratively doing `buf = Buffer.concat([buf, chunk])` inside a `'data'` event has $O(N^2)$ memory copying overhead!

```javascript
// ❌ ANTI-PATTERN: O(N^2) memory reallocation and copying!
let fullBuffer = Buffer.alloc(0);
stream.on('data', (chunk) => {
  fullBuffer = Buffer.concat([fullBuffer, chunk]);
});

// ✅ BEST PRACTICE: Push chunks into an array and concatenate ONCE on end
const chunks = [];
let totalLength = 0;
stream.on('data', (chunk) => {
  chunks.push(chunk);
  totalLength += chunk.length;
});
stream.on('end', () => {
  const completeBuffer = Buffer.concat(chunks, totalLength);
});
```

---

### Q62: What is the StringDecoder module and why is it required when decoding multi-byte UTF-8 character chunks?
**Answer:**
UTF-8 characters can range from 1 to 4 bytes (e.g., emojis `🚀` or Chinese characters `你`).
If a 64KB stream chunk cuts a 4-byte UTF-8 character in half (2 bytes in chunk 1, 2 bytes in chunk 2):
- `chunk.toString('utf8')` will corrupt the character and output `` replacement characters.
- `StringDecoder` buffers incomplete multi-byte sequences until the remaining bytes arrive in the next chunk.

```javascript
import { StringDecoder } from 'node:string_decoder';

const decoder = new StringDecoder('utf8');
const part1 = Buffer.from([0xF0, 0x9F]); // First 2 bytes of 🚀 (0xF0 0x9F 0x9A 0x80)
const part2 = Buffer.from([0x9A, 0x80]); // Remaining 2 bytes

console.log(part1.toString('utf8')); //  (Corrupted!)
console.log(decoder.write(part1));   // '' (Buffered, waiting for rest)
console.log(decoder.write(part2));   // '🚀' (Decoded perfectly!)
```

---

### Q63: What are File Descriptors (FDs) and how do you handle `EMFILE` / `ENFILE` errors?
**Answer:**
- A File Descriptor (FD) is a positive integer assigned by the OS kernel to track an open file or socket.
- **`EMFILE`**: Too many open files in the current process (process limit `ulimit -n`).
- **`ENFILE`**: Too many open files across the entire operating system.

**Production Mitigations:**
1. Increase OS limits: `ulimit -n 65535` in container launch script.
2. Pool/Queue file operations using graceful-fs or custom async concurrency limiters.
3. Always close file handles in `finally` blocks:

```javascript
import fsPromises from 'node:fs/promises';

async function safeReadFile(path) {
  let fileHandle;
  try {
    fileHandle = await fsPromises.open(path, 'r');
    return await fileHandle.readFile();
  } finally {
    if (fileHandle) {
      await fileHandle.close(); // Guarantee file descriptor is returned to OS
    }
  }
}
```

---

### Q64: How does `fs.watch()` differ from `fs.watchFile()` and what are their trade-offs?
**Answer:**
- **`fs.watch()`**: Uses native OS kernel notification subsystems (`inotify` on Linux, `FSEvents` on macOS, `ReadDirectoryChangesW` on Windows). Highly efficient, event-driven, zero polling CPU overhead.
- **`fs.watchFile()`**: Periodically polls the filesystem using `fs.stat()` at a specified interval (e.g. 5007ms). Works across network mounts (NFS), but causes high CPU and disk I/O overhead.

---

### Q65: What is Zero-Copy I/O in Node.js and how can `fs.copyFile` leverage OS copy-on-write?
**Answer:**
- Traditional file copying reads bytes into userland JS buffers and writes them back out via kernel syscalls (`read -> kernel -> userland -> kernel -> write`).
- `fs.copyFile` utilizes native OS zero-copy syscalls:
  - Linux: `copy_file_range(2)` or `sendfile(2)`
  - macOS: `clonefile(2)` (Copy-on-Write / CoW snapshotting on APFS)
  - Data remains entirely in kernel space without passing through Node.js user memory.

```javascript
import fs from 'node:fs/promises';
import { constants } from 'node:fs';

// Fast APFS/Btrfs Copy-on-Write (Zero disk duplication until modified)
await fs.copyFile('source.db', 'backup.db', constants.COPYFILE_FICLONE);
```

---

### Q66: How do you stream large Multipart Form Data uploads (e.g. 10GB video) directly to S3 / Cloud Storage without disk buffering?
**Answer:**
Buffering a 10GB upload to disk or RAM crashes servers and degrades SSD life. Streams allow piping directly from incoming HTTP request into S3 Multipart Upload API.

```javascript
import http from 'node:http';
import { PassThrough } from 'node:stream';
import { Upload } from '@aws-sdk/lib-storage';
import { S3Client } from '@aws-sdk/client-s3';

const s3 = new S3Client({ region: 'us-east-1' });

http.createServer(async (req, res) => {
  if (req.method === 'POST' && req.url === '/upload') {
    const uploadStream = new PassThrough();
    req.pipe(uploadStream); // Pipe direct from network socket

    const parallelUpload = new Upload({
      client: s3,
      params: {
        Bucket: 'enterprise-data-lake',
        Key: `uploads/${Date.now()}-video.mp4`,
        Body: uploadStream
      },
      queueSize: 4,
      partSize: 10 * 1024 * 1024 // 10MB chunk parts
    });

    await parallelUpload.done();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'Uploaded successfully' }));
  }
}).listen(8080);
```

---

### Q67: What is a `PassThrough` stream and what are its standard production use cases?
**Answer:**
A `PassThrough` stream is a trivial implementation of a `Transform` stream that simply outputs the exact data it receives without modifying it.

**Key Use Cases:**
1. **Tee-ing / Forking Streams**: Piping one data source to multiple destinations.
2. **Stream Abstraction**: Passing a writable handle to a producer while returning a readable handle to a consumer.

```javascript
import { PassThrough } from 'node:stream';
import fs from 'node:fs';

const source = fs.createReadStream('audit.log');
const pass1 = new PassThrough();
const pass2 = new PassThrough();

source.pipe(pass1);
source.pipe(pass2);

pass1.pipe(fs.createWriteStream('archive_replica_1.log'));
pass2.pipe(fs.createWriteStream('archive_replica_2.log'));
```

---

### Q68: What is the difference between `fs.constants.O_DIRECT`, `O_SYNC`, and standard buffered I/O?
**Answer:**
- **Standard Buffered I/O**: Writes go to OS Page Cache. Kernel flushes to physical disk asynchronously. Fast, but crash can cause data loss.
- **`O_SYNC`**: Syscalls block until data AND filesystem metadata are physically written to underlying storage hardware.
- **`O_DIRECT`**: Bypasses the OS page cache entirely, reading/writing directly between user memory buffers and disk controller (used in custom database engines).

---

### Q69: How do you handle NDJSON (Newline Delimited JSON) streams efficiently in Node.js?
**Answer:**
Parsing multi-gigabyte JSON files with `JSON.parse()` fails because V8 strings have a 512MB max size limit and JSON.parse blocks the event loop. NDJSON processes objects line-by-line.

```javascript
import readline from 'node:readline';
import fs from 'node:fs';
import { Transform } from 'node:stream';

async function* parseNdjson(stream) {
  const rl = readline.createInterface({ input: stream, crlfDelay: Infinity });
  for await (const line of rl) {
    if (line.trim()) {
      yield JSON.parse(line);
    }
  }
}

// Consuming NDJSON stream with minimal memory
const fileStream = fs.createReadStream('transactions.ndjson');
for await (const transaction of parseNdjson(fileStream)) {
  await processPayment(transaction);
}
```

---

### Q70: What are TypedArrays and DataView and how do they interact with Node.js Buffers?
**Answer:**
- `ArrayBuffer`: Fixed-length raw memory buffer.
- `TypedArray` (`Uint8Array`, `Int32Array`, `Float64Array`): Provides a typed indexed view onto an `ArrayBuffer`.
- `DataView`: Provides low-level getter/setter methods with explicit control over endianness (Big-Endian vs. Little-Endian) at arbitrary byte offsets.

```javascript
const ab = new ArrayBuffer(8);
const view = new DataView(ab);

// Write 32-bit integer in Big-Endian at offset 0
view.setInt32(0, 42000, false);

// Node.js Buffer wrapping the same ArrayBuffer
const nodeBuf = Buffer.from(ab);
console.log(nodeBuf.readInt32BE(0)); // 42000
```

---

### Q71: How does Node.js handle Stream destruction (`stream.destroy([error])`)?
**Answer:**
Calling `.destroy()` closes the underlying resource (file descriptor, TCP socket) immediately.
- If an `error` argument is passed, an `'error'` event is emitted.
- All internal buffers are flushed and memory released.
- Emits the `'close'` event.

---

### Q72: What is the difference between `fs.stat()`, `fs.lstat()`, and `fs.fstat()`?
**Answer:**
- `fs.stat(path)`: Follows symbolic links and returns stats of the **target file**.
- `fs.lstat(path)`: Does not follow symbolic links; returns stats of the **symbolic link itself**.
- `fs.fstat(fd)`: Returns stats for an **already open file descriptor** directly.

---

### Q73: How do you create an infinite readable stream that safely pauses and resumes?
**Answer:**

```javascript
import { Readable } from 'node:stream';

class InfiniteCounterStream extends Readable {
  constructor(options) {
    super(options);
    this.count = 0;
  }

  _read(size) {
    // Called when consumer is ready for more data
    const chunk = Buffer.from(`Event Count: ${this.count++}
`);
    // push() returns false if consumer buffer is full (backpressure)
    const canContinue = this.push(chunk);
    if (!canContinue) {
      // Stream is paused automatically by Node internals
    }
  }
}
```

---

### Q74: What is the `Blob` API in Node.js and how does it compare to `Buffer`?
**Answer:**
Introduced in Node.js v18 (standard Web API):
- `Blob` represents immutable, raw data with a MIME type.
- Unlike `Buffer`, `Blob` is cross-platform compatible with standard browser Web APIs.
- Convert between them:
  ```javascript
  const blob = new Blob(['Hello World'], { type: 'text/plain' });
  const buf = Buffer.from(await blob.arrayBuffer());
  ```

---

### Q75: How do you implement stream throttling / rate-limiting (Token Bucket) in Node.js?
**Answer:**

```javascript
import { Transform } from 'node:stream';

class ThrottleStream extends Transform {
  constructor(bytesPerSecond) {
    super();
    this.bytesPerSecond = bytesPerSecond;
  }

  _transform(chunk, encoding, callback) {
    const delay = (chunk.length / this.bytesPerSecond) * 1000;
    setTimeout(() => {
      this.push(chunk);
      callback();
    }, delay);
  }
}

// Example: Throttle download to 500 KB/s
fs.createReadStream('large_file.iso')
  .pipe(new ThrottleStream(500 * 1024))
  .pipe(res);
```


---

# Part 4: Concurrency, Worker Threads, Child Processes & Cluster (Q76 - Q100)

---

### Q76: What are the fundamental differences between Worker Threads, Child Processes, and the Cluster Module?
**Answer:**

| Feature | `worker_threads` | `child_process` (`fork`/`spawn`) | `cluster` Module |
|---|---|---|---|
| **OS Entity** | OS Thread inside same process | Separate OS Process | Separate OS Processes (Master/Worker) |
| **Memory Isolation** | Shared memory address space (via `SharedArrayBuffer`) | Isolated Memory | Isolated Memory |
| **Startup Overhead** | Low (~10-20MB RAM per isolate) | High (~30-50MB RAM + process spawn) | High (Full Node.js runtime per worker) |
| **Port Sharing** | Cannot share TCP listen ports directly | Cannot share TCP ports directly | Master process distributes TCP sockets across workers |
| **Communication** | Fast `MessagePort` / Structured Clone / Shared RAM | IPC Channel / stdin / stdout | Master-to-Worker IPC |
| **Primary Use Case** | CPU-intensive algorithms (image resizing, ML, crypto) | Executing external binaries (`git`, `ffmpeg`, Python) | Horizontal multi-core scaling of HTTP servers |

---

### Q77: How do Worker Threads communicate using `MessagePort`, `MessageChannel`, and Structured Clone Algorithm?
**Answer:**
`worker_threads` communicate asynchronously via message passing.
- Data passed through `port.postMessage(value)` is serialized using HTML5 **Structured Clone Algorithm**.
- Objects, Maps, Sets, Dates, RegExps, Buffers, and ArrayBuffers are cloned deeply without manual `JSON.stringify()`.
- Functions and Symbols cannot be cloned.

```javascript
// main.js
import { Worker } from 'node:worker_threads';

const worker = new Worker('./worker.js', {
  workerData: { matrixSize: 1000 }
});

worker.on('message', (result) => {
  console.log('Result from worker:', result);
});

worker.on('error', (err) => console.error('Worker error:', err));
worker.on('exit', (code) => console.log(`Worker exited with code ${code}`));

// worker.js
import { parentPort, workerData } from 'node:worker_threads';

const size = workerData.matrixSize;
// Perform heavy computation
let sum = 0;
for (let i = 0; i < size * size; i++) sum += i;

parentPort.postMessage({ sum });
```

---

### Q78: How do you use `SharedArrayBuffer` and `Atomics` for zero-copy lock-free thread synchronization?
**Answer:**
Passing massive arrays between worker threads via structured clone incurs memory copying overhead. `SharedArrayBuffer` allows multiple threads to read and write the **exact same physical memory buffer simultaneously**.

To prevent data race conditions, `Atomics` provides atomic operations (compare-and-swap, add, load, store, wait, notify).

```javascript
// main.js
import { Worker } from 'node:worker_threads';

// 4 bytes shared buffer (1 Int32)
const sharedBuffer = new SharedArrayBuffer(4);
const sharedArray = new Int32Array(sharedBuffer);
sharedArray[0] = 0;

const worker = new Worker('./worker.js', { workerData: { sharedBuffer } });

// Wait for worker to signal
console.log('Main thread waiting for worker...');
// Atomics.wait puts thread to sleep until notified (available in worker threads)

// worker.js
import { workerData } from 'node:worker_threads';

const sharedArray = new Int32Array(workerData.sharedBuffer);

// Atomically increment without race conditions
Atomics.add(sharedArray, 0, 100);
console.log('Worker updated shared value to:', Atomics.load(sharedArray, 0));
```

---

### Q79: How do you build a Thread Pool (`piscina` pattern) in Node.js to reuse Worker Threads?
**Answer:**
Spawning a `new Worker()` on every single HTTP request is an anti-pattern (creates ~20ms latency and V8 Isolate initialization cost). A **Worker Thread Pool** keeps a fixed pool of pre-initialized workers alive and queues incoming CPU tasks.

```javascript
import { Worker } from 'node:worker_threads';
import { EventEmitter } from 'node:events';

class StaticThreadPool extends EventEmitter {
  constructor(workerPath, numThreads) {
    super();
    this.workerPath = workerPath;
    this.numThreads = numThreads;
    this.workers = [];
    this.freeWorkers = [];
    this.taskQueue = [];

    for (let i = 0; i < numThreads; i++) {
      this.addNewWorker();
    }
  }

  addNewWorker() {
    const worker = new Worker(this.workerPath);
    worker.on('message', (result) => {
      worker.activeTask.resolve(result);
      worker.activeTask = null;
      this.freeWorkers.push(worker);
      this.processQueue();
    });
    this.workers.push(worker);
    this.freeWorkers.push(worker);
  }

  runTask(taskData) {
    return new Promise((resolve, reject) => {
      this.taskQueue.push({ taskData, resolve, reject });
      this.processQueue();
    });
  }

  processQueue() {
    if (this.taskQueue.length === 0 || this.freeWorkers.length === 0) return;
    const worker = this.freeWorkers.pop();
    const task = this.taskQueue.shift();
    worker.activeTask = task;
    worker.postMessage(task.taskData);
  }
}
```

---

### Q80: What is Transferable Objects (`transferList`) in Worker Threads?
**Answer:**
`postMessage(data, [transferList])` transfers the underlying memory allocation of an `ArrayBuffer` or `MessagePort` directly from the sending thread to the receiving thread **without copying**.
- The sending thread **loses access** to the buffer (`byteLength` becomes 0 / detached).
- Zero-copy, $O(1)$ memory transfer of gigabytes of data instantly.

```javascript
const buffer = new ArrayBuffer(1024 * 1024 * 100); // 100MB buffer
console.log('Before transfer:', buffer.byteLength); // 104857600

// Transfer ownership to worker (zero memory copying)
worker.postMessage({ buf: buffer }, [buffer]);

console.log('After transfer (detached):', buffer.byteLength); // 0!
```

---

### Q81: How does the `cluster` module work under the hood (Round-Robin vs. OS-distributed)?
**Answer:**
The `cluster` module forks identical child processes to scale across multiple CPU cores.

**How Master and Workers share Port 80/443:**
1. **Windows**: The master process creates the listening socket and passes incoming handles directly to worker processes.
2. **Linux/macOS (Default - Round-Robin `cluster.SCHED_RR`)**:
   - The **Primary (Master) process** binds to the network port (e.g. 3000).
   - When a new TCP connection arrives, the master accepts the connection and hands off the connection handle (`net.Socket`) to a worker over an internal IPC pipe using Round-Robin load balancing.
3. **OS-Distributed (`cluster.SCHED_NONE`)**:
   - Master creates the listening socket and passes the raw socket descriptor to all workers.
   - Workers accept incoming connections directly from the OS kernel (can cause thundering herd problem where one worker takes most requests).

```javascript
import cluster from 'node:cluster';
import http from 'node:http';
import os from 'node:os';

const numCPUs = os.cpus().length;

if (cluster.isPrimary) {
  console.log(`Primary ${process.pid} is running`);

  // Fork workers for each CPU core
  for (let i = 0; i < numCPUs; i++) {
    cluster.fork();
  }

  cluster.on('exit', (worker, code, signal) => {
    console.warn(`Worker ${worker.process.pid} died. Forking replacement...`);
    cluster.fork();
  });
} else {
  // Workers share the TCP connection on port 3000
  http.createServer((req, res) => {
    res.writeHead(200);
    res.end(`Handled by worker PID: ${process.pid}
`);
  }).listen(3000);
}
```

---

### Q82: How do you achieve Zero-Downtime Rolling Restarts in a Node.js Cluster?
**Answer:**
In production deployments, you must restart worker processes one-by-one without dropping active in-flight user requests.

```javascript
// Primary Process Rolling Restart Logic
async function zeroDowntimeReload() {
  const workers = Object.values(cluster.workers);

  for (const worker of workers) {
    // 1. Fork replacement worker first
    const newWorker = cluster.fork();

    await new Promise((resolve) => {
      newWorker.on('listening', resolve); // Wait until new worker is ready to accept traffic
    });

    // 2. Disconnect old worker gracefully (stops accepting new connections, drains active ones)
    worker.disconnect();
    
    // 3. Force kill if not exited after 10 seconds
    const timeout = setTimeout(() => worker.kill(), 10000);
    worker.on('exit', () => clearTimeout(timeout));
  }
}
```

---

### Q83: What are the differences between `child_process.spawn()`, `exec()`, `execFile()`, and `fork()`?
**Answer:**

| Method | Spawns Shell? | Buffer Output in RAM? | Streaming? | Primary Use Case |
|---|---|---|---|---|
| **`spawn(cmd, args)`** | No (by default) | No | **Yes** (Streams `stdout`/`stderr`) | Large data, long-running processes (`ffmpeg`, continuous logs) |
| **`exec(cmd)`** | **Yes** (`/bin/sh` or `cmd.exe`) | **Yes** (`maxBuffer: 1MB`) | No (Callback on completion) | Shell pipes/wildcards (`ls -la \| grep txt`). Vulnerable to command injection! |
| **`execFile(file, args)`** | **No** (Direct binary execution) | **Yes** | No (Callback on completion) | Safe CLI execution without shell injection risk |
| **`fork(modulePath)`** | No | No | Yes + IPC Channel | Spawning dedicated Node.js child processes with `process.send()` |

---

### Q84: How do you prevent Command Injection vulnerabilities with `child_process`?
**Answer:**
`child_process.exec()` invokes a system shell, allowing attackers to inject arbitrary shell commands via unsanitized input.

```javascript
import { exec, execFile } from 'node:child_process';

const userInput = 'file.txt; rm -rf /'; // Malicious payload

// ❌ VULNERABLE: Executes arbitrary shell commands!
exec(`cat ${userInput}`, (err, stdout) => { ... });

// ✅ SECURE: execFile and spawn do NOT invoke a shell; arguments are passed directly to OS execve syscall
execFile('cat', [userInput], (err, stdout) => {
  // Safe: Tries to open a literal file named "file.txt; rm -rf /"
});
```

---

### Q85: How does IPC (Inter-Process Communication) work between Node.js Parent and Child processes?
**Answer:**
When using `child_process.fork()` or `spawn()` with `stdio: ['pipe', 'pipe', 'pipe', 'ipc']`:
- Node.js opens a **Unix Domain Socket** (Linux/macOS) or **Named Pipe** (Windows) mapped to file descriptor 3.
- Messages are formatted as JSON frames and deserialized automatically by Node.js core.

```javascript
// parent.js
import { fork } from 'node:child_process';
const child = fork('./child.js');

child.send({ type: 'CALCULATE_REPORT', payload: { month: 'September' } });
child.on('message', (msg) => console.log('From child:', msg));

// child.js
process.on('message', (msg) => {
  if (msg.type === 'CALCULATE_REPORT') {
    process.send({ status: 'SUCCESS', result: 42000 });
  }
});
```

---

### Q86: How do you pass open Sockets and Server Handles between processes using IPC?
**Answer:**
Node.js IPC allows passing OS socket handles (`net.Socket`, `net.Server`) directly to child processes using `child.send(message, sendHandle)`.

```javascript
// master.js
import { fork } from 'node:child_process';
import net from 'node:net';

const child = fork('./worker.js');
const server = net.createServer();

server.listen(3000, () => {
  // Pass the listening TCP server handle to the child process
  child.send('server-handle', server);
  server.close(); // Master closes its own handle; child handles traffic
});

// worker.js
import http from 'node:http';

process.on('message', (msg, handle) => {
  if (msg === 'server-handle') {
    http.createServer((req, res) => {
      res.end(`Served by child worker ${process.pid}`);
    }).listen(handle);
  }
});
```

---

### Q87: What are Zombie and Orphan processes in Node.js and how do you prevent them?
**Answer:**
- **Zombie Process**: A child process that has terminated, but its parent has not read its exit status code via `waitpid()`. Remains in OS process table.
- **Orphan Process**: A child process whose parent crashed or exited while the child continues running. Adopted by `init` (PID 1).

**Prevention:**
1. Always listen to the `'exit'` event on child processes.
2. Forward termination signals (`SIGTERM`, `SIGINT`) to child process groups.
3. Clean up children in exit handlers:

```javascript
const child = spawn('worker_binary', [], { detached: false });

process.on('SIGTERM', () => {
  child.kill('SIGTERM');
  process.exit(0);
});
```

---

### Q88: What is `child_process.spawnSync()` and when is it acceptable in production?
**Answer:**
`spawnSync` synchronously blocks the entire Node.js Event Loop until the spawned process exits.
- **Acceptable**: CLI utility tools, build scripts, or during service bootstrap initialization before listening on network ports.
- **Unacceptable**: Inside any HTTP route handler or asynchronous message consumer (freezes all concurrent users).

---

### Q89: How does the `cluster` module handle Session Affinity (Sticky Sessions) with WebSockets?
**Answer:**
Because WebSockets require an initial HTTP Upgrade handshake followed by continuous TCP communication on the same worker, random Round-Robin dispatch will route subsequent packets to different workers, breaking WebSocket connections.

**Solutions:**
1. **Reverse Proxy Load Balancer**: Use NGINX / HAProxy / AWS ALB configured with IP hash or cookie-based sticky sessions.
2. **Redis Adapter**: Use `@socket.io/redis-adapter` so WebSocket messages are broadcast across all cluster workers over Redis Pub/Sub regardless of which worker holds the connection.

---

### Q90: How do you handle CPU-Bound tasks (e.g. Scrypt password hashing, PDF generation) without blocking the Event Loop?
**Answer:**
1. **Worker Threads**: Offload CPU task to a worker thread pool.
2. **Built-in Async APIs**: Node.js built-ins (`crypto.scrypt`, `crypto.pbkdf2`) already offload work to the Libuv thread pool.
3. **Dedicated Microservices**: Extract heavy jobs (e.g. Puppeteer PDF generation) to a separate async job queue (BullMQ + Redis).

```javascript
// Native async offloading to Libuv thread pool
import crypto from 'node:crypto';

// ✅ Does NOT block main thread:
crypto.scrypt('password123', 'saltKey', 64, (err, derivedKey) => {
  console.log('Derived key:', derivedKey.toString('hex'));
});
```

---

### Q91: What is the `broadcastChannel` API in Node.js `worker_threads`?
**Answer:**
Introduced in Node.js v18 (standard Web API), `BroadcastChannel` provides a 1-to-many communication channel across all Worker Threads in the same process without manually passing `MessagePort` handles.

```javascript
// In main.js or any worker:
import { BroadcastChannel } from 'node:worker_threads';

const channel = new BroadcastChannel('app_cache_invalidation');

// Worker A listens:
channel.onmessage = (event) => {
  console.log('Invalidate key:', event.data.key);
};

// Worker B broadcasts:
channel.postMessage({ key: 'user:1001' });
```

---

### Q92: What is the overhead of a Worker Thread compared to a native OS thread?
**Answer:**
A Node.js Worker Thread is a real OS thread, but it initializes its own **V8 Isolate, V8 Context, and Libuv Event Loop**.
- **Memory Overhead**: ~15MB to 30MB base RAM per worker thread.
- **Creation Latency**: ~20ms - 50ms startup time.
- **Recommendation**: Never spawn on-demand per request; use a warm thread pool of size `os.availableParallelism()`.

---

### Q93: What is `os.availableParallelism()` and why does it supersede `os.cpus().length` in containerized environments (Docker/K8s)?
**Answer:**
`os.cpus().length` returns the physical CPU cores of the host hardware. Inside a Docker container or Kubernetes Pod with CPU limits (e.g., `cpu: 2` on a 64-core machine):
- `os.cpus().length` returns `64` (spawns 64 workers, causing massive CPU throttling and thrashing).
- `os.availableParallelism()` (Node.js v19.4+) reads CFS (Completely Fair Scheduler) quotas from `/sys/fs/cgroup` and correctly returns `2`.

```javascript
import os from 'node:os';
import cluster from 'node:cluster';

// ✅ Accurate container-aware parallelism
const concurrency = os.availableParallelism();
```

---

### Q94: How do you handle unhandled errors in Worker Threads without crashing the host process?
**Answer:**
Errors in worker threads do not crash the main process if an `'error'` listener is attached to the Worker instance.

```javascript
import { Worker } from 'node:worker_threads';

const worker = new Worker('./riskyWorker.js');

worker.on('error', (err) => {
  console.error('Worker crashed safely:', err.message);
  // Spawn replacement worker
});
```

---

### Q95: How do you execute WebAssembly (Wasm) inside Node.js for near-native compute performance?
**Answer:**
V8 supports standard WebAssembly compilation and execution with SIMD support directly in Node.js.

```javascript
import fs from 'node:fs/promises';

const wasmBuffer = await fs.readFile('./math.wasm');
const wasmModule = await WebAssembly.instantiate(wasmBuffer, {
  env: {
    log: (val) => console.log('Wasm log:', val)
  }
});

const { fastFibonacci } = wasmModule.instance.exports;
console.log('Result:', fastFibonacci(40));
```

---

### Q96: What is the difference between `child_process.exec` and `child_process.execFile` regarding memory limits?
**Answer:**
Both buffer standard output in RAM, controlled by `maxBuffer` (default: 1024 * 1024 = 1MB).
If the child process produces 1MB + 1 byte of output, Node.js kills the child process with `ERR_CHILD_PROCESS_STDIO_MAXBUFFER`.
For large outputs, always use `spawn()` and stream the stdout.

---

### Q97: How do you run external Python or Go scripts reliably from Node.js in production?
**Answer:**
For high-scale production:
1. **Do NOT spawn a new Python process per request** (Python interpreter startup overhead is 50-100ms).
2. Use a long-running Python daemon communicating over **gRPC, Unix Domain Sockets, or Redis Queue**.
3. If using `spawn()`, pipe NDJSON streams through stdin/stdout.

---

### Q98: How do you share environment variables safely between parent and child processes?
**Answer:**

```javascript
import { spawn } from 'node:child_process';

// Do NOT pass full process.env if child should not see sensitive master secrets (AWS keys, DB passwords)
const safeEnv = {
  NODE_ENV: 'production',
  PATH: process.env.PATH,
  CHILD_SPECIFIC_TOKEN: 'token_abc'
};

const child = spawn('node', ['worker.js'], {
  env: safeEnv, // Explicit whitelist
  stdio: 'inherit'
});
```

---

### Q99: What is `process.channel` and when is it defined?
**Answer:**
`process.channel` is a reference to the internal IPC channel. It is `undefined` in normal standalone processes and only exists when the process was spawned with an IPC channel (e.g. via `child_process.fork()` or `cluster`).

---

### Q100: How do you build a Mutex / Spinlock using `Atomics.wait` and `Atomics.notify` in Node.js Worker Threads?
**Answer:**

```javascript
// Mutex implementation using Atomics and SharedArrayBuffer
class SharedMutex {
  constructor(sharedBuffer, offset = 0) {
    this.array = new Int32Array(sharedBuffer, offset, 1);
  }

  lock() {
    while (true) {
      // Try to acquire lock (0 -> 1)
      if (Atomics.compareExchange(this.array, 0, 0, 1) === 0) {
        return; // Acquired!
      }
      // Wait until value changes from 1
      Atomics.wait(this.array, 0, 1);
    }
  }

  unlock() {
    Atomics.store(this.array, 0, 0); // Release lock (1 -> 0)
    Atomics.notify(this.array, 0, 1); // Wake 1 waiting worker
  }
}
```


---

# Part 5: Memory Management, V8 Garbage Collection & Leak Profiling (Q101 - Q125)

---

### Q101: How is memory structured in a Node.js process (`process.memoryUsage()`)?
**Answer:**
A Node.js process memory space consists of V8 Heap, C++ Core Allocations, and OS mappings:

```
+-------------------------------------------------------------------+
|                        Resident Set Size (RSS)                    |
| +-----------------------------+  +------------------------------+ |
| |        V8 Managed Heap      |  |       External Memory        | |
| |  - New Space (Nursery/Eden) |  |  - C++ Bound Objects         | |
| |  - Old Pointer Space        |  |  - Node-API native memory    | |
| |  - Old Data Space           |  +------------------------------+ |
| |  - Large Object Space (LOS) |  |      ArrayBuffers Store      | |
| |  - Code / Map Spaces        |  |  - Node.js Buffers           | |
| +-----------------------------+  |  - TypedArrays Memory        | |
|                                  +------------------------------+ |
+-------------------------------------------------------------------+
```

- **`rss` (Resident Set Size)**: Total physical RAM occupied by the process in the OS.
- **`heapTotal`**: Total memory allocated for the V8 heap.
- **`heapUsed`**: Actual memory currently used by active JavaScript objects.
- **`external`**: Memory used by C++ objects bound to JavaScript (OpenSSL contexts, zlib).
- **`arrayBuffers`**: Memory allocated for Buffers and `ArrayBuffer` instances (part of external memory).

```javascript
console.log(process.memoryUsage());
/* Output:
{
  rss: 38453248,        // ~38 MB
  heapTotal: 6520832,   // ~6.5 MB
  heapUsed: 4984216,    // ~4.9 MB
  external: 1245904,    // ~1.2 MB
  arrayBuffers: 1048576 // ~1.0 MB
}
*/
```

---

### Q102: How does the V8 Generational Garbage Collector (Scavenger vs. Major GC) work?
**Answer:**
V8 uses the **Weak Generational Hypothesis**: most objects die young (short lifetime).

1. **Young Generation (New Space)**:
   - Divided into **Nursery** and **Intermediate (From/To)** semispaces.
   - Managed by the **Scavenger (Cheney's Copying Algorithm)**:
     - When Nursery fills, active reachable objects are copied to the "To" space.
     - Dead objects are discarded instantly in bulk ($O(\text{live objects})$ cost).
     - Surviving objects that survive two Scavenge cycles are **promoted** to Old Space.
   - Extremely fast (1-3ms pause).

2. **Old Generation (Old Space)**:
   - Holds long-lived objects (singletons, caches, configuration, promoted objects).
   - Managed by **Major GC (Mark-Sweep-Compact)**:
     - **Marking**: Traverses object graph from root pointers (globals, active stack frames) to identify reachable objects. Uses Tri-color marking (White = unvisited, Grey = discovered, Black = processed).
     - **Sweeping**: Reclaims memory addresses of unmarked (white) objects and adds them to free-lists.
     - **Compacting**: Moves live objects to defragment fragmented memory pages.

---

### Q103: What are Concurrent Marking, Incremental Marking, and Parallel Scavenging in V8?
**Answer:**
To avoid "Stop-the-World" pauses that freeze HTTP request processing for hundreds of milliseconds:
- **Incremental Marking**: V8 breaks the major GC marking phase into tiny 1ms increments interspersed between regular JavaScript execution.
- **Concurrent Marking**: Background helper worker threads traverse the object graph while JavaScript executes on the main thread.
- **Parallel Scavenging & Compacting**: Multiple worker threads move and copy objects in parallel during Scavenge and Compaction cycles.

---

### Q104: How do you configure and increase the V8 Max Heap Size (`--max-old-space-size`) in production?
**Answer:**
By default, 64-bit Node.js processes limit old space to approximately **1.4 GB to 2 GB** (depending on version and physical memory detection).

```bash
# Increase heap limit to 8GB for memory-intensive batch processing
node --max-old-space-size=8192 server.js
```

**Production Kubernetes Rule:**
If a container has a memory limit of `10Gi`, set `--max-old-space-size=8192` (8GB), leaving 2GB headroom for `rss` overhead, C++ Buffers, Libuv threads, and kernel page tables. Otherwise, the Kubernetes OOM-Killer (`OOMKilled - Exit Code 137`) will abruptly terminate the container.

---

### Q105: What is `v8.getHeapStatistics()` and `v8.getHeapSpaceStatistics()`?
**Answer:**
Provides granular, programmatic inspection of V8 heap internals:

```javascript
import v8 from 'node:v8';

const heapStats = v8.getHeapStatistics();
console.log({
  total_heap_size: (heapStats.total_heap_size / 1024 / 1024).toFixed(2) + ' MB',
  used_heap_size: (heapStats.used_heap_size / 1024 / 1024).toFixed(2) + ' MB',
  heap_size_limit: (heapStats.heap_size_limit / 1024 / 1024).toFixed(2) + ' MB',
  malloced_memory: (heapStats.malloced_memory / 1024 / 1024).toFixed(2) + ' MB'
});

const spaces = v8.getHeapSpaceStatistics();
// Details on: new_space, old_space, code_space, map_space, large_object_space
```

---

### Q106: What are the most common causes of Memory Leaks in Node.js applications?
**Answer:**

1. **Global Variables & Unbounded In-Memory Caches**:
   ```javascript
   const cache = {}; // ❌ Grows forever without TTL or eviction limit!
   app.get('/user/:id', (req, res) => { cache[req.params.id] = req.body; });
   ```
2. **Uncleared Timers & Intervals**:
   ```javascript
   const interval = setInterval(heartbeat, 1000); // ❌ Retains closures if never clearInterval()
   ```
3. **Forgotten EventEmitter Listeners**:
   Adding `.on('event', fn)` repeatedly without `.off()` or `{ signal }`.
4. **Closures Retaining Large Outer Scope References**:
   Accidentally capturing large buffers or request objects in long-lived callback functions.
5. **Detached DOM/Buffer References & Circular Structures**:
   Retaining references to parent objects in long-lived caches.

---

### Q107: How do Closures cause accidental "Metered" Memory Leaks?
**Answer:**
V8 creates a shared lexical environment object for all closures inside the same outer scope. If one closure references a large variable, **all closures in that scope retain the large variable**, even if they do not explicitly use it!

```javascript
let theThing = null;

function replaceThing() {
  const originalThing = theThing; // Reference to previous instance
  
  // Unused closure that captures originalThing
  const unused = function () {
    if (originalThing) console.log("hi");
  };

  theThing = {
    longStr: new Array(1000000).join('*'),
    someMethod: function () {
      // someMethod shares the lexical environment with `unused`!
      // This causes originalThing to be held in memory forever,
      // creating an infinite linked list of retained memory!
    }
  };
}

setInterval(replaceThing, 10); // 💥 Crashes process in seconds!
```

---

### Q108: How do you take and analyze a Heap Snapshot using Chrome DevTools or `v8.writeHeapSnapshot()`?
**Answer:**
Programmatic generation in production without stopping the service:

```javascript
import v8 from 'node:v8';
import fs from 'node:fs';

function captureHeapSnapshot() {
  const fileName = `/tmp/heap-${Date.now()}.heapsnapshot`;
  const savedPath = v8.writeHeapSnapshot(fileName);
  console.log(`Heap snapshot written to ${savedPath}`);
}

// Trigger snapshot on demand or when heapUsed > 80%
if (process.memoryUsage().heapUsed > 1024 * 1024 * 1024) {
  captureHeapSnapshot();
}
```

**Analysis in Chrome DevTools:**
1. Open Chrome -> `chrome://inspect` -> Click "Open dedicated DevTools for Node".
2. Go to "Memory" tab -> Click "Load" -> Select `.heapsnapshot` file.
3. Compare two snapshots (Snapshot 1 vs. Snapshot 2) in **Comparison View** sorted by **# Alloc** and **Size Delta**.

---

### Q109: What is the difference between Shallow Size and Retained Size in Heap Profiling?
**Answer:**
- **Shallow Size**: The memory directly allocated for the object itself (e.g. its own properties and type descriptors, typically 32-64 bytes).
- **Retained Size**: The total memory that would be freed if this object were deleted and garbage collected (includes all child objects, arrays, and buffers reachable **only** through this object).

---

### Q110: What are `WeakMap`, `WeakSet`, and `WeakRef` and how do they prevent Memory Leaks?
**Answer:**
- **`WeakMap` / `WeakSet`**: Hold "weak" references to key objects. If no other references to the key object exist, the entry is automatically collected by GC without manual deletion.
- **`WeakRef`**: Lets you hold a weak reference to a target object while still allowing dereferencing via `.deref()`.
- **`FinalizationRegistry`**: Registers a callback to execute after an object has been garbage collected (useful for C++ resource cleanup).

```javascript
// Metadata cache that automatically cleans up when user object is GC'ed
const userMetadataCache = new WeakMap();

function trackUser(userObj) {
  userMetadataCache.set(userObj, { lastSeen: Date.now() });
}

let user = { id: 101, name: 'Alice' };
trackUser(user);

user = null; // user object and its entry in userMetadataCache are both freed by GC!
```

---

### Q111: How do you use the Node.js V8 Profiler (`--prof`) and `node --prof-process` to detect CPU and memory bottlenecks?
**Answer:**

```bash
# 1. Start application with V8 sampling profiler
node --prof app.js

# 2. Run load test (e.g. using autocannon)
npx autocannon -c 100 -d 30 http://localhost:3000

# 3. Stop app -> produces isolate-0x...-v8.log

# 4. Process raw V8 tick log into human-readable analysis
node --prof-process isolate-0x*.log > processed_profile.txt
```
The output identifies the top C++ and JavaScript "Hot Spots" consuming CPU ticks.

---

### Q112: How do you configure `node --inspect` and connect remote debugging securely in Kubernetes/Production?
**Answer:**
Never expose `--inspect=0.0.0.0:9229` directly to the public internet (allows arbitrary remote code execution).

```bash
# 1. Run Node.js with local loopback inspection
node --inspect=127.0.0.1:9229 app.js

# 2. Forward port securely via SSH or kubectl:
kubectl port-forward pod/my-node-pod-6f5d 9229:9229

# 3. Connect Chrome:
# Open chrome://inspect -> Target localhost:9229
```

---

### Q113: What is the `--diagnostic-dir` flag and Diagnostic Report (`process.report`)?
**Answer:**
Diagnostic reports generate a comprehensive JSON crash report containing JavaScript stack traces, native C++ call stacks, OS resource limits, loaded dynamic libraries, and memory statistics.

```javascript
// Trigger report on fatal error or unhandled signal
import process from 'node:process';

process.report.directory = '/var/log/reports';
process.report.filename = `report-${Date.now()}.json`;

// Programmatic capture:
process.report.writeReport();
```

CLI flags:
```bash
node --report-uncaught-exception --report-on-signal --report-on-fatalerror app.js
```

---

### Q114: How does Node.js handle Off-Heap Memory and C++ Object Finalization?
**Answer:**
- Memory allocated via `malloc` in C++ addons or `ArrayBuffer` backing stores resides outside the V8 heap.
- V8 notifies the GC of external memory pressure using `v8::Isolate::AdjustAmountOfExternalAllocatedMemory()`.
- When the wrapping JavaScript object is collected, V8 invokes the C++ destructor or Node-API finalizer callback to free the raw OS memory.

---

### Q115: What is Garbage Collection Thrashing and how do you diagnose it?
**Answer:**
GC Thrashing occurs when the application allocates short-lived objects so rapidly that the GC spends 30-80% of total CPU time continuously running Scavenge and Mark-Sweep cycles, leaving minimal CPU for application logic.

**Diagnosis:**
```bash
node --trace-gc --trace-gc-nvp app.js
```
Shows frequency, duration, and memory reclaimed per GC pause.

---

### Q116: How do you implement an In-Memory LRU (Least Recently Used) Cache with strict memory and entry boundaries?
**Answer:**
Using an unconstrained JS `Map` or Object leads to memory leaks. An LRU cache enforces an upper bound and evicts the oldest accessed items.

```javascript
class LRUCache {
  constructor(capacity) {
    this.capacity = capacity;
    this.cache = new Map(); // Map preserves insertion order in JS!
  }

  get(key) {
    if (!this.cache.has(key)) return undefined;
    const val = this.cache.get(key);
    // Refresh position (delete and re-insert at tail)
    this.cache.delete(key);
    this.cache.set(key, val);
    return val;
  }

  set(key, value) {
    if (this.cache.has(key)) {
      this.cache.delete(key);
    } else if (this.cache.size >= this.capacity) {
      // Evict oldest item (first key in map)
      const oldestKey = this.cache.keys().next().value;
      this.cache.delete(oldestKey);
    }
    this.cache.set(key, value);
  }
}
```

---

### Q117: What is Object Pooling and how does it reduce V8 GC pressure in high-throughput servers?
**Answer:**
Object pooling reuses pre-allocated object instances instead of constantly instantiating and discarding thousands of objects per second.

```javascript
class RequestContextPool {
  constructor(size = 1000) {
    this.pool = Array.from({ length: size }, () => ({
      requestId: null,
      startTime: 0,
      user: null
    }));
  }

  acquire(requestId) {
    const ctx = this.pool.pop() || { requestId: null, startTime: 0, user: null };
    ctx.requestId = requestId;
    ctx.startTime = Date.now();
    return ctx;
  }

  release(ctx) {
    ctx.requestId = null;
    ctx.user = null;
    this.pool.push(ctx); // Reused for next request!
  }
}
```

---

### Q118: How do String Interning and String Slicing affect memory in V8?
**Answer:**
- **Sliced Strings**: In older V8 versions, `str.slice(0, 10)` on a 10MB string created a "sliced string" pointer that retained the **entire 10MB parent string in memory**!
- In modern V8, small slices are flattened and copied to prevent parent string leaks.

---

### Q119: What is `gc()` in Node.js and why should `--expose-gc` never be used in production?
**Answer:**
Running `node --expose-gc` exposes the global `gc()` function.
- Manually calling `gc()` forces a **full synchronous Stop-the-World Major GC cycle**, freezing the Event Loop and stalling all active user requests.
- V8's adaptive heuristics are far better at scheduling incremental, concurrent GC cycles than manual triggers.

---

### Q120: How do you detect native memory leaks in C++ Addons using Valgrind or ASan (AddressSanitizer)?
**Answer:**

```bash
# Compile addon with AddressSanitizer flags
export CFLAGS="-fsanitize=address -g"
export CXXFLAGS="-fsanitize=address -g"
npm rebuild

# Run node with ASan preloaded
ASAN_OPTIONS=detect_leaks=1 node test_addon.js
```
ASan prints the exact C++ source line, file, and call stack where un-freed `malloc`/`new` memory was allocated.

---

### Q121: How do you monitor Memory Leaks automatically in Continuous Integration (CI/CD)?
**Answer:**
Run load test scripts with `v8.getHeapStatistics()` before and after. If `used_heap_size` grows monotonically across 10,000 requests without leveling off, fail the CI test.

---

### Q122: What is the impact of JSON parsing on memory allocation and how can large payloads cause crashes?
**Answer:**
`JSON.parse(hugeString)` creates millions of small V8 heap objects in milliseconds.
If a 200MB JSON payload is parsed:
- Consumes 200MB string + ~800MB V8 Heap objects (4x expansion factor).
- Blocks the Event Loop for 300-800ms.
- **Solution**: Stream and filter with `stream-json` or SAX parsers.

---

### Q123: What are Finalizers and the `node:v8` Startup Snapshot API?
**Answer:**
Introduced in Node.js v18.6+:
- `v8.startupSnapshot`: Allows creating a customized heap snapshot at build time that contains pre-initialized application state, pre-parsed schemas, and loaded dependencies.
- Booting from a startup snapshot reduces cold-start latency (e.g. AWS Lambda) from 500ms to <10ms.

---

### Q124: How does V8 handle Large Object Space (LOS)?
**Answer:**
- Objects larger than a certain threshold (usually >512KB) bypass New Space entirely and are allocated directly in **Large Object Space**.
- Large Object Space items are never moved or compacted during GC (moved pointers for multi-megabyte arrays would be too expensive); their pages are simply unmapped when collected.

---

### Q125: What is the Memory Cost of Async Stack Traces (`Error.stackTraceLimit`)?
**Answer:**
`Error.stackTraceLimit` controls how many stack frames V8 captures when `new Error()` is constructed (default: 10).
Setting `Error.stackTraceLimit = Infinity` causes massive memory retention and CPU overhead on high-frequency error construction. Keep it at 10-20 in production.


---

# Part 6: High-Scale Networking, HTTP/2, HTTP/3 & WebSockets (Q126 - Q145)

---

### Q126: How does HTTP Keep-Alive connection pooling work in Node.js, and why is `http.Agent` critical?
**Answer:**
By default in Node.js, `http.globalAgent` has `keepAlive: true` (since v19). In older versions or custom clients, omitting Keep-Alive causes Node.js to perform a full **TCP 3-Way Handshake + TLS Handshake on EVERY single outbound HTTP request**, destroying throughput and exhausting ephemeral ports (`EADDRNOTAVAIL`).

```javascript
import http from 'node:http';
import https from 'node:https';

// Production HTTP Agent Configuration
const keepAliveAgent = new https.Agent({
  keepAlive: true,
  keepAliveMsecs: 30000,   // Ping interval on idle sockets
  maxSockets: 100,         // Max concurrent sockets per host
  maxFreeSockets: 10,      // Max idle sockets kept open in pool
  timeout: 60000           // Socket inactivity timeout
});

// Using the pooled agent in outbound fetch / request
const req = https.request('https://api.internal-mesh.com/data', {
  agent: keepAliveAgent
}, (res) => { ... });
```

---

### Q127: What is Socket Starvation and Ephemeral Port Exhaustion (`EADDRNOTAVAIL`)?
**Answer:**
- When an outgoing TCP connection is closed, the OS keeps the socket in a `TIME_WAIT` state for 60-120 seconds to catch delayed network packets.
- An OS has ~65,535 total ports, with ~28,000 ephemeral ports.
- Firing 500 requests/sec without Keep-Alive consumes all available ephemeral ports within 1 minute, throwing `Error: connect EADDRNOTAVAIL`.
- **Fix**: Reuse sockets via `keepAlive: true` and increase OS range `net.ipv4.ip_local_port_range`.

---

### Q128: How do HTTP/1.1, HTTP/2, and HTTP/3 (QUIC) differ in Node.js architecture?
**Answer:**

```
+-----------------------------------------------------------------------------+
| Feature             | HTTP/1.1          | HTTP/2               | HTTP/3     |
+---------------------+-------------------+----------------------+------------+
| Transport           | TCP               | TCP                  | UDP (QUIC) |
| Multiplexing        | No (1 req/conn)   | Yes (Binary Streams) | Yes        |
| Head-of-Line (HoL)  | Application-level | TCP-level packet loss| Solved     |
| Framing             | Text-based        | Binary               | Binary     |
| Header Compression  | None              | HPACK                | QPACK      |
| Node.js Module      | `node:http`       | `node:http2`         | `undici`   |
+-----------------------------------------------------------------------------+
```

---

### Q129: How do you build an HTTP/2 Server with Multiplexing and Server Push in Node.js?
**Answer:**

```javascript
import http2 from 'node:http2';
import fs from 'node:fs';

const server = http2.createSecureServer({
  key: fs.readFileSync('server.key'),
  cert: fs.readFileSync('server.crt')
});

server.on('stream', (stream, headers) => {
  const path = headers[':path'];

  if (path === '/') {
    stream.respond({
      'content-type': 'text/html; charset=utf-8',
      ':status': 200
    });
    stream.end('<h1>Welcome to HTTP/2 Multiplexed Server</h1>');
  }
});

server.listen(8443);
```

---

### Q130: What is the WebSocket Handshake and Upgrade process in Node.js (`http.Server` `'upgrade'` event)?
**Answer:**
A WebSocket connection begins as a standard HTTP/1.1 `GET` request with upgrade headers:
- `Upgrade: websocket`
- `Connection: Upgrade`
- `Sec-WebSocket-Key: <base64-random-key>`

Node.js emits the `'upgrade'` event on `http.Server`, providing the raw duplex `net.Socket`:

```javascript
import http from 'node:http';
import { WebSocketServer } from 'ws';

const server = http.createServer((req, res) => {
  res.writeHead(200);
  res.end('HTTP API Running');
});

const wss = new WebSocketServer({ noServer: true });

server.on('upgrade', (request, socket, head) => {
  const pathname = new URL(request.url, `http://${request.headers.host}`).pathname;

  // Custom Authentication & Path Routing before upgrading socket
  if (pathname === '/ws/live-feed') {
    const token = request.headers['sec-websocket-protocol'];
    if (!isValidToken(token)) {
      socket.write('HTTP/1.1 401 Unauthorized\r\n\r\n');
      socket.destroy();
      return;
    }

    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});

server.listen(3000);
```

---

### Q131: How do you scale WebSockets to 1,000,000+ concurrent connections across multiple Node.js nodes?
**Answer:**
1. **OS Kernel Socket Tuning (`/etc/sysctl.conf`)**:
   ```ini
   fs.file-max = 2097152
   net.core.somaxconn = 65535
   net.ipv4.tcp_max_syn_backlog = 65535
   ```
2. **Cluster & Multi-Pod Architecture**:
   - WebSockets are stateful; clients connect to different Node.js servers.
   - Use **Redis Pub/Sub** or **Kafka** message bus to broadcast events across nodes.
3. **Memory Footprint**:
   - Use `ws` with `perMessageDeflate: false` (compression allocates ~300KB RAM per socket, ballooning 1M sockets to 300GB RAM!).

---

### Q132: What is Undici and why is it faster than the native `node:http` client?
**Answer:**
`undici` is the official modern, next-generation HTTP/1.1 client written from scratch for Node.js:
- Powers global `fetch()` in Node.js v18+.
- **Zero-Copy Parser**: Directly integrated with V8 memory pointers without intermediate string/buffer allocations.
- **Pipelining & Connection Pooling**: Built-in HTTP Pipelining support.
- Up to **300% higher throughput** and lower CPU utilization than legacy `http.request`.

```javascript
import { Client } from 'undici';

const client = new Client('https://api.github.com');
const { statusCode, body } = await client.request({
  path: '/users/octocat',
  method: 'GET',
  headers: { 'user-agent': 'node-undici' }
});

const data = await body.json();
```

---

### Q133: What is TCP Nagle's Algorithm (`socket.setNoDelay()`) and when should it be disabled?
**Answer:**
- **Nagle's Algorithm**: Buffers small outgoing packets and waits for an ACK from the receiver before sending more data to avoid network congestion.
- **The Problem**: In real-time apps (gaming, chat, microservice RPC), Nagle's algorithm + TCP Delayed ACK causes **40ms - 200ms latency spikes**.
- **`socket.setNoDelay(true)`**: Disables Nagle's algorithm (`TCP_NODELAY`), sending packets immediately with minimal latency. Enabled by default in Node.js HTTP servers.

---

### Q134: How do you build a Raw TCP Server and Client using the `node:net` module?
**Answer:**

```javascript
import net from 'node:net';

// TCP Echo Server
const server = net.createServer((socket) => {
  console.log('Client connected:', socket.remoteAddress, socket.remotePort);
  socket.setNoDelay(true); // Disable Nagle's

  socket.on('data', (buffer) => {
    console.log(`Received: ${buffer.toString().trim()}`);
    socket.write(`ECHO: ${buffer}`);
  });

  socket.on('close', () => console.log('Client disconnected'));
});

server.listen(9000, () => console.log('TCP Server listening on port 9000'));
```

---

### Q135: What is Server-Sent Events (SSE) and how does it compare to WebSockets in Node.js?
**Answer:**
SSE provides a lightweight, unidirectional (Server -> Client) text stream over standard HTTP/1.1 or HTTP/2 without WebSocket handshake complexity.

```javascript
import http from 'node:http';

http.createServer((req, res) => {
  if (req.url === '/events') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive'
    });

    const intervalId = setInterval(() => {
      const payload = JSON.stringify({ time: new Date().toISOString() });
      res.write(`data: ${payload}\n\n`); // Standard SSE framing format
    }, 1000);

    req.on('close', () => {
      clearInterval(intervalId);
      res.end();
    });
  }
}).listen(3000);
```

---

### Q136: How do you configure TLS/SSL Termination, ALPN, and SNI in `node:tls`?
**Answer:**
- **ALPN (Application-Layer Protocol Negotiation)**: Negotiates HTTP/2 (`h2`) vs. HTTP/1.1 during TLS handshake.
- **SNI (Server Name Indication)**: Allows hosting multiple SSL certificates for different domain names on a single IP address.

```javascript
import tls from 'node:tls';
import fs from 'node:fs';

const certA = tls.createSecureContext({
  key: fs.readFileSync('domainA.key'),
  cert: fs.readFileSync('domainA.crt')
});

const options = {
  SNICallback: (servername, cb) => {
    if (servername === 'domainA.com') {
      cb(null, certA);
    } else {
      cb(new Error('Unknown SNI domain'));
    }
  },
  ALPNProtocols: ['h2', 'http/1.1']
};
```

---

### Q137: How do you handle DNS Resolution caching to avoid blocking lookups in high-throughput services?
**Answer:**
`dns.lookup()` invokes the C library `getaddrinfo()`, which runs on the Libuv thread pool and **does not cache DNS TTLs**. Under 10,000 req/sec, this saturates thread pool workers.

**Solution**:
1. Use `c-ares` based `dns.resolve4()` (pure async network socket).
2. Use a caching DNS agent like `dnscache` or Undici built-in DNS cache:

```javascript
import dnscache from 'dnscache';

dnscache({
  enable: true,
  ttl: 300,      // 5 minutes
  cachesize: 1000
});
```

---

### Q138: What is HTTP Request Smuggling and how does Node.js defend against it?
**Answer:**
HTTP Request Smuggling occurs when a front-end proxy and a backend Node.js server interpret ambiguous `Content-Length` (CL) and `Transfer-Encoding: chunked` (TE) headers differently (CL-TE or TE-CL conflicts).
- **Node.js Defense**: `llhttp` parser strictly rejects requests containing conflicting `Content-Length` and `Transfer-Encoding` headers or malformed chunked encodings with `400 Bad Request` (`HPE_UNEXPECTED_CONTENT_LENGTH`).

---

### Q139: How do you implement Graceful Drain for TCP sockets during deployment?
**Answer:**

```javascript
function gracefulSocketDrain(server) {
  const openSockets = new Set();

  server.on('connection', (socket) => {
    openSockets.add(socket);
    socket.on('close', () => openSockets.delete(socket));
  });

  return async function closeAll() {
    server.close();
    for (const socket of openSockets) {
      // Send FIN packet to politely close idle connections
      socket.end();
    }
  };
}
```

---

### Q140: How do you implement a Rate Limiter using the Leaking Bucket / Sliding Window Algorithm in Node.js?
**Answer:**

```javascript
class SlidingWindowRateLimiter {
  constructor(limit, windowMs) {
    this.limit = limit;
    this.windowMs = windowMs;
    this.requests = new Map(); // IP -> Array of timestamps
  }

  isAllowed(ip) {
    const now = Date.now();
    const windowStart = now - this.windowMs;

    let timestamps = this.requests.get(ip) || [];
    // Filter timestamps outside current sliding window
    timestamps = timestamps.filter(ts => ts > windowStart);

    if (timestamps.length >= this.limit) {
      this.requests.set(ip, timestamps);
      return false; // Rate limit exceeded!
    }

    timestamps.push(now);
    this.requests.set(ip, timestamps);
    return true;
  }
}
```

---

### Q141: What is the difference between `socket.destroy()` and `socket.end()`?
**Answer:**
- **`socket.end([data])`**: Sends a TCP `FIN` packet. Half-closes the socket (graceful flush of outgoing buffer, still allows receiving remaining incoming data until peer closes).
- **`socket.destroy([error])`**: Immediately closes and destroys the underlying network handle. Discards unsent buffers and sends a `RST` packet if unread data is pending.

---

### Q142: How do you handle Cross-Origin Resource Sharing (CORS) preflight requests manually in Node.js core?
**Answer:**

```javascript
http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', 'https://trusted-domain.com');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Access-Control-Max-Age', '86400'); // Cache preflight for 24h

  if (req.method === 'OPTIONS') {
    res.writeHead(204); // No content for preflight
    res.end();
    return;
  }

  // Handle actual API requests
  res.writeHead(200);
  res.end(JSON.stringify({ status: 'OK' }));
});
```

---

### Q143: How do you configure Unix Domain Sockets (UDS) for ultra-fast inter-container communication?
**Answer:**
Unix Domain Sockets bypass the TCP/IP network stack entirely (zero checksum calculation, zero packet routing overhead), providing ~2x lower latency than `localhost:3000`.

```javascript
import http from 'node:http';
import fs from 'node:fs';

const SOCKET_PATH = '/tmp/app.sock';

// Clean up stale socket file if present
if (fs.existsSync(SOCKET_PATH)) {
  fs.unlinkSync(SOCKET_PATH);
}

const server = http.createServer((req, res) => res.end('Fast UDS Response'));
server.listen(SOCKET_PATH, () => {
  fs.chmodSync(SOCKET_PATH, '0777'); // Set permissions
  console.log(`Listening on Unix Domain Socket: ${SOCKET_PATH}`);
});
```

---

### Q144: What is gRPC and Protocol Buffers (protobuf) integration in Node.js?
**Answer:**
gRPC uses HTTP/2 transport and binary Protocol Buffers for fast, strongly-typed Remote Procedure Calls (RPC) between microservices.
- Uses `@grpc/grpc-js` (pure JS implementation) or `@grpc/proto-loader`.
- Streams bidirectional data efficiently over single multiplexed HTTP/2 connections.

---

### Q145: How do you implement a Reverse Proxy Gateway with Request Retries and Load Balancing?
**Answer:**

```javascript
import http from 'node:http';

const BACKENDS = ['http://backend-1:3000', 'http://backend-2:3000'];
let current = 0;

http.createServer((req, res) => {
  const target = BACKENDS[current++ % BACKENDS.length];
  const proxyReq = http.request(target + req.url, {
    method: req.method,
    headers: req.headers
  }, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', () => {
    res.writeHead(502);
    res.end('Bad Gateway');
  });

  req.pipe(proxyReq);
}).listen(80);
```


---

# Part 7: Databases, Caching, Storage & Messaging Architecture (Q146 - Q165)

---

### Q146: How does Database Connection Pooling work in Node.js, and how do you size a pool for PostgreSQL/MySQL?
**Answer:**
Establishing a TCP connection + TLS Handshake + DB authentication takes 30-100ms. A Connection Pool maintains a persistent pool of authenticated database client connections.

```javascript
import pg from 'pg';
const { Pool } = pg;

const pool = new Pool({
  host: 'postgres-db.production.internal',
  user: 'app_user',
  password: process.env.DB_PASSWORD,
  database: 'payments',
  max: 20,                   // Maximum pool connections per Node.js process
  min: 4,                    // Keep 4 idle connections always warm
  idleTimeoutMillis: 30000,  // Close idle connection after 30s
  connectionTimeoutMillis: 2000 // Fail-fast if no connection available in 2s
});
```

**Sizing Formula (HikariCP / Postgres Rule):**
$$\text{Pool Size} = (\text{CPU Cores} \times 2) + \text{Effective Spindle Count (Disk IOPS)}$$
For a database server with 8 cores and fast NVMe SSD, pool size should be around 16-20 total active connections.
If you have 10 Kubernetes Node.js pods with `max: 20`, total connections = 200 (ensure Postgres `max_connections` is configured to 300+ or use **PgBouncer** connection pooler).

---

### Q147: How do you handle Database Transactions with Isolation Levels and Rollback safely?
**Answer:**
Transactions must always release the acquired client back to the pool in a `finally` block to prevent connection pool exhaustion.

```javascript
async function transferFunds(fromAccount, toAccount, amount) {
  const client = await pool.connect(); // Checkout dedicated connection
  try {
    await client.query('BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE');

    const debitRes = await client.query(
      'UPDATE accounts SET balance = balance - $1 WHERE id = $2 AND balance >= $1 RETURNING balance',
      [amount, fromAccount]
    );
    if (debitRes.rowCount === 0) {
      throw new Error('Insufficient funds or account not found');
    }

    await client.query(
      'UPDATE accounts SET balance = balance + $1 WHERE id = $2',
      [amount, toAccount]
    );

    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err; // Re-throw for upstream error handling
  } finally {
    client.release(); // 🛡️ CRITICAL: Always return client to pool!
  }
}
```

---

### Q148: What is Cache Stampede (Thundering Herd) and how do you prevent it using Mutex / SingleFlight in Node.js?
**Answer:**
When a hot cache key (e.g. `homepage_feed`) expires in Redis under 50,000 req/sec, thousands of concurrent requests simultaneously miss the cache and hit the primary database, crashing the database.

**Solution: SingleFlight (Promise Coalescing)**:
Ensures only **one** in-flight database query executes for duplicate concurrent requests; all other callers wait for the same Promise.

```javascript
class SingleFlight {
  constructor() {
    this.inFlight = new Map();
  }

  async do(key, fetchFn) {
    if (this.inFlight.has(key)) {
      return this.inFlight.get(key); // Join active in-flight Promise!
    }

    const promise = (async () => {
      try {
        return await fetchFn();
      } finally {
        this.inFlight.delete(key); // Cleanup when done
      }
    })();

    this.inFlight.set(key, promise);
    return promise;
  }
}

const flight = new SingleFlight();

async function getHotProduct(id) {
  const cached = await redis.get(`product:${id}`);
  if (cached) return JSON.parse(cached);

  return flight.do(`fetch_product_${id}`, async () => {
    // Exactly 1 database query executes regardless of concurrent hits
    const data = await db.query('SELECT * FROM products WHERE id = $1', [id]);
    await redis.set(`product:${id}`, JSON.stringify(data), 'EX', 60);
    return data;
  });
}
```

---

### Q149: How do you implement Distributed Locking using Redis (Redlock Algorithm) in Node.js?
**Answer:**
In a multi-pod cluster, in-memory locks do not work across processes. Redis distributed locking coordinates exclusive tasks (e.g. running midnight billing calculation once across 20 pods).

```javascript
import crypto from 'node:crypto';

class RedisLock {
  constructor(redisClient) {
    this.redis = redisClient;
  }

  async acquireLock(resourceKey, ttlMs = 5000) {
    const lockValue = crypto.randomUUID(); // Unique identifier for the lock owner
    // SET resourceKey lockValue NX PX ttlMs (Atomic lock acquisition)
    const acquired = await this.redis.set(resourceKey, lockValue, 'PX', ttlMs, 'NX');
    return acquired === 'OK' ? lockValue : null;
  }

  async releaseLock(resourceKey, lockValue) {
    // Lua script: Release ONLY if lockValue matches (prevents releasing someone else's expired lock)
    const luaScript = `
      if redis.call("get", KEYS[1]) == ARGV[1] then
        return redis.call("del", KEYS[1])
      else
        return 0
      end
    `;
    return await this.redis.eval(luaScript, 1, resourceKey, lockValue);
  }
}
```

---

### Q150: What is the N+1 Query Problem in ORMs (Prisma / TypeORM / Sequelize) and how is it solved using DataLoader?
**Answer:**
When fetching 100 users and their posts:
- 1 Query to get 100 users (`SELECT * FROM users`).
- 100 Queries in a loop to get posts for each user (`SELECT * FROM posts WHERE user_id = ?`).
- Total: 101 queries (N+1)!

**DataLoader Solution (Batching & Per-Request Caching)**:
Coalesces individual requests inside a single Event Loop tick into a single `IN (...)` batch query.

```javascript
import DataLoader from 'dataloader';

// Batch loader function
const postLoader = new DataLoader(async (userIds) => {
  // Executes 1 query for ALL user IDs in the tick:
  const posts = await db.query('SELECT * FROM posts WHERE user_id = ANY($1)', [userIds]);
  
  // Map posts back to their respective userIds in exact array order
  return userIds.map(id => posts.filter(p => p.userId === id));
});

// Inside GraphQL or REST controller:
const postsUser1 = postLoader.load(1); // Batch enqueued
const postsUser2 = postLoader.load(2); // Batch enqueued
// Exactly 1 SQL query fired!
```

---

### Q151: How do you implement a robust Background Job Queue using BullMQ and Redis Streams?
**Answer:**
BullMQ uses Redis Streams and sorted sets to manage reliable job queues with exponential backoff retries, concurrency limits, and dead-letter queues (DLQ).

```javascript
import { Queue, Worker } from 'bullmq';

const redisConfig = { host: 'localhost', port: 6379 };

// 1. Job Producer
const emailQueue = new Queue('emailQueue', { connection: redisConfig });
await emailQueue.add('welcome-email', {
  to: 'user@domain.com',
  template: 'welcome'
}, {
  attempts: 3,
  backoff: { type: 'exponential', delay: 2000 }
});

// 2. Job Consumer / Worker
const worker = new Worker('emailQueue', async (job) => {
  console.log(`Processing email to: ${job.data.to}`);
  await sendActualEmail(job.data);
}, {
  connection: redisConfig,
  concurrency: 10 // 10 parallel jobs per process
});

worker.on('failed', (job, err) => {
  console.error(`Job ${job.id} failed after ${job.attemptsMade} attempts:`, err);
});
```

---

### Q152: How do you stream Millions of Database Records to an HTTP Response without High Memory Usage?
**Answer:**
Loading 5,000,000 database rows into an array blows through V8 heap limits. Use **Cursor Streaming**.

```javascript
import QueryStream from 'pg-query-stream';
import { pipeline } from 'node:stream/promises';
import { Transform } from 'node:stream';

async function exportLargeCsv(req, res) {
  const client = await pool.connect();
  try {
    const query = new QueryStream('SELECT id, name, email, created_at FROM large_users_table');
    const stream = client.query(query);

    res.setHeader('Content-Type', 'text/csv');
    res.setHeader('Content-Disposition', 'attachment; filename="export.csv"');

    const csvFormat = new Transform({
      objectMode: true,
      transform(row, enc, cb) {
        cb(null, `${row.id},"${row.name}","${row.email}",${row.created_at}\n`);
      }
    });

    await pipeline(stream, csvFormat, res);
  } finally {
    client.release();
  }
}
```

---

### Q153: How do you implement Multi-Level Caching (L1 In-Memory + L2 Redis) with Cache Invalidation?
**Answer:**
- **L1 Cache (In-Memory)**: Sub-microsecond access (~0.05ms) using `lru-cache`.
- **L2 Cache (Distributed Redis)**: Sub-millisecond access (~1ms) shared across all pods.
- **Cache Invalidation**: Broadcast invalidation events via Redis Pub/Sub so all pod L1 caches invalidate stale keys simultaneously.

```javascript
import { LRUCache } from 'lru-cache';

const l1Cache = new LRUCache({ max: 5000, ttl: 1000 * 60 * 5 }); // 5 min TTL

async function getCachedUser(id) {
  // 1. Check L1
  if (l1Cache.has(id)) return l1Cache.get(id);

  // 2. Check L2
  const redisVal = await redis.get(`user:${id}`);
  if (redisVal) {
    const parsed = JSON.parse(redisVal);
    l1Cache.set(id, parsed);
    return parsed;
  }

  // 3. Query DB
  const user = await db.getUserById(id);
  if (user) {
    await redis.set(`user:${id}`, JSON.stringify(user), 'EX', 3600);
    l1Cache.set(id, user);
  }
  return user;
}
```

---

### Q154: What is Optimistic vs. Pessimistic Locking in database models and how is it implemented in Node.js?
**Answer:**
- **Pessimistic Locking**: `SELECT ... FOR UPDATE`. Locks the row at database level until transaction finishes. Prevents any other transaction from reading/updating.
- **Optimistic Locking**: Uses a `version` column. No database locks; updates succeed only if the version hasn't changed.

```javascript
// Optimistic Locking Pattern
async function updateProductStock(productId, quantityToDeduct) {
  while (true) {
    const product = await db.query('SELECT stock, version FROM products WHERE id = $1', [productId]);
    if (product.stock < quantityToDeduct) throw new Error('Out of stock');

    const result = await db.query(
      'UPDATE products SET stock = stock - $1, version = version + 1 WHERE id = $2 AND version = $3',
      [quantityToDeduct, productId, product.version]
    );

    if (result.rowCount === 1) {
      return; // Success!
    }
    // If rowCount === 0, another concurrent transaction updated the row; retry loop!
  }
}
```

---

### Q155: How do you handle Read/Write Replica routing in Node.js applications?
**Answer:**
Route write queries (`INSERT`, `UPDATE`, `DELETE`) to Master DB instance and read queries (`SELECT`) to Read Replica pool with replication lag awareness.

```javascript
class DatabaseRouter {
  constructor(masterPool, replicaPool) {
    this.master = masterPool;
    this.replica = replicaPool;
  }

  query(sql, params, { useMaster = false } = {}) {
    const isWrite = /^\s*(INSERT|UPDATE|DELETE|ALTER|CREATE)/i.test(sql);
    if (isWrite || useMaster) {
      return this.master.query(sql, params);
    }
    return this.replica.query(sql, params);
  }
}
```

---

### Q156: How do you implement Idempotency Keys for Payment / Order APIs in Node.js?
**Answer:**
Prevents duplicate charges when a mobile client retries a network request.

```javascript
async function processPaymentWithIdempotency(req, res) {
  const idempotencyKey = req.headers['x-idempotency-key'];
  if (!idempotencyKey) return res.status(400).send('Missing Idempotency-Key header');

  const cacheKey = `idempotency:${idempotencyKey}`;
  // Atomic SET NX with 24-hour expiration
  const acquired = await redis.set(cacheKey, JSON.stringify({ state: 'PROCESSING' }), 'EX', 86400, 'NX');

  if (!acquired) {
    const existing = JSON.parse(await redis.get(cacheKey));
    if (existing.state === 'PROCESSING') {
      return res.status(409).send('Transaction already in progress. Please retry shortly.');
    }
    return res.status(200).json(existing.response); // Return cached response
  }

  try {
    const paymentResult = await stripe.charges.create(req.body);
    await redis.set(cacheKey, JSON.stringify({ state: 'COMPLETED', response: paymentResult }), 'EX', 86400);
    return res.status(200).json(paymentResult);
  } catch (err) {
    await redis.del(cacheKey); // Release lock on error
    return res.status(500).send(err.message);
  }
}
```

---

### Q157: What is Database Connection Leak and how do you detect it in Node.js?
**Answer:**
A connection leak occurs when `pool.connect()` is called without `client.release()`. Over time, the pool runs out of available connections, causing all subsequent queries to hang indefinitely until `connectionTimeoutMillis` triggers.

**Detection:**
Log `pool.totalCount`, `pool.idleCount`, and `pool.waitingCount` metrics every 10 seconds. If `waitingCount > 0` and `idleCount === 0`, a leak exists.

---

### Q158: How do you integrate Apache Kafka with Node.js using `kafkajs` for High-Throughput Event Streaming?
**Answer:**

```javascript
import { Kafka } from 'kafkajs';

const kafka = new Kafka({
  clientId: 'order-service',
  brokers: ['kafka-broker-1:9092', 'kafka-broker-2:9092']
});

// Producer
const producer = kafka.producer();
await producer.connect();
await producer.send({
  topic: 'order-events',
  messages: [{ key: 'user-123', value: JSON.stringify({ orderId: 99, total: 150 }) }]
});

// Consumer Group
const consumer = kafka.consumer({ groupId: 'inventory-group' });
await consumer.connect();
await consumer.subscribe({ topic: 'order-events', fromBeginning: false });

await consumer.run({
  eachMessage: async ({ topic, partition, message }) => {
    const event = JSON.parse(message.value.toString());
    console.log(`Received order event: ${event.orderId} from partition ${partition}`);
  }
});
```

---

### Q159: What is the Outbox Pattern and why is it essential for Distributed Microservices in Node.js?
**Answer:**
When an API updates a database AND sends a message to Kafka/RabbitMQ:
If the DB commit succeeds but the Kafka publish fails (or process crashes), data is in an inconsistent state.

**Transactional Outbox Pattern**:
1. Save business data AND an outbox record in the **same database transaction**.
2. A separate background worker reads the outbox table and publishes to Kafka with guaranteed at-least-once delivery.

---

### Q160: How does ElasticSearch / OpenSearch integration work with Node.js for Full-Text Search?
**Answer:**
Use `@elastic/elasticsearch` with bulk indexing pipelines and scroll/search_after pagination for millions of documents.

---

### Q161: How do you handle MongoDB Replica Sets, Write Concerns (`w: "majority"`), and Read Preferences in Mongoose?
**Answer:**
- **Write Concern `w: "majority"`**: Write is only acknowledged after being committed to a majority of replica set members (prevents data loss during leader failover).
- **Read Preference `secondaryPreferred`**: Offloads reads to secondary nodes.

```javascript
import mongoose from 'mongoose';

await mongoose.connect('mongodb://mongo-1:27017,mongo-2:27017/shop?replicaSet=rs0', {
  writeConcern: { w: 'majority', j: true, wtimeout: 5000 },
  readPreference: 'secondaryPreferred'
});
```

---

### Q162: What is the difference between Redis Pub/Sub and Redis Streams?
**Answer:**
- **Redis Pub/Sub**: Fire-and-forget. Messages are **not persisted**. If a consumer is offline, messages are lost forever.
- **Redis Streams (`XADD`, `XREADGROUP`)**: Persistent, append-only log with consumer groups, message acknowledgement (`XACK`), and replay capability.

---

### Q163: How do you implement Database Connection Retry with Exponential Backoff and Jitter?
**Answer:**

```javascript
async function connectWithRetry(connectFn, maxRetries = 5, baseDelayMs = 500) {
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await connectFn();
    } catch (err) {
      if (attempt === maxRetries) throw err;
      // Exponential backoff with Full Jitter
      const delay = Math.random() * (baseDelayMs * Math.pow(2, attempt));
      console.warn(`Connection failed (attempt ${attempt}/${maxRetries}). Retrying in ${delay.toFixed(0)}ms...`);
      await new Promise(r => setTimeout(r, delay));
    }
  }
}
```

---

### Q164: How do you handle Database Schema Migrations reliably in CI/CD without application downtime?
**Answer:**
**Expand-Contract (Parallel Run) Migration Pattern**:
1. **Expand**: Add new columns/tables as optional (nullable). Deploy database migration.
2. **Deploy Code**: Deploy Node.js code that writes to both old and new columns, reads from new.
3. **Backfill**: Migrate historical records in batches.
4. **Contract**: Remove old columns in subsequent migration.

---

### Q165: How do you implement CQRS (Command Query Responsibility Segregation) in Node.js?
**Answer:**
Separates write models (Commands) that mutate relational DB state from read models (Queries) optimized with Denormalized NoSQL / Elasticsearch views populated asynchronously via Change Data Capture (CDC / Debezium).


---

# Part 8: Security, Cryptography, Authentication & Hardening (Q166 - Q185)

---

### Q166: What is Prototype Pollution and how does it compromise Node.js applications?
**Answer:**
Prototype Pollution occurs when user-controlled input modifies `Object.prototype`, injecting or altering properties on **all** JavaScript objects created across the entire Node.js runtime.

```javascript
// Malicious JSON payload:
// { "__proto__": { "isAdmin": true } }

function deepMerge(target, source) {
  for (const key in source) {
    if (typeof source[key] === 'object' && source[key] !== null) {
      if (!target[key]) target[key] = {};
      deepMerge(target[key], source[key]); // 🚨 VULNERABLE to prototype pollution!
    } else {
      target[key] = source[key];
    }
  }
  return target;
}
```

**Defense & Hardening:**
1. Block dangerous keys: `key === '__proto__' || key === 'constructor' || key === 'prototype'`.
2. Use objects with no prototype: `Object.create(null)` or `Map`.
3. Freeze prototype: `Object.freeze(Object.prototype)` or launch Node with `--disable-proto=delete` or `--disable-proto=throw`.

---

### Q167: What is Timing Attack and how do you use `crypto.timingSafeEqual()` to defend against it?
**Answer:**
Standard string comparison `strA === strB` returns `false` on the **first mismatched character** (fail-fast).
An attacker measuring nanosecond response times can guess passwords, API keys, or HMAC signatures character-by-character.

`crypto.timingSafeEqual(bufA, bufB)` executes in constant time regardless of where or whether characters differ.

```javascript
import crypto from 'node:crypto';

function secureCompare(userInput, secretToken) {
  const userBuf = Buffer.from(userInput);
  const secretBuf = Buffer.from(secretToken);

  // Buffers MUST be equal length for timingSafeEqual
  if (userBuf.length !== secretBuf.length) {
    // Hash both to equalize lengths before timingSafeEqual to avoid length oracle
    const hashA = crypto.createHash('sha256').update(userBuf).digest();
    const hashB = crypto.createHash('sha256').update(secretBuf).digest();
    crypto.timingSafeEqual(hashA, hashB);
    return false;
  }

  return crypto.timingSafeEqual(userBuf, secretBuf);
}
```

---

### Q168: How do you safely hash and verify passwords using `crypto.scrypt` or `argon2`?
**Answer:**
Never use fast cryptographic hashes (MD5, SHA-256) for passwords; GPUs can compute billions of SHA-256 hashes per second. Use memory-hard key derivation functions like **Argon2id** or **Scrypt**.

```javascript
import crypto from 'node:crypto';
import util from 'node:util';

const scrypt = util.promisify(crypto.scrypt);

async function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const derivedKey = await scrypt(password, salt, 64, {
    N: 16384, // CPU/memory cost
    r: 8,     // Block size
    p: 1      // Parallelization
  });
  return `${salt}:${derivedKey.toString('hex')}`;
}

async function verifyPassword(password, storedHash) {
  const [salt, key] = storedHash.split(':');
  const keyBuffer = Buffer.from(key, 'hex');
  const derivedKey = await scrypt(password, salt, 64, { N: 16384, r: 8, p: 1 });
  return crypto.timingSafeEqual(keyBuffer, derivedKey);
}
```

---

### Q169: What is ReDoS (Regular Expression Denial of Service) and how do you mitigate it?
**Answer:**
ReDoS occurs when a vulnerable regular expression with "evil regex" patterns (nested quantifiers like `(a+)+$`) experiences **catastrophic backtracking** on crafted non-matching input strings, locking the Event Loop at 100% CPU.

```javascript
// 🚨 VULNERABLE Regex:
const evilRegex = /(a+)+$/;
evilRegex.test('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaX'); // Freezes Node process for minutes!
```

**Mitigations:**
1. Audit regex with linters (`eslint-plugin-regexp`).
2. Set regexp backtrack limits in V8.
3. Use linear-time regular expression engines (e.g. `re2`).

---

### Q170: How do you implement Secure AES-256-GCM Encryption and Decryption in Node.js?
**Answer:**
AES-GCM (Galois/Counter Mode) provides **Authenticated Encryption with Associated Data (AEAD)**, ensuring both confidentiality and tamper-proof data integrity.

```javascript
import crypto from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 12; // 96-bit IV recommended for GCM
const KEY = crypto.randomBytes(32); // 256-bit key

function encrypt(plainText) {
  const iv = crypto.randomBytes(IV_LENGTH);
  const cipher = crypto.createCipheriv(ALGORITHM, KEY, iv);

  let encrypted = cipher.update(plainText, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  const authTag = cipher.getAuthTag().toString('hex'); // 16-byte authentication tag

  return {
    iv: iv.toString('hex'),
    encryptedData: encrypted,
    authTag
  };
}

function decrypt({ encryptedData, iv, authTag }) {
  const decipher = crypto.createDecipheriv(ALGORITHM, KEY, Buffer.from(iv, 'hex'));
  decipher.setAuthTag(Buffer.from(authTag, 'hex')); // Throws error if data was tampered!

  let decrypted = decipher.update(encryptedData, 'hex', 'utf8');
  decrypted += decipher.final('utf8');
  return decrypted;
}
```

---

### Q171: How do you sign and verify JWTs using Asymmetric RS256/ES256 instead of Symmetric HS256?
**Answer:**
- **HS256 (Shared Secret)**: Both the authentication server (signer) and every microservice (verifier) must share the secret key. If one service is compromised, attackers can forge valid tokens.
- **RS256 / ES256 (Public/Private Key)**: Only the Auth service holds the private key to sign. Downstream microservices only need the public key to verify signatures.

```javascript
import crypto from 'node:crypto';

// Generate asymmetric elliptic curve keypair
const { publicKey, privateKey } = crypto.generateKeyPairSync('ec', {
  namedCurve: 'prime256v1'
});

// Signing
function signPayload(data) {
  const signer = crypto.createSign('SHA256');
  signer.update(JSON.stringify(data));
  return signer.sign(privateKey, 'base64url');
}

// Verification
function verifyPayload(data, signature) {
  const verifier = crypto.createVerify('SHA256');
  verifier.update(JSON.stringify(data));
  return verifier.verify(publicKey, signature, 'base64url');
}
```

---

### Q172: What are essential HTTP Security Headers and how does `helmet` configure them?
**Answer:**

```javascript
import helmet from 'helmet';
import express from 'express';

const app = express();

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'trusted-cdn.com'"],
      objectSrc: ["'none'"]
    }
  },
  strictTransportSecurity: {
    maxAge: 63072000, // 2 years
    includeSubDomains: true,
    preload: true
  },
  frameguard: { action: 'deny' }, // X-Frame-Options: Clickjacking defense
  noSniff: true                   // X-Content-Type-Options: nosniff
}));
```

---

### Q173: How do you prevent SQL Injection and NoSQL Injection in Node.js?
**Answer:**
1. **SQL**: Always use **Parameterized Prepared Statements** (never concatenate strings!).
   ```javascript
   // ✅ SECURE Parameterized Query
   await pool.query('SELECT * FROM users WHERE email = $1', [email]);
   ```
2. **NoSQL (MongoDB/Mongoose)**: Sanitize object queries (prevent `$gt: ""` operator injection).
   ```javascript
   // ❌ VULNERABLE: req.body.password = { "$gt": "" } matches all users!
   User.findOne({ email: req.body.email, password: req.body.password });

   // ✅ SECURE: Sanitize input or force string types:
   User.findOne({
     email: String(req.body.email),
     password: String(req.body.password)
   });
   ```

---

### Q174: How do you implement Cross-Site Request Forgery (CSRF) Protection using SameSite Cookies and Double-Submit Tokens?
**Answer:**
1. **`SameSite=Strict` / `SameSite=Lax` Cookies**: Modern browsers automatically block cookies from being sent on cross-site sub-requests.
2. **Double-Submit Cookie Pattern**: Server sets a cryptographically random token in both a cookie and an HTTP header (`x-csrf-token`). Incoming mutation requests verify both match.

---

### Q175: What is Server-Side Request Forgery (SSRF) and how do you prevent it in Node.js fetch clients?
**Answer:**
SSRF occurs when an attacker forces the Node.js server to make outbound HTTP requests to internal cloud metadata IP addresses (`http://169.254.169.254/latest/meta-data/`) or internal network IPs (`10.0.0.0/8`, `192.168.0.0/16`, `127.0.0.1`).

**Defense**:
Resolve DNS before making request and validate that the resolved IP does not belong to private CIDR blocks.

```javascript
import dns from 'node:dns/promises';
import ipaddr from 'ipaddr.js';

async function safeFetch(urlStr) {
  const parsed = new URL(urlStr);
  const { address } = await dns.lookup(parsed.hostname);

  const ip = ipaddr.parse(address);
  if (ip.range() !== 'unicast') {
    throw new Error('Forbidden: SSRF attempt to internal/private IP blocked!');
  }

  return await fetch(urlStr);
}
```

---

### Q176: How do you securely handle Secrets and Environment Variables in Node.js without leaking to child processes or logs?
**Answer:**
- Use Node.js v20.6+ built-in `--env-file=.env` (avoids third-party `dotenv` dependency).
- Never log `process.env`.
- Explicitly pass whitelisted variables to child processes (`spawn(cmd, args, { env: whitelist })`).
- Fetch dynamic secrets at runtime from AWS Secrets Manager / HashiCorp Vault.

---

### Q177: What is Dependency Confusion and Typosquatting in NPM and how do you protect against it?
**Answer:**
- **Dependency Confusion**: Attacker registers a public npm package with the same name as a company's internal private package. If npm registry resolution is misconfigured, npm pulls the malicious public package.
- **Defenses**:
  1. Scope all internal packages (`@my-org/auth-lib`).
  2. Commit `package-lock.json` and enforce `npm ci` in CI/CD.
  3. Use `.npmrc` with strict scoping and private registry proxies (Artifactory/Nexus).

---

### Q178: How do you sanitize HTML to prevent Stored & Reflected XSS using `DOMPurify` / `sanitize-html`?
**Answer:**

```javascript
import sanitizeHtml from 'sanitize-html';

const dirty = '<script>alert("xss")</script><b>Hello</b> <a href="javascript:steal()">Click</a>';
const clean = sanitizeHtml(dirty, {
  allowedTags: ['b', 'i', 'em', 'strong', 'a'],
  allowedAttributes: { 'a': ['href'] },
  allowedSchemes: ['http', 'https', 'mailto'] // Blocks javascript: URLs
});

console.log(clean); // <b>Hello</b> <a>Click</a>
```

---

### Q179: What is the Node.js Policy System (`--experimental-policy`)?
**Answer:**
Allows enforcing strict cryptographic integrity checks on every loaded module via `policy.json`. If a module in `node_modules` is tampered with or dependencies are altered, Node.js terminates execution immediately.

```json
{
  "resources": {
    "./app.js": {
      "integrity": "sha384-xyz..."
    }
  }
}
```

---

### Q180: How do you implement Secure Session Management with `httpOnly`, `secure`, and `sameSite` cookies?
**Answer:**

```javascript
import session from 'express-session';
import RedisStore from 'connect-redis';

app.use(session({
  store: new RedisStore({ client: redisClient }),
  name: '__Host-sessionid', // Prefix ensures Secure + root path enforcement
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,        // Blocks document.cookie XSS access
    secure: true,          // Enforces HTTPS only
    sameSite: 'strict',    // CSRF defense
    maxAge: 1000 * 60 * 60 // 1 hour expiration
  }
}));
```

---

### Q181: How do you prevent Path Traversal attacks in file-serving APIs?
**Answer:**
Attackers pass `../../../../etc/passwd` to file download routes.

```javascript
import path from 'node:path';
import fs from 'node:fs';

const SAFE_ROOT = '/var/www/uploads';

function serveUserFile(filename, res) {
  // Resolve absolute path
  const safePath = path.resolve(SAFE_ROOT, filename);

  // Verify that resolved path begins with SAFE_ROOT directory
  if (!safePath.startsWith(SAFE_ROOT + path.sep)) {
    res.statusCode = 403;
    return res.end('Access Denied');
  }

  fs.createReadStream(safePath).pipe(res);
}
```

---

### Q182: What is AST Injection and Dynamic Code Execution vulnerability (`eval`, `new Function`, `vm`)?
**Answer:**
Executing user-supplied strings inside `eval()`, `new Function()`, or even Node's built-in `vm.runInContext()` allows attackers to break out and execute arbitrary OS commands.
- `vm` in Node.js is **NOT a security sandbox**; code running inside a VM can access the host `Function` constructor and execute `process.exit()`.
- Use isolated V8 engines like `isolated-vm` if running untrusted JavaScript.

---

### Q183: How do you enforce Mutual TLS (mTLS) between Node.js Microservices?
**Answer:**
mTLS requires both client and server to present and verify each other's X.509 SSL certificates against a private Certificate Authority (CA).

```javascript
import https from 'node:https';
import fs from 'node:fs';

const server = https.createServer({
  key: fs.readFileSync('server.key'),
  cert: fs.readFileSync('server.crt'),
  ca: fs.readFileSync('ca.crt'),
  requestCert: true,        // Demand client certificate
  rejectUnauthorized: true  // Reject handshake if client cert is invalid/untrusted
}, (req, res) => {
  const clientCert = req.socket.getPeerCertificate();
  console.log('Authenticated microservice client:', clientCert.subject.CN);
  res.end('mTLS Handshake Verified');
});

server.listen(443);
```

---

### Q184: How do you securely generate Cryptographically Strong Random Identifiers in Node.js?
**Answer:**
Never use `Math.random()` for tokens, session IDs, or password reset links (it is pseudo-random and predictable).
Use `crypto.randomBytes()`, `crypto.randomUUID()`, or `crypto.getRandomValues()`.

```javascript
import crypto from 'node:crypto';

const uuid = crypto.randomUUID(); // Fast v4 UUID
const randomToken = crypto.randomBytes(32).toString('hex'); // 256-bit cryptographically secure token
```

---

### Q185: What is Content Security Policy (CSP) Nonce and how is it generated per-request in Node.js?
**Answer:**
CSP Nonce generates a unique cryptographically random token per HTTP request and embeds it in the `Content-Security-Policy` header. Only inline `<script nonce="...">` tags matching the nonce are executed by the browser, blocking all inline XSS injections.


---

# Part 9: Production Scale, Observability, Modern Node.js (v18 - v22+) & Best Practices (Q186 - Q210)

---

### Q186: How do you instrument a Node.js Microservice with OpenTelemetry (OTel) for Distributed Tracing?
**Answer:**
Distributed Tracing propagates a `traceparent` context header (`W3C Trace Context`) across HTTP/gRPC boundaries to trace a request through dozens of microservices.

```javascript
// tracer.js (Loaded BEFORE application entrypoint via node --require ./tracer.js)
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';

const sdk = new NodeSDK({
  traceExporter: new OTLPTraceExporter({ url: 'grpc://otel-collector:4317' }),
  instrumentations: [getNodeAutoInstrumentations()]
});

sdk.start();

process.on('SIGTERM', () => {
  sdk.shutdown().finally(() => process.exit(0));
});
```

---

### Q187: How does `node:diagnostics_channel` work and why is it preferred over Monkey-Patching for APMs?
**Answer:**
Historically, APM tools (Datadog, New Relic) monkey-patched core Node.js methods (`http.createServer`, `fs.readFile`), causing performance regressions, memory leaks, and broken stack traces.

`diagnostics_channel` provides a native, zero-overhead pub/sub bus in Node.js core for emitting and subscribing to internal telemetry events.

```javascript
import diagnostics_channel from 'node:diagnostics_channel';

// 1. Subscribe to HTTP client request start channel
const channel = diagnostics_channel.channel('undici:request:create');

channel.subscribe((message) => {
  console.log(`[Telemetry] Outbound HTTP Request: ${message.request.method} ${message.request.origin}`);
});
```

---

### Q188: How do you build high-performance structured JSON logging in production using `pino`?
**Answer:**
`console.log()` is **synchronous** when writing to stdout on TTY or file streams in certain OS configurations, which blocks the Event Loop.
`pino` writes asynchronous, zero-allocation newline-delimited JSON (NDJSON) directly to file descriptors with extreme speed.

```javascript
import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  formatters: {
    level: (label) => ({ level: label.toUpperCase() })
  },
  timestamp: pino.stdTimeFunctions.isoTime,
  base: {
    service: 'order-service',
    env: process.env.NODE_ENV,
    pid: process.pid
  }
});

logger.info({ orderId: 1045, userId: 'u_992' }, 'Order processed successfully');
```

---

### Q189: How do you use the Native Node.js Test Runner (`node:test`) and Assertion Library (`node:assert/strict`)?
**Answer:**
Introduced in Node.js v18/v20+, eliminating the need for external test frameworks like Jest or Mocha.

```javascript
import { test, describe, it, before, after, mock } from 'node:test';
import assert from 'node:assert/strict';

describe('Payment Service Test Suite', () => {
  let dbPool;

  before(async () => {
    dbPool = await initTestDatabase();
  });

  it('should calculate discount with tax correctly', () => {
    const total = 100 * 0.9 * 1.08;
    assert.equal(total, 97.2);
    assert.deepEqual({ a: 1 }, { a: 1 });
  });

  it('should mock async API calls', async () => {
    const fetchMock = mock.fn(async () => ({ ok: true }));
    const res = await fetchMock();
    assert.equal(fetchMock.mock.callCount(), 1);
  });
});
```
Run with: `node --test` or `node --test --watch --experimental-test-coverage`.

---

### Q190: What is `node:util.parseArgs` and how do you build CLI tools natively in Node.js?
**Answer:**
`node:util.parseArgs` (Node.js v18.3+) provides native, robust CLI argument parsing without `commander` or `yargs`.

```javascript
import { parseArgs } from 'node:util';

const options = {
  port: { type: 'string', short: 'p', default: '3000' },
  verbose: { type: 'boolean', short: 'v', default: false },
  workers: { type: 'string', default: '4' }
};

const { values, positionals } = parseArgs({
  args: process.argv.slice(2),
  options,
  allowPositionals: true
});

console.log('Parsed CLI Options:', values);
```

---

### Q191: What is the Node.js Watch Mode (`node --watch`) and how does it replace `nodemon`?
**Answer:**
Introduced in Node.js v18.11+, `--watch` uses OS-native file watching to restart the process automatically on code changes without third-party dependencies.

```bash
# Watch entry file and all imported modules
node --watch server.js

# Watch specific directory pattern
node --watch-path=./src --watch-path=./config server.js
```

---

### Q192: How do you implement Health Checks (Liveness and Readiness Probes) for Kubernetes in Node.js?
**Answer:**
- **Liveness Probe (`/healthz/liveness`)**: Is the process running and the Event Loop responsive? (If fail -> Kubernetes restarts the pod).
- **Readiness Probe (`/healthz/readiness`)**: Is the pod ready to receive user traffic? (Checks database connections, cache readiness).

```javascript
import http from 'node:http';

let isShuttingDown = false;

http.createServer(async (req, res) => {
  if (req.url === '/healthz/liveness') {
    // Return 200 if process is alive and not deadlocked
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'alive' }));
  }

  if (req.url === '/healthz/readiness') {
    if (isShuttingDown) {
      res.writeHead(503);
      return res.end(JSON.stringify({ status: 'draining' }));
    }

    // Verify downstream database connection health
    try {
      await db.query('SELECT 1');
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'ready', db: 'connected' }));
    } catch (err) {
      res.writeHead(500);
      res.end(JSON.stringify({ status: 'unhealthy', error: err.message }));
    }
  }
}).listen(3000);
```

---

### Q193: How do you collect Prometheus Metrics natively using `prom-client` in Node.js?
**Answer:**

```javascript
import client from 'prom-client';
import express from 'express';

const app = express();

// Enable default runtime metrics (V8 heap, Event loop lag, GC pauses, Libuv threads)
client.collectDefaultMetrics({ prefix: 'nodejs_app_' });

// Custom business metric
const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.01, 0.05, 0.1, 0.5, 1, 2, 5]
});

app.use((req, res, next) => {
  const end = httpRequestDuration.startTimer();
  res.on('finish', () => {
    end({ method: req.method, route: req.route?.path || req.path, status_code: res.statusCode });
  });
  next();
});

// Expose /metrics endpoint for Prometheus scraper
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', client.register.contentType);
  res.end(await client.register.metrics());
});
```

---

### Q194: What is the `--env-file` flag introduced in modern Node.js?
**Answer:**
Starting in Node.js v20.6+, Node.js supports loading environment variables natively from `.env` files without installing the `dotenv` package:

```bash
node --env-file=.env --env-file=.env.production server.js
```

---

### Q195: How do you package and run Node.js in Docker with a Multi-Stage Distroless / Alpine Build?
**Answer:**

```dockerfile
# Stage 1: Build & Dependencies
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

# Stage 2: Minimal Distroless Production Image
FROM gcr.io/distroless/nodejs22-debian12
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY src ./src
COPY package.json ./

USER nonroot
EXPOSE 3000
ENV NODE_ENV=production
CMD ["src/server.js"]
```
Produces an ultra-secure, minimal image (~60MB) with no shell, no package manager, and running as a non-root user.

---

### Q196: What is the difference between Operational Errors and Programmer Errors in Node.js?
**Answer:**
- **Operational Errors**: Normal runtime failure conditions that must be anticipated and handled gracefully (e.g. `ECONNRESET`, Invalid User Input 400, Rate Limit Exceeded 429, File Not Found 404).
- **Programmer Errors**: Bugs in the code that indicate an unpredictable state (e.g. `TypeError: Cannot read properties of undefined`, Syntax Error, Memory Leak).
- **Rule**: Operational errors are caught and returned as HTTP error responses; programmer errors should trigger logging, alert, and clean process restart.

---

### Q197: How do you implement a Global Error Handling Middleware in Express/Fastify?
**Answer:**

```javascript
// Express Error Handling Middleware (Must have 4 parameters: err, req, res, next)
app.use((err, req, res, next) => {
  const statusCode = err.statusCode || 500;
  const isProduction = process.env.NODE_ENV === 'production';

  logger.error({
    err: {
      message: err.message,
      stack: err.stack,
      code: err.code
    },
    req: {
      method: req.method,
      url: req.url,
      headers: req.headers
    }
  }, 'Unhandled Request Error');

  res.status(statusCode).json({
    status: 'error',
    message: isProduction && statusCode === 500 ? 'Internal Server Error' : err.message,
    ...(isProduction ? {} : { stack: err.stack })
  });
});
```

---

### Q198: What is Fastify and why is it faster than Express in high-scale architectures?
**Answer:**
Fastify provides up to **5x higher throughput** than Express because:
1. **JSON Schema Compilation (`fast-json-stringify`)**: Pre-compiles JSON response serializers into optimized C++-like functions instead of runtime reflection.
2. **High-Performance Routing (`find-my-way`)**: Uses a Radix Tree (Prefix Tree) router ($O(K)$ lookup time).
3. **Pino Logging Native Integration**: Low-overhead logging by default.

---

### Q199: How do you build a Native TypeScript application with Node.js v22.6+ Type Stripping (`--experimental-strip-types`)?
**Answer:**
Node.js v22.6+ natively executes TypeScript files directly without `tsc`, `ts-node`, or `tsx` compilation steps by stripping type annotations at parse time:

```bash
# Execute TypeScript directly
node --experimental-strip-types src/index.ts
```

---

### Q200: How do you handle Graceful Degradation and Load Shedding under extreme traffic spikes?
**Answer:**
When CPU or Event Loop lag exceeds safe thresholds (e.g. lag > 100ms), Load Shedding immediately rejects non-essential incoming requests with `503 Service Unavailable` before they queue up and crash the server.

```javascript
import { monitorEventLoopDelay } from 'node:perf_hooks';

const h = monitorEventLoopDelay();
h.enable();

function loadSheddingMiddleware(req, res, next) {
  const lagMs = h.mean / 1_000_000;

  if (lagMs > 100) { // If event loop lag > 100ms
    res.setHeader('Retry-After', '5');
    return res.status(503).json({ error: 'Server under high load. Please retry.' });
  }

  next();
}
```

---

### Q201: What is Corepack in Node.js and how does it manage Yarn and PNPM package managers?
**Answer:**
Corepack is a built-in Node.js zero-install bridge tool that automatically provisions and enforces the exact version of package managers specified in `package.json` `"packageManager"` field.

```json
{
  "packageManager": "pnpm@9.5.0"
}
```
Running `corepack enable` ensures all developers and CI/CD pipelines run the identical package manager version.

---

### Q202: How do you optimize Cold Starts in Serverless Node.js (AWS Lambda / Google Cloud Functions)?
**Answer:**
1. **Tree-shaking & Minification**: Bundle code using `esbuild` into a single compact file.
2. **Lazy Require / Dynamic Import**: Load heavy SDKs only inside invoked handlers.
3. **Disable Keep-Alive destruction on freeze**: Set `keepAlive: true` in HTTP agents.
4. **Provisioned Concurrency**: Keeps warm isolates ready for instant invocation.

---

### Q203: What is the purpose of `node:perf_hooks` Performance Timeline API?
**Answer:**
Provides standard W3C High Resolution Time and Performance Timeline marks/measures.

```javascript
import { performance, PerformanceObserver } from 'node:perf_hooks';

const obs = new PerformanceObserver((items) => {
  items.getEntries().forEach((entry) => {
    console.log(`${entry.name}: ${entry.duration.toFixed(2)}ms`);
  });
});
obs.observe({ entryTypes: ['measure'] });

performance.mark('A');
await doWork();
performance.mark('B');
performance.measure('doWorkDuration', 'A', 'B');
```

---

### Q204: How do you securely manage Feature Flags and Dynamic Config Updates in Node.js?
**Answer:**
Use external configuration stores (LaunchDarkly, Unleash, AWS AppConfig) with background polling or SSE streaming, updating an in-memory configuration singleton without restarting the process.

---

### Q205: What is the Node.js Garbage Collection Finalizer Callback in Node-API?
**Answer:**
In C++ addons, `napi_add_finalizer` registers a native destructor callback that is called when a wrapping JavaScript object is garbage collected, allowing developers to safely release custom C memory pointers, GPU buffers, or hardware sockets.

---

### Q206: How do you handle Memory Limits and CPU Pinning (Taskset / Numactl) on Bare-Metal / High-Core Servers?
**Answer:**
On large multi-socket NUMA servers (e.g. 128 cores), cross-socket memory access causes latency spikes.
Pin Node.js cluster worker processes to specific CPU cores and local NUMA nodes:
```bash
numactl --cpunodebind=0 --membind=0 node server.js
```

---

### Q207: How do you write End-to-End (E2E) Integration Tests for WebSockets and HTTP APIs in Node.js?
**Answer:**
Spin up an ephemeral `http.Server` listening on port `0` (OS automatically assigns a free random port), run tests, and close the server in an `after` hook to prevent port conflicts in parallel CI runners.

```javascript
import http from 'node:http';
import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';

let server;
let baseUrl;

before(async () => {
  server = http.createServer((req, res) => res.end('OK'));
  await new Promise(resolve => server.listen(0, resolve));
  const port = server.address().port;
  baseUrl = `http://127.0.0.1:${port}`;
});

after(() => server.close());

test('E2E health endpoint returns 200', async () => {
  const res = await fetch(`${baseUrl}/health`);
  assert.equal(res.status, 200);
});
```

---

### Q208: What are Source Maps and how does Node.js v12.12+ handle them natively (`--enable-source-maps`)?
**Answer:**
When TypeScript or bundled code is executed, errors print line numbers corresponding to the compiled `.js` bundle, making debugging difficult.
`node --enable-source-maps app.js` natively parses embedded or inline Source Maps to output original `.ts` / source file paths and line numbers in error stack traces.

---

### Q209: How do you implement Distributed Rate Limiting across a fleet of Node.js servers using Redis sliding logs?
**Answer:**
Using Redis sorted sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`) evaluated inside an atomic **Lua Script** guarantees atomic sliding window rate limiting across hundreds of distributed pods with zero race conditions.

---

### Q210: What are the Architectural Best Practices for designing a Production-Grade, Fault-Tolerant Node.js Enterprise System?
**Answer:**

```
+─────────────────────────────────────────────────────────────────────────────+
|                         Global CDN & DDoS Shield                            |
|                       (Cloudflare / AWS CloudFront)                         |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
+──────────────────────────────────────▼──────────────────────────────────────+
|                     Ingress Load Balancer / API Gateway                     |
|                 (NGINX / Envoy / Kubernetes Ingress / ALB)                  |
+──────────────────────────────────────┬──────────────────────────────────────+
                                       │
+──────────────────────────────────────▼──────────────────────────────────────+
|              Node.js Container Fleet (Docker / Kubernetes Pods)             |
|  - Stateless Containers running with Non-Root Distroless Alpine Base        |
|  - OS Parallelism Aware Clustering (os.availableParallelism())              |
|  - Event Loop Lag & GC Monitoring (OpenTelemetry + Prometheus)              |
|  - AsyncLocalStorage Request Tracing Context                                |
|  - Zero-Copy Streams with Backpressure Management                           |
|  - Graceful Shutdown Handling (SIGTERM/SIGINT with connection drain)        |
+───────────────────┬──────────────────┬───────────────────┬──────────────────+
                    │                  │                   │
+───────────────────▼──+     +─────────▼────────+     +────▼─────────────────+
| Multi-Level Caching  |     | Relational / NoSQL|     | Event Streaming /    |
| - L1 In-Memory LRU   |     | Primary & Replicas|     | Message Broker       |
| - L2 Redis Cluster   |     | PgBouncer / Pool  |     | Apache Kafka/BullMQ  |
+──────────────────────+     +──────────────────+     +──────────────────────+
```

**Key Production Pillars:**
1. **Stateless Node Layer**: Store no session state in process RAM; persist sessions in Redis/DB to allow elastic horizontal autoscaling.
2. **Backpressure & Stream Hygiene**: Always use `stream.pipeline()` to prevent memory leaks and handle slow consumers.
3. **Telemetry & Observability**: Instrument distributed tracing with OpenTelemetry, structured JSON logging with Pino, and APM lag metrics.
4. **Resilience & Fault Tolerance**: Protect downstream microservices with Circuit Breakers, SingleFlight cache deduplication, and Exponential Backoff Retries.
5. **Robust Lifecycle**: Implement Kubernetes liveness/readiness probes, socket draining, and graceful shutdown on `SIGTERM`.


---

# Part 10: Advanced Caching Architectures, Write Strategies & Eviction Policies

---

### Q211: What are the Core Caching Strategies (Cache-Aside, Read-Through, Write-Through, Write-Behind, Write-Around, Refresh-Ahead) and when should each be used?
**Answer:**

```
+──────────────────────────────────────────────────────────────────────────────────────────────────────+
|                                    Caching Strategies Comparison                                    |
+───────────────────┬────────────────────────────────────────────┬──────────────────┬──────────────────+
| Strategy          | Write Flow                                 | Read Flow        | Best For         |
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Cache-Aside**   | App writes to DB, invalidates Cache        | App checks Cache | Read-heavy,      |
| (Lazy Loading)    |                                            | on miss reads DB | general apps     |
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Read-Through**  | App writes to Cache/DB                     | Cache library    | Clean code       |
|                   |                                            | reads DB on miss | abstractions     |
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Write-Through** | App writes to Cache; Cache synchronously   | Read from Cache  | Strict read      |
|                   | writes to DB before returning              |                  | consistency      |
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Write-Behind**  | App writes to Cache; Cache asynchronously  | Read from Cache  | High write-rate  |
| (Write-Back)      | queues batch writes to DB                  |                  | (analytics, logs)|
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Write-Around**  | App writes directly to DB bypassing Cache  | Read from Cache; | Infrequently     |
|                   |                                            | reads DB on miss | re-read writes   |
+───────────────────┼────────────────────────────────────────────┼──────────────────┼──────────────────+
| **Refresh-Ahead** | App writes to DB                           | Cache predicts & | Predictable hot  |
|                   |                                            | refreshes before | access keys      |
|                   |                                            | TTL expires      | (e.g. top news)  |
+───────────────────┴────────────────────────────────────────────┴──────────────────┴──────────────────+
```

---

### Q212: How do you implement the Write-Through and Write-Behind (Write-Back) Caching Patterns in Node.js?
**Answer:**

#### 1. Write-Through Pattern (Synchronous DB write via Cache Layer):
Guarantees that cache and database are always in sync before completing the request.

```javascript
class WriteThroughCache {
  constructor(redisClient, dbPool) {
    this.redis = redisClient;
    this.db = dbPool;
  }

  async set(key, value, ttlSeconds = 3600) {
    // 1. Write to Database First inside transaction
    await this.db.query('INSERT INTO items (id, data) VALUES ($1, $2) ON CONFLICT (id) DO UPDATE SET data = $2', [key, JSON.stringify(value)]);
    
    // 2. Immediately populate cache
    await this.redis.set(`item:${key}`, JSON.stringify(value), 'EX', ttlSeconds);
  }

  async get(key) {
    const cached = await this.redis.get(`item:${key}`);
    if (cached) return JSON.parse(cached);

    const res = await this.db.query('SELECT data FROM items WHERE id = $1', [key]);
    if (res.rows.length === 0) return null;

    const data = res.rows[0].data;
    await this.redis.set(`item:${key}`, JSON.stringify(data), 'EX', 3600);
    return data;
  }
}
```

#### 2. Write-Behind / Write-Back Pattern (Asynchronous Batched Writes):
Absorbs massive write spikes (e.g. 100,000 writes/sec) by saving immediately to in-memory/Redis queue and flushing asynchronously to the DB in bulk batches.

```javascript
class WriteBehindCache {
  constructor(redisClient, dbPool, flushIntervalMs = 5000, batchSize = 500) {
    this.redis = redisClient;
    this.db = dbPool;
    this.writeBuffer = new Map();
    this.batchSize = batchSize;

    // Background asynchronous batch flusher
    setInterval(() => this.flush(), flushIntervalMs).unref();
  }

  async set(key, value) {
    // 1. Update cache immediately (Sub-millisecond response time!)
    await this.redis.set(`item:${key}`, JSON.stringify(value));
    
    // 2. Buffer for asynchronous database write
    this.writeBuffer.set(key, value);

    if (this.writeBuffer.size >= this.batchSize) {
      await this.flush();
    }
  }

  async flush() {
    if (this.writeBuffer.size === 0) return;

    const entries = Array.from(this.writeBuffer.entries());
    this.writeBuffer.clear();

    const ids = entries.map(([id]) => id);
    const dataList = entries.map(([, data]) => JSON.stringify(data));

    try {
      // Bulk UPSERT into database in a single query
      await this.db.query(`
        INSERT INTO items (id, data)
        SELECT * FROM UNNEST($1::text[], $2::jsonb[])
        ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data
      `, [ids, dataList]);
      console.log(`[WriteBehind] Flushed ${entries.length} records to DB`);
    } catch (err) {
      console.error('[WriteBehind] Failed to flush batch, restoring buffer:', err);
      // Restore buffer on failure to prevent data loss
      entries.forEach(([k, v]) => this.writeBuffer.set(k, v));
    }
  }
}
```

---

### Q213: What are the different Cache Eviction & Recency Algorithms (LRU, LFU, FIFO, 2Q, ARC, W-TinyLFU) and how do they differ?
**Answer:**

```
+──────────────────────────────────────────────────────────────────────────────────────────────────────────+
|                                     Cache Eviction Algorithms                                           |
+──────────────┬──────────────────────────────────────────┬──────────────────────┬─────────────────────────+
| Algorithm    | Eviction Criteria                        | Complexity           | Best Use Case           |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **LRU**      | Least Recently Used (oldest timestamp)   | O(1) Doubly Linked   | General purpose,        |
|              |                                          | List + HashMap       | temporal locality       |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **LFU**      | Least Frequently Used (lowest hit count) | O(1) Min-Heap / Doubly| Long-term popular items |
|              |                                          | Linked Frequency List| (frequency locality)    |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **FIFO**     | First In First Out (oldest inserted key) | O(1) Queue           | Streaming pipelines     |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **2Q**       | Uses two queues: FIFO for first-time     | O(1)                 | Resists "scan pollution"|
| (Two-Queue)  | accesses, LRU for items accessed ≥2 times|                      | (one-off large queries) |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **ARC**      | Adaptive Replacement: Dynamically balances| O(1) Self-tuning     | File systems, databases |
|              | between Recency (LRU) and Frequency (LFU)|                      | with fluctuating loads  |
+──────────────┼──────────────────────────────────────────┼──────────────────────┼─────────────────────────+
| **W-TinyLFU**| Window-TinyLFU: Small LRU window + Bloom | O(1) High hit ratio  | High-throughput caches  |
|              | filter frequency sketch + SLRU main space| low memory           | (Caffeine / Ristretto)  |
+──────────────┴──────────────────────────────────────────┴──────────────────────┴─────────────────────────+
```

---

### Q214: How do you build an $O(1)$ Least Recently Used (LRU) Cache from scratch using a Doubly Linked List + Hash Map?
**Answer:**

```javascript
class Node {
  constructor(key, value) {
    this.key = key;
    this.value = value;
    this.prev = null;
    this.next = null;
  }
}

class DoublyLinkedListLRUCache {
  constructor(capacity) {
    this.capacity = capacity;
    this.map = new Map(); // key -> Node
    this.size = 0;

    // Dummy head and tail sentinel nodes
    this.head = new Node(0, 0);
    this.tail = new Node(0, 0);
    this.head.next = this.tail;
    this.tail.prev = this.head;
  }

  _addNode(node) {
    node.prev = this.head;
    node.next = this.head.next;
    this.head.next.prev = node;
    this.head.next = node;
  }

  _removeNode(node) {
    const prev = node.prev;
    const next = node.next;
    prev.next = next;
    next.prev = prev;
  }

  _moveToHead(node) {
    this._removeNode(node);
    this._addNode(node);
  }

  _popTail() {
    const res = this.tail.prev;
    this._removeNode(res);
    return res;
  }

  get(key) {
    const node = this.map.get(key);
    if (!node) return -1;
    this._moveToHead(node); // Accessed -> move to MRU position
    return node.value;
  }

  put(key, value) {
    const node = this.map.get(key);
    if (node) {
      node.value = value;
      this._moveToHead(node);
    } else {
      const newNode = new Node(key, value);
      this.map.set(key, newNode);
      this._addNode(newNode);
      this.size++;

      if (this.size > this.capacity) {
        const tail = this._popTail(); // Evict LRU
        this.map.delete(tail.key);
        this.size--;
      }
    }
  }
}
```

---

### Q215: How do you implement an $O(1)$ Least Frequently Used (LFU) Cache in Node.js?
**Answer:**

```javascript
class LFUCache {
  constructor(capacity) {
    this.capacity = capacity;
    this.values = new Map();     // key -> value
    this.counts = new Map();     // key -> frequency count
    this.freqLists = new Map();  // frequency -> Set(keys)
    this.minFreq = 0;
  }

  get(key) {
    if (!this.values.has(key)) return -1;
    const count = this.counts.get(key);
    this.counts.set(key, count + 1);

    this.freqLists.get(count).delete(key);
    if (this.freqLists.get(count).size === 0) {
      this.freqLists.delete(count);
      if (this.minFreq === count) this.minFreq++;
    }

    if (!this.freqLists.has(count + 1)) {
      this.freqLists.set(count + 1, new Set());
    }
    this.freqLists.get(count + 1).add(key);

    return this.values.get(key);
  }

  put(key, value) {
    if (this.capacity <= 0) return;

    if (this.values.has(key)) {
      this.values.set(key, value);
      this.get(key);
      return;
    }

    if (this.values.size >= this.capacity) {
      const minList = this.freqLists.get(this.minFreq);
      const evictKey = minList.keys().next().value;
      minList.delete(evictKey);
      this.values.delete(evictKey);
      this.counts.delete(evictKey);
    }

    this.values.set(key, value);
    this.counts.set(key, 1);
    this.minFreq = 1;
    if (!this.freqLists.has(1)) this.freqLists.set(1, new Set());
    this.freqLists.get(1).add(key);
  }
}
```

---

### Q216: What is the difference between Absolute TTL vs. Sliding (Inactivity) TTL and how do you implement them?
**Answer:**
- **Absolute TTL**: Fixed expiration timestamp regardless of reads (OTPs, stock quotes).
- **Sliding (Inactivity) TTL**: Resets TTL on each read (`GETEX`), expiring only after $N$ seconds of complete inactivity (sessions, shopping carts).

```javascript
class SlidingTTLCache {
  constructor(redisClient, defaultTtlSeconds = 1800) {
    this.redis = redisClient;
    this.defaultTtl = defaultTtlSeconds;
  }

  async getAndSlide(key) {
    const value = await this.redis.getex(key, 'EX', this.defaultTtl);
    return value ? JSON.parse(value) : null;
  }

  async setAbsolute(key, value, ttlSeconds) {
    await this.redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
  }
}
```

---

### Q217: What is TTL Jitter and why is it essential to prevent Mass Cache Expiration Cascades?
**Answer:**
Prevents 100,000 keys loaded during a cron job from expiring simultaneously:

```javascript
function calculateJitteredTtl(baseTtlSeconds, jitterPercent = 0.15) {
  const maxJitter = baseTtlSeconds * jitterPercent;
  const randomJitter = (Math.random() * 2 - 1) * maxJitter;
  return Math.floor(baseTtlSeconds + randomJitter);
}

const ttl = calculateJitteredTtl(3600, 0.15); // 3060s to 4140s
await redis.set(`catalog:${categoryId}`, JSON.stringify(data), 'EX', ttl);
```

---

### Q218: What is Probabilistic Early Expiration (The XFetch Algorithm) and how does it prevent Cache Stampedes?
**Answer:**
Recomputes and refreshes cached items in the background *before* expiration based on computation delta $\delta$:

```javascript
class XFetchCache {
  constructor(redisClient) {
    this.redis = redisClient;
  }

  async get(key, fetchFn, ttlSeconds = 60, beta = 1.0) {
    const raw = await this.redis.get(key);
    let cached = raw ? JSON.parse(raw) : null;

    const now = Date.now();
    const shouldRecompute = !cached || 
      (now - cached.delta * beta * Math.log(Math.random()) > cached.expiry);

    if (shouldRecompute) {
      const startTime = Date.now();
      const freshValue = await fetchFn();
      const delta = Date.now() - startTime;
      const expiry = now + (ttlSeconds * 1000);

      const payload = { value: freshValue, delta, expiry };
      await this.redis.set(key, JSON.stringify(payload), 'EX', ttlSeconds);
      return freshValue;
    }

    return cached.value;
  }
}
```

---

### Q219: How do Active vs. Passive Cache Expiration work in Redis and In-Memory Caches?
**Answer:**
- **Passive (Lazy)**: Expiration evaluated only when a client attempts a `GET`.
- **Active (Background Scrubbing)**: Periodic random sampling (e.g. 10 times/sec in Redis) to proactively evict expired keys and free RAM.

---

### Q220: How do you implement Multi-Tier Cache Synchronization (L1 Node.js In-Memory + L2 Distributed Redis) with Keyspace Notifications?
**Answer:**
Combines sub-microsecond L1 in-memory hits with distributed Redis L2, synchronized across 50+ pods via Redis Pub/Sub invalidations.

---

### Q221: How do you achieve a "Zero-Cache-Miss" Architecture using Stale-While-Revalidate (SWR) in Node.js?
**Answer:**
In high-scale systems, waiting for a database read on a cache miss causes latency spikes ($>100\text{ms}$).
**Stale-While-Revalidate (SWR)** guarantees **zero cache misses and $<1\text{ms}$ response times**:
1. Returns the **cached (stale) data instantly** to the user.
2. Simultaneously spawns an asynchronous background worker to fetch fresh data from the DB and update the cache for subsequent requests.

```javascript
class SWRCache {
  constructor(redisClient) {
    this.redis = redisClient;
    this.refreshingKeys = new Set();
  }

  async get(key, fetchFreshFn, { freshTtl = 60, staleTtl = 3600 } = {}) {
    const raw = await this.redis.get(`swr:${key}`);
    const now = Date.now();

    if (raw) {
      const { value, expiresAt } = JSON.parse(raw);

      // If data is stale but still within allowed stale window -> Trigger async background refresh
      if (now > expiresAt && !this.refreshingKeys.has(key)) {
        this.refreshingKeys.add(key);
        // Fire-and-forget background refresh without blocking user response!
        setImmediate(async () => {
          try {
            const freshValue = await fetchFreshFn();
            const payload = { value: freshValue, expiresAt: Date.now() + (freshTtl * 1000) };
            await this.redis.set(`swr:${key}`, JSON.stringify(payload), 'EX', staleTtl);
          } catch (err) {
            console.error(`[SWR] Background refresh failed for ${key}:`, err.message);
          } finally {
            this.refreshingKeys.delete(key);
          }
        });
      }

      // ⚡ Return cached data instantly (Zero wait time!)
      return value;
    }

    // Complete Cache Miss (Cold Start): Fetch synchronously and populate
    const freshValue = await fetchFreshFn();
    const payload = { value: freshValue, expiresAt: now + (freshTtl * 1000) };
    await this.redis.set(`swr:${key}`, JSON.stringify(payload), 'EX', staleTtl);
    return freshValue;
  }
}
```

---

### Q222: How does Change Data Capture (CDC via Debezium & Kafka) achieve Zero-Miss Push-Populated Caching?
**Answer:**
Instead of traditional pull-based "Cache-Aside" (which suffers from initial cache misses on newly inserted or updated rows), **CDC Push-Populated Caching** streams database transaction logs directly into the cache.

```
[PostgreSQL Database] ──(Write-Ahead Log / WAL)──> [Debezium Connector]
                                                           │
                                                           ▼
[Node.js Cache Populator Fleet] <──(Kafka Events Topic)────┘
       │
       ▼ (Atomic SET / Invalidate)
[Redis Cluster / In-Memory Cache] <── (Sub-millisecond 100% Cache Hits!) ── [API Gateway Users]
```

**Benefits:**
1. **100% Hit Rate (Zero Misses)**: The cache is populated proactively the millisecond a DB transaction commits, *before* any user requests it.
2. **Zero Cache Invalidation Race Conditions**: Order of updates is strictly preserved by Kafka partition keys.

---

### Q223: How do you implement Cache Pre-Warming on Deployment and Blue-Green Rollouts?
**Answer:**
When deploying a new service version or launching a new Redis cluster, a "Cold Cache" causes a flood of database queries (Cold Start Spike), taking down the DB.

**Cache Warming Script Strategy**:
1. Before routing live user traffic to new pods or new Redis cluster:
2. Query Top-N queries from analytics / database access logs (`SELECT key FROM top_10000_hot_products`).
3. Bulk-populate Redis via pipeline/MSET.
4. Pass Kubernetes Readiness Probe only *after* warming finishes:

```javascript
async function warmHotCache(db, redis) {
  console.log('[CacheWarming] Pre-warming top 10,000 hot keys...');
  const hotRecords = await db.query('SELECT id, data FROM products ORDER BY view_count DESC LIMIT 10000');

  const pipeline = redis.pipeline();
  for (const row of hotRecords.rows) {
    pipeline.set(`product:${row.id}`, JSON.stringify(row.data), 'EX', 86400);
  }
  await pipeline.exec();
  console.log('[CacheWarming] Cache successfully primed. Ready for production traffic.');
}
```

---

### Q224: How do you combine Read-Through, SingleFlight, and Fallback Stale Cache for Zero-Downtime High Availability?
**Answer:**
If the primary database crashes, a resilient system serves stale cached data indefinitely rather than throwing 500 Internal Server Errors to users.

```javascript
class ResilientZeroMissCache {
  constructor(redisClient, dbPool) {
    this.redis = redisClient;
    this.db = dbPool;
    this.inFlight = new Map();
  }

  async get(id) {
    const cacheKey = `entity:${id}`;
    const cached = await this.redis.get(cacheKey);

    if (cached) return JSON.parse(cached);

    // SingleFlight: Only 1 query to DB if 1,000 requests hit simultaneously
    if (this.inFlight.has(id)) {
      return this.inFlight.get(id);
    }

    const fetchPromise = (async () => {
      try {
        const res = await this.db.query('SELECT * FROM entities WHERE id = $1', [id]);
        const data = res.rows[0];
        // Save with 1-hour active TTL and 24-hour backup stale copy
        await this.redis.set(cacheKey, JSON.stringify(data), 'EX', 3600);
        await this.redis.set(`stale:${cacheKey}`, JSON.stringify(data), 'EX', 86400);
        return data;
      } catch (dbErr) {
        console.warn(`[Resilience] DB failure, checking stale fallback for ${id}:`, dbErr.message);
        const staleBackup = await this.redis.get(`stale:${cacheKey}`);
        if (staleBackup) {
          return JSON.parse(staleBackup); // Serves stale data with zero downtime!
        }
        throw dbErr;
      } finally {
        this.inFlight.delete(id);
      }
    })();

    this.inFlight.set(id, fetchPromise);
    return fetchPromise;
  }
}
```

---

### Q225: What are Bloom Filters and Cuckoo Filters and how do they eliminate Cache Penetration on Non-Existent Keys?
**Answer:**
- **Cache Penetration**: An attacker requests millions of non-existent IDs (`/user/random_uuid_9999`). Since they are not in the cache, every single request hits the database, exhausting connection pools.
- **Bloom Filter Solution**: A space-efficient probabilistic data structure that tests whether an element is a member of a set.
  - Returns either *"Definitely NOT in database"* (0% false negatives) or *"Possibly in database"* ($<1\%$ false positives).
  - If Bloom filter returns `false`, Node.js rejects the request immediately **without querying Redis or the Database**.

```javascript
import { BloomFilter } from 'bloomfilter';

// 1. Initialize Bloom Filter with 32 * 256KB bits and 16 hash functions
const bloom = new BloomFilter(32 * 256 * 1024, 16);

// 2. Pre-populate with all existing user IDs
const existingUsers = await db.query('SELECT id FROM users');
existingUsers.rows.forEach(u => bloom.add(u.id));

// 3. Request Handler:
async function getUserSafely(userId, res) {
  // If definitely not in database -> Short-circuit instantly!
  if (!bloom.test(userId)) {
    return res.status(404).json({ error: 'User does not exist (Bloom Filter Protected)' });
  }

  // Safe to check cache and database
  const user = await cache.get(userId);
  return res.json(user);
}
```
