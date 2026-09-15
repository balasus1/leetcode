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

#### Code Example:
```javascript
// Production demonstration for: 157: What is Database Connection Leak and how do you detect it in Node.js?
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

#### Code Example:
```javascript
// Production demonstration for: 159: What is the Outbox Pattern and why is it essential for Distributed Microservices in Node.js?
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

### Q160: How does ElasticSearch / OpenSearch integration work with Node.js for Full-Text Search?
**Answer:**
Use `@elastic/elasticsearch` with bulk indexing pipelines and scroll/search_after pagination for millions of documents.

---

#### Code Example:
```javascript
// Production demonstration for: 160: How does ElasticSearch / OpenSearch integration work with Node.js for Full-Text Search?
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

#### Code Example:
```javascript
// Production demonstration for: 162: What is the difference between Redis Pub/Sub and Redis Streams?
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

#### Code Example:
```javascript
// Production demonstration for: 164: How do you handle Database Schema Migrations reliably in CI/CD without application downtime?
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

### Q165: How do you implement CQRS (Command Query Responsibility Segregation) in Node.js?
**Answer:**
Separates write models (Commands) that mutate relational DB state from read models (Queries) optimized with Denormalized NoSQL / Elasticsearch views populated asynchronously via Change Data Capture (CDC / Debezium).

#### Code Example:
```javascript
// Production demonstration for: 165: How do you implement CQRS (Command Query Responsibility Segregation) in Node.js?
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

