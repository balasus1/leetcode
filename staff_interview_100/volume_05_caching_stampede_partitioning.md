# Volume 5: Caching Architecture, Stampede Defense & Data Partitioning (Q41 - Q50)
*Targeting Staff / Principal Full-Stack Interviews (Mercor, micro1, FAANG)*
*Candidate Context: Balasubramanian Shanmugham (L1/L2 Caching, Redis Cluster, PostgreSQL Partitioning, High-Throughput Ingestion)*

---

## Question 41: Multi-Tier L1 (Caffeine) & L2 (Redis) Cache Coherence via Pub/Sub
**Interviewer:** *"You implemented L1 and L2 caching on your resume. When an entity is updated in node A's local in-memory Caffeine cache, how do you prevent node B from serving stale L1 data without flooding the network with eviction broadcasts?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In high-throughput microservices, querying an external Redis cache over the network on every request introduces a 1ms to 2ms network round-trip. To achieve sub-millisecond P99 latencies, we introduce an **In-Memory L1 Cache (Caffeine)** running directly in JVM heap memory (< 500 nanoseconds), backed by an **L2 Distributed Cache (Redis Cluster)**.
>
> The fundamental architectural challenge is **Cache Coherence**: if Pod A modifies User 101's profile, it updates PostgreSQL and L2 Redis. However, Pod B's local JVM still holds the old User 101 object in its in-memory Caffeine cache.
>
> We solve this using a **Two-Tier Coherence Engine backed by Redis Pub/Sub**:
>
> 1. When a write occurs on Pod A, it writes to the database, updates L2 Redis, and publishes a lightweight invalidation message to a Redis Pub/Sub topic: `cache:invalidate:user:101`.
> 2. All active pods subscribe to this channel. When Pod B receives the message, it invokes `caffeineCache.invalidate('user:101')`. The next time Pod B receives a read request, it misses L1, fetches the fresh record from L2 Redis, and re-populates its local Caffeine cache.
>
> To defend against network partitions or dropped Pub/Sub packets:
> We never rely exclusively on Pub/Sub. Every L1 Caffeine entry is configured with a strict, short time-to-live (`expireAfterWrite(60, TimeUnit.SECONDS)`). This guarantees that even if a network partition drops the eviction broadcast, the maximum duration Pod B can ever serve stale data is bounded at 60 seconds."

### Working Java Multi-Tier Cache Engine
```java
package com.bala.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class MultiTierCacheService {
    private final Cache<String, String> l1Caffeine;
    private final StringRedisTemplate l2Redis;
    private static final String INVALIDATION_TOPIC = "cache:invalidation:channel";

    public MultiTierCacheService(StringRedisTemplate l2Redis) {
        this.l2Redis = l2Redis;
        this.l1Caffeine = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(60, TimeUnit.SECONDS) // Hard staleness bound
                .build();
    }

    public String get(String key, Supplier<String> dbFallback) {
        // 1. Check L1 In-Memory RAM (~500ns)
        String val = l1Caffeine.getIfPresent(key);
        if (val != null) return val;

        // 2. Check L2 Distributed Redis (~1.5ms)
        val = l2Redis.opsForValue().get(key);
        if (val != null) {
            l1Caffeine.put(key, val);
            return val;
        }

        // 3. Fallback to Database
        val = dbFallback.get();
        if (val != null) {
            l2Redis.opsForValue().set(key, val, Duration.ofMinutes(10));
            l1Caffeine.put(key, val);
        }
        return val;
    }

    public void putAndInvalidate(String key, String value) {
        // Update L2 Redis
        l2Redis.opsForValue().set(key, value, Duration.ofMinutes(10));
        // Evict local L1
        l1Caffeine.invalidate(key);
        // Broadcast eviction to all other pods
        l2Redis.convertAndSend(INVALIDATION_TOPIC, key);
    }

    public void handleRemoteInvalidation(String key) {
        l1Caffeine.invalidate(key);
    }
}
```

---

