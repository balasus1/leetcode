# Volume 1: Low-Level Java Concurrency, Memory Model & JVM Tuning (Q1 - Q10)
*Targeting Staff / Principal Full-Stack Interviews (Mercor, micro1, FAANG)*
*Candidate Context: Balasubramanian Shanmugham (16+ Years, Morgan Stanley CRM Batch Ingestion, SCJP 5.0, JCA/JCE)*

---

## Question 1: High-Throughput Ingestion (7,000 writes/sec) via Bounded Ring Buffers
**Interviewer:** *"On your resume, you note that at Morgan Stanley CRM you scaled batch writes 4x to sustain 7,000 interactions per second across 6 servers with zero production defects. Why didn't you just use standard Spring `@Async` or an `ExecutorService`? Walk me through the exact concurrency architecture, how you batched writes, and how you protected database connection pools from exhaustion."*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Using standard Spring `@Async` or an unbounded `ThreadPoolTaskExecutor` under 7,000 requests per second is a classic recipe for JVM OutOfMemory errors and database connection exhaustion. Each incoming HTTP request would spin up or block a thread, and if downstream database latency experienced a minor spike, the internal `LinkedBlockingQueue` would rapidly accumulate millions of interaction objects, triggering severe GC pauses and eventual crash.
>
> To sustain 7,000 interactions per second across 6 nodes—roughly 1,200 writes per second per machine—I decoupled the ingestion path from the database write path using a memory-bounded ring buffer pattern inspired by the LMAX Disruptor, backed by Java's `ArrayBlockingQueue` with a fixed capacity.
>
> Incoming interactions from email, SMS, and chats are accepted immediately at the HTTP layer, validated, and pushed into the local bounded queue in under 2 milliseconds. A pool of dedicated background worker threads drains this queue in bulk using `drainTo()`. Instead of executing individual `INSERT` statements, the worker flushes records using JDBC batching with an optimal batch size of 500 records per round-trip: `rewriteBatchedStatements=true` in PostgreSQL.
>
> To safeguard the database, the HikariCP connection pool was strictly capped at 20 connections per node. If the local queue reached 80% capacity, we applied backpressure by throttling non-critical channels and signaling the API gateway with HTTP 429 using a sliding window. 
>
> The key trade-off is crash durability: because records buffer briefly in memory (up to 50ms before flushing), an unexpected hardware crash could lose uncommitted interactions. We mitigated this by holding the external message broker's offset uncommitted until the JDBC batch physically committed to PostgreSQL disk."

### Working Java Code
```java
package com.bala.crm.ingestion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.sql.DataSource;

public class HighThroughputBatchIngestor {
    private final BlockingQueue<InteractionRecord> buffer;
    private final ScheduledExecutorService flusher;
    private final DataSource dataSource;
    private static final int BATCH_SIZE = 500;
    private static final int MAX_BUFFER_CAPACITY = 20_000;

    public record InteractionRecord(String interactionId, String customerId, String channel, String payload, long timestamp) {}

    public HighThroughputBatchIngestor(DataSource dataSource) {
        this.dataSource = dataSource;
        this.buffer = new ArrayBlockingQueue<>(MAX_BUFFER_CAPACITY);
        this.flusher = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "batch-writer");
            t.setDaemon(true);
            return t;
        });

        // Flush every 50ms or when batch size is reached
        for (int i = 0; i < 4; i++) {
            flusher.scheduleWithFixedDelay(this::flushBatch, 10, 50, TimeUnit.MILLISECONDS);
        }
    }

    public boolean ingest(InteractionRecord record) {
        return buffer.offer(record); // Non-blocking backpressure
    }

    private void flushBatch() {
        List<InteractionRecord> batch = new ArrayList<>(BATCH_SIZE);
        buffer.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) return;

        String sql = "INSERT INTO customer_interactions (interaction_id, customer_id, channel, payload, created_at) " +
                     "VALUES (?, ?, ?, ?, ?) ON CONFLICT (interaction_id) DO NOTHING";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (InteractionRecord rec : batch) {
                ps.setString(1, rec.interactionId());
                ps.setString(2, rec.customerId());
                ps.setString(3, rec.channel());
                ps.setString(4, rec.payload());
                ps.setLong(5, rec.timestamp());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException ex) {
            System.err.println("Batch write error: " + ex.getMessage());
        }
    }
}
```

