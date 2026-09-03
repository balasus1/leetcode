# Master Capacity Planning, Scaling & Distributed Architecture Framework
### The 20-Year Principal Distributed Systems Architect's Universal Blueprint

---

## 1. Executive Philosophy: How to Lead Capacity Planning in Interviews

When evaluating staff and principal engineering candidates, interviewers do not look for memorized numbers; they look for **systematic thought process, dimensional analysis, sanity-checking instincts, and production intuition**.

```
+--------------------------------------------------------------------------------------------------+
| THE 6-STEP CAPACITY PLANNING PIPELINE                                                            |
|                                                                                                  |
| [1] Traffic QPS       -->  [2] Storage Sizing   -->  [3] Memory & Cache (80/20)                  |
|     (Read/Write, Peak)     (Payload + Indexes +      (Hot Working Set in RAM)                    |
|                             Replication + Snapshots)                                             |
|                                     │                                                            |
|                                     ▼                                                            |
| [4] Network Bandwidth -->  [5] Compute Sizing   -->  [6] Cloud Cost Model                        |
|     (Ingress/Egress Gbps)  (Little's Law Cores,      (Compute, Storage, RAM,                     |
|                             Pod Replicas, ThreadPool) Egress Network Tolls)                      |
+--------------------------------------------------------------------------------------------------+
```

### Interview Dialogue Framework (How to Start Small & Explain)
1. **State Baseline & Conversions First**: "Before designing the components, let's establish our traffic baseline, compute read/write ratios, and translate monthly volume into per-second transactional throughput."
2. **Separate Reads from Writes**: Never lump reads and writes together. Writes dictate storage growth, ACID contention, and write-ahead-log (WAL) disk IOPS; reads dictate cache sizing, CDN offload, and read-replica scaling.
3. **Apply the 3x Peak Multiplier**: Real-world traffic is diurnal (peaks during midday, valleys at night). Standard production sizing accounts for at least a $3\times$ peak-to-average ratio (or $5\times - 10\times$ for flash sales/breaking news).
4. **Account for the "Hidden Multipliers"**:
   - **Replication Factor**: Data is rarely stored once; production databases use at least $3\times$ replication across Availability Zones (AZs).
   - **Indexing Overhead**: B-Tree indices, primary keys, and metadata add $20\% - 30\%$ on top of raw payload bytes.
   - **Network Egress Tolls**: Cloud providers (AWS, GCP, Azure) charge heavily for outbound internet egress ($~0.08 - 0.09 per GB); this is often the single highest line item on high-traffic systems.

---

## 2. Universal Constants & Conversions Cheatsheet

```text
================================================================================
TIME CONSTANTS & POWER-OF-10 CONVERSIONS
================================================================================
1 Minute                 = 60 Seconds
1 Hour                   = 3,600 Seconds ≈ 3.6 * 10^3 Seconds
1 Day                    = 24 * 3,600 = 86,400 Seconds ≈ 8.64 * 10^4 Seconds
                         (Rule of thumb for mental math: ≈ 10^5 Seconds)
1 Month (30 Days)        = 30 * 86,400 = 2,592,000 Seconds ≈ 2.592 * 10^6 Seconds
                         (Rule of thumb: ≈ 2.6 Million Seconds)
1 Year (365 Days)        = 365 * 86,400 = 31,536,000 Seconds ≈ 3.15 * 10^7 Seconds
                         (Rule of thumb: ≈ 31.5 Million Seconds)
5 Years                  = 5 * 31.536M = 157,680,000 Seconds ≈ 1.58 * 10^8 Seconds
10 Years                 = 10 * 31.536M = 315,360,000 Seconds ≈ 3.15 * 10^8 Seconds

================================================================================
DATA STORAGE & BANDWIDTH CONVERSIONS
================================================================================
1 Byte (B)               = 8 bits (b)
1 Kilobyte (KB)          = 1,000 Bytes (10^3 B)
1 Megabyte (MB)          = 1,000 KB = 10^6 Bytes
1 Gigabyte (GB)          = 1,000 MB = 10^9 Bytes
1 Terabyte (TB)          = 1,000 GB = 10^12 Bytes
1 Petabyte (PB)          = 1,000 TB = 10^15 Bytes

Bandwidth Conversion Rule:
  Throughput in Mbps     = (Throughput in Bytes/sec * 8) / 1,000,000
================================================================================
```

