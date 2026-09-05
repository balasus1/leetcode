# Caching Architectures, Eviction Policies & Recency Mechanisms
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Multi-Tier Caching Topology

A resilient distributed architecture deploys multi-tier caching at every layer of the request pipeline to maximize cache hit ratio ($> 99\%$) and insulate the database layer.

```
+---------------------------------------------------------------------------------------------------+
| THE MULTI-TIER CACHE HIERARCHY                                                                    |
|                                                                                                   |
| [ Client Browser / Mobile App ] (L0: Memory / Disk Cache, HTTP ETag)                              |
|               │                                                                                   |
|               ▼ Latency: 5ms - 15ms                                                               |
| [ Global Anycast CDN: Cloudflare / CloudFront ] (L1 Edge Cache: Static Assets, ISR Pages)         |
|               │                                                                                   |
|               ▼ Latency: 20ms - 50ms                                                              |
| [ API Gateway / Reverse Proxy: Nginx / Varnish ] (L2 Gateway Cache: Whole Response JSON)          |
|               │                                                                                   |
|               ▼ Latency: 100 microseconds                                                         |
| [ App Server Process: In-Memory Caffeine / LRU ] (L3 L1-App Cache: Hot Deserialized Objects)      |
|               │                                                                                   |
|               ▼ Latency: 1ms - 3ms                                                                |
| [ Distributed Cache Cluster: Redis Cluster ] (L4 Central Cache: Sessions, Shared Entities)        |
|               │                                                                                   |
|               ▼ Latency: 10ms - 100ms                                                             |
| [ Persistent Database: Postgres / Cassandra / Spanner ] (Primary Storage Tier)                    |
+---------------------------------------------------------------------------------------------------+
```

---

## 2. Caching Strategies: Write-Through vs Write-Behind vs Cache-Aside

```
+---------------------------------------------------------------------------------------------------+
| CACHE READ & WRITE STRATEGIES                                                                     |
|                                                                                                   |
| 1. CACHE-ASIDE (Lazy Loading - Application coordinates):                                          |
|    - Read: App checks Cache. If Miss: Reads DB -> Writes to Cache -> Returns.                     |
|    - Write: App writes to DB -> Invalidate / Delete Key from Cache.                               |
|                                                                                                   |
| 2. READ-THROUGH / WRITE-THROUGH (Cache coordinates):                                              |
|    - App treats Cache as main store. Cache layer synchronously writes to DB before returning.     |
|                                                                                                   |
| 3. WRITE-BEHIND (Write-Back - Asynchronous batching):                                             |
|    - App writes to Cache immediately. Cache asynchronously flushes batch writes to DB via worker. |
|    - Extreme write throughput; risk of data loss if cache node crashes before flush.               |
+---------------------------------------------------------------------------------------------------+
```

### Invalidation Dilemma: Update Cache vs Invalidate Cache on Write?

In distributed high-concurrency environments, **always Invalidate (Delete) the cache key upon database update**, rather than updating the cache with the new value.