## Question 42: Cache Stampede (Thundering Herd) & The XFetch Algorithm
**Interviewer:** *"When a heavily cached entity expires under 20,000 requests per second, thousands of requests experience a simultaneous cache miss and crush the database. Explain the mathematics of the XFetch probabilistic early refresh algorithm, and how it eliminates cache stampedes."*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When a high-traffic cache key reaches its hard expiration timestamp, thousands of concurrent threads experience a simultaneous cache miss. If all those threads attempt to compute or query the database in parallel, you get a catastrophic **cache stampede** (or thundering herd), which spikes database CPU to 100% and crashes the connection pool.
>
> While mutex locks (Singleflight) can block redundant queries, they introduce thread contention and serialization latency at the moment of expiry.
>
> The most elegant solution is the **XFetch Probabilistic Early Expiration Algorithm** (invented by Vattani et al.):
> Instead of waiting for a key to hit its hard TTL, read requests probabilistically decide to refresh the cache in the background *before* the key ever expires.
>
> The decision rule is:
> $$\Delta - \beta \times \ln(\text{random}()) > \text{remaining\_ttl}$$
> Where:
> - $\Delta$ is the computation time taken to compute/fetch the value (in milliseconds).
> - $\beta$ is a tuning constant ($\beta > 0$, typically $1.0$).
> - $\ln(\text{random}())$ produces a negative number that scales as a random exponential distribution.
> - $\text{remaining\_ttl}$ is the milliseconds left before hard expiration.
>
> When the key is fresh ($\text{remaining\_ttl}$ is large), the condition is mathematically impossible to satisfy. As the key nears its end of life, the probability of triggering early refresh gradually increases. A single worker thread probabilistically triggers the refresh, fetches the database, and resets the TTL, while all other 19,999 threads continue serving warm data from cache with zero wait time."

### Working Java Implementation
```java
package com.bala.cache;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class XFetchCacheProtector<T> {
    private static final Random RANDOM = new Random();
    private final ConcurrentHashMap<String, CacheItem<T>> store = new ConcurrentHashMap<>();

    public record CacheItem<T>(T data, long expiresAtMs, double computeDurationMs) {}

    public T get(String key, double beta, Supplier<T> loader) {
        CacheItem<T> item = store.get(key);
        long now = System.currentTimeMillis();

        if (item != null) {
            long remainingTtl = item.expiresAtMs() - now;
            // XFetch formula: -beta * delta * ln(random())
            double threshold = -beta * item.computeDurationMs() * Math.log(RANDOM.nextDouble());

            if (remainingTtl > threshold) {
                return item.data(); // Cache hit
            }
        }

        // Probabilistic early refresh triggered
        long start = System.currentTimeMillis();
        T freshData = loader.get();
        double duration = System.currentTimeMillis() - start;

        store.put(key, new CacheItem<>(freshData, System.currentTimeMillis() + 300_000, duration));
        return freshData;
    }
}
```

---

## Question 43: Request Coalescing (Singleflight) for Cache Misses
**Interviewer:** *"If a cache key is completely missing on startup (cold cache), XFetch cannot work because there is no existing key to evaluate. How does the Singleflight (request coalescing) pattern ensure that only one thread queries the database while hundreds of other concurrent callers share the result?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When an application boots up with a cold cache, or when a brand-new viral item is queried for the very first time, probabilistic early expiration cannot help because no cached entry exists. If 5,000 requests hit the endpoint within the first 50 milliseconds, all 5,000 requests miss the cache and hammer the database simultaneously.
>
> To eliminate this, we implement **Request Coalescing**, famously known in the Go ecosystem as **Singleflight**.
>
> The Singleflight engine maintains an internal concurrent map of in-flight executions:
> `Map<Key, CompletableFuture<Value>>`
>
> When a request arrives for Key A:
> 1. It checks the in-flight map atomically:
>    `future = map.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> queryDatabase(k)))`
> 2. The very first thread initiates the asynchronous database query.
> 3. Threads 2 through 5,000 execute `computeIfAbsent`, discover that a `CompletableFuture` is already in flight for Key A, and attach a listener: `future.join()`.
> 4. When the single database query completes 40 milliseconds later, the single result is broadcast simultaneously to all 5,000 awaiting caller threads, and the key is removed from the in-flight map.
>
> Exactly **1 database query** was executed instead of 5,000, reducing database load by 99.98% while keeping point-in-time responses identical."

### Working Java Singleflight Engine
```java
package com.bala.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SingleflightCoalescer<T> {
    private final ConcurrentHashMap<String, CompletableFuture<T>> inFlight = new ConcurrentHashMap<>();

    public T execute(String key, Supplier<T> dbLoader) {
        CompletableFuture<T> future = inFlight.computeIfAbsent(key, k -> {
            CompletableFuture<T> call = new CompletableFuture<>();
            try {
                T result = dbLoader.get();
                call.complete(result);
            } catch (Throwable t) {
                call.completeExceptionally(t);
            } finally {
                // Ensure inFlight cleanup upon completion
                inFlight.remove(k);
            }
            return call;
        });

        return future.join();
    }
}
```

