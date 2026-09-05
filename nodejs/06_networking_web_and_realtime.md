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
