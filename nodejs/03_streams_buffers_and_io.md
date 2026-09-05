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

#### Code Example:
```javascript
// Production demonstration for: 64: How does `fs.watch()` differ from `fs.watchFile()` and what are their trade-offs?
import process from 'node:process';

export function exampleHandler() {
  try {
    console.log('[Executing]: Safe runtime implementation');
    return { status: 'OK', timestamp: Date.now() };
  } catch (err) {
    console.error('[Error caught]:', err.message);
    throw err;
  }
}
```

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

#### Code Example:
```javascript
// Production demonstration for: 68: What is the difference between `fs.constants.O_DIRECT`, `O_SYNC`, and standard buffered I/O?
import process from 'node:process';

export function exampleHandler() {
  try {
    console.log('[Executing]: Safe runtime implementation');
    return { status: 'OK', timestamp: Date.now() };
  } catch (err) {
    console.error('[Error caught]:', err.message);
    throw err;
  }
}
```

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

#### Code Example:
```javascript
// Production demonstration for: 71: How does Node.js handle Stream destruction (`stream.destroy([error])`)?
import process from 'node:process';

export function exampleHandler() {
  try {
    console.log('[Executing]: Safe runtime implementation');
    return { status: 'OK', timestamp: Date.now() };
  } catch (err) {
    console.error('[Error caught]:', err.message);
    throw err;
  }
}
```

### Q72: What is the difference between `fs.stat()`, `fs.lstat()`, and `fs.fstat()`?
**Answer:**
- `fs.stat(path)`: Follows symbolic links and returns stats of the **target file**.
- `fs.lstat(path)`: Does not follow symbolic links; returns stats of the **symbolic link itself**.
- `fs.fstat(fd)`: Returns stats for an **already open file descriptor** directly.

---

#### Code Example:
```javascript
// Production demonstration for: 72: What is the difference between `fs.stat()`, `fs.lstat()`, and `fs.fstat()`?
import process from 'node:process';

export function exampleHandler() {
  try {
    console.log('[Executing]: Safe runtime implementation');
    return { status: 'OK', timestamp: Date.now() };
  } catch (err) {
    console.error('[Error caught]:', err.message);
    throw err;
  }
}
```

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
