# Reliability, Fault Tolerance & Resilience Patterns
### Master Architecture Guide for Senior & Principal Interviews

---

## 1. Reliability Metrics: SLAs, SLOs, SLIs & The High-Availability Math

In production distributed systems:
- **SLI (Service Level Indicator)**: The quantitative measurement of service behavior (e.g., $99.95\%$ of HTTP requests return status $< 500$ within $200\text{ms}$).
- **SLO (Service Level Objective)**: The target reliability agreed upon by engineering and product (e.g., $99.99\%$ monthly availability).
- **SLA (Service Level Agreement)**: The legal/contractual commitment to customers with financial penalties for breach.

```
+---------------------------------------------------------------------------------------------------+
| THE DOWNTIME TOLERANCE PER AVAILABILITY TIER                                                      |
|                                                                                                   |
| Availability "Nines" | Monthly Downtime  | Yearly Downtime   | Typical Architecture Archetype     |
| -------------------- | ----------------- | ----------------- | ---------------------------------- |
| 99.0%  (2 Nines)     | 7.31 Hours        | 3.65 Days         | Single VM, local MySQL, no backup  |
| 99.9%  (3 Nines)     | 43.83 Minutes     | 8.77 Hours        | Multi-AZ VMs, Read Replicas, Auto-scale |
| 99.99% (4 Nines)     | 4.38 Minutes      | 52.60 Minutes     | Multi-AZ Active-Active, Auto Failover|
| 99.999%(5 Nines)     | 26.30 Seconds     | 5.26 Minutes      | Multi-Region Active-Active, Spanner|
+---------------------------------------------------------------------------------------------------+
```

### Composite Availability in Distributed Topologies

When multiple components interact in a distributed path:

1. **Components in Series (Dependent Chains)**:
   Overall system availability is the **product** of individual component availabilities. It is always **lower** than the least reliable component.
   $$A_{\text{series}} = A_1 \times A_2 \times A_3 \times \dots \times A_n$$
   *Example*: Service ($99.9\%$), Cache ($99.9\%$), Database ($99.9\%$):
   $$A_{\text{series}} = 0.999 \times 0.999 \times 0.999 = 0.9970 \quad (99.70\% \text{ Availability})$$

2. **Components in Parallel (Redundant Replicas)**:
   Overall availability increases because failure requires **all** redundant nodes to fail simultaneously.
   $$A_{\text{parallel}} = 1 - (1 - A_1) \times (1 - A_2) \times \dots \times (1 - A_n)$$
   *Example*: Two redundant database replicas, each $99.0\%$ available:
   $$A_{\text{parallel}} = 1 - (1 - 0.99)^2 = 1 - (0.01)^2 = 1 - 0.0001 = 0.9999 \quad (99.99\% \text{ Availability})$$

---

## 2. Circuit Breaker Pattern: State Machine & Failure Isolation

When a downstream dependency (e.g., third-party payment gateway, recommendation service) fails or experiences high latency, upstream callers blocking on timeouts will exhaust their own thread pools, causing **Cascading Failures** across the entire microservice mesh.

```
+---------------------------------------------------------------------------------------------------+
| THE CIRCUIT BREAKER FINITE STATE MACHINE (FSM)                                                    |
|                                                                                                   |
|                       ┌──────────────────────────────────────────────┐                            |
|                       │                                              │                            |
|                       ▼                                              │ Success Rate > Threshold   |
|               ┌───────────────┐     Failure Rate > Limit    ┌─────────────────┐                   |
|               │    CLOSED     │ ──────────────────────────► │      OPEN       │                   |
|               │ (Normal Flow) │                             │ (Fail Fast 0ms) │                   |
|               └───────────────┘                             └─────────────────┘                   |
|                       ▲                                              │                            |
|                       │                                              │ Sleep Window Expired       |
|                       │                                              ▼ (e.g. 10 seconds)          |
|                       │                                     ┌─────────────────┐                   |
|                       │                                     │    HALF-OPEN    │                   |
|                       └──────────────────────────────────── │ (Canary Probes) │                   |
|                                                             └─────────────────┘                   |
|                                                                      │                            |
|                                                                      │ Canary Probe Failed        |
|                                                                      └────────────────────────────┘
+---------------------------------------------------------------------------------------------------+
```

### State Transitions & Implementation Mechanics