---

## Question 2: HikariCP Connection Pool Sizing & Starvation Under Spikes
**Interviewer:** *"Under sudden spikes of 10,000 QPS, junior engineers often increase HikariCP `maximumPoolSize` from 10 to 200, which actually degrades throughput and causes CPU thrashing. How do you size a connection pool mathematically, and how do you diagnose connection leak starvation in production?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Increasing database connection pool size to hundreds of connections is a common anti-pattern. PostgreSQL and MySQL assign a separate OS backend process or thread per connection. When you allow 200 active connections on an 8-core database server, the CPU spends more time performing OS context switching and disk spindle/IO queue contention than executing actual SQL queries.
>
> We use the standard PostgreSQL/HikariCP sizing formula:
> $$\text{Connections} = (\text{Core Count} \times 2) + \text{Effective Spindle Count}$$
> For an 8-core database with SSD storage, optimal pool size is approximately $8 \times 2 + 1 = 17 \text{ to } 20$ connections per node.
>
> When connections appear starved, the problem is rarely that the pool is too small; it is almost always **Connection Holding Time**. Developers frequently keep transactions open while performing slow external HTTP calls, file I/O, or parsing large JSON payloads.
>
> In production, we configure two critical HikariCP metrics:
> 1. `leakDetectionThreshold = 2000` (2 seconds): If a thread holds a connection without closing it within 2 seconds, HikariCP logs an exception with the full allocation stack trace without terminating the connection.
> 2. `connectionTimeout = 1500` (1.5 seconds): If a thread waits longer than 1.5 seconds to acquire a connection, it fast-fails, preventing thread pileup.
>
> We monitor HikariCP via Dropwizard/Prometheus metrics: `hikaricp.connections.pending` (threads queued waiting) vs `hikaricp.connections.active`. If pending connections spike while active is at maximum, we examine thread dumps to isolate the exact method holding the unclosed connection."

### Working Java Configuration
```java
package com.bala.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class OptimizedDataSourceConfig {
    public static DataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://db-primary.internal:5432/crm_db");
        config.setUsername("app_user");
        config.setPassword("vault_secret_token");

        // Optimal pool sizing: coreCount * 2 + 1
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);

        // Fast-fail timeouts to prevent thread pile-up
        config.setConnectionTimeout(1500); // 1.5s max wait for connection
        config.setIdleTimeout(300_000);     // 5m idle cleanup
        config.setMaxLifetime(1800_000);   // 30m max lifetime to avoid DB server timeout drops

        // Active connection leak detector (Logs stacktrace if held > 2000ms)
        config.setLeakDetectionThreshold(2000);

        // Performance properties for PostgreSQL
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(config);
    }
}
```

---

## Question 3: Java Memory Model (JMM), `volatile` & AtomicReference CAS
**Interviewer:** *"Explain the Java Memory Model's happens-before relationship. Why does `volatile boolean flag = true` prevent thread reordering, and why is `volatile count++` not thread-safe? How does `AtomicReference` or `VarHandle` solve this at the hardware assembly level?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In modern multi-core architectures, CPU cores have local L1/L2 caches and write buffers. Modern JIT compilers and CPU out-of-order execution pipelines routinely reorder instructions for performance. The **Java Memory Model (JMM)** defines the formal contract for when one thread's memory write is guaranteed to be visible to another thread through **happens-before ordering**.
>
> When a variable is declared `volatile`:
> 1. The compiler emits hardware memory barriers (a `StoreStore` and `StoreLoad` fence on x86, or `dmb` on ARM).
> 2. Any write to a `volatile` variable establishes a happens-before relationship with subsequent reads of that variable. It flushes the CPU store buffer to coherent cache, ensuring that all writes performed *before* the volatile write become visible to any thread reading that volatile variable.
>
> However, `volatile count++` is **not atomic** because `count++` is three distinct bytecode operations: `getfield` (read), `iadd` (increment), and `putfield` (write). If two threads read simultaneously, both increment the same base value, resulting in lost updates.
>
> To achieve lock-free thread safety, we use `AtomicInteger` or `AtomicReference`, which compile directly to hardware Compare-And-Swap instructions, such as `LOCK CMPXCHG` on x86. In Java 9+, we use `VarHandle`, which provides fine-grained control over access modes (`getVolatile`, `compareAndSet`, `getAcquire`, `setRelease`), eliminating object allocation overhead while preserving hardware cache coherence."

