# Master System Design Estimation & Infrastructure Sizing Playbook
### The Principal Architect's Complete Blueprint: Probing Questions & Answers, Quantitative Derivations, and Metric-to-Infra Translation

---

## 1. Universal Scaling & Infrastructure Derivation Framework

Before diving into individual systems, here is the exact 6-step derivation pipeline used in Staff/Principal FAANG system design interviews:

```
+---------------------------------------------------------------------------------------------------------+
|                                INFRASTRUCTURE DERIVATION PIPELINE                                       |
|                                                                                                         |
|   1. Active Users (DAU/MAU)  ──>  2. Read & Write QPS (Avg & Peak 3x-5x)                                |
|                                             │                                                           |
|       ┌─────────────────────────────────────┼────────────────────────────────────────┐                  |
|       ▼                                     ▼                                        ▼                  |
| 3. Compute Sizing                   4. Storage & Cache Sizing               5. Messaging & Async        |
|    - Latency (p99) & Little's Law      - Daily Ingress/Egress (Gbps)            - Kafka Ingress (MB/s)  |
|    - Worker Threads / Connection Pool  - Redis 80/20 Hot Set RAM                - Partitions Needed     |
|    - App Pods / Instances Needed       - 5-Year DB Storage + Replication        - Consumer Pods Sizing  |
|    - CPU Headroom (70% Max Target)     - Disk IOPS & DB Shards Needed           - Consumer Groups       |
+---------------------------------------------------------------------------------------------------------+
```

---

## 2. Universal Derivation Formulas & Infrastructure Selection Rules

### 2.1 Traffic & QPS Conversion
- **Seconds in a Day**: $24 \times 3600 = 86,400 \approx \mathbf{10^5 \text{ seconds}}$ (Mental Math Shortcut).
- **Average QPS**:
  $$\text{Average QPS} = \frac{\text{Total Daily Requests}}{10^5 \text{ seconds}}$$
- **Peak QPS**:
  $$\text{Peak QPS} = \text{Average QPS} \times \text{Peak Factor } (3\times \text{ to } 5\times)$$

---

### 2.2 Little's Law for Server Provisioning & Worker Threads
- **Little's Law Formula**:
  $$L = \lambda \times W$$
  - $L$ = Number of concurrent requests in the system.
  - $\lambda$ = Arrival rate (Peak QPS).
  - $W$ = Average processing time / latency per request (in seconds).

#### Core Thread Pool Sizing Formula (Goetz Formula):
$$\text{Optimal Threads per Server} = N_{\text{CPU}} \times U_{\text{CPU}} \times \left(1 + \frac{W}{C}\right)$$
- $N_{\text{CPU}}$ = Number of CPU cores available.
- $U_{\text{CPU}}$ = Target CPU utilization (typically $0.70$ or $70\%$).
- $W$ = Wait time / I/O blocking time (DB query, network, cache fetch).
- $C$ = Computation time (CPU cycles spent processing).

#### Total Application Server Pods Needed:
$$\text{Instances / Pods Needed} = \frac{\text{Peak QPS} \times \text{Latency (sec)}}{\text{Max Concurrent Requests per Pod}} \times \text{Redundancy Factor } (1.3\times \text{ for } N+2)$$

---

### 2.3 Cache (Redis / Memcached) Sizing & Eviction Rules
- **Pareto 80/20 Rule**: $20\%$ of the keys generate $80\%$ of all read traffic.
- **Daily Cache Working Set**:
  $$\text{Hot Cache Data} = (\text{Daily Unique Read Volume}) \times 20\%$$
- **Redis RAM Multiplier**: Add $25\% - 30\%$ overhead for Redis data structure pointers, jemalloc fragmentation, and dict metadata:
  $$\text{Actual Redis RAM} = \text{Hot Cache Data} \times 1.30$$
- **Redis Node Sizing**: Standard rule is $\le 32\text{GB - } 64\text{GB}$ per Redis master instance (to prevent long RDB snapshot fork pauses).
  $$\text{Redis Master Shards} = \left\lceil \frac{\text{Actual Redis RAM}}{32\text{ GB}} \right\rceil$$

---

### 2.4 Database Storage, IOPS & Shards Sizing
- **Raw 5-Year Storage**:
  $$\text{Raw Storage} = \text{Write QPS} \times \text{Payload Size (Bytes)} \times (86,400 \times 365 \times 5)$$
- **Usable to Physical Storage Multiplier**:
  $$\text{Total Disk Storage} = \text{Raw Storage} \times 1.25 (\text{Indexes}) \times 3 (\text{Replication Factor}) \times 1.30 (\text{Free Space Buffer})$$