1. **Closed (Healthy)**: Requests pass through normally. A sliding window (time-based or count-based, e.g., last 100 requests) records successes and failures.
2. **Open (Fault Detected)**: When failure rate exceeds threshold (e.g., $> 50\%$ errors or $> 2000\text{ms}$ latency), the breaker trips to `OPEN`. All incoming calls immediately **fail fast (0ms latency)** without making a network call, returning a cached fallback or error.
3. **Half-Open (Canary Testing)**: After a sleep duration (e.g., 10 seconds), the breaker transitions to `HALF-OPEN`. It permits a small bounded number of trial requests (e.g., 5 probe requests).
   - If trial requests succeed, the breaker returns to `CLOSED`.
   - If any trial request fails, it immediately returns to `OPEN` for another sleep cycle.

---

## 3. Exponential Backoff & Jitter: Mitigating the Thundering Herd

When a downstream database recovers after a temporary outage, millions of waiting clients retrying at identical fixed intervals create a destructive **Retry Storm** (Thundering Herd) that knocks the service back down.

```
+---------------------------------------------------------------------------------------------------+
| RETRY POLICIES COMPARISON                                                                         |
|                                                                                                   |
| 1. Naive Fixed Retry:                                                                             |
|    Retry at: t=1s, 2s, 3s, 4s  --> Synchronized spikes crash recovering database.                 |
|                                                                                                   |
| 2. Exponential Backoff:                                                                           |
|    Interval = Base * 2^(Attempt)                                                                  |
|    Retry at: t=1s, 2s, 4s, 8s, 16s                                                                |
|                                                                                                   |
| 3. Full Jitter (AWS Recommended Algorithm):                                                       |
|    Sleep = Random_Between(0, Min(Max_Sleep, Base * 2^(Attempt)))                                  |
|    Spreads requests uniformly across time spectrum, eliminating synchronized traffic peaks.       |
+---------------------------------------------------------------------------------------------------+
```

### Mathematical Jitter Formulas (AWS Architecture Research)

```typescript
export class RetryPolicy {
  public static calculateFullJitter(attempt: number, baseMs: number = 100, maxMs: number = 10000): number {
    const exponentialCap = Math.min(maxMs, baseMs * Math.pow(2, attempt));
    return Math.floor(Math.random() * exponentialCap); // Uniform distribution [0, exponentialCap]
  }

  public static calculateDecorrelatedJitter(previousSleep: number, baseMs: number = 100, maxMs: number = 10000): number {
    const sleep = Math.min(maxMs, Math.random() * (previousSleep * 3 - baseMs) + baseMs);
    return Math.floor(sleep);
  }
}
```

---

## 4. Bulkhead Pattern: Thread & Resource Isolation

Named after the watertight bulkheads in naval ships (which prevent a hull breach in one compartment from sinking the entire vessel).

```
+---------------------------------------------------------------------------------------------------+
| BULKHEAD RESOURCE ISOLATION                                                                       |
|                                                                                                   |
| [ Global Service ThreadPool (100 Threads) ] -- NAIVE SPOF:                                        |
| If /analytics becomes slow, it consumes all 100 threads. /checkout and /auth are starved and die! |
|                                                                                                   |
| [ BULKHEAD ISOLATION (Dedicated Thread Pools per critical path) ]:                                |
| ┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐             |
| │  Auth & Identity Pool   │  │  Checkout & Order Pool  │  │  Analytics & Recs Pool  │             |
| │  (30 Dedicated Threads) │  │  (50 Dedicated Threads) │  │  (20 Dedicated Threads) │             |
| └─────────────────────────┘  └─────────────────────────┘  └─────────────────────────┘             |
|  * If Analytics degrades, its 20 threads fill up and reject with HTTP 429.                        |
|  * Checkout & Auth continue operating at 100% throughput and zero impact.                         |
+---------------------------------------------------------------------------------------------------+
```

---

## 5. Rate Limiting Algorithms: Architectural Comparison & Redis Lua

Rate limiting protects APIs from denial-of-service, abusive scraping, brute-force attacks, and cascading queue overflows.

