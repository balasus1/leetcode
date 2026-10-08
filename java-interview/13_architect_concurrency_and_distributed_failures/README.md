# 13. Senior Architect: Concurrency, High Load, JVM RCA & Distributed Failures

5 deep-dive architectural scenarios asked in Staff/Principal/Architect interviews focusing on system behavior under load, failure handling, and resilient distributed design.

---

## 📑 Topics Index
1. [Concurrency Decision Matrix: `synchronized` vs `ReentrantLock` vs `ConcurrentHashMap` vs Atomics](#1-concurrency-decision-matrix)
2. [Production JVM Incident: High CPU & Long GC Pauses RCA](#2-production-jvm-incident-high-cpu--long-gc-pauses-rca)
3. [Asynchronous Orchestration: 5 Downstream Services with a 2-Second SLA](#3-asynchronous-orchestration-5-downstream-services-with-a-2-second-sla)
4. [`HashMap` vs `ConcurrentHashMap`: Internal Failure Mechanics under Concurrency](#4-hashmap-vs-concurrenthashmap-internal-failure-mechanics)
5. [Mitigating Retry Storms & Cascading Failures in Distributed Systems](#5-mitigating-retry-storms--cascading-failures-in-distributed-systems)

---

### 1. Concurrency Decision Matrix: `synchronized` vs `ReentrantLock` vs `ConcurrentHashMap` vs Atomics

#### 🎙️ 60-Second Verbal Script
> "When designing concurrent Java applications, I select the synchronization primitive based on **contention level, locking granularity, and failure recovery requirements**:
>
> 1. **`Atomic` Variables (`AtomicInteger`, `AtomicReference`, `LongAdder`)**: Used for single-variable counters or state transitions. They use hardware-level **Lock-Free Compare-And-Swap (CAS)** via CPU instructions, providing the highest throughput under low-to-medium contention. For massive multi-threaded counters, `LongAdder` is preferred over `AtomicLong` because it stripes counters across cells to eliminate CAS cache-line bouncing.
> 2. **`ConcurrentHashMap`**: Used for thread-safe shared associative storage. It avoids coarse global locking by utilizing **lock-free CAS on empty bucket heads and fine-grained `synchronized` locks per bucket node/tree**.
> 3. **`synchronized` keyword**: Ideal for simple, coarse-grained mutual exclusion when code blocks are small. Modern JVMs optimize it heavily with biased locking and lock coarsening.
> 4. **`ReentrantLock` / `StampedLock`**: Chosen when advanced locking semantics are required: **timed lock acquisition (`tryLock(timeout)`)**, interruptible locks, fairness policies, multiple `Condition` queues, or **optimistic read validation via `StampedLock`** for read-heavy caches."

---

### 2. Production JVM Incident: High CPU & Long GC Pauses RCA

#### 🎙️ 60-Second Verbal Script
> "When a production service suddenly exhibits both high CPU and long GC pauses, high CPU is frequently a **symptom of GC thrashing (GC Overhead Limit)** rather than raw application computation.
>
> **My Step-by-Step Investigation Playbook**:
> 1. **Correlate Metrics in APM/Grafana**: Check if CPU spikes align perfectly with GC activity (`jvm.gc.pause`). If CPU is 95% and GC pauses consume 90% of runtime, application threads are repeatedly paused while GC threads burn CPU attempting to reclaim memory.
> 2. **Check Heap & Allocation Rate**: If heap baseline is pinned near maximum (sawtooth pattern flatlining at the top), Old Generation is exhausted, triggering continuous concurrent marking and Full GC cycles.
> 3. **Inspect Thread Dumps (`jcmd <PID> Thread.print`)**: If thread dumps show multiple threads in `RUNNABLE` state inside `G1 Conc#0` or `VM Thread`, CPU is spent on garbage collection.
> 4. **Generate & Analyze Heap Dump (`-XX:+HeapDumpOnOutOfMemoryError` or `jcmd GC.heap_dump`)**: Load the dump into Eclipse MAT. Check the **Dominator Tree** to identify the retaining GC Root (e.g. unbounded in-memory cache, unpaged database query returning 500,000 rows into memory, or unclosed connection leaks).
> 5. **Remediation**: Tune memory allocation, add circuit breakers, paginate queries, or resize JVM heap parameters."

---

### 3. Asynchronous Orchestration: 5 Downstream Services with a 2-Second SLA

#### 🎙️ 60-Second Verbal Script
> "To aggregate 5 downstream microservices within a strict 2-second SLA, I design a **non-blocking, parallel fan-out architecture using `CompletableFuture` (or Virtual Threads in Java 21) with timeout guards, bulkheads, and partial failure fallback**:
>
> 1. **Isolated Thread Pool / Bulkhead**: Execute calls on a dedicated bounded `ExecutorService` (or Virtual Thread Per Task Executor) to prevent downstream latency from starving the main web container.
> 2. **Timeouts & Fallbacks per Service**: Wrap each service call in `.orTimeout(1800, TimeUnit.MILLISECONDS)` and `.exceptionally(fallback)`. This gives each call 1.8s, reserving 200ms for aggregation and network overhead.
> 3. **Parallel Fan-out**: Trigger all 5 futures concurrently and join using `CompletableFuture.allOf()`.
> 4. **Degraded Response Handling**: Differentiate between critical and non-critical services. If the Recommendation service times out, return an empty list or cached fallback, while still returning the primary Order/Account response to the user."

```java
public CompletableFuture<AggregatedDashboardResponse> getAggregatedDashboard(String userId) {
    var executor = customBulkheadPool;

    CompletableFuture<UserProfile> userFuture = CompletableFuture
            .supplyAsync(() -> userClient.getUser(userId), executor)
            .orTimeout(1500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> UserProfile.empty());

    CompletableFuture<AccountBalance> balanceFuture = CompletableFuture
            .supplyAsync(() -> accountClient.getBalance(userId), executor)
            .orTimeout(1500, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> AccountBalance.defaultBalance());

    CompletableFuture<List<Transaction>> txnFuture = CompletableFuture
            .supplyAsync(() -> txnClient.getRecentTransactions(userId), executor)
            .orTimeout(1800, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> Collections.emptyList());

    return CompletableFuture.allOf(userFuture, balanceFuture, txnFuture)
            .thenApply(v -> new AggregatedDashboardResponse(
                    userFuture.join(),
                    balanceFuture.join(),
                    txnFuture.join()
            ));
}
```

---

### 4. `HashMap` vs `ConcurrentHashMap`: Internal Failure Mechanics

#### 🎙️ 60-Second Verbal Script
> "Using a standard `HashMap` in a concurrent environment leads to catastrophic silent bugs and service outages:
>
> 1. **Lost Updates & Data Corruption**: Multiple threads inserting keys simultaneously can overwrite bucket nodes without synchronization, causing keys to vanish silently.
> 2. **Infinite Loops & 100% CPU (Java 7 and earlier)**: In Java 7, concurrent rehashing during table resizing caused circular linked-list references in buckets. Subsequent `get()` calls entered an infinite loop, pinning CPU cores at 100%.
> 3. **Tree Corruption (Java 8+)**: In Java 8+, hash collisions transform buckets into Red-Black Trees (`TreeNode`). Concurrent writes without synchronization corrupt tree pointers, throwing unexpected `ClassCastException` or infinite tree traversal loops.
>
> **How `ConcurrentHashMap` solves this**:
> - It uses **Lock-Free CAS (`compareAndSet`)** to insert the first node in an empty bucket.
> - For populated buckets, it synchronizes **only on the individual bucket's root node** (`synchronized(f)`), allowing concurrent writes across different buckets without blocking reads."

---

### 5. Mitigating Retry Storms & Cascading Failures in Distributed Systems

#### 🎙️ 60-Second Verbal Script
> "A **Retry Storm** occurs when a momentary downstream degradation causes calling clients to retry requests simultaneously. These retried requests multiply incoming traffic ($N \times \text{retries}$), overwhelming the already struggling downstream service into a total catastrophic collapse.
>
> **My Architectural Solution**:
> 1. **Exponential Backoff with Full Jitter**: Never retry immediately or on fixed intervals. Calculate delay as:
>    $$\text{Sleep} = \text{random}(0, \min(\text{MaxBackoff}, \text{Base} \times 2^{\text{attempt}}))$$
>    Jitter desynchronizes client retry waves, smoothing traffic spikes.
> 2. **Circuit Breakers (Resilience4j)**: Stop sending retries once downstream failure exceeds threshold (e.g. 50%), failing fast immediately.
> 3. **Strict Retry Budgets**: Limit retries to a maximum of 10% of total incoming request volume.
> 4. **Non-Retryable HTTP Statuses**: Only retry transient `503 Service Unavailable` or connection timeouts; **never retry `4xx Client Errors` or non-idempotent `POST` mutations without an Idempotency Key**."