- **Database Sharding Count**:
  $$\text{DB Shards Needed} = \max\left( \frac{\text{Total Storage}}{2\text{ TB per Shard}}, \frac{\text{Peak Write IOPS}}{20,000 \text{ IOPS per Shard}} \right)$$

---

### 2.5 Messaging Systems (Kafka / Event Streaming) Sizing
- **Kafka Partitions Formula**:
  $$\text{Partitions Needed} = \max\left( \frac{\text{Producer Throughput (MB/s)}}{P_{\text{max}} (10\text{ MB/s})}, \frac{\text{Consumer Target Throughput (MB/s)}}{C_{\text{max}} (5\text{ MB/s})} \right)$$
- **Consumer Group Sizing**: 1 Consumer Group per distinct business domain.
- **Consumer Pods**: Exactly 1 Consumer Thread per Partition (extra threads within the same consumer group remain idle).

---

## 3. Top 10 Case Studies: Complete Probing Q&A & Mathematical Derivations

---

### 🏛️ Case Study 1: URL Shortener Service (TinyURL / Bitly)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the expected monthly volume of newly shortened URLs, and what is the read-to-write ratio?"*
  - **Interviewer Response**: *"Assume 100 Million new URLs created per month, with a 100:1 read-to-write ratio (10 Billion redirects per month)."*
- **Candidate Probe 2**: *"What is our latency SLA for redirects, and what is our default link retention policy?"*
  - **Interviewer Response**: *"Redirects must complete in under 15ms (p99). Links should be stored for 5 years by default unless an explicit expiration is provided."*
- **Candidate Probe 3**: *"Can users customize short aliases (vanity URLs), and what character set should we support?"*
  - **Interviewer Response**: *"Yes, vanity URLs up to 16 characters are supported. For generated aliases, use Base62 `[0-9, a-z, A-Z]`."*
- **Candidate Probe 4**: *"What are the data consistency requirements between creation and redirection?"*
  - **Interviewer Response**: *"Read-after-write strong consistency. Once a user creates a short link, it must immediately resolve globally."*

#### Phase 2: Quantitative Derivations
- **Traffic & QPS**:
  - **Write QPS**: $\frac{100 \times 10^6 \text{ URLs}}{2.6 \times 10^6 \text{ sec/month}} \approx \mathbf{40 \text{ writes/sec}}$ (Peak $3\times = \mathbf{120 \text{ writes/sec}}$).
  - **Read QPS**: $40 \times 100 = \mathbf{4,000 \text{ reads/sec}}$ (Peak $3\times = \mathbf{12,000 \text{ reads/sec}}$).
- **Payload & 5-Year Storage Sizing**:
  - Record Schema: `short_url` (7 B) + `long_url` (500 B) + `user_id` (16 B) + `created_at` (8 B) + `expires_at` (8 B) $\approx \mathbf{600 \text{ Bytes}}$.
  - 5-Year URLs: $100\text{M/month} \times 12 \times 5 = \mathbf{6 \text{ Billion records}}$.
  - Raw Storage: $6 \text{ Billion} \times 600 \text{ Bytes} = \mathbf{3.6 \text{ TB}}$.
  - Physical Storage (with $3\times$ replication + $25\%$ indexes + $30\%$ disk headroom):
    $$3.6 \text{ TB} \times 3 \times 1.25 \times 1.30 \approx \mathbf{17.5 \text{ TB Disk Storage}}$$.
- **Cache Sizing (Redis 80/20 Rule)**:
  - Daily Read Volume: $4,000 \text{ QPS} \times 86,400 = 345.6 \text{ Million redirects/day}$.
  - Daily Hot Working Set ($20\%$ of daily reads): $345.6\text{M} \times 0.20 \times 600 \text{ B} \approx \mathbf{41.5 \text{ GB RAM}}$.
  - Actual Redis Memory (with $30\%$ pointer/fragmentation overhead): $41.5 \times 1.30 = \mathbf{54 \text{ GB RAM}}$.
  - **Cluster Config**: $2 \times 32\text{GB Redis Master Nodes}$ with 2 Read Replicas (Active-Replica).