---

## Question 44: Cache Penetration Defense using Bloom Filters
**Interviewer:** *"A malicious attacker sends 50,000 requests per second querying non-existent product IDs (e.g. `/api/products/-9999` or `/api/products/random_uuid`). Because the IDs don't exist, the cache always misses, forcing linear database scans that exhaust database connections. How do you defend against Cache Penetration?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "**Cache Penetration** occurs when queries target non-existent data. Because the records do not exist in the database, the cache never stores them, allowing every single request to penetrate straight through to the database and exhaust disk I/O and connection pools.
>
> We defend against Cache Penetration using two complementary techniques:
>
> First, **Bloom Filter Pre-check**:
> We load all valid product IDs into an in-memory **Bloom Filter** (sized for a 1% false positive rate using ~10 bits per item). When an incoming request arrives for ID $X$:
> - The application checks `bloomFilter.mightContain(X)`.
> - If the Bloom Filter returns `false`, we have a 100% mathematical guarantee that the ID does not exist. The application immediately rejects the request with HTTP 404 in under 50 nanoseconds, completely bypassing both Redis and PostgreSQL.
> - If it returns `true`, the query proceeds to the cache and database.
>
> Second, **Caching Null Values with Short TTL**:
> Because Bloom Filters have a 1% false positive probability, approximately 1 in 100 non-existent IDs will pass the filter and reach the database. When the database confirms that the record does not exist, we explicitly cache a placeholder:
> `SET product:null:{id} '__EMPTY__' EX 60`
> Subsequent requests within the next 60 seconds hit the null cache and return 404 without touching the database."

---

## Question 45: Redis Cluster Failover: Split-Brain & Asynchronous Replication Loss
**Interviewer:** *"Redis replication to replicas is asynchronous. During a network partition, a Redis master becomes isolated, Sentinel/Cluster promotes a replica to new master, and the old master reconnects. How does Redis handle split-brain, and how much committed write data can be silently lost?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Redis Sentinel and Redis Cluster use **asynchronous replication** by default for maximum write throughput. When a client writes to the Master, the Master appends the write to its memory buffer, sends an acknowledgment to the client immediately, and streams the replication stream to replicas asynchronously.
>
> This introduces a catastrophic failure mode during a network partition: **Asynchronous Data Loss under Master Isolation (Split-Brain)**:
>
> 1. Master A is isolated on the minority side of a network partition with Client 1.
> 2. The majority side of the cluster notices Master A is missing, declares it dead after `cluster-node-timeout`, and promotes Replica B to be the **New Master**.
> 3. Meanwhile, Client 1 continues writing to Old Master A.
> 4. When the network partition heals, Old Master A reconnects. It is demoted to a replica of New Master B. 
> 5. As a newly demoted replica, it completely flushes its local dataset and performs a full synchronization from Master B.
> 6. **Every single write Client 1 made to Old Master A during the partition is permanently wiped out.**
>
> To mitigate this risk, we configure Redis safety guards:
> - `min-replicas-to-write 1`
> - `min-replicas-max-lag 10`
>
> If Master A cannot communicate with at least 1 replica with less than 10 seconds of lag, Master A **refuses all incoming writes** and returns an error, bounding maximum data loss."

---

## Question 46: Consistent Hashing with Virtual Nodes: Mathematical Distribution
**Interviewer:** *"Walk me through the exact mathematics of Consistent Hashing. Why does standard consistent hashing suffer from load variance ($O(\ln N)$), and how do Virtual Nodes (vnodes) reduce variance to within 5% of ideal uniform distribution?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In traditional hashing (`hash(key) % N`), changing $N$ forces almost 100% of keys to migrate. Consistent Hashing maps both keys and servers to an abstract 32-bit or 64-bit integer ring (from $0$ to $2^{64}-1$). Keys are assigned to the first server whose position on the ring is greater than or equal to the key's hash coordinate.
>
> However, pure consistent hashing has a major mathematical flaw: **Non-uniform Token Variance**.
> With $N$ physical nodes placed randomly on the ring, the arc length between adjacent nodes varies wildly according to a Poisson distribution. The most heavily loaded node typically receives $O(\ln N)$ times more traffic than the average node, creating severe hotspots where one server CPU reaches 100% while its neighbor sits idle at 10%.
>
> We resolve this mathematically by assigning each physical node $V$ **Virtual Nodes (vnodes)**:
> Instead of mapping 'Server A' once, we map `Server_A#VN0`, `Server_A#VN1`, ..., `Server_A#VN199` using distinct hash seeds.
>
> By the Central Limit Theorem:
> As the number of virtual nodes per server increases to $V = 150 \text{ to } 256$, the standard deviation of key allocation drops sharply:
> $$\sigma \approx \frac{1}{\sqrt{V}}$$
> With $V = 200$, load variance across physical machines is held within **5% of ideal uniform balance**, completely eliminating cache hotspots."

