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
