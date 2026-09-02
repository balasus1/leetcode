---
name: system-design-architect
description: Master Principal System Architect framework for breaking down, designing, calculating capacity, and delivering high-scoring system design solutions for FAANG/Tier-1 interviews.
---

# Principal System Architect: Master System Design Framework

This skill provides an end-to-end framework, methodology, calculation cheatsheet, and architectural blueprint for tackling any Distributed System Design problem in staff and principal level interviews.

---

## 1. The 45-Minute Interview Battle Plan

```
+-------------------------------------------------------------------------------+
| PHASE 1: Scoping & Requirements (3-5 mins)                                    |
| - Clarify functional use-cases (top 3-4 features)                             |
| - Define non-functional targets (P99 latency, Availability %s, Scale, CAP)   |
| - Establish explicit out-of-scope boundaries                                  |
+-------------------------------------------------------------------------------+
                                      │
                                      ▼
+-------------------------------------------------------------------------------+
| PHASE 2: Back-of-the-Envelope Capacity Planning (5-7 mins)                    |
| - Time conversions (1 month = 2.592M sec, 1 day = 86,400 sec)               |
| - QPS calculations (Read QPS, Write QPS, Peak QPS with 3x multiplier)         |
| - Storage over 5 & 10 years (data payload + B-tree index overhead)           |
| - Memory cache sizing (80/20 rule: 20% hot data in RAM)                      |
| - Ingress & Egress network bandwidth (Mbps / Gbps)                            |
+-------------------------------------------------------------------------------+
                                      │
                                      ▼
+-------------------------------------------------------------------------------+
| PHASE 3: High-Level Architecture & API Design (10-12 mins)                    |
| - Clean REST / gRPC API contracts with idempotent headers                    |
| - Core data entities & SQL / NoSQL database schema                            |
| - End-to-End request flow diagram (Client -> Edge -> ALB -> App -> DB/Cache) |
+-------------------------------------------------------------------------------+
                                      │
                                      ▼
+-------------------------------------------------------------------------------+
| PHASE 4: Deep Dive & Distributed Systems Engineering (15-18 mins)             |
| - Data partitioning & Consistent Hashing (Virtual nodes)                      |
| - Caching strategies (Cache-aside, Singleflight, XFetch stampede prevention)  |
| - Distributed transactions (Transactional Outbox, Saga, 2PC)                  |
| - Concurrency & Locking (Optimistic OCC vs Pessimistic vs Redlock)            |
| - Tail latency control (P95/P99 budgets, Hedge requests, Timeout budgets)     |
| - Fault tolerance (Circuit breakers, Rate limiters, Bulkhead, DLQ)            |
+-------------------------------------------------------------------------------+
                                      │
                                      ▼
+-------------------------------------------------------------------------------+
| PHASE 5: Bottlenecks, Trade-offs & Operational Readiness (5 mins)             |
| - Single points of failure (SPOF) identification                              |
| - CAP theorem trade-off defense (AP vs CP)                                    |
| - Observability (OpenTelemetry Traces, RED / USE metrics, Structured logs)    |
| - Graceful degradation & Disaster recovery (Multi-region active-active)       |
+-------------------------------------------------------------------------------+
```

---

## 2. Percentile Latency Cheatsheet (P50, P90, P95, P99, P99.9)

### What are Percentiles?
- **P50 (Median)**: 50% of user requests are served faster than this duration. Represents the typical user experience.
- **P95**: 95% of requests are faster than this duration. 1 in 20 users experience worse latency.
- **P99**: 99% of requests are faster than this duration. 1 in 100 requests experience worse latency.
- **P99.9 (Three Nines Tail)**: 99.9% of requests are faster than this duration. Crucial for massive scale where millions of requests flow per minute.

### The "Flaw of Averages" Trap
- **Never use average (mean) latency in distributed systems.**
- An average hides severe outliers. If 99 requests take 2ms and 1 request takes 2,000ms:
  - Average = $\frac{99 \times 2 + 2000}{100} = 21.98\text{ ms}$ (Looks fine on paper).
  - But for that 1% of users, the application felt frozen for 2 full seconds!

### Cascading Tail Latency in Microservice Fan-Out
In a microservice architecture where an aggregator calls $N$ downstream services in parallel:
$$\text{Probability of a slow response} = 1 - (1 - p)^N$$
Where $p$ is the probability of a single service being slow (e.g., $1\% = 0.01$ for P99):
- If $N = 1$ service: $\text{Tail risk} = 1 - (0.99)^1 = 1\%$
- If $N = 10$ services: $\text{Tail risk} = 1 - (0.99)^{10} \approx 9.56\%$
- If $N = 100$ services: $\text{Tail risk} = 1 - (0.99)^{100} \approx 63.4\%$
*Nearly two-thirds of your aggregate requests will experience the P99 worst-case latency!*