---

## Question 47: PostgreSQL Table Partitioning (Range vs Hash) for 100M+ Records
**Interviewer:** *"At Morgan Stanley CRM, you stored millions of interaction records. When a PostgreSQL table exceeds 100 million rows, B-tree indexes exceed RAM capacity, causing disk thrashing on every insert and query. How did you design table partitioning (Range vs Hash), and how does Partition Pruning speed up queries?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When a relational table exceeds 100 million rows, the B-tree index alone can consume 40GB+ of storage. Because the database buffer pool cannot hold a 40GB index in RAM, every single insert and read forces the engine to page index blocks in and out of disk, crippling throughput.
>
> In our CRM system, we implemented **PostgreSQL Declarative Range Partitioning by `created_at`** (Monthly partitions):
>
> 1. The parent table `customer_interactions` is partitioned:
>    `CREATE TABLE customer_interactions (...) PARTITION BY RANGE (created_at);`
> 2. Each month has its own physical partition table:
>    `customer_interactions_2025_01 PARTITION FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');`
> 3. Each monthly partition has its own isolated B-tree index on `(customer_id, created_at)`.
>
> The architectural advantages are enormous:
> - **RAM Working Set**: Active queries and writes target the current month. The index for the current month is only 1.5GB, fitting completely in RAM buffer cache and keeping writes fast.
> - **Partition Pruning**: When a user queries `WHERE created_at >= '2025-01-15'`, the PostgreSQL query planner evaluates the constraint during parsing and completely excludes all other 23 monthly partitions from the query execution plan (`enable_partition_pruning = on`).
> - **Instant Archival (Zero-Downtime Eviction)**: Instead of running a slow `DELETE WHERE created_at < 2024`, which generates massive WAL bloat, we simply execute:
>   `ALTER TABLE customer_interactions DETACH PARTITION customer_interactions_2024_01;`
>   This drops 10 million rows in 5 milliseconds without generating any database transaction overhead."

### Working PostgreSQL DDL
```sql
-- Master Partitioned Table
CREATE TABLE customer_interactions (
    interaction_id UUID NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (interaction_id, created_at)
) PARTITION BY RANGE (created_at);

-- Monthly Physical Partitions
CREATE TABLE interactions_y2025m01 PARTITION OF customer_interactions
    FOR VALUES FROM ('2025-01-01 00:00:00+00') TO ('2025-02-01 00:00:00+00');

CREATE TABLE interactions_y2025m02 PARTITION OF customer_interactions
    FOR VALUES FROM ('2025-02-01 00:00:00+00') TO ('2025-03-01 00:00:00+00');

-- Partition-Specific Index (Fits cleanly in RAM)
CREATE INDEX idx_interactions_cust_time_2025m01 
    ON interactions_y2025m01 (customer_id, created_at DESC);
```

---

## Question 48: Cache Eviction Mechanics: LRU vs LFU vs W-TinyLFU
**Interviewer:** *"Why does classical Least Recently Used (LRU) fail under database batch table scans (polluting the cache with one-time records), and how does Caffeine's Window TinyLFU (W-TinyLFU) achieve near-optimal hit ratios?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Classical **LRU (Least Recently Used)** evicts the item that has not been accessed for the longest time. While simple, LRU suffers from severe cache pollution under cyclical or batch scan patterns: if a batch export job reads 100,000 cold records that will never be requested again, those one-time records flood the cache, evicting frequently requested hot items.
>
> Conversely, classical **LFU (Least Frequently Used)** tracks access frequency. However, LFU suffers from historical bias: an item that was viral last week retains an enormous frequency counter, remaining pinned in the cache long after its popularity has died.
>
> **Caffeine** solves this using **Window TinyLFU (W-TinyLFU)**:
>
> 1. **TinyLFU Frequency Sketch**: It uses a Count-Min Sketch with a 4-bit counter per item to track frequency in sub-linear space (only 8 bits per key). A periodic aging reset halves all counters, allowing old viral items to decay naturally.
> 2. **Window Cache (Admission Filter)**: Incoming items first enter a small Window Cache (typically 1% of total capacity) governed by LRU. This allows brand new burst items to establish frequency.
> 3. **Main Cache (Segmented LRU)**: When an item is pushed out of the Window, TinyLFU acts as a gatekeeper: it compares the frequency of the candidate item against the frequency of the victim item currently slated for eviction in the Main Cache. If the candidate has higher recent frequency, it is admitted; if not, it is rejected immediately.
>
> W-TinyLFU consistently outperforms classical LRU by 15% to 30% in hit ratios across enterprise workloads."