---

## 3. Latency Numbers Every Systems Architect Must Know

```text
================================================================================
LATENCY REFERENCE SHEET (Peter Norvig / Jeff Dean Scale)
================================================================================
Operation                                       Latency (Real Time)
--------------------------------------------------------------------------------
L1 CPU Cache Reference                          0.5 ns
Branch Mispredict                               5.0 ns
L2 CPU Cache Reference                          7.0 ns
Mutex Lock / Unlock                             25.0 ns
Main Memory (DRAM) Reference                    100.0 ns  (0.1 µs)
Compress 1 KB with Zstandard                    2.0 µs
Read 1 MB sequentially from Memory (RAM)        3.0 µs
Read 1 MB sequentially from NVMe SSD            50.0 µs   (0.05 ms)
Read 1 MB randomly from NVMe SSD                100.0 µs  (0.1 ms)
Round-trip in same Data Center (LAN)            500.0 µs  (0.5 ms)
Disk Seek (Traditional Mechanical HDD)          10,000.0 µs (10 ms)
Read 1 MB sequentially from HDD                 20,000.0 µs (20 ms)
Cross-Continent WAN Round-Trip (US East to West)40,000.0 µs (40 ms)
Transatlantic WAN (US East to Europe)           80,000.0 µs (80 ms)
================================================================================
```

---

## 4. Scaling Complex System Archetypes

Scaling differs drastically depending on whether the system is **High-Contention Checkout**, **Search-to-Booking Asymmetry**, or **High-Throughput Ingestion**.

---

### 4.1 Archetype 1: E-Commerce & Flash Sales (Amazon / Shopify / Flipkart)

```
+-----------------------------------------------------------------------------------------------------+
| E-COMMERCE ARCHITECTURE: THE DUAL-SPEED ENGINE                                                      |
|                                                                                                     |
| [Browsing Path: Read-Heavy, Eventual Consistency]                                                   |
| User ---> Cloudflare CDN (Edge HTML/Static) ---> Redis Cluster (Product Catalog) ---> ElasticSearch |
|                                                                                                     |
| [Checkout Path: High-Contention, Strict Serializability]                                            |
| User ---> Virtual Waiting Room (Queue-it) ---> Token Bucket API ---> Redis Atomic Lua Inventory     |
|                                                                      │ (Stock Hold with 10-min TTL) |
|                                                                      ▼                              |
|                                                     Transactional Outbox -> Kafka -> Saga Engine    |
|                                                                      │                              |
|                                                                      ▼                              |
|                                                     PostgreSQL Partitioned Order Database           |
+-----------------------------------------------------------------------------------------------------+
```

#### Core Challenges & Architectural Solutions:
1. **The Hot-Item Inventory Contention (The "100 iPhones for 1 Million Buyers" Problem)**:
   - *Anti-Pattern*: `SELECT stock FROM inventory WHERE item_id = 123 FOR UPDATE;` (Database row lock blocks all threads, causing connection pool exhaustion and crash).
   - *Production Solution (Atomic In-Memory Reservation)*: Maintain item stock inside **Redis using an Atomic Lua Script**:
     ```lua
     -- Atomic Inventory Decrement with Auto-Sold-Out Flag
     local stock = tonumber(redis.call('get', KEYS[1]) or '0')
     if stock > 0 then
         redis.call('decr', KEYS[1])
         redis.call('sadd', KEYS[2], ARGV[1]) -- Track User Reservation
         return 1 -- Success (Reserve 10-minute hold)
     else
         return 0 -- Sold Out
     end
     ```
