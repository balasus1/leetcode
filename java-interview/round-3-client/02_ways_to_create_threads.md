# 02. Different Ways to Create Threads in Java, with Examples

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "In Java, there are five primary ways to create and manage threads, evolving from low-level manual thread creation to modern high-throughput virtual threads:
>
> 1. **Extending `Thread` Class**: Subclassing `java.lang.Thread` and overriding `run()`. Limits extensibility due to Java's single inheritance rule.
> 2. **Implementing `Runnable` Interface**: Separates the task from the thread execution mechanism. Best for `void` fire-and-forget tasks without return values.
> 3. **Implementing `Callable<V>` with `Future`**: Designed for tasks that **return a result** and can throw checked exceptions, managed via an `ExecutorService`.
> 4. **`ExecutorService` Thread Pools**: Production standard for reusing a managed pool of platform threads (`FixedThreadPool`, `CachedThreadPool`, `ScheduledThreadPool`), avoiding thread allocation thrashing.
> 5. **Virtual Threads (`Thread.ofVirtual()` / Java 21 Project Loom)**: Lightweight user-mode threads managed directly by the JVM. Millions of virtual threads can be spawned concurrently for blocking I/O workloads with near-zero memory footprint (a few KB vs 1MB per OS thread)."

---

## 🧠 Comparison Matrix of Threading Approaches

| Approach | Return Value? | Checked Exceptions? | Resource Overhead | Modern Production Usage |
|---|---|---|---|---|
| **`extends Thread`** | No (`void`) | No | High (~1MB stack) | Anti-pattern (Avoid) |
| **`implements Runnable`**| No (`void`) | No | High (~1MB stack) | Good for simple tasks |
| **`Callable<V>` + `Future`**| **Yes (`V`)** | **Yes** | High (~1MB stack) | Standard with ThreadPools |
| **`ExecutorService`** | Yes / No | Yes | Reuses fixed threads | Standard for CPU-bound tasks |
| **Virtual Threads (Java 21)**| Yes / No | Yes | **Extremely Low (~1KB)** | **Standard for high-concurrency I/O** |

---

## 💻 Working Java Code: All 5 Threading Paradigms

```java
package clientround;

import java.util.concurrent.*;

public class ThreadCreationMastery {

    // 1. Extending Thread
    static class WorkerThread extends Thread {
        @Override
        public void run() {
            System.out.println("1. Running via extends Thread: " + Thread.currentThread());
        }
    }

    public static void main(String[] args) throws Exception {
        // Approach 1: Subclass Thread
        WorkerThread t1 = new WorkerThread();
        t1.start();

        // Approach 2: Implement Runnable (Lambda)
        Runnable runnableTask = () -> System.out.println("2. Running via Runnable lambda: " + Thread.currentThread());
        new Thread(runnableTask).start();

        // Approach 3: Callable with Future (returns result)
        Callable<String> callableTask = () -> {
            Thread.sleep(100);
            return "3. Result from Callable task!";
        };

        // Approach 4: ExecutorService (Reusing Thread Pool)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> futureResult = executor.submit(callableTask);
        System.out.println("Got Future Value: " + futureResult.get());
        executor.shutdown();

        // Approach 5: Modern Java 21 Virtual Threads (Project Loom)
        try (var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> vFuture = virtualExecutor.submit(() -> {
                System.out.println("5. Running inside lightweight Virtual Thread: " + Thread.currentThread());
                return "Virtual Thread Success";
            });
            System.out.println("Result: " + vFuture.get());
        } // Auto-closes and waits for completion
    }
}
```

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "Why should you never use `Executors.newFixedThreadPool()` directly in critical enterprise applications?"
**Answer:**
> "`Executors.newFixedThreadPool()` creates an **unbounded `LinkedBlockingQueue` (Integer.MAX_VALUE)**. Under heavy traffic spikes where workers process slower than requests arrive, tasks accumulate in the queue without backpressure, leading to severe **`OutOfMemoryError` (Heap exhaustion)**. Best practice is to configure a custom `ThreadPoolExecutor` with a bounded queue and explicit `RejectedExecutionHandler` (e.g., `CallerRunsPolicy`)."

### 2. "When should you NOT use Virtual Threads?"
**Answer:**
> "For **CPU-intensive computations** (like cryptographic hashing or heavy video encoding), Virtual Threads offer no advantage over standard platform threads. They are specifically designed for **blocking I/O operations** (database calls, REST/HTTP requests, Kafka messaging)."