### Working Java Code
```java
package com.bala.concurrency;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class LockFreeMetricsCounter {
    private volatile long counter = 0L;
    private static final VarHandle COUNTER_HANDLE;

    static {
        try {
            COUNTER_HANDLE = MethodHandles.lookup()
                .findVarHandle(LockFreeMetricsCounter.class, "counter", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void increment() {
        long current;
        do {
            current = (long) COUNTER_HANDLE.getVolatile(this);
            // Hardware LOCK CMPXCHG loop
        } while (!COUNTER_HANDLE.compareAndSet(this, current, current + 1));
    }

    public long get() {
        return (long) COUNTER_HANDLE.getVolatile(this);
    }
}
```

---

## Question 4: CPU Cache Line Bouncing, False Sharing & `@Contended`
**Interviewer:** *"In a high-concurrency multi-threaded counter where 16 worker threads write to adjacent array indices in RAM, throughput collapses even though threads never write to the same index. What is happening at the hardware L3/L1 cache level, and how do you fix it in Java?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This performance collapse is caused by **False Sharing** at the CPU cache coherence level.
>
> CPUs do not read and write individual bytes in main memory; they transfer data in fixed 64-byte blocks known as **Cache Lines**. If 16 worker threads each update adjacent 8-byte `long` variables located next to each other in an array or object, those variables reside on the exact same 64-byte cache line.
>
> Under cache coherence protocols like MESI (Modified, Exclusive, Shared, Invalid):
> When Core 1 writes to `index 0`, it must invalidate the entire 64-byte cache line across all other CPU cores. When Core 2 on another socket attempts to write to `index 1`, its L1 cache misses because Core 1 just invalidated the line. Core 2 must wait, reload the line across the CPU interconnect (Intel UPI or AMD Infinity Fabric), and invalidate Core 1's copy.
>
> The cache line bounces between cores continuously—known as **cache line bouncing** or cache thrashing—saturating the memory bus while CPU cores spend 90% of their cycles stalled.
>
> In Java, we eliminate false sharing by padding memory so that each independent variable sits on its own isolated 64-byte or 128-byte cache line. Prior to Java 8, we manually added dummy `long p1, p2, p3, p4, p5, p6, p7;` variables. In Java 8+, we use `@jdk.internal.vm.annotation.Contended` with the `-XX:-RestrictContended` JVM flag, which automatically pads 128 bytes around fields. This is the exact design used inside Java's `LongAdder` and `ConcurrentHashMap`."

### Working Java Code
```java
package com.bala.concurrency;

import java.util.concurrent.atomic.AtomicLong;

public class FalseSharingBenchmark {
    // Bad layout: counters share the same 64-byte cache line
    public static class UnpaddedCounter {
        public volatile long value = 0L;
    }

    // Good layout: padded to prevent adjacent cache line invalidation
    public static class PaddedCounter {
        public long p1, p2, p3, p4, p5, p6, p7; // 56 bytes padding
        public volatile long value = 0L;        // 8 bytes (occupies unique 64-byte line)
        public long p8, p9, p10, p11, p12, p13, p14; // trailing padding
    }

    // Modern Java: using LongAdder (internally uses cell padding)
    public static class ModernCounter {
        private final java.util.concurrent.atomic.LongAdder adder = new java.util.concurrent.atomic.LongAdder();
        public void increment() { adder.increment(); }
        public long sum() { return adder.sum(); }
    }
}
```

---

