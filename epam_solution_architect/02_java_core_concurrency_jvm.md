# 02: Java: Core, Collections, Concurrency, JVM, Modern Java (Basic → Advanced)

> JD: *"Expert-level Java… production-grade Java code, refactoring complex codebases, resolve deep technical bottlenecks."*
> Basic answers are kept to **30–60 s**, because padding a basic answer sounds unnatural. Medium and Advanced answers are **1–2 min**, linked to your projects.
> Deeper existing material: [java-interview/](../java-interview/README.md).

| Level | Questions |
|---|---|
| Basic | B1–B14 |
| Medium | M1–M14 |
| Advanced | A1–A10 |

---

## Basic

### B1. JDK vs JRE vs JVM?
🎤 "The **JVM** executes bytecode: class loading, the JIT compiler, garbage collection. The **JRE** was the JVM plus the standard libraries needed to run apps. The **JDK** adds development tools like `javac`, `jlink`, `jcmd` and `jfr`. Since Java 11 there's no separate JRE download. For containers I use `jlink` to build a trimmed runtime with only the modules the app needs, which shrinks images considerably."

### B2. `==` vs `equals()`, and the `hashCode` contract?
🎤 "`==` compares references for objects and values for primitives. `equals()` compares logical equality. The contract: if two objects are equal, they **must** have the same `hashCode`. The reverse isn't required. Break it and `HashMap` or `HashSet` silently lose entries, because the lookup goes to the wrong bucket. For value types I now use **records**, which generate a consistent `equals` and `hashCode` from all components."

### B3. Why is `String` immutable?
🎤 "Four reasons. **Security**: class names, file paths and URLs can't be changed after validation. **Thread safety** without locks. **Hash caching**: the hash is computed once, which makes strings ideal map keys. And the **string pool**: literals are interned and shared. The cost is that concatenation in loops creates garbage, so use `StringBuilder`. Java 9+ compact strings store Latin-1 text in one byte per character."

### B4. `ArrayList` vs `LinkedList`?
🎤 "`ArrayList` is a resizable array: O(1) random access, cache-friendly, amortised O(1) append. `LinkedList` gives O(1) insert and remove *at a known node*, but O(n) access and poor cache locality. In practice `ArrayList` wins almost always, and for queues I use `ArrayDeque`. I can't remember the last time `LinkedList` was the right choice in production."

### B5. How does `HashMap` work internally?
🎤 "It's an array of buckets. The key's `hashCode` is spread (high bits XORed into low bits) and masked to an index. Collisions go into a linked list, which becomes a **red-black tree once a bucket exceeds 8 entries** (with capacity at least 64), so the worst case drops from O(n) to O(log n). It resizes by doubling when size exceeds capacity × load factor (0.75). It isn't thread-safe. Concurrent resizes in old JDKs could even create infinite loops. Use `ConcurrentHashMap` instead."

### B6. Checked vs unchecked exceptions? How do you design exception handling?
🎤 "Checked exceptions must be declared or caught. Unchecked ones extend `RuntimeException`. In service code I prefer **unchecked domain exceptions** (`SeatUnavailableException`, `JobNotFoundException`), translated once at the edge by a Spring `@ControllerAdvice` into RFC 9457 problem-details responses. Rules: never swallow exceptions, preserve the cause, log once at the boundary rather than at every layer, and don't use exceptions for normal control flow."

### B7. `final` vs `finally` vs `finalize`?
🎤 "`final` stops reassignment, overriding or subclassing. `finally` always runs after try/catch, though I prefer **try-with-resources** for cleanup. `finalize` is deprecated for removal. It's unpredictable and slows GC. Use `Cleaner` or explicit `close()` instead."

### B8. Interface vs abstract class (modern Java)?
🎤 "Interfaces define capabilities. A class can implement many, and since Java 8 they can have default and static methods. Abstract classes hold **state and constructors** and allow only single inheritance. I default to interfaces plus composition. **Sealed interfaces** (Java 17) let me model closed hierarchies, like `JobCommand permits Pause, Resume, Stop`, so the compiler checks exhaustive `switch` statements."

### B9. `Comparable` vs `Comparator`?
🎤 "`Comparable` is the type's natural ordering, one per class. `Comparator` is an external, composable ordering: `Comparator.comparing(Job::priority).reversed().thenComparing(Job::createdAt)`. Keep `compareTo` consistent with `equals`, or sorted sets behave strangely."

