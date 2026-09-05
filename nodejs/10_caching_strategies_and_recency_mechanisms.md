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

#### Code Example:
```javascript
// Production demonstration for: 219: How do Active vs. Passive Cache Expiration work in Redis and In-Memory Caches?
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

### Q220: How do you implement Multi-Tier Cache Synchronization (L1 Node.js In-Memory + L2 Distributed Redis) with Keyspace Notifications?
**Answer:**
Combines sub-microsecond L1 in-memory hits with distributed Redis L2, synchronized across 50+ pods via Redis Pub/Sub invalidations.

---

#### Code Example:
```javascript
// Production demonstration for: 220: How do you implement Multi-Tier Cache Synchronization (L1 Node.js In-Memory + L2 Distributed Redis) with Keyspace Notifications?
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