2. **Virtual Waiting Rooms (Fair Queueing)**:
   - When traffic spikes $100\times$ during flash sales, incoming users are assigned a signed HMAC cryptographic queue token (e.g. Cloudflare Waiting Room or Redis Sorted Set `ZADD waiting_room <timestamp> <user_id>`).
   - Only a metered batch of $N$ users per second are admitted to the checkout microservice.
3. **Saga Pattern for Distributed Checkouts**:
   - Order creation touches: `Inventory Service` $\rightarrow$ `Payment Service` $\rightarrow$ `Reward Points Service` $\rightarrow$ `Shipping Service`.
   - Use **Choreographed or Orchestrated Sagas (Temporal / Cadence)** with compensating transactions (e.g., if payment fails, automatically trigger `CompensateInventory` to restore reserved stock).

---

### 4.2 Archetype 2: Travel & Flight Booking Platforms (Airbnb / Booking.com / Kayak)

```
+-----------------------------------------------------------------------------------------------------+
| TRAVEL BOOKING ARCHITECTURE: SEARCH-TO-BOOK ASYMMETRY (10,000 : 1 RATIO)                            |
|                                                                                                     |
| [Flight/Hotel Search Pipeline]                                                                      |
| User ---> Global Search API ---> Multi-Tier Fare Cache (Redis + Varnish)                            |
|                                        │ (Cache Miss)                                               |
|                                        ▼                                                            |
|                           Async GDS / Airline Partner Aggregator (Circuit Breakers / Resilient Pool)|
|                                                                                                     |
| [Seat/Room Reservation Pipeline]                                                                    |
| User ---> Hold Lease Engine (Redis Key with 15-Minute TTL + Redlock) ---> Stripe / Payment Gateway  |
|                                        │                                                            |
|                                        ▼ (Confirmed)                                                |
|                           Commit Booking & Generate PNR in RDBMS                                    |
+-----------------------------------------------------------------------------------------------------+
```

#### Core Challenges & Architectural Solutions:
1. **Extreme Search-to-Booking Asymmetry ($10,000:1$ to $50,000:1$)**:
   - Searching for flights/hotels requires querying hundreds of third-party Global Distribution Systems (GDS: Sabre, Amadeus) and partner APIs. Third-party APIs are slow ($1 - 3\text{ seconds}$) and expensive per query.
   - *Solution*: **Multi-tier Hierarchical Fare Caching**:
     - **L1 Cache (Edge CDN / Redis)**: Caches popular route price estimates (e.g., `NYC -> LON` on `2026-10-15`) with a 15-minute TTL.
     - **Cache Warming / Proactive Crawlers**: Background workers pre-fetch fares for top $10,000$ global city pairs.
2. **Temporary Hold Leases (The 15-Minute Seat Lock)**:
   - When a user selects a flight seat or hotel room, the item cannot be sold to others, but cannot be permanently committed until payment finishes.
   - *Solution*: **Distributed Lock with TTL Lease**:
     ```text
     Lock Key: "lease:flight:BA104:seat:14B"
     Value   : "<user_id>:<uuid>"
     TTL     : 900 seconds (15 minutes)
     ```
   - If the user abandons payment, the Redis TTL automatically expires the key, making the seat immediately available without manual database rollback jobs.
3. **Partner API Circuit Breaking**:
   - External airline GDS endpoints frequently experience brownouts. Wrap every external partner integration in a **Resilience4j Circuit Breaker** with fallbacks to cached/stale fare indicators.

---

## 5. Comprehensive Caching Architectures & Mechanisms