- **Compute Sizing (Little's Law)**:
  - Average Redirect Latency $W$: $80\%$ cache hit (5ms) + $20\%$ DB lookup (25ms) $\implies W = (0.8 \times 0.005) + (0.2 \times 0.025) = \mathbf{9\text{ms} = 0.009\text{s}}$.
  - Concurrent In-Flight Requests at Peak: $L = 12,000 \text{ QPS} \times 0.009\text{s} = \mathbf{108 \text{ concurrent requests}}$.
  - Single 4-vCPU App Pod handles 250 concurrent requests.
  - **App Pod Fleet**: $\lceil 108 / 250 \rceil + 2 \text{ redundancy} = \mathbf{4 \text{ App Pods}}$.

#### Phase 3: Infrastructure Decisions
- **CDN**: Cloudflare / CloudFront for static 301 Permanent Redirect caching on immutable links.
- **Key Generation**: Pre-generated Key Generation Service (KGS) loading tokens in 10,000-key memory batches to avoid DB collision retries.
- **Storage Engine**: NoSQL Key-Value Store (AWS DynamoDB / MongoDB) partitioned by `short_key` hash.

---

### 🛡️ Case Study 2: Distributed Rate Limiter

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"At what layer does this rate limiter operate, and what is the overall system throughput?"*
  - **Interviewer Response**: *"It sits at the API Gateway layer protecting all downstream microservices, processing 500,000 Requests/Second across 10,000 distinct API clients."*
- **Candidate Probe 2**: *"What rate limiting algorithm should we implement, and what is the latency tolerance?"*
  - **Interviewer Response**: *"Sliding Window Counter or Token Bucket. The latency budget is $< 2\text{ms}$ overhead per request."*
- **Candidate Probe 3**: *"What happens if the rate limiter cluster fails or becomes unreachable?"*
  - **Interviewer Response**: *"Fail-Open with alerting, allowing traffic through so critical user flows are not broken by rate limiter downtime."*

#### Phase 2: Quantitative Derivations
- **Traffic & IOPS**:
  - Throughput: $\mathbf{500,000 \text{ QPS}}$ checking rate limits.
  - Key Schema: `rate:{client_id}:{endpoint}` (64 B) + Sliding window sorted set / bucket counter (64 B) $\approx \mathbf{128 \text{ Bytes}}$.
  - Active Keys: $10,000 \text{ clients} \times 50 \text{ endpoints} = 500,000 \text{ keys}$.
  - Total Memory: $500,000 \times 128 \text{ Bytes} \approx \mathbf{64 \text{ MB RAM}}$ (Extremely small memory, but massive I/O!).
- **Redis Cluster Sharding by Throughput (Not Memory)**:
  - A single Redis node executes $\sim 80,000 - 100,000 \text{ ops/sec}$ on a single core.
  - Safe Operating Target: $60,000 \text{ QPS}$ per Redis Master.
  - **Redis Shards Needed**:
    $$\text{Shards} = \left\lceil \frac{500,000 \text{ QPS}}{60,000 \text{ QPS}} \right\rceil = \mathbf{9 \text{ Redis Shards (18 nodes including 1 replica per shard)}}$$.
- **Network Bandwidth**:
  $$\text{Bandwidth} = 500,000 \text{ QPS} \times 128 \text{ Bytes} \times 8 \approx \mathbf{512 \text{ Mbps Ingress / Egress}}$$.

#### Phase 3: Infrastructure Decisions
- **Algorithm**: Redis Lua Script executing Token Bucket atomically to eliminate race conditions.
- **Local Cache Tier**: In-memory L1 cache (Caffeine) inside API Gateway instances caching client rules for 60 seconds with TTL to reduce Redis round-trips.

---

### 🗄️ Case Study 3: Distributed Key-Value Store (DynamoDB / Cassandra Style)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the total data volume, key count, and expected read-to-write ratio?"*
  - **Interviewer Response**: *"1 Billion keys, average item size 2 KB. Read-to-write ratio is 5:1 with 50,000 writes/sec and 250,000 reads/sec."*
- **Candidate Probe 2**: *"What consistency model is required (Strict Linearizability vs Eventual Consistency)?"*
  - **Interviewer Response**: *"Tunable Quorum consistency ($N=3, R=2, W=2$). We must withstand the loss of an entire Availability Zone without data loss."*
- **Candidate Probe 3**: *"What are the access patterns and range query requirements?"*
  - **Interviewer Response**: *"Point lookups by Partition Key `GET(key)` and `PUT(key, val)`. No complex multi-table joins or relational scans."*

#### Phase 2: Quantitative Derivations
- **Storage & Disk Sizing**:
  - Raw Data: $1 \times 10^9 \text{ keys} \times 2 \text{ KB} = \mathbf{2 \text{ TB}}$.
  - Replicated Data ($RF=3$): $2 \text{ TB} \times 3 = \mathbf{6 \text{ TB}}$.
  - SSTable Compaction & Disk Buffer ($50\%$ headroom for LSM compaction): $6 \text{ TB} \times 1.5 = \mathbf{9 \text{ TB NVMe SSD Storage}}$.
- **RAM Sizing (Bloom Filters & MemTables)**:
  - Bloom Filter (10 bits per key): $1 \text{ Billion keys} \times 10 \text{ bits} \approx \mathbf{1.25 \text{ GB RAM}}$.
  - MemTable buffer per node: $512 \text{ MB}$.
- **Node Cluster Sizing**:
  - Per Node Safe Target: Max 1.5 TB SSD storage and 15,000 QPS throughput.
  - Storage constraint: $9 \text{ TB} / 1.5 \text{ TB} = 6 \text{ nodes}$.
  - Throughput constraint: Each write touches 2 nodes ($50\text{k} \times 2 = 100\text{k}$ ops); each read touches 2 nodes ($250\text{k} \times 2 = 500\text{k}$ ops).
  - Total Node Operations = $600,000 \text{ ops/sec}$.
  - **Storage Node Count**: $\lceil 600,000 / 15,000 \rceil = \mathbf{40 \text{ Storage Nodes (e.g., AWS i3en.2xlarge NVMe)}}$.

#### Phase 3: Infrastructure Decisions
- **Storage Engine**: LSM-Tree (Log-Structured Merge-Tree) with Write-Ahead Log (WAL) and SSTables for fast append writes.
- **Partitioning**: Consistent Hashing with 256 Virtual Nodes (VNodes) per physical server.

---

### 💬 Case Study 4: Scalable Real-Time Chat System (WhatsApp / Slack)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is our Daily Active User (DAU) count and average messaging volume per user?"*
  - **Interviewer Response**: *"500 Million DAU. On average, each user sends 40 messages per day (20 Billion messages/day total)."*
- **Candidate Probe 2**: *"What is the peak concurrent connected user count, and what media types do we support?"*
  - **Interviewer Response**: *"At peak, 20% of DAU (100 Million users) maintain active concurrent connections. 1 in 10 messages includes an image/video (avg 200 KB)."*
- **Candidate Probe 3**: *"What are our delivery guarantees (At-least-once, Exactly-once, Message ordering)?"*
  - **Interviewer Response**: *"Strict per-chat sequential message ordering. Messages must be delivered with sub-100ms latency to active recipients."*

#### Phase 2: Quantitative Derivations
- **Message QPS & Bandwidth**:
  - Average Message QPS: $\frac{20 \times 10^9}{10^5 \text{ sec}} = \mathbf{200,000 \text{ msgs/sec}}$.
  - Peak Message QPS ($3\times$): $\mathbf{600,000 \text{ msgs/sec}}$.
  - Text Message Size: $200 \text{ Bytes}$ (Payload + sender/receiver metadata).
  - Ingress Message Throughput: $600,000 \text{ msgs/s} \times 200 \text{ B} = \mathbf{120 \text{ MB/sec}}$.
- **WebSocket Gateway Connection Fleet**:
  - Peak Concurrent Connections: $\mathbf{100 \text{ Million WebSockets}}$.
  - Memory per open TLS WebSocket: $\sim \mathbf{10 \text{ KB}}$ (Socket buffers + file descriptors).
  - Total Connection RAM: $100\text{M} \times 10\text{ KB} = \mathbf{1 \text{ TB RAM}}$.
  - Gateway Server (64 GB RAM, 16 vCPU) handles $\sim 1\text{ Million connections}$ safely.
  - **WebSocket Gateway Servers Needed**: $100\text{M} / 1\text{M} = \mathbf{100 \text{ Gateway Instances}}$ (with $N+20$ buffer $\implies \mathbf{120 \text{ Pods}}$).
- **Kafka Partition Sizing for Chat Delivery**:
  - Ingress Rate = $120 \text{ MB/s}$.
  - Target throughput per partition = $5 \text{ MB/s}$.
  - **Kafka Partitions**: $120 / 5 = \mathbf{24 \text{ Partitions (Minimum)}}$.
- **Storage Sizing (5 Years)**:
  - Text Messages: $20\text{B msgs/day} \times 365 \times 5 \times 200\text{ B} = \mathbf{7.3 \text{ PB}}$ (HBase / Cassandra wide-column store).
  - Media: $2\text{B media/day} \times 365 \times 5 \times 200\text{ KB} = \mathbf{730 \text{ PB}}$ (AWS S3 / GCS + CDN).

#### Phase 3: Infrastructure Decisions
- **Session Registry**: Distributed Redis cluster mapping `user_id -> gateway_server_id` for routing incoming messages to the correct WebSocket node.
- **Client Protocol**: WebSocket over TLS (WSS) with Heartbeat Ping/Pong every 30s.

---

### 📰 Case Study 5: News Feed & Social Timeline (Twitter / Facebook Feed)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the active user base and the read-to-write ratio between viewing feeds and posting?"*
  - **Interviewer Response**: *"300 Million DAU. Users view their feed 5 times per day (1.5 Billion feed views/day) and post 1 time per day (300 Million posts/day)."*
- **Candidate Probe 2**: *"How do we handle the 'Celebrity / Hotspot' problem where an account with 50M followers posts?"*
  - **Interviewer Response**: *"Use a Hybrid Fanout Model: Fanout-on-Write (Push) for standard users, and Fanout-on-Read (Pull) for accounts with $> 20,000$ followers."*
- **Candidate Probe 3**: *"How many posts should be pre-computed and stored in each user's timeline cache?"*
  - **Interviewer Response**: *"Cache the top 800 recent post IDs per active user timeline in memory for instant rendering."*

#### Phase 2: Quantitative Derivations
- **QPS Metrics**:
  - **Post Creation QPS**: $\frac{300 \times 10^6}{10^5} = \mathbf{3,000 \text{ QPS}}$ (Peak $3\times = \mathbf{9,000 \text{ QPS}}$).
  - **Feed Fetch QPS**: $\frac{1.5 \times 10^9}{10^5} = \mathbf{15,000 \text{ QPS}}$ (Peak $3\times = \mathbf{45,000 \text{ QPS}}$).
- **Fanout Write Volume**:
  - Average follower count = 200.
  - Push Fanout Rate = $3,000 \text{ posts/s} \times 200 \text{ followers} = \mathbf{600,000 \text{ timeline updates/sec}}$.
- **Timeline Cache Sizing (Redis Cluster)**:
  - Cache entry per user: 800 Post IDs $\times 8 \text{ Bytes} + \text{metadata} \approx \mathbf{8 \text{ KB per user timeline}}$.
  - 300 Million DAU in cache: $300\text{M} \times 8 \text{ KB} = \mathbf{2.4 \text{ TB RAM}}$.
  - With Redis overhead ($1.35\times$): $\mathbf{3.24 \text{ TB RAM}}$.
  - **Redis Master Shards (64 GB nodes)**: $\lceil 3.24\text{TB} / 64\text{GB} \rceil = \mathbf{51 \text{ Shards (102 nodes with replicas)}}$.
- **Fanout Worker Fleet Sizing (Async Kafka Consumers)**:
  - 600,000 updates/sec. A single worker thread inserts 2,000 timeline entries/sec into Redis.
  - Worker Threads Required = $600,000 / 2,000 = \mathbf{300 \text{ Worker Threads}}$.
  - **Worker Fleet**: $30 \text{ Worker Pods}$ (each running 10 consumer threads connected to Kafka).

#### Phase 3: Infrastructure Decisions
- **Feed Cache**: Redis Sorted Sets (`ZSET`) where `score = post_timestamp` and `value = post_id`.
- **Media Delivery**: CloudFront CDN with presigned S3 uploads.

---

### 🕷️ Case Study 6: Distributed Web Crawler (Googlebot Scale)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the target crawling volume per month and average HTML document size?"*
  - **Interviewer Response**: *"1 Billion web pages crawled per month. Average page size is 500 KB (HTML, text, headers)."*
- **Candidate Probe 2**: *"How do we guarantee crawler politeness and avoid overwhelming target websites?"*
  - **Interviewer Response**: *"Politeness delay of $\ge 1\text{ second}$ between successive requests to the same host domain using host-partitioned queues."*
- **Candidate Probe 3**: *"How do we handle duplicate content and circular URL links?"*
  - **Interviewer Response**: *"Use 64-bit SimHash / Fingerprints for document deduplication and a distributed Bloom Filter for visited URL tracking."*

#### Phase 2: Quantitative Derivations
- **Crawl Throughput & Bandwidth**:
  - Average Crawl Rate: $\frac{10^9 \text{ pages}}{2.6 \times 10^6 \text{ sec/month}} \approx \mathbf{385 \text{ pages/sec}}$.
  - Peak Crawl Rate ($3\times$): $\mathbf{1,150 \text{ pages/sec}}$.
  - Ingress Network Bandwidth: $1,150 \text{ pages/s} \times 500 \text{ KB} \times 8 = \mathbf{4.6 \text{ Gbps Continuous Ingress Bandwidth}}$.
- **Visited URL Bloom Filter Memory**:
  - Target: 10 Billion discovered URLs over time.
  - Bloom Filter (10 bits per item, $1\%$ false positive rate):
    $$10 \times 10^9 \times 10 \text{ bits} = 100 \text{ Billion bits} \approx \mathbf{12.5 \text{ GB RAM}}$$.
- **URL Frontier Queue Storage**:
  - 1 Billion pending URLs in frontier. URL length = 100 Bytes.
  - Queue Memory = $1\text{B} \times 100\text{ B} = \mathbf{100 \text{ GB Storage}}$ (Redis / Kafka backed).
- **Raw Storage per Year**:
  $$1 \text{ Billion pages/month} \times 12 \text{ months} \times 500 \text{ KB} = \mathbf{6 \text{ PB Raw HTML Storage / Year}}$$.

#### Phase 3: Infrastructure Decisions
- **DNS Resolver Tier**: Dedicated local caching DNS resolver fleet (e.g., Unbound/Bind9) to eliminate DNS lookup latency bottlenecks.
- **Document Store**: Distributed object store (Ceph / AWS S3) with Snappy/Zstandard compression ($3\times$ reduction $\implies 2\text{ PB/year}$).

---

### 💳 Case Study 7: Payment Processing System (Stripe / PayPal)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the daily transaction volume and peak Transactions Per Second (TPS)?"*
  - **Interviewer Response**: *"100 Million transactions per day. Average 1,000 TPS, with peak flash sale spikes up to 5,000 TPS."*
- **Candidate Probe 2**: *"What are our data durability and ledger accounting standards?"*
  - **Interviewer Response**: *"Zero data loss (RPO = 0, RTO < 1 min). Immutable Double-Entry Ledger where Total Debits must equal Total Credits for every transaction."*
- **Candidate Probe 3**: *"How do we prevent duplicate charges when network timeouts occur?"*
  - **Interviewer Response**: *"Client-supplied Idempotency Keys enforced via distributed locking and unique database constraints."*

#### Phase 2: Quantitative Derivations
- **TPS & Database Write IOPS**:
  - Peak TPS: $\mathbf{5,000 \text{ Payment TPS}}$.
  - Double-Entry Record Multiplier: Each payment writes:
    1. Transaction Record (1 row)
    2. Debit Entry (1 row)
    3. Credit Entry (1 row)
    4. Audit Log Entry (1 row)
    $\implies \mathbf{4 \text{ database writes per payment}}$.
  - **Peak Database IOPS**: $5,000 \text{ TPS} \times 4 \text{ rows} = \mathbf{20,000 \text{ Write IOPS}}$.
- **Storage Sizing (5 Years)**:
  - Row size = 500 Bytes.
  - Daily writes: $100\text{M transactions} \times 4 \text{ rows} \times 500 \text{ B} = \mathbf{200 \text{ GB / day}}$.
  - **5-Year Replicated Storage**:
    $$200 \text{ GB/day} \times 365 \times 5 \times 3 (\text{Replication}) \times 1.25 (\text{Indexes}) \approx \mathbf{1.36 \text{ PB}}$$.
- **Idempotency Cache Sizing**:
  - Active 24-hour Idempotency Keys in Redis:
    $$100\text{M keys} \times 256 \text{ Bytes} \approx \mathbf{25.6 \text{ GB RAM}}$$ (Fits on 1 Redis Master + Replica).

#### Phase 3: Infrastructure Decisions
- **Storage Engine**: Relational / Distributed SQL with Serializable ACID isolation (Google Cloud Spanner / CockroachDB / PostgreSQL with multi-AZ replication).
- **Asynchronous Processing**: Transactional Outbox Pattern with Debezium CDC publishing events to Kafka for analytics and notification delivery.

---

### ⏰ Case Study 8: Distributed Task Scheduler & Cron (Quartz / Temporal)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the daily volume of scheduled tasks and execution precision window?"*
  - **Interviewer Response**: *"100 Million tasks scheduled per day. Tasks must trigger within $\pm 500\text{ms}$ of their target execution timestamp."*
- **Candidate Probe 2**: *"What task execution durations and failure retry policies should we anticipate?"*
  - **Interviewer Response**: *"Tasks range from quick HTTP webhooks (200ms) to long data jobs (10 mins). Exponential backoff retry with Dead-Letter Queues (DLQ)."*
- **Candidate Probe 3**: *"How do we prevent duplicate task execution across a clustered scheduler fleet?"*
  - **Interviewer Response**: *"Distributed leasing / CAS status transition: `UPDATE tasks SET status='RUNNING' WHERE id=:id AND status='SCHEDULED'`."*

#### Phase 2: Quantitative Derivations
- **Throughput & Little's Law Compute Sizing**:
  - Average Trigger Rate: $\frac{100 \times 10^6}{10^5} = \mathbf{1,000 \text{ tasks/sec}}$.
  - Peak Trigger Rate ($4\times$): $\mathbf{4,000 \text{ tasks/sec}}$.
  - Average Webhook Execution Latency $W = 200\text{ms} = 0.2\text{s}$.
  - In-Flight Concurrent Executions (Little's Law): $L = 4,000 \text{ tasks/s} \times 0.2\text{s} = \mathbf{800 \text{ concurrent executions}}$.
  - Using Go Goroutines / Java Virtual Threads, 800 lightweight threads require $< 2 \text{ MB RAM}$.
  - **Worker Pod Fleet**: $\mathbf{4 \text{ Worker Pods (4 vCPU, 8 GB RAM)}}$.
- **Delay Queue Storage (Redis Sorted Set / Time Wheel)**:
  - Active 24-hour task index in Redis ZSET: `ZADD scheduled_tasks <epoch_millis> <task_id>`.
  - Memory: $100\text{M tasks} \times 200 \text{ Bytes metadata} = \mathbf{20 \text{ GB RAM}}$.

#### Phase 3: Infrastructure Decisions
- **Time Wheel Engine**: Redis Sorted Set partitioned into 60 time buckets (1 per second) polled by workers via `ZRANGEBYSCORE` + Lua script to eliminate lock contention.
- **Execution Buffer**: RabbitMQ / Kafka dispatching ready tasks to worker pools.

---

### 🎟️ Case Study 9: High-Concurrency Ticket Booking System (Ticketmaster / Flash Sale)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the inventory scale and peak concurrent checkout demand during a flash sale?"*
  - **Interviewer Response**: *"100,000 stadium seats released at 10:00 AM. 1 Million concurrent users attempt checkout in the first 60 seconds."*
- **Candidate Probe 2**: *"How do we manage temporary seat holds while users enter payment details?"*
  - **Interviewer Response**: *"Hold seats for 10 minutes. If payment is not completed within 10 minutes, automatically release seats back to the public pool."*
- **Candidate Probe 3**: *"What is our strategy to completely prevent overselling / double-booking under extreme concurrency?"*
  - **Interviewer Response**: *"Atomic decrement at the cache layer with distributed locks, backed by strict DB row-versioning constraints (`version = version + 1`)."*

#### Phase 2: Quantitative Derivations
- **Traffic Spike Throughput**:
  $$\text{Instantaneous Checkout QPS} = \frac{1,000,000 \text{ users}}{10 \text{ sec}} = \mathbf{100,000 \text{ Checkout QPS}}$$.
- **Inventory Cache Footprint**:
  - 100,000 seats $\times 64 \text{ Bytes state} < \mathbf{10 \text{ MB RAM}}$.
  - The entire inventory fits in Redis memory!
- **Lock Contention & Atomic Lua Scripting**:
  - Avoid heavy database row locks (`SELECT FOR UPDATE`) during the 100k QPS flash spike.
  - Execute atomic Redis Lua script: `HSETNX seat:101 "HELD:user_99"` with a 600s TTL.
  - Redis executes $> 80,000 \text{ ops/sec}$ single-threaded, serializing reservations safely.
- **Order Processing Queue (Kafka)**:
  - Successful reservations (100,000 events) published to Kafka over 60s $\implies \mathbf{1,666 \text{ events/sec}}$.
  - **Kafka Partitions**: $\mathbf{8 \text{ Partitions}}$ (smoothly processed by downstream payment workers).

#### Phase 3: Infrastructure Decisions
- **Queueing / Virtual Waiting Room**: Cloudflare Waiting Room / AWS API Gateway throttling to queue users and meter ingress traffic to 20,000 QPS.
- **Seat Expiration Engine**: Redis Keyspace Expiration Notifications / Delayed Message Queue triggering release of unpaid seats.

---

### 📊 Case Study 10: Distributed Web Analytics Engine (Google Analytics / Datadog)

#### Phase 1: Probing Questions & Clarifications (Dialogue)
- **Candidate Probe 1**: *"What is the daily event ingestion rate and query latency SLA?"*
  - **Interviewer Response**: *"100 Billion events tracked per day. Query dashboard aggregation latency should be $< 1\text{ second}$ over 30-day windows."*
- **Candidate Probe 2**: *"What is the event payload structure and data retention requirement?"*
  - **Interviewer Response**: *"Beacon payload is 500 Bytes (timestamp, site_id, user_id, URL, browser, geolocation). Retain raw data for 1 year."*
- **Candidate Probe 3**: *"Can we tolerate approximate analytics for distinct user counts?"*
  - **Interviewer Response**: *"Yes, HyperLogLog (HLL) approximation with $\le 1\%$ error is acceptable for unique visitor metrics."*

#### Phase 2: Quantitative Derivations
- **Ingress QPS & Network Bandwidth**:
  - Average Ingress QPS: $\frac{100 \times 10^9}{10^5} = \mathbf{1,000,000 \text{ Events/sec (1M QPS)}}$.
  - Peak Ingress QPS ($2.5\times$): $\mathbf{2,500,000 \text{ Events/sec (2.5M QPS)}}$.
  - Network Ingress Bandwidth: $2.5 \times 10^6 \times 500 \text{ B} \times 8 = \mathbf{10 \text{ Gbps Continuous Ingress Bandwidth}}$.
- **Kafka Streaming Pipeline Dimensioning**:
  - Ingress Throughput: $2.5\text{M events/s} \times 500\text{ B} = \mathbf{1.25 \text{ GB/sec}}$.
  - Partitions Needed ($10 \text{ MB/s}$ per partition):
    $$\text{Partitions} = \frac{1,250 \text{ MB/s}}{10 \text{ MB/s}} = \mathbf{125 \text{ Kafka Partitions}}$$.
- **Analytical Columnar Storage (ClickHouse / BigQuery / Pinot)**:
  - Daily Ingestion: $100\text{B events} \times 500\text{ B} = \mathbf{50 \text{ TB / day}}$.
  - Columnar Compression ($5\times$ ratio): $50 \text{ TB} / 5 = \mathbf{10 \text{ TB Compressed / day}}$.
  - **1-Year Storage**: $10 \text{ TB/day} \times 365 = \mathbf{3.65 \text{ PB / Year}}$ (Stored on AWS S3 / Google Cloud Storage in Parquet format).

#### Phase 3: Infrastructure Decisions
- **Ingestion Buffer**: High-throughput Go HTTP Collector fleet writing micro-batches directly to Kafka.
- **OLAP Query Engine**: ClickHouse / Apache Pinot with pre-aggregated rollups and HyperLogLog data sketches.

---

## 4. Master Interview Cheat Sheet: Metric-to-Infra Translation Table

| Calculated Metric | Threshold | Recommended Infrastructure Decision |
| :--- | :--- | :--- |
| **Read QPS** | $> 10,000 \text{ QPS}$ | Add **CDN** (Cloudflare/CloudFront) for static assets + **Redis Cluster** (80/20 cache) + DB **Read Replicas**. |
| **Write QPS** | $> 5,000 \text{ QPS}$ | Do not write directly to relational DB. Buffer writes via **Kafka / SQS**, apply database **Sharding / Partitioning** or migrate to **LSM-Tree NoSQL** (Cassandra/DynamoDB). |
| **Active Concurrent WebSockets** | $> 100,000 \text{ Conns}$ | Dedicated **Stateful Gateway Fleet** (Netty / Go / epoll) with Redis Pub/Sub backplane; size based on 10 KB RAM per open socket. |
| **Total DB Storage (5 Yrs)** | $> 2 \text{ TB}$ | Single PostgreSQL node bottleneck. Apply **Range/Hash Sharding** by `user_id` or migrate to distributed SQL (**CockroachDB / TiDB / Spanner**). |
| **Cache RAM Required** | $> 64 \text{ GB}$ | Single Redis node too large for fast RDB forks. Split into **Redis Cluster** with $N$ shards (32 GB RAM per master). |
| **Kafka Ingress Rate** | $> 10 \text{ MB/sec}$ | Partition count $= \text{Ingress (MB/s)} / 10$. Add 1 consumer thread per partition to maintain linear parallel processing. |
| **P99 Latency Requirement** | $< 10 \text{ ms}$ | Serve strictly from **RAM (Redis / In-memory)**; pre-compute and warm caches via async CDC workers. |
| **Financial / Ledger Data** | Any scale | **Double-entry bookkeeping**, transactional outbox with CDC, idempotent keys in Redis, immutable append-only logs. |