### B10. When should you use `Optional`?
🎤 "As a **return type** for 'may be absent', like `findById`. Not for fields, parameters or collections; return an empty list instead. Avoid `get()` without a check. Use `orElseThrow`, `map` and `ifPresentOrElse`."

### B11. Streams: intermediate vs terminal; `map` vs `flatMap`?
🎤 "Intermediate operations (`filter`, `map`) are lazy and return a stream. A terminal operation (`collect`, `forEach`, `reduce`) triggers execution. `map` is one-to-one. `flatMap` is one-to-many-then-flatten, for example `orders.stream().flatMap(o -> o.lines().stream())`. Streams can't be reused after a terminal operation."

### B12. What are records? When would you not use them?
🎤 "Records are immutable data carriers with generated constructor, accessors, `equals`, `hashCode` and `toString`. I use them for DTOs, events and value objects, like `record SeatHeld(String bookingId, String seatId, Instant expiresAt)`. I don't use them for **JPA entities**, which need mutability, a no-arg constructor and proxies."

### B13. What's new and useful from Java 11 → 21 for an architect?
🎤 "**Records, sealed types, pattern matching** for `instanceof` and `switch`, text blocks, `var`, **virtual threads** (Java 21), sequenced collections, the HTTP client, ZGC and generational ZGC, and JFR always-on profiling. Virtual threads and pattern matching change how we write I/O-heavy services and domain logic."

### B14. Stack vs heap memory?
🎤 "Each thread has a **stack** of frames holding local primitives and references. The **heap** holds objects and is shared and garbage-collected. A `StackOverflowError` usually means unbounded recursion. `OutOfMemoryError: Java heap space` means too many live objects or a leak. Metaspace holds class metadata, and leaks there come from classloader leaks in redeploys."

---

## Medium

### M1. How does `ConcurrentHashMap` achieve thread safety?
🎤 (~1 min) "In Java 8+, there are no segment locks. Reads are **lock-free** using volatile reads of the bucket array. Writes use **CAS** to insert into an empty bucket, and only **synchronize on the first node of that bucket** when there's a collision, so contention is per bucket. Resizing is cooperative: threads help transfer bins. Use the atomic compound operations (`computeIfAbsent`, `merge`, `compute`) instead of check-then-act. `if (!map.containsKey(k)) map.put(k, v)` is a race even on a concurrent map. One caution: keep the function passed to `computeIfAbsent` short and non-recursive. It runs while holding the bin lock."

```java
// per-tenant counters without races
ConcurrentHashMap<String, LongAdder> perTenant = new ConcurrentHashMap<>();
perTenant.computeIfAbsent(tenantId, t -> new LongAdder()).increment();
```

### M2. Explain `volatile`, happens-before and the Java Memory Model.
🎤 (~1 min) "The JMM defines when one thread's writes are **visible** to another. Without a happens-before relationship, the CPU and JIT may reorder or cache values. `volatile` gives **visibility and ordering**: a write to a volatile happens-before every subsequent read of it. But it gives **no atomicity**, so `count++` on a volatile is still a race. Other happens-before edges are `synchronized` unlock→lock, `Thread.start`/`join`, and the `java.util.concurrent` utilities. Classic uses are a volatile `running` flag for shutdown, and safe publication in double-checked locking, though I'd use the holder idiom or an enum singleton, or simply let Spring manage singletons."

### M3. `synchronized` vs `ReentrantLock` vs `ReadWriteLock` vs `StampedLock`?
🎤 (~1 min) "`synchronized` is simple and JIT-optimised. Before Java 24 it **pinned virtual threads** while blocking inside it. `ReentrantLock` adds tryLock with timeouts, interruptible locking, fairness and multiple conditions, which helps avoid deadlocks via timeouts. `ReentrantReadWriteLock` lets many readers proceed, but writers can starve. `StampedLock` adds **optimistic reads** that are very fast for read-heavy data, but it's non-reentrant and easy to misuse. In the CRM engine the bigger win was **removing shared locks entirely**: single-writer threads and ring buffers."

### M4. How do you size thread pools?
🎤 (~1.5 min) "It depends on the workload. **CPU-bound**: about the number of cores (plus one). **I/O-bound**: cores × (1 + wait time / compute time), and that's only a starting point. Then validate with a load test while watching latency, CPU and queue depth. Three rules from production. **Always use bounded queues** and an explicit rejection policy, because `Executors.newFixedThreadPool` uses an unbounded queue that hides overload until you run out of memory. **Separate pools per dependency** (bulkheads), so one slow downstream can't starve the rest. That's exactly the scheduler cascading-failure lesson. And **name your threads** so thread dumps are readable. With Java 21, for blocking I/O I often use **virtual threads** instead, and limit concurrency with a semaphore to protect the downstream."