```
+-----------------------------------------------------------------------------------------------------+
| MULTI-TIER CACHE TAXONOMY                                                                           |
|                                                                                                     |
| [Tier 1: Client / Browser]       --> Local Storage / HTTP Cache Headers (Cache-Control, ETag)       |
| [Tier 2: Edge CDN]               --> Cloudflare / CloudFront (HTML Pages, Static Assets, Edge 302)  |
| [Tier 3: Reverse Proxy Gateway]  --> Varnish / NGINX Micro-caching                                  |
| [Tier 4: In-Process L1 Cache]    --> Java Caffeine / Go BigCache / Guava (Sub-microsecond RAM)      |
| [Tier 5: Distributed L2 Cache]   --> Redis Cluster / Memcached (Sub-millisecond Shared Store)       |
| [Tier 6: Database Buffer Pool]   --> PostgreSQL shared_buffers / InnoDB Buffer Pool                 |
+-----------------------------------------------------------------------------------------------------+
```

### 5.1 Cache Write Strategies

```text
+-----------------------------------------------------------------------------------------------------+
| COMPARISON OF CACHE WRITE PATTERNS                                                                 |
+----------------------+--------------------+---------------------+-----------------+-----------------+
| Strategy             | Write Latency      | Consistency         | Risk of Data    | Best Used For   |
|                      |                    |                     | Loss on Crash   |                 |
+----------------------+--------------------+---------------------+-----------------+-----------------+
| 1. Cache-Aside       | Fast (DB only)     | Eventual (Lazy Load)| Zero            | Read-heavy,     |
|    (Lazy Loading)    |                    |                     |                 | general apps    |
| 2. Read-Through /    | Moderate (App ->   | Strong              | Zero            | Reference data, |
|    Write-Through     | Cache -> DB sync)  |                     |                 | User profiles   |
| 3. Write-Behind      | Ultra-Fast (Writes | Eventual            | High (If cache  | Click counters, |
|    (Write-Back)      | to RAM; async DB)  |                     | crashes pre-DB) | IoT telemetries |
| 4. Write-Around      | Fast (Writes direct| Eventual            | Zero            | Infrequently    |
|                      | to DB; skips cache)|                     |                 | read logs/files |
| 5. Refresh-Ahead     | Proactive (Auto-   | High (Always warm)  | Zero            | Viral news,     |
|                      | refresh on TTL)    |                     |                 | Top flight fares|
+----------------------+--------------------+---------------------+-----------------+-----------------+
```

### 5.2 Cache Failure Modes & Defense Matrix

```text
+-----------------------------------------------------------------------------------------------------+
| CACHE FAILURE MODES & HARDENING STRATEGIES                                                          |
+----------------------+-----------------------------------+------------------------------------------+
| Failure Mode         | Description                       | Production Solution                      |
+----------------------+-----------------------------------+------------------------------------------+
| 1. Cache Stampede    | Hot key expires; 50,000 requests  | • Singleflight Request Coalescing        |
|    (Thundering Herd) | miss cache simultaneously and hit | • XFetch Probabilistic Early Refresh     |
|                      | database at the same millisecond. | • Mutex Locking (Redlock)                |
+----------------------+-----------------------------------+------------------------------------------+
| 2. Cache Penetration | Malicious requests query keys that| • Bloom Filter in front of cache         |
|                      | DO NOT exist in cache OR database | • Cache Null Objects with short TTL (60s)|
|                      | (e.g. `user_id = -999999`).       |                                          |
+----------------------+-----------------------------------+------------------------------------------+
| 3. Cache Breakdown   | A single viral super-hot key      | • Multi-level Local L1 Cache (Caffeine)  |
|    (Hot-Key Overheat)| exceeds single Redis node NIC/CPU | • Key Replication with Random Suffixes   |
|                      | bandwidth (e.g. `item:iphone16`). |   (`item:iphone16_1`, `item:iphone16_2`) |
+----------------------+-----------------------------------+------------------------------------------+
| 4. Cache Avalanche   | Thousands of keys are saved with  | • Add Random Jitter to TTL:              |
|                      | the exact same TTL (e.g. 1 hour)  |   `TTL = Base_TTL + rand(-300, 300)`     |
|                      | and expire at the exact same sec. |                                          |
+----------------------+-----------------------------------+------------------------------------------+
```