```
+---------------------------------------------------------------------------------------------------+
| RATE LIMITING ALGORITHMS                                                                          |
|                                                                                                   |
| 1. Token Bucket: Tokens added at constant rate. Burst allowed up to bucket capacity.             |
| 2. Leaky Bucket: Requests enter FIFO queue; processed at smooth constant leak rate. Zero bursts.  |
| 3. Fixed Window: Counts requests per discrete minute. Flaw: 2x burst across window boundaries.    |
| 4. Sliding Window Log: Stores timestamp per request in Sorted Set. High memory: O(Requests).      |
| 5. Sliding Window Counter: Approximates rate using weighted sum of previous & current window.     |
+---------------------------------------------------------------------------------------------------+
```

### Sliding Window Counter (Weighted Interpolation Formula)

Given window size $W = 60\text{s}$, previous window count $C_{\text{prev}}$, current window count $C_{\text{curr}}$, and current time offset into current window $t_{\text{offset}}$:

$$\text{Estimated Requests} = C_{\text{prev}} \times \left( \frac{W - t_{\text{offset}}}{W} \right) + C_{\text{curr}}$$

If $\text{Estimated Requests} \ge \text{Limit}$, reject with `HTTP 429 Too Many Requests`. Memory footprint is constant: **$O(1)$ space (2 integers per user)**.

```
+---------------------------------------------------------------------------------------------------+
| SLIDING WINDOW INTERPOLATION DIAGRAM                                                              |
|                                                                                                   |
|  [ Previous Window: 12:00 - 12:01 ]      [ Current Window: 12:01 - 12:02 ]                        |
|  Count = 80 Requests                     Count = 30 Requests                                      |
|                                          Current Time = 12:01:18 (Offset = 18s / 30% of window)   |
|                                                                                                   |
|  Weight of Previous Window = 70% (100% - 30%)                                                     |
|  Estimated Requests = (80 * 0.70) + 30 = 56 + 30 = 86 Requests                                    |
|  If Limit = 100: PASS (86 < 100). Increment current counter to 31.                               |
+---------------------------------------------------------------------------------------------------+
```

### Production Distributed Rate Limiter in Redis (Atomic Lua Script)

```lua
-- KEYS[1]: Rate limit key (e.g., "ratelimit:user_45892:1201")
-- KEYS[2]: Previous window key (e.g., "ratelimit:user_45892:1200")
-- ARGV[1]: Max limit per window (e.g., 100)
-- ARGV[2]: Current window elapsed percentage (0.0 to 1.0, e.g., 0.30)
-- ARGV[3]: Window TTL in seconds (e.g., 120)

local current_count = tonumber(redis.call('get', KEYS[1]) or '0')
local previous_count = tonumber(redis.call('get', KEYS[2]) or '0')
local limit = tonumber(ARGV[1])
local elapsed_percent = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])

local estimated_requests = math.floor(previous_count * (1.0 - elapsed_percent) + current_count)

if estimated_requests >= limit then
    return 0 -- REJECT (HTTP 429)
else
    redis.call('incr', KEYS[1])
    redis.call('expire', KEYS[1], ttl)
    return 1 -- ALLOW
end
```

---

## 6. Health Checks & Kubernetes Probes: Liveness vs Readiness vs Startup

```
+---------------------------------------------------------------------------------------------------+
| KUBERNETES PROBE LIFE CYCLE                                                                       |
|                                                                                                   |
| 1. Startup Probe:                                                                                 |
|    - Runs on container launch until initialization completes (DB schema migrations, JVM warmup).  |
|    - Disables liveness/readiness probes until it passes, preventing premature kill cycles.        |
|                                                                                                   |
| 2. Readiness Probe (/health/ready):                                                               |
|    - Validates if pod can accept live traffic (e.g., DB pool connected, cache primed).            |
|    - If FAIL: Removes pod IP from K8s Service / Ingress endpoints. DOES NOT RESTART CONTAINER.     |
|                                                                                                   |
| 3. Liveness Probe (/health/live):                                                                 |
|    - Validates if container process is deadlocked or in a fatal unrecoverable loop.               |
|    - If FAIL: Kubelet immediately sends SIGKILL and restarts the container.                       |
|    * CRITICAL RULE: NEVER check downstream external dependencies (DB/Redis) in Liveness probes.   |
|      If DB goes down, every pod's liveness probe fails, restarting all pods in a catastrophic      |
|      global crash loop!                                                                           |
+---------------------------------------------------------------------------------------------------+
```