## Question 5: Diagnosing G1GC Mixed Collection Stalls vs ZGC Low-Latency Tuning
**Interviewer:** *"In a Spring Boot service handling 5,000 QPS with a 32GB heap, P99 latency spikes to 2.5 seconds every 10 minutes under G1GC. How do you analyze GC logs, identify humongous object allocations, and decide whether to tune G1GC or migrate to Generational ZGC?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When G1GC exhibits periodic multi-second P99 latency spikes, the root cause is almost always **Mixed Collection evacuation failures** or **Humongous Object allocation churn**.
>
> In G1GC, the heap is partitioned into thousands of equal-sized regions (typically 1MB to 32MB). Any object whose size exceeds 50% of a G1 region is classified as a 'Humongous Object' and allocated directly into a contiguous sequence of Old Generation regions. When a burst of large payloads arrives (e.g. 5MB email attachments or large JSON reports), these allocations fragment the Old Gen, forcing premature concurrent marking cycles and triggering a full Stop-The-World evacuation failure.
>
> We diagnose this by enabling unified JVM GC logging:
> `-Xlog:gc*,gc+phases=debug:file=/var/log/jvm/gc.log:time,uptime,pid:filecount=5,filesize=100M`
>
> We scan the log for lines like: `GC(124) Concurrent Mark (G1 Humongous Allocation)` and `to-space exhausted`.
>
> To remediate inside G1GC:
> 1. Increase region size: `-XX:G1HeapRegionSize=16m` or `32m` so that typical payloads are no longer treated as humongous.
> 2. Initiate marking earlier: `-XX:InitiatingHeapOccupancyPercent=40` (down from default 45) to give background concurrent marking enough head-start before old gen fills.
>
> However, for modern ultra-low-latency requirements, the definitive upgrade is migrating to **Generational ZGC** (available in JDK 21 via `-XX:+UseZGC -XX:+ZGenerational`). Generational ZGC performs concurrent marking, relocation, and reference processing concurrently with application execution via colored pointers and load barriers, keeping pause times sub-millisecond (< 1ms) even on a 32GB heap."

---

## Question 6: JVM Profiling: JDK Flight Recorder (JFR) & `async-profiler` Flame Graphs
**Interviewer:** *"A critical Spring Boot pod in production is consuming 100% CPU, but thread dumps show no obvious deadlocks. You cannot attach a heavy GUI profiler like JProfiler or VisualVM in production due to overhead. How do you profile this live pod with under 2% overhead using JFR and `async-profiler`?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Attaching traditional bytecode-instrumenting profilers in production is dangerous because bytecode injection creates significant CPU overhead, alters hot-spot inlining, and can trigger service degradation.
>
> Instead, I use **JDK Flight Recorder (JFR)** and **async-profiler**, both of which use non-intrusive sampling with less than 1% overhead.
>
> Step 1: Trigger an ad-hoc JFR recording directly via `jcmd`:
> `jcmd <PID> JFR.start name=cpu_profile settings=profile.jfc duration=60s filename=/tmp/profile.jfr`
> JFR operates directly inside the HotSpot JVM kernel, recording CPU execution samples, memory allocations, thread locks, and file/socket I/O events with zero application interruption.
>
> Step 2: To isolate exact CPU hotspot methods without the 'Safepoint Bias' problem inherent in standard JVM stack sampling, I run `async-profiler`:
> `./profiler.sh -e cpu -d 60 -f /tmp/flamegraph.html <PID>`
> `async-profiler` leverages Linux `perf_events` and HotSpot's `AsyncGetCallTrace` API to sample both Java code and native kernel/C++ frames (such as garbage collection or network socket polling) across JVM safepoints.
>
> By opening the resulting interactive HTML Flame Graph:
> - Wide plateaus at the top indicate methods monopolizing CPU cycles (e.g. inefficient regex parsing, redundant JSON serialization, or un-indexed collections).
> - We can switch the event mode to `-e lock` to isolate lock contention, or `-e alloc` to pinpoint the exact line allocating millions of transient objects."

---