```java
ThreadPoolExecutor ingest = new ThreadPoolExecutor(
        16, 32, 30, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(10_000),                      // bounded: backpressure
        Thread.ofPlatform().name("ingest-", 0).factory(),
        new ThreadPoolExecutor.CallerRunsPolicy());            // slow the producer instead of dropping
```

### M5. `CompletableFuture`: composition, error handling, pitfalls?
🎤 (~1 min) "`thenApply` maps, `thenCompose` flattens a dependent async call, `thenCombine` or `allOf` runs work in parallel, and `exceptionally` or `handle` recovers. Pitfalls. Async methods without an executor run on the **common ForkJoinPool**, which is bad for blocking I/O, so always pass an executor. Add `orTimeout` or `completeOnTimeout`, otherwise a hung call hangs forever. And exceptions get wrapped in `CompletionException`, so unwrap them."

```java
var seats = CompletableFuture.supplyAsync(() -> inventory.available(trainId), io)
        .orTimeout(300, MILLISECONDS);
var fares = CompletableFuture.supplyAsync(() -> pricing.fares(trainId), io)
        .completeOnTimeout(Fares.cached(trainId), 200, MILLISECONDS);   // degrade, don't fail
SearchResult r = seats.thenCombine(fares, SearchResult::new).join();
```

### M6. Virtual threads: when to use them, and when not to?
🎤 (~1.5 min) "Virtual threads (Java 21) are cheap, JVM-scheduled threads, so you can write **simple blocking code** and have thousands or millions of concurrent requests. The carrier thread is released while the virtual thread blocks on I/O. They're great for I/O-bound services: REST calls, JDBC, the thread-per-request model. In Spring Boot 3.2+ it's a single property, `spring.threads.virtual.enabled=true`. When not to use them, or to be careful: **CPU-bound work** gets no benefit. **Pinning**: before Java 24, blocking inside `synchronized` or native code pins the carrier, so prefer `ReentrantLock` on hot paths. **Don't pool them**; create one per task. **ThreadLocal-heavy code** multiplies memory, so consider `ScopedValue`. And **downstream limits still apply**: a million virtual threads will exhaust a 50-connection DB pool, so bound concurrency with semaphores or bulkheads. Virtual threads make blocking cheap; they don't make your database bigger."

### M7. G1 vs ZGC vs Shenandoah vs Parallel: how do you choose?
🎤 (~1 min) "**Parallel GC** is best for throughput in batch jobs where pauses don't matter. **G1** is the default and a balanced, region-based collector with a pause-time target, typically 200 ms. Good for most services. **ZGC** (generational since Java 21) and **Shenandoah** do concurrent compaction with **sub-millisecond pauses** regardless of heap size, ideal for latency-sensitive APIs with big heaps, at a small throughput and CPU cost. I choose by measuring: GC logs or JFR under realistic load, comparing P99 latency and CPU. For the scheduler and booking APIs, the P99 target justified trying ZGC. For Spark-style batch, Parallel or G1."

### M8. How do you diagnose a memory leak in production?
🎤 (~1.5 min) "Symptoms: old-gen usage climbs after every full GC, then `OutOfMemoryError` or GC thrashing. Steps. **One**, confirm with GC logs or JFR that the post-GC baseline keeps rising. **Two**, always run with `-XX:+HeapDumpOnOutOfMemoryError` and a dump path on a volume. Or take a live dump with `jcmd <pid> GC.heap_dump`, carefully, since it pauses the JVM. **Three**, analyse in Eclipse MAT: the dominator tree and leak suspects, then the path to GC roots for the biggest retainers. Common culprits I've seen: **unbounded caches** (`static Map` with no eviction; fix with Caffeine max size and TTL), **ThreadLocal in pooled threads** never cleared, listeners never deregistered, and huge result sets loaded fully. **Four**, fix it and add a **regression guard**: a cache size metric and alert."

### M9. Immutability and defensive copying: why does an architect care?
🎤 (~45 s) "Immutable objects are thread-safe by construction, safe to cache and share, and easier to reason about. Use `final` fields, no setters, `List.copyOf` for collections, and records. In event-driven systems, **events must be immutable facts**. A mutated event object after publishing is a subtle and nasty bug."