### Strategies to Tame Tail Latency
1. **Hedged Requests**: If a downstream request does not respond within P95 duration, fire a secondary redundant request to an alternate replica and take whichever responds first.
2. **Strict Timeouts & Deadline Propagation**: Pass `deadline_ms` in gRPC/HTTP headers. If the budget is exhausted, downstream nodes abort immediately instead of wasting compute.
3. **Queue Eviction / LIFO under load**: Under extreme backlog, dropping oldest requests or switching to LIFO prevents serving already-timed-out requests.
4. **Singleflight / Request Coalescing**: When a cache miss occurs for a hot key, coalesce hundreds of simultaneous requests into a single database query.

---

## 3. Distributed Systems Patterns Master Matrix

| Concept | Primary Purpose | Standard Technology / Algorithm | Interview Trigger Keywords |
| :--- | :--- | :--- | :--- |
| **Consistent Hashing** | Distribute keys uniformly across dynamic nodes with minimal rebalancing. | Ketama, MurmurHash3, Virtual Nodes | Sharding, Cache nodes scaling, Partitioning |
| **Circuit Breaker** | Prevent cascading failure by fast-failing remote calls when error thresholds breach. | Resilience4j, Envoy, Istio | Downstream outage, Service degradation |
| **Rate Limiter** | Protect resources against abuse, DDoS, and starvation. | Token Bucket, Leaky Bucket, Sliding Window Redis Lua | Too many requests, Abuse, Fair usage |
| **Transactional Outbox** | Guarantee atomic DB write + Message publishing without 2PC. | Debezium CDC, Polling publisher, Kafka | Eventual consistency, Dual-write problem |
| **Saga Pattern** | Manage distributed transactions across microservices via compensating actions. | Temporal, Cadence, Event-driven Choreography | Multi-service checkout, Booking flow |
| **Optimistic Locking** | Prevent lost updates in high-read low-write concurrent environments. | Database `version` integer column (OCC) | Inventory reservation, Balance updates |
| **Distributed Lock** | Mutual exclusion across independent microservice instances. | Redis Redlock, ZooKeeper / etcd Leases | Single-node leader election, Cron jobs |
| **XFetch (Probabilistic)**| Prevent cache stampedes by proactively refreshing keys before actual TTL expires. | $e^{-\Delta / (\beta \times \text{TTL})} \le \text{random()}$ | Thundering herd, Hot key expiration |
| **Lamport / Vector Clocks**| Track causality and ordering of distributed events without clock sync. | Lamport Logical Clock, Hybrid Logical Clocks (HLC) | Multi-master replication, Conflict resolution |

---

## 4. Universal Capacity Calculation Formulae

```text
================================================================================
TIME CONSTANTS:
  1 Day     = 86,400 seconds ≈ 10^5 seconds (rough estimate) or 8.64 * 10^4
  1 Month   = 30 Days = 2,592,000 seconds ≈ 2.592 * 10^6 seconds
  1 Year    = 365 Days = 31,536,000 seconds ≈ 3.15 * 10^7 seconds
  5 Years   = 155,520,000 seconds ≈ 1.55 * 10^8 seconds

TRAFFIC FORMULAE:
  Average QPS        = Total Requests in Period / Total Seconds in Period
  Peak QPS           = Average QPS * 3  (Standard industry multiplier)

STORAGE FORMULAE:
  Raw Row Size       = Sum(Field types in bytes)
  Total Row Size     = Raw Row Size + Indexing Overhead (~20-30%)
  5-Year Storage     = Total Row Size * Total Writes per Year * 5

MEMORY CACHE (80/20 RULE):
  Daily Read Traffic = Total Monthly Reads / 30
  Daily Unique Items = 20% of Daily Read Traffic
  Active RAM Cache   = Daily Unique Items * Cached Object Size in Bytes
  With Safety Buffer = Active RAM Cache * 1.5 (or 7-day window)

BANDWIDTH FORMULAE:
  Ingress (Bytes/s)  = Write QPS * Request Payload Bytes + Read QPS * Request Header Bytes
  Egress (Bytes/s)   = Read QPS * Response Payload Bytes + Write QPS * Response Ack Bytes
  Mbps Conversion    = (Bytes/s * 8) / 1,000,000
```