```
+---------------------------------------------------------------------------------------------------+
| RACE CONDITION IN "UPDATE CACHE" PATTERN                                                          |
|                                                                                                   |
| Thread 1 (Update User to "Alice")    Thread 2 (Update User to "Bob")                              |
| │                                     │                                                           |
| 1. Writes DB: "Alice"                 │                                                           |
| │                                     2. Writes DB: "Bob"                                         |
| │                                     3. Writes Cache: "Bob"                                      |
| 4. Writes Cache: "Alice" (STALE OVERWRITE!) ──► DB is "Bob", but Cache is permanently "Alice"!   |
|                                                                                                   |
| SOLUTION (CACHE INVARIATION / DELETE):                                                            |
| Both threads delete the key. The subsequent read cleanly repopulates from DB with latest version.  |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Cache Eviction Policies: LRU vs LFU vs ARC vs TinyLFU

```
+---------------------------------------------------------------------------------------------------+
| CACHE EVICTION ALGORITHMS COMPARISON                                                              |
|                                                                                                   |
| 1. LRU (Least Recently Used):                                                                     |
|    - Evicts items unaccessed for longest duration.                                                |
|    - Implementation: Hash Map + Doubly Linked List (O(1) get & put).                              |
|    - Flaw: Vulnerable to "Scan Pollution" (a batch query touches 1M keys, flushing all hot items).|
|                                                                                                   |
| 2. LFU (Least Frequently Used):                                                                   |
|    - Evicts items with lowest access count.                                                       |
|    - Flaw: Old historical items accumulate huge counters and stay forever even if no longer hot. |
|                                                                                                   |
| 3. ARC (Adaptive Replacement Cache - IBM Nimrod Megiddo):                                         |
|    - Dynamically balances two queues: T1 (Recency) and T2 (Frequency) based on hit/miss feedback.|
|    - Self-tuning in real-time; immune to scan pollution.                                          |
|                                                                                                   |
| 4. Window TinyLFU (Used in Java Caffeine Cache / Go Ristretto):                                   |
|    - Employs Count-Min Sketch (probabilistic frequency tracking) + 1% Window LRU admission filter.|
|    - Achieves theoretical optimal hit ratios with 8x lower memory footprint than standard LFU.    |
+---------------------------------------------------------------------------------------------------+
```

---

## 4. Cache Anomalies & Production Solutions

```
+---------------------------------------------------------------------------------------------------+
| THE 4 CRITICAL CACHE FAILURE PATTERNS                                                             |
|                                                                                                   |
| Anomaly          | Root Cause                              | Production Solution                  |
| ---------------- | --------------------------------------- | ------------------------------------ |
| Cache Stampede   | Hot key expires; 10,000 concurrent reqs | XFetch Algorithm / Mutex Singleflight|
| (Thundering Herd)| simultaneously query DB to recalculate. |                                      |
| ---------------- | --------------------------------------- | ------------------------------------ |
| Cache            | Malicious requests for non-existent     | Bloom Filters + Null Value Caching   |
| Penetration      | keys (ID: -999) bypass cache to DB.     | with short TTL.                      |
| ---------------- | --------------------------------------- | ------------------------------------ |
| Cache Avalanche  | Millions of keys expire at same second  | TTL Jitter (TTL = Base + Random Jitter|
|                  | (e.g. at midnight). DB gets crushed.    | [0 to 300s]).                        |
| ---------------- | --------------------------------------- | ------------------------------------ |
| Cache Breakdown  | Specific ultra-hot VIP key deleted or   | Early Background Refresh / Dual-key  |
| (Hot Key)        | invalidated. Heavy read volume hits DB. | replication across Redis nodes.       |
+---------------------------------------------------------------------------------------------------+
```

### The Optimal Solution for Cache Stampede: XFetch Probabilistic Algorithm

Instead of waiting for a hot key to expire (which guarantees a race condition), the **XFetch algorithm** causes a single lucky background worker to probabilistically recompute the cache **shortly before expiry**:

$$\text{Recompute If:} \quad -\beta \times \delta \times \ln(\text{random}(0, 1)) > \text{Expiry} - \text{Now}$$

Where:
- $\delta$ = Computation time taken to compute the key (e.g., $200\text{ms}$).
- $\beta > 0$ = Aggressiveness factor (default $\beta = 1.0$).
- $\text{random}(0, 1)$ = Uniform floating point random number $(0, 1]$.

As time approaches expiration ($\text{Expiry} - \text{Now} \rightarrow 0$), the probability of triggering early recompute smoothly approaches $100\%$. **Only 1 thread performs the compute** while all other $9,999$ threads continue reading the existing valid cache value with zero latency.

---

## 5. Production TypeScript Implementation: Bloom Filter

A Bloom Filter is a space-efficient probabilistic data structure used to test whether an element is a member of a set:
- If Bloom filter says **"False"**: The element is **guaranteed NOT in the set** (Zero DB read needed).
- If Bloom filter says **"True"**: The element is **likely in the set** (Small false positive probability $p$).

```typescript
import crypto from 'node:crypto';

export class BloomFilter {
  private readonly size: number; // m: bit array size
  private readonly hashCount: number; // k: number of hash functions
  private readonly bitArray: Uint8Array;

  constructor(expectedElements: number, falsePositiveRate: number = 0.01) {
    // Optimal bit array size m = - (n * ln(p)) / (ln(2)^2)
    this.size = Math.ceil(- (expectedElements * Math.log(falsePositiveRate)) / (Math.LN2 ** 2));
    // Optimal number of hash functions k = (m / n) * ln(2)
    this.hashCount = Math.ceil((this.size / expectedElements) * Math.LN2);
    this.bitArray = new Uint8Array(Math.ceil(this.size / 8));
  }

  // Kirsch-Mitzenmacher optimization: generate k hashes using only 2 hash invocations
  private getHashIndices(item: string): number[] {
    const hash1 = crypto.createHash('sha256').update(item).digest().readUInt32BE(0);
    const hash2 = crypto.createHash('md5').update(item).digest().readUInt32BE(0);
    const indices: number[] = [];

    for (let i = 0; i < this.hashCount; i++) {
      const combinedHash = (hash1 + i * hash2) >>> 0;
      indices.push(combinedHash % this.size);
    }
    return indices;
  }

  public add(item: string): void {
    const indices = this.getHashIndices(item);
    for (const bitIndex of indices) {
      const byteIndex = Math.floor(bitIndex / 8);
      const bitOffset = bitIndex % 8;
      this.bitArray[byteIndex] |= (1 << bitOffset);
    }
  }

  public mightContain(item: string): boolean {
    const indices = this.getHashIndices(item);
    for (const bitIndex of indices) {
      const byteIndex = Math.floor(bitIndex / 8);
      const bitOffset = bitIndex % 8;
      if ((this.bitArray[byteIndex] & (1 << bitOffset)) === 0) {
        return false; // DEFINITELY NOT IN SET (100% Certainty)
      }
    }
    return true; // PROBABLY IN SET (Within false-positive probability p)
  }
}
```