### M10. `equals`/`hashCode` for JPA entities?
🎤 (~1 min) "Don't use all fields, which breaks with lazy proxies and collections, and don't use a DB-generated ID alone, because it's null before persist and the hash changes after save. That breaks `HashSet` membership. Options: a **business key** that's immutable and unique, like `bookingReference`. Or an application-assigned UUID at construction. Or the Vlad Mihalcea pattern: equals on non-null id, and a **constant `hashCode`** from the class."

### M11. `ThreadLocal` pitfalls, especially with thread pools and tenant context?
🎤 (~1 min) "Pool threads are reused, so a `ThreadLocal` that isn't cleared **leaks data to the next request**. In a multi-tenant system that's a cross-tenant data leak, not just a memory leak. Always clear it in `finally`, typically in a servlet filter or AOP aspect. Also, `ThreadLocal` doesn't propagate to async executors automatically. Use a `TaskDecorator`, or context propagation (Micrometer context-propagation). With virtual threads and Java 25, prefer `ScopedValue`, which is immutable and bounded to a scope."

```java
try { TenantContext.set(tenantId); chain.doFilter(req, res); }
finally { TenantContext.clear(); }     // never skip this
```

### M12. Parallel streams: when are they a bad idea?
🎤 (~45 s) "Avoid them in server code. They use the **shared common ForkJoinPool**, so one request's parallel stream competes with every other request. Bad also for blocking I/O inside the stream, for small data sets where overhead exceeds benefit, and for order-dependent or stateful lambdas. Fine for large CPU-bound batch computations with a custom pool, or better, use explicit executors."

### M13. Generics: type erasure, wildcards (PECS)?
🎤 (~45 s) "Generics are compile-time only and erased at runtime, which is why you can't do `new T[]` or `instanceof List<String>`. PECS: **Producer Extends, Consumer Super**. `copy(List<? super T> dst, List<? extends T> src)`. In framework code I pass a `Class<T>` or a `TypeReference` when the runtime type is needed, as Jackson does."

### M14. How do you make code testable and refactorable in a legacy Java codebase?
🎤 (~1.5 min) "Start with **characterization tests**: tests that capture current behaviour, even if it's wrong, before changing anything. Find **seams**: extract interfaces around static calls, database access and time (`Clock`), so dependencies can be injected. Refactor in **small, behaviour-preserving steps** (extract method or class, introduce parameter object, replace conditional with polymorphism) with tests green after each step. Use **ArchUnit** to stop new violations, like 'controllers must not call repositories directly'. And track hotspots by change frequency × complexity, so effort goes where bugs actually happen. That's how I approach modernization at code level: strangler fig at the architecture level, characterization tests and seams at the code level."

---

## Advanced

### A1. Design a lock-free, high-throughput ingestion pipeline in Java (CRM case) ✅
🎤 (~2 min) "The requirement was to ingest bursty interaction events (email, SMS, calls, chat) at up to around 7,000 per second on six servers, with no loss and audit-ready records.

The design was **single-writer stages connected by bounded ring buffers**. Producer threads (the email and voice connectors) claim slots in a pre-allocated ring buffer. No locks, just a sequence counter with CAS, and pre-allocated entries so there's no garbage per event. A small number of **consumer stages** each own one job: parse and validate, enrich, then the **batch writer**. The writer accumulates records and flushes on **500 records or 50 milliseconds**, whichever comes first, using JDBC `addBatch`/`executeBatch` into partitioned tables, with **idempotent upserts** keyed by interaction ID. **Backpressure**: when the buffer is full, producers wait, or connectors slow their polling, so we never run out of memory.

Why it was fast: no lock contention, sequential memory access, a few round trips per thousand records instead of one per record, and no per-event allocation. That was the 4× throughput gain.

Today I'd add a durable log (Kafka) before the in-memory stage for crash safety, and use virtual threads for the connector I/O."