---

## 6. Circuit Breakers, Fault Tolerance & Error Processing

Distributed systems must be designed for **graceful degradation** under partial failure.

```
+-----------------------------------------------------------------------------------------------------+
| CIRCUIT BREAKER STATE MACHINE (Resilience4j / Envoy)                                                |
|                                                                                                     |
|              ┌───────────────────────────┐                                                          |
|              │          CLOSED           │ (Normal Operation: All calls pass through)               |
|              └─────────────┬─────────────┘                                                          |
|                            │ Failure Rate Exceeds Threshold (e.g. > 50% errors in 10s)              |
|                            ▼                                                                        |
|              ┌───────────────────────────┐                                                          |
|              │           OPEN            │ (Tripped: Fast-fail immediately with Fallback)           |
|              └─────────────┬─────────────┘                                                          |
|                            │ Wait Duration Expires (e.g. 30s cooldown)                              |
|                            ▼                                                                        |
|              ┌───────────────────────────┐                                                          |
|              │         HALF-OPEN         │ (Probing: Allow 10 trial requests)                       |
|              └──────┬─────────────┬──────┘                                                          |
|      Probes Succeed │             │ Probes Fail                                                     |
|                     ▼             ▼                                                                 |
|                 (CLOSED)        (OPEN)                                                              |
+-----------------------------------------------------------------------------------------------------+
```

### 6.1 Retry with Exponential Backoff + Full Jitter
Never retry immediately in a tight loop (which causes a self-inflicted DDoS retry storm).

$$\text{Sleep Time} = \text{random}(0, \min(\text{Max\_Sleep}, \text{Base\_Sleep} \times 2^{\text{attempt}}))$$

```java
public class ResilientRetry {
    private static final int BASE_SLEEP_MS = 100;
    private static final int MAX_SLEEP_MS = 3000;

    public static long calculateFullJitter(int attempt) {
        long exponentialBackoff = Math.min(MAX_SLEEP_MS, BASE_SLEEP_MS * (1L << attempt));
        return ThreadLocalRandom.current().nextLong(0, exponentialBackoff);
    }
}
```

### 6.2 Dead Letter Queues (DLQ) & Poison Pills
When a consumer encounters a malformed payload (poison pill) or unrecoverable business error:
1. **Immediate Retry (3x)** with exponential backoff.
2. If all retries fail, publish message to a **Dead Letter Queue (DLQ)** along with error headers (`x-exception-message`, `x-original-topic`, `x-retry-count`).
3. Ack the original queue so the pipeline is not blocked.
4. Alerts trigger on DLQ depth $> 0$; engineers inspect and replay messages via a DLQ Redrive worker once bug is fixed.

---

## 7. Leader Election, Consensus & Distributed Coordination

When multiple microservice instances need to coordinate single-leader tasks (e.g., cron jobs, shard leaders, ID range allocators):

```
+-----------------------------------------------------------------------------------------------------+
| COMPARISON OF LEADER ELECTION & CONSENSUS MECHANISMS                                                |
+----------------------+-------------------+-----------------------+--------------------+-------------+
| Mechanism            | Consensus Protocol| Partition Tolerance   | Split-Brain Defense| Best Used For|
+----------------------+-------------------+-----------------------+--------------------+-------------+
| 1. Raft (etcd)       | Quorum $(N/2 + 1)$| Strict Consistency    | Pre-vote + Lease   | Kubernetes, |
|                      |                   | (CP in CAP)           | Heartbeats         | Config mgmt |
| 2. ZAB (ZooKeeper)   | Multi-Paxos variant| Strict Consistency   | Quorum Ephemeral   | Kafka KRaft,|
|                      |                   | (CP in CAP)           | Znodes             | Hadoop, Solr|
| 3. Redis Redlock     | Non-consensus TTL | High Availability     | Fencing Tokens     | Lightweight |
|                      | (Time-based lock) | (AP in CAP)           | (Weak lease)       | App Locks   |
| 4. Database Locking  | ACID Row Lock     | DB Single Primary     | DB Primary Lock    | Low scale   |
|    (`FOR UPDATE`)    |                   |                       |                    | monoliths   |
+----------------------+-------------------+-----------------------+--------------------+-------------+
```