## Question 7: Direct Memory (`sun.misc.Unsafe` / Off-Heap `ByteBuffer`) vs Heap
**Interviewer:** *"When handling streaming network data or building an off-heap cache, why would an architect choose `ByteBuffer.allocateDirect()` over standard byte arrays, and what are the trade-offs regarding GC pressure, JNI zero-copy, and memory leaks?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Standard on-heap byte arrays (`byte[]`) live inside the JVM Garbage Collection heap. When writing a heap buffer to a network socket channel (`SocketChannel.write()`), the JVM cannot pass the heap array address directly to the OS kernel `write()` system call because the garbage collector could relocate the memory buffer mid-write during a GC compaction phase.
>
> To circumvent this, the JVM is forced to perform an invisible intermediate copy: it allocates a temporary direct native buffer, copies the heap bytes into native memory, and then invokes the kernel system call.
>
> **Direct Memory (`ByteBuffer.allocateDirect()` or Netty `ByteBuf`)** allocates memory outside the JVM heap using native OS memory (via `malloc` in C).
>
> The key architectural advantages are:
> 1. **Zero-Copy I/O**: The direct buffer pointer is passed straight to OS kernel descriptors via DMA (Direct Memory Access), eliminating CPU buffer copying and slashing context switches.
> 2. **Zero GC Pressure**: Because the buffer lives off-heap, gigabytes of cached network chunks can sit in memory without adding a single byte to GC tracking overhead, completely preventing GC pause spikes.
>
> The trade-offs:
> Allocating and deallocating direct memory is significantly more expensive than on-heap allocation. Therefore, direct buffers must always be **pooled** (e.g. Netty's `PooledByteBufAllocator`). Furthermore, off-heap memory is not subject to `-Xmx` heap limits; if direct buffers are leaked, the process triggers Linux OOM-Killer crashes without throwing a standard Java `OutOfMemoryError`."

### Working Java Code
```java
package com.bala.io;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ZeroCopyDirectBuffer {
    public static void streamLargeFileDirect(Path source, Path destination) throws Exception {
        // Direct allocation bypasses JVM heap into native C memory
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(64 * 1024); // 64KB native chunk

        try (FileChannel inChannel = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel outChannel = FileChannel.open(destination, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            while (inChannel.read(directBuffer) > 0) {
                directBuffer.flip(); // Prepare buffer for draining
                outChannel.write(directBuffer); // Direct kernel DMA transfer
                directBuffer.clear(); // Reset pointers for next read
            }
        }
    }
}
```

---

## Question 8: Java 21 Virtual Threads (Project Loom) & Carrier Thread Pinning
**Interviewer:** *"Java 21 introduced Virtual Threads (Project Loom) allowing millions of concurrent tasks. Why can't you simply replace every fixed thread pool with `Executors.newVirtualThreadPerTaskExecutor()`? What is carrier thread pinning, and how do legacy `synchronized` blocks destroy Loom scalability?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Virtual Threads are lightweight, user-mode threads managed by the JVM rather than the operating system. Instead of consuming a 1MB OS stack, a virtual thread starts with a few hundred bytes on the heap. When a virtual thread executes a blocking operation (like JDBC socket read or HTTP fetch), the JVM unmounts the virtual thread from its underlying OS **Carrier Thread** and parks its continuation stack on the heap, allowing the carrier thread to execute other virtual threads.
>
> However, migrating blindly to virtual threads without auditing legacy code introduces a major failure mode: **Carrier Thread Pinning**.
>
> A virtual thread becomes pinned to its carrier thread in two scenarios:
> 1. Inside a `synchronized` block or method.
> 2. When executing a native JNI method or foreign function.
>
> When a pinned virtual thread blocks on I/O, the underlying OS carrier thread is also blocked and cannot be unmounted. If your application has 16 CPU cores, your carrier thread pool only has 16 OS threads. If 16 virtual threads enter a `synchronized` block and execute a slow database query, all 16 carrier threads become frozen. The entire JVM virtual thread scheduler stalls, leading to catastrophic starvation across the application.
>
> To migrate safely:
> We replace legacy `synchronized (lock)` with `ReentrantLock`, which supports Loom's non-pinning continuation unmounting.
> We monitor pinning in production using the JVM flag:
> `-Djdk.tracePinnedThreads=full`"

### Working Java Code: Eliminating Pinning
```java
package com.bala.concurrency;

import java.util.concurrent.locks.ReentrantLock;

public class LoomSafeService {
    // AVOID: synchronized blocks pin carrier threads during blocking I/O!
    // public synchronized void legacyBlockingCall() { ... }

    // PREFERRED: ReentrantLock yields carrier thread cleanly during blocking I/O
    private final ReentrantLock lock = new ReentrantLock();

    public void loomSafeBlockingCall() {
        lock.lock();
        try {
            // Virtual thread unmounts cleanly here during blocking network I/O
            Thread.sleep(100); // Represents remote DB/HTTP call
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }
}
```

---

## Question 9: Preventing ThreadLocal Memory Leaks in Pooled Application Servers
**Interviewer:** *"In a Spring Boot microservice running on Tomcat, you use `ThreadLocal<SecurityContext>` to hold user credentials. After running for 3 weeks, the JVM crashes with an OutOfMemoryError in Metaspace / Heap. How does `ThreadLocal` cause memory leaks in thread pools, and how do you guarantee cleanup?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "`ThreadLocal` memory leaks in thread-pooled environments (like Tomcat or Netty) are caused by the mismatch between the lifecycle of a web request and the lifecycle of a worker thread.
>
> Inside the JVM, each `Thread` object maintains a reference to a `ThreadLocalMap`. The keys in this map are `WeakReference<ThreadLocal<?>>`, but the **values are strong references**.
>
> When an HTTP request finishes, the `ThreadLocal` variable reference in application code goes out of scope. However, in an application server, the worker thread does not terminate; it returns to the thread pool to serve future requests. Because the thread remains alive, the thread's internal `ThreadLocalMap` retains a strong reference chain: `Thread -> ThreadLocalMap -> Entry -> Value (SecurityContext / ClassLoader)`.
>
> Even though the key is weakly referenced and collected by GC, the value remains pinned in memory. Under dynamic class reloading or high request volume, this leaks megabytes of context objects and pins their `ClassLoader`, eventually exhausting JVM heap or Metaspace.
>
> To guarantee complete cleanup, we follow an ironclad rule: `ThreadLocal.remove()` must always be invoked inside a `finally` block in a Spring `HandlerInterceptor` or Servlet `Filter`. In modern Java 21, we replace `ThreadLocal` with **Scoped Values (`ScopedValue`)**, which are immutable, bound to a specific execution scope, and automatically released when the scope exits."

### Working Java Code: Filter Cleanup & Scoped Values
```java
package com.bala.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class SecurityContextFilter implements Filter {
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest req = (HttpServletRequest) request;
            String tenantId = req.getHeader("X-Tenant-ID");
            TENANT_CONTEXT.set(tenantId);

            chain.doFilter(request, response);
        } finally {
            // MANDATORY: Always remove in finally block to prevent thread pool leak
            TENANT_CONTEXT.remove();
        }
    }

    public static String getCurrentTenant() {
        return TENANT_CONTEXT.get();
    }
}
```

---

## Question 10: ForkJoinPool Work-Stealing Internals & Recursive Deadlocks
**Interviewer:** *"How does Java's `ForkJoinPool` work-stealing algorithm differ from a standard `ThreadPoolExecutor`, why does running blocking network calls inside Java Parallel Streams collapse application throughput, and how do you prevent recursive task deadlocks?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "`ThreadPoolExecutor` uses a single shared queue that all worker threads compete to drain, which causes lock contention when thousands of small tasks are queued.
>
> In contrast, **`ForkJoinPool`** assigns every worker thread its own private double-ended queue (deque):
> - When a worker spawns subtasks via `fork()`, it pushes them to the **head** of its own deque (LIFO order), optimizing for CPU L1 cache locality.
> - When a worker finishes its own tasks, it looks at other workers' deques and **steals** tasks from the **tail** (FIFO order). This minimizes contention because the owner and the thief operate on opposite ends of the deque.
>
> The critical architectural pitfall is **Java Parallel Streams**. By default, all parallel streams across the entire JVM share a single common pool: `ForkJoinPool.commonPool()`, which has only as many worker threads as CPU cores.
>
> If a developer writes `list.parallelStream().map(url -> httpFetch(url))` and calls a slow remote HTTP service, all common pool threads become blocked waiting for network I/O. As a result, unrelated parallel operations across the entire JVM (including garbage collection helper tasks or UI event processing) grind to a halt.
>
> To prevent this:
> 1. Never execute blocking I/O in parallel streams.
> 2. If parallel blocking I/O is unavoidable, execute the task inside a custom, isolated `ForkJoinPool` instance or wrap the blocking call in `ForkJoinPool.managedBlock()`, which hints the runtime to spawn temporary compensation threads to preserve CPU parallelism."

### Working Java Code
```java
package com.bala.concurrency;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class IsolatedParallelStreamRunner {
    public static void runIsolatedTasks(List<String> items) {
        // Isolated pool: keeps commonPool free for system tasks
        ForkJoinPool customPool = new ForkJoinPool(8);
        try {
            customPool.submit(() -> 
                items.parallelStream().forEach(item -> {
                    // CPU-intensive computation
                    processComputation(item);
                })
            ).get();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        } finally {
            customPool.shutdown();
        }
    }

    private static void processComputation(String item) {
        // Pure CPU work
        item.hashCode();
    }
}
```