```java
/** Size-or-time micro-batcher: flush on 500 records or 50 ms, whichever first. */
public final class MicroBatchWriter implements Runnable {
    private final BlockingQueue<Interaction> queue = new ArrayBlockingQueue<>(50_000); // bounded = backpressure
    private final DataSource ds;
    private volatile boolean running = true;

    public MicroBatchWriter(DataSource ds) { this.ds = ds; }

    public void submit(Interaction i) throws InterruptedException { queue.put(i); }   // blocks when full

    @Override public void run() {
        List<Interaction> batch = new ArrayList<>(500);
        while (running || !queue.isEmpty()) {
            try {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(50);
                while (batch.size() < 500) {
                    long left = deadline - System.nanoTime();
                    if (left <= 0) break;
                    Interaction i = queue.poll(left, TimeUnit.NANOSECONDS);
                    if (i == null) break;
                    batch.add(i);
                    queue.drainTo(batch, 500 - batch.size());           // grab whatever is ready
                }
                if (!batch.isEmpty()) { flush(batch); batch.clear(); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); running = false; }
        }
    }

    private void flush(List<Interaction> batch) {
        String sql = """
            INSERT INTO interactions (interaction_id, client_id, channel, occurred_at, payload)
            VALUES (?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (interaction_id) DO NOTHING""";                  // idempotent on retry
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            c.setAutoCommit(false);
            for (Interaction i : batch) {
                ps.setString(1, i.id()); ps.setString(2, i.clientId()); ps.setString(3, i.channel());
                ps.setTimestamp(4, Timestamp.from(i.occurredAt())); ps.setString(5, i.payloadJson());
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("batch flush failed; batch will be retried", e);
        }
    }
    public void stop() { running = false; }
}
// JDBC URL for PostgreSQL batch rewriting:  jdbc:postgresql://db/crm?reWriteBatchedInserts=true
```

### A2. How do you detect and prevent deadlocks?
🎤 (~1.5 min) "**Detect**: take a thread dump with `jstack` or `jcmd Thread.print`. The JVM reports 'Found one Java-level deadlock' with the cycle. Programmatically, `ThreadMXBean.findDeadlockedThreads()` can feed a health check. At the database level, PostgreSQL logs deadlocks and aborts one transaction.

**Prevent**: **consistent lock ordering**. In the scheduler, bulk operations lock job rows in ascending ID order (`ORDER BY id FOR UPDATE`), so two bulk pauses can't deadlock each other. Use **tryLock with timeouts**. **Hold locks briefly**, with no I/O or remote calls while holding a lock. That was the key scheduler fix: narrowing the pessimistic lock to the state acquisition only. And prefer **lock-free designs** or `SKIP LOCKED` work queues where possible."

```sql
-- Workers claim jobs without blocking each other (no deadlock, no duplicate claim)
WITH next AS (
  SELECT id FROM job_execution
  WHERE status = 'SCHEDULED' AND fire_at <= now()
  ORDER BY fire_at
  FOR UPDATE SKIP LOCKED
  LIMIT 50)
UPDATE job_execution j SET status = 'RUNNING', owner = :podId, started_at = now()
FROM next WHERE j.id = next.id
RETURNING j.id;
```

### A3. Production: high CPU or latency spikes in a Java service. Walk me through your diagnosis ✅
🎤 (~2 min) "I separate **'working too hard'** from **'waiting too long'**.

High CPU: use `top -H` to find hot thread IDs, convert them to hex and match them in a thread dump, or better, use **async-profiler** or **JFR** for a flame graph. Typical culprits are regex backtracking, JSON serialization of large payloads, tight retry loops, and GC thrashing (check GC logs: is time going into GC?).

High latency with moderate CPU means waiting: take **several thread dumps a few seconds apart** and look for threads stuck in the same place, such as `BLOCKED` on a monitor, waiting on `HikariPool.getConnection`, or socket reads with no timeout. Correlate with metrics: connection-pool active and pending counts, executor queue depth, downstream latency. Then go to the database with **EXPLAIN (ANALYZE, BUFFERS)** for the slow queries.

That's the methodology on my resume, and it's how I found the scheduler issue: threads waiting on row locks held too long, not slow code. The fix was architectural (lock scope plus a cache), and I verified it with the same JMeter profile: P99 went from 680 to 210 milliseconds."

### A4. What is false sharing and how do you avoid it?
🎤 (~1 min) "CPU caches work in 64-byte lines. If two threads write to **different variables on the same cache line**, the line ping-pongs between cores and performance collapses, even though there's no logical sharing. The Disruptor pads its sequence counters for this reason. In Java, use `@jdk.internal.vm.annotation.Contended` (needs a JVM flag), manual padding, or **`LongAdder`**, which stripes counters across cells, instead of one hot `AtomicLong`. It only matters in very hot paths, so profile first."