### The Split-Brain Problem & Fencing Tokens
In asynchronous networks, GC pauses or network partitions can make a node *think* it is still leader when a new leader has already been elected.

* **Fencing Token Solution**: The consensus cluster issues a strictly monotonically increasing **Epoch / Generation ID** with every lease.
  1. Old Leader (paused by GC) wakes up and writes to storage with Token `Epoch = 41`.
  2. Storage layer has already processed an update from New Leader with Token `Epoch = 42`.
  3. Storage layer **rejects Token 41** with `FencingTokenRejectedException`, preventing split-brain data corruption.

---

## 8. Message Queues vs Event Streams: When to Use What

```
+-----------------------------------------------------------------------------------------------------+
| DECISION MATRIX: KAFKA vs RABBITMQ vs AWS SQS vs APACHE PULSAR                                      |
+----------------------+-----------------------+-----------------------+------------------------------+
| Dimension            | Apache Kafka          | RabbitMQ (AMQP)       | AWS SQS / Azure ServiceBus   |
+----------------------+-----------------------+-----------------------+------------------------------+
| 1. Core Model        | Append-Only Log       | Smart Broker /        | Cloud-Managed Distributed    |
|                      | (Pull-based)          | Dumb Consumer (Push)  | Queue (Pull-based)           |
| 2. Throughput        | 1M+ msgs/sec per node | ~50K - 100K msgs/sec  | Virtually Unlimited          |
|                      | (Zero-Copy OS buffer) |                       | (Auto-scaled by AWS)         |
| 3. Message Retention | Long-term (Days/Years)| Ephemeral (Deleted    | 14 Days Max                  |
|                      | Replay from offset 0  | immediately after ACK)| (Deleted on ACK)             |
| 4. Ordering          | Strict per Partition  | Total order in queue  | FIFO SQS (3000 QPS max);     |
|                      | Key (`hash(key)%N`)   | (Breaks on retries)   | Standard SQS has best-effort |
| 5. Routing Complexity| Basic (Topic/Partition| Advanced (Exchange,   | Basic (Topic to Queue)       |
|                      | key based)            | Direct, Topic, Fanout)|                              |
| 6. Backpressure      | Inherent (Pull model; | Broker memory buffers | Invisible (Consumer controls |
|                      | Consumer polls speed) | can overflow/choke    | poll batch size)             |
+----------------------+-----------------------+-----------------------+------------------------------+
```

### Architectural Decision Tree: "When to Use What?"

```
                       [ Do you need to stream/process events? ]
                                          │
                    ┌─────────────────────┴─────────────────────┐
                    ▼                                           ▼
          [ High-Throughput (>100k/s) ]                [ Complex Routing, Task Queues, ]
          [ Event Sourcing & Analytics]                [ Delayed / Priority Messages   ]
          [ Multiple Consumer Groups  ]                [ Fast ACK & Pop Semantics      ]
          [ Replayability Needed      ]                         │
                    │                                           ▼
                    ▼                                   [ Cloud Managed? ]
             ★ APACHE KAFKA ★                          ┌────────┴────────┐
             (or Apache Pulsar)                        ▼                 ▼
                                                 ★ AWS SQS ★       ★ RABBITMQ ★
                                                 (Zero-Ops)        (Custom AMQP Routing)
```

---

## 9. End-to-End Latency Budget Allocation (P99 Strategy)