---

## Question 49: Cache-Aside vs Write-Through vs Write-Behind (Write-Back)
**Interviewer:** *"Compare the three foundational cache update patterns: Cache-Aside, Write-Through, and Write-Behind. Why would an architect choose Write-Behind for high-frequency user telemetry, and how do you protect against data loss during sudden pod crashes?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "The choice of cache topology defines the consistency and durability guarantees of your storage tier:
>
> 1. **Cache-Aside (Lazy Loading)**:
>    The application coordinates the cache and database. On reads, it checks cache; on misses, it reads database and populates cache. On writes, it writes to database and invalidates cache. 
>    *Trade-off*: Most resilient against cache failures, but requires application-level handling of cache stampedes.
>
> 2. **Write-Through**:
>    The application writes to the cache layer, and the cache synchronously writes to the database before returning success.
>    *Trade-off*: Guarantees cache and DB are always in sync, but write latency is penalized by the sum of cache write + database write time.
>
> 3. **Write-Behind (Write-Back)**:
>    The application writes to the cache, which acknowledges success immediately (< 1ms). The cache asynchronously batches writes and flushes them to the database in background intervals (e.g. every 5 seconds).
>
> We choose **Write-Behind** for write-heavy, non-financial workloads—such as tracking passenger viewing telemetry or CRM chat interaction drafts—because it coalesces multiple updates to the same key into a single database write.
>
> To defend against data loss during pod crashes:
> The in-memory buffer must be paired with an append-only transaction log or backed by a persistent Redis queue (via Redis Streams with AOF `appendfsync everysec`), ensuring that uncommitted mutations survive process restarts."

---

## Question 50: Hotspot Key Sharding: Salted Keys for Viral Entities
**Interviewer:** *"In a multi-master cache cluster, a single viral entity (e.g. live World Cup score or train schedule for 500,000 concurrent viewers) maps to a single Redis node on the hash ring. That single node's network NIC saturates at 100%, causing cluster degradation. How do you shard hot keys using salting?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In consistent hash rings, even if you have 100 cache nodes, all read requests for a single key (`train:schedule:101`) map to the exact same hash slot and node on the ring. When an entity goes viral with 100,000 reads per second, that single Redis server's CPU core and network bandwidth saturate, while the remaining 99 nodes sit idle.
>
> We resolve this using **Hotspot Key Sharding with Random Salting**:
>
> 1. **Write Path (Fan-out Replication)**:
>    When the train schedule updates, the writer does not write to a single key. It writes the data across $N$ salted keys:
>    `train:schedule:101:shard_0`
>    `train:schedule:101:shard_1`
>    ...
>    `train:schedule:101:shard_9`
>    Because each key has a distinct salt suffix, MurmurHash places each shard on completely different physical Redis nodes across the cluster.
> 2. **Read Path (Random Load-Balanced Access)**:
>    When a client application queries the schedule, it appends a randomly selected salt:
>    `int salt = ThreadLocalRandom.current().nextInt(10);`
>    `String key = "train:schedule:101:shard_" + salt;`
>    The 100,000 incoming reads are evenly dispersed across all 10 cache nodes, dividing network traffic and CPU load by 10x and eliminating the hotspot."

### Working Java Salted Hotspot Cache
```java
package com.bala.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SaltedHotspotCacheService {
    private final StringRedisTemplate redis;
    private static final int SHARD_COUNT = 10;

    public SaltedHotspotCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void putHotKey(String baseKey, String value, Duration ttl) {
        // Broadcast write to all salted shards
        for (int i = 0; i < SHARD_COUNT; i++) {
            redis.opsForValue().set(baseKey + "#shard_" + i, value, ttl);
        }
    }

    public String getHotKey(String baseKey) {
        // Read from randomly selected shard to balance cluster load
        int randomShard = ThreadLocalRandom.current().nextInt(SHARD_COUNT);
        return redis.opsForValue().get(baseKey + "#shard_" + randomShard);
    }
}
```