### A5. Structured concurrency and scoped values: why should an architect care?
🎤 (~1 min) "Structured concurrency, finalized in recent JDKs, treats a group of subtasks as one unit. If one fails, the siblings are cancelled, and the scope never leaks threads. It's perfect for fan-out like 'get seats, fares and offers in parallel, fail fast'. `ScopedValue` replaces `ThreadLocal` for request context like tenant or trace, being immutable, cheap with virtual threads, and automatically bounded. Together they make cancellation and context propagation correct by construction, which is exactly where enterprise async code usually goes wrong."

### A6. Running the JVM in containers on OpenShift/Kubernetes: what do you tune?
🎤 (~1.5 min) "Modern JVMs are container-aware, but I still set these explicitly. **Heap as a percentage**, `-XX:MaxRAMPercentage=70–75`, leaving headroom for metaspace, thread stacks, direct buffers and the code cache. A container OOM-kill is invisible to Java's heap-dump hooks. Know that **CPU limits throttle**: the JVM sizes GC and ForkJoin threads from available CPUs, and tight CPU limits cause long pauses. I often set requests without strict CPU limits for latency-sensitive services, or set `-XX:ActiveProcessorCount`. **Fast startup**: CDS archives, Spring AOT, or a native image when cold start matters. **Graceful shutdown**: `server.shutdown=graceful` plus a preStop hook so in-flight requests finish. Readiness should reflect real dependencies; liveness should not, or you'll restart-loop during a database blip. And use **observability-friendly flags**: GC logging to stdout, JFR enabled, and a heap dump path on a mounted volume."

### A7. How do you implement backpressure in Java services?
🎤 (~1 min) "Backpressure means the consumer controls the rate, so overload becomes slowdown rather than a crash. In-process: **bounded queues** with blocking or caller-runs policies, and semaphores around downstream calls. Reactive: the `Flow`/Reactive Streams `request(n)` protocol, as in Project Reactor. Across services: **Kafka consumer pull** is natural backpressure (lag grows instead of the service dying), **HTTP 429 with Retry-After**, rate limiters, and load shedding at the gateway. In the CRM engine, a full ring buffer slowed the connectors; in the scheduler, the Kafka channel absorbed spikes when downstream services slowed."

### A8. Refactor a legacy 5,000-line God class. What's your hands-on approach?
🎤 (~1.5 min) "First, **don't rewrite it**. **One, safety net**: characterization tests around its public behaviour, often recorded from real inputs and outputs. **Two, find responsibilities**: group methods by the fields they use and the reasons they change. Usually there are four to six hidden classes. **Three, extract in small steps**: extract method, then move methods to new classes behind the same façade, so callers don't change yet. **Four, introduce seams** for I/O and time so the new classes are unit-testable. **Five, flip callers gradually** and delete dead code, with the IDE's safe refactorings and tests green on every commit. **Six, lock it in** with ArchUnit rules so it can't regrow. I timebox it and tie it to feature work that touches that code. Refactoring for its own sake rarely survives prioritisation."

### A9. Design an in-memory rate limiter / token bucket in Java (thread-safe).
🎤 (~1 min) "A token bucket holds up to N tokens and refills at R per second. Each request takes a token or is rejected. For a single JVM, an atomic or CAS-based implementation is enough. For a cluster, use Redis with a Lua script or Bucket4j, as I did for per-user rate limits in my RAG agent."

```java
public final class TokenBucket {
    private final long capacity; private final double refillPerNano;
    private double tokens; private long last;
    public TokenBucket(long capacity, double perSecond) {
        this.capacity = capacity; this.refillPerNano = perSecond / 1e9;
        this.tokens = capacity; this.last = System.nanoTime();
    }
    public synchronized boolean tryAcquire() {        // short critical section; fine for per-key buckets
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - last) * refillPerNano);
        last = now;
        if (tokens >= 1) { tokens -= 1; return true; }
        return false;
    }
}
```

### A10. Java decisions you made that could have been better?
🎤 (~1.5 min) "Three honest ones. **First, the in-memory buffer in the CRM engine.** Fast, but a crash before flush meant relying on source re-reads. A durable log would have been cleaner. **Second, the pessimistic locking in the scheduler.** We narrowed lock scope and got 680 → 210 ms, but a design built on `FOR UPDATE SKIP LOCKED` work claiming plus optimistic versioning for admin edits would have avoided most contention from the start. **Third, raw Java sockets for live train data.** They worked and scaled with caching and CDN, but we owned connection management, heartbeats and reconnection logic that managed WebSocket gateways or SSE give you for free. In each case the decision was reasonable for the constraints at the time, and I wrote ADRs. What I've changed since is adding a 'revisit when…' trigger to every ADR."