```
+--------------------------------------------------------------------------------------------------+
| P99 LATENCY BUDGET BREAKDOWN (Total SLA: 100 ms)                                                 |
|                                                                                                  |
| [1] DNS & Client Network RTT (Anycast / 5G / Fiber)                : 30 ms                       |
| [2] TLS Handshake & Edge CDN Termination (Cloudflare WAF)          : 15 ms                       |
| [3] Application Load Balancer & Routing (Envoy)                    :  5 ms                       |
| [4] API Gateway Auth & Rate Limiting (Redis Token Bucket)          :  5 ms                       |
| [5] Microservice Internal Compute & JSON Serialization             : 10 ms                       |
| [6] Internal Fan-out (Hedged Redis Cache / DB Read)                : 15 ms                       |
| [7] Outbound Network Egress Serialization                          : 10 ms                       |
| [8] Headroom Buffer (Garbage Collection / Jitter)                  : 10 ms                       |
| --------------------------------------------------------------------------                       |
| TOTAL P99 LATENCY BUDGET                                           = 100 ms                      |
+--------------------------------------------------------------------------------------------------+
```

---

## 10. Universal 6-Step Capacity & Cloud Cost Calculation Template

```text
================================================================================
STEP 1: TRAFFIC & QPS FORMULAE
================================================================================
[1] Write Traffic:
    QPS_write_avg = N_write / 2,592,000
    QPS_write_peak = QPS_write_avg * 3  (Use 5x to 10x for Flash Sales)

[2] Read Traffic:
    N_read = N_write * R
    QPS_read_avg = N_read / 2,592,000
    QPS_read_peak = QPS_read_avg * 3

================================================================================
STEP 2: STORAGE CONSUMPTION (5-YEAR & 10-YEAR)
================================================================================
Per-Row Effective Size (with 25% B-Tree Index & Metadata Overhead):
  S_effective = S_raw * 1.25

5-Year Storage Total:
  Storage_5yr = N_write * S_effective * 12 * 5

10-Year Storage Total:
  Storage_10yr = N_write * S_effective * 12 * 10

Production Replicated Storage (3x Multi-AZ + 50% Snapshot Backups):
  Total_Usable_Storage = (Storage_5yr * 3) + (Storage_5yr * 0.5)

================================================================================
STEP 3: DISTRIBUTED CACHE SIZING (80/20 PARETO)
================================================================================
Daily Read Volume:
  Daily_reads = N_read / 30

Daily Unique Active Working Set:
  Hot_objects_daily = Daily_reads * 0.20

Active Memory Cache:
  Cache_RAM = Hot_objects_daily * S_effective * 7 days (Buffer) * 1.20 (Metadata)

================================================================================
STEP 4: NETWORK BANDWIDTH (INGRESS / EGRESS)
================================================================================
Ingress Bandwidth (Mbps) = ((QPS_write_avg * S_raw + QPS_read_avg * 100) * 8) / 1,000,000
Egress Bandwidth (Mbps)  = ((QPS_read_avg * S_response + QPS_write_avg * 200) * 8) / 1,000,000

================================================================================
STEP 5: COMPUTE SIZING VIA LITTLE'S LAW
================================================================================
Formula: L = λ * W
  Concurrent In-Flight Requests (L) = Peak_QPS * Avg_Latency_Seconds
  Required Node Count = ceil((Peak_QPS * Latency_sec) / (Throughput_per_core * Cores_per_node)) * 2

================================================================================
STEP 6: CLOUD COST ESTIMATION WORKSHEET (AWS BASELINE)
================================================================================
[1] Compute Pods: Instances * Cost/mo (c6i.xlarge = $124.10/mo)
[2] Distributed Cache: Cache Nodes * Cost/mo (cache.m6g.large = $99.28/mo)
[3] Database Tier: RDS PostgreSQL Multi-AZ + Replicas ($560.64 + $280.32/mo)
[4] Block & Object Storage: EBS ($0.08/GB-mo) + S3 ($0.023/GB-mo)
[5] Outbound Egress: Egress_TB * $90.00/TB
[6] Surcharge: 15% Managed Services & Observability
================================================================================
```
