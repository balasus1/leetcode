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
