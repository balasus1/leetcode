# 01. Java Core, Stream API & Modern Java 8 / 17 / 21 Mastery

This module covers deep-dive answers, 60-second verbal scripts, JVM bytecode mechanics, and working runnable code for Core Java, Stream API, and version upgrades.

---

## 📑 Topics Index
1. [GC Improvements in Java 8 vs Java 17 vs Java 21](#1-gc-improvements-in-java-8-vs-java-17-vs-java-21)
2. [What is Stream API & How It Works Internally](#2-what-is-stream-api--how-it-works-internally)
3. [Stream Reusability & `IllegalStateException`](#3-stream-reusability--illegalstateexception)
4. [Using Non-Thread-Safe Collections with Streams](#4-using-non-thread-safe-collections-with-streams)
5. [Types of Thread Pools in Java (`Executors` vs `ThreadPoolExecutor`)](#5-types-of-thread-pools-in-java)
6. [Operator Overloading in Java](#6-operator-overloading-in-java)
7. [Method Overriding: Rules, Parameter Names, & Compiler Checks](#7-method-overriding-rules-parameter-names--compiler-checks)
8. [Try-with-Resources & `AutoCloseable` Internals](#8-try-with-resources--autocloseable-internals)
9. [Serialization through Inheritance (Parent vs Child)](#9-serialization-through-inheritance-parent-vs-child)
10. [`==` vs `.equals()` vs `hashCode()`](#10--vs-equals-vs-hashcode)
11. [💻 Live Coding Task: Find Frequency of Duplicate Numbers using Streams](#11--live-coding-task-find-frequency-of-duplicate-numbers-using-streams)

---

### 1. GC Improvements in Java 8 vs Java 17 vs Java 21
#### 🎙️ 60-Second Verbal Script
> "Garbage collection in the JVM has evolved from high-pause throughput collectors to concurrent, sub-millisecond pause collectors:
>
> - **In Java 8**: The biggest milestone was the complete removal of **Permanent Generation (PermGen)** in favor of off-heap **Metaspace**, eliminating frequent `java.lang.OutOfMemoryError: PermGen space`. Java 8 also brought initial production support for **G1 GC**, though Parallel GC remained default.
> - **In Java 17 (LTS)**: **G1 GC** became drastically more efficient with parallel full-GC evacuation and proactive memory uncommitting back to the OS. Most importantly, **ZGC (Z Garbage Collector)** reached production maturity with concurrent thread-stack processing, bringing Stop-The-World (STW) pauses below **1 millisecond** even for multi-terabyte heaps.
> - **In Java 21 (LTS)**: **Generational ZGC** (`-XX:+UseZGC -XX:+ZGenerational`) was introduced, dividing ZGC into young and old generations. This drastically reduces CPU overhead while maintaining sub-millisecond pauses, making it the premier choice for modern low-latency microservices."

---

### 2. What is Stream API & How It Works Internally
#### 🎙️ 60-Second Verbal Script
> "The **Java Stream API** (introduced in Java 8) is a declarative, functional pipeline for processing sequences of elements. Streams do not store data; they convey data from sources (collections, arrays, I/O) through computational steps.
>
> Internally, a Stream is modeled as a **doubly-linked pipeline of `ReferencePipeline` stages (Ops)** using the **Spliterator** abstraction. It has two phases:
> 1. **Intermediate Operations (`map`, `filter`, `flatMap`)**: These are **lazy**. They simply attach a new stateless or stateful `Sink` stage to the pipeline chain without iterating over elements.
> 2. **Terminal Operations (`collect`, `forEach`, `reduce`)**: Trigger the actual execution. The Stream traverses the `Sink` chain in a single pass. Each element travels completely through all intermediate sinks (fusion) before the next element is processed, drastically optimizing cache locality and preventing temporary intermediary collections."

---

### 3. Stream Reusability & `IllegalStateException`
#### 🎙️ 60-Second Verbal Script
> "A Java Stream **can only be consumed exactly once**. 
>
> Once a terminal operation is called on a Stream, the underlying pipeline is marked as 'operated/consumed' by setting an internal `linkedOrConsumed` boolean flag. 
>
> If you attempt to invoke another intermediate or terminal operation on the same Stream instance, the JVM throws an **`IllegalStateException: stream has already been operated upon or closed`**.
>
> If you need to re-run the stream logic over the same dataset, you must supply a `Supplier<Stream<T>>` or recreate a new stream from the source collection."

```java
Supplier<Stream<String>> streamSupplier = () -> List.of("A", "B", "C").stream();
streamSupplier.get().forEach(System.out::println);
long count = streamSupplier.get().count(); // Works perfectly!
```

---

### 4. Using Non-Thread-Safe Collections with Streams
#### 🎙️ 60-Second Verbal Script
> "When working with non-thread-safe collections (like `ArrayList` or `HashMap`) in standard sequential streams, it is completely thread-safe because execution happens on the caller thread.
>
> However, with **`parallelStream()`**, multiple worker threads in the common `ForkJoinPool` access the pipeline concurrently. Modifying shared non-thread-safe collections (e.g. `list.add()` inside `.forEach()`) causes race conditions, corrupted sizing, and `ConcurrentModificationException`.
>
> To safely use streams with parallelism:
> 1. **Avoid shared mutable state**: Use standard thread-safe reduction/collection collectors like `Collectors.toList()` or `Collectors.toConcurrentMap()`.
> 2. **Concurrent Collections**: Wrap backing stores in `Collections.synchronizedList()` or use `CopyOnWriteArrayList` / `ConcurrentHashMap`."

---

### 5. Types of Thread Pools in Java
#### 🎙️ 60-Second Verbal Script
> "Java provides standard thread pool implementations via `java.util.concurrent.Executors`:
>
> 1. **FixedThreadPool (`newFixedThreadPool(n)`)**: Fixed number of reusable threads backed by an unbounded `LinkedBlockingQueue`.
> 2. **CachedThreadPool (`newCachedThreadPool()`)**: Dynamic pool that creates new threads on-demand with a 60-second idle keep-alive, backed by a `SynchronousQueue`. Ideal for short, bursty tasks.
> 3. **SingleThreadExecutor (`newSingleThreadExecutor()`)**: Exactly one worker thread guaranteeing FIFO task order.
> 4. **ScheduledThreadPool (`newScheduledThreadPool(n)`)**: Supports delayed and periodic task execution using a delayed work queue.
> 5. **WorkStealingPool (`newWorkStealingPool()`)**: Backed by `ForkJoinPool` where idle threads steal work from busy queues.
> 6. **VirtualThreadPerTaskExecutor (`newVirtualThreadPerTaskExecutor()` - Java 21)**: Spawns an ephemeral user-mode Virtual Thread per task.
>
> In production, we avoid `Executors.newFixedThreadPool` because its unbounded queue can lead to OutOfMemoryErrors under load; we configure custom `ThreadPoolExecutor`s with bounded queues and rejection policies."

---

### 6. Operator Overloading in Java
#### 🎙️ 60-Second Verbal Script
> "Java **does not support user-defined operator overloading**. 
>
> The language designers explicitly omitted operator overloading to prevent code obfuscation, maintain readability, and keep the compiler simple.
>
> The only built-in operator overloading in Java is the **`+` operator for `String` concatenation**, where `String + Object` is automatically desugared by the compiler into `StringBuilder.append()` (or `StringConcatFactory` via `invokedynamic` in Java 9+)."

---

### 7. Method Overriding: Rules, Parameter Names, & Compiler Checks
#### 🎙️ 60-Second Verbal Script
> "Method Overriding is runtime polymorphism where a subclass provides its own implementation of a parent's method.
>
> **Compiler Checks for Overriding**:
> 1. **Exact Signature**: Same method name and exact same parameter types in identical order. **Parameter names DO NOT matter** — if parameter types match, it is overriding.
> 2. **Return Type**: Must be identical or a **Covariant return type** (a subtype).
> 3. **Access Modifier**: Cannot be more restrictive (e.g. `public` method cannot be overridden as `protected` or `private`).
> 4. **Checked Exceptions**: Can declare fewer or narrower checked exceptions, but **cannot declare new or broader checked exceptions**.
> 5. **Static/Final/Private**: Cannot override `static` (which is method hiding), `final`, or `private` methods."

---

### 8. Try-with-Resources & `AutoCloseable` Internals
#### 🎙️ 60-Second Verbal Script
> "**Try-with-resources** (Java 7+) is an exception-handling construct that automatically closes any resource implementing `java.lang.AutoCloseable` or `java.io.Closeable` when exiting the block.
>
> Under the hood, the Java compiler desugars the try-with-resources block into a robust `try-catch-finally` bytecode pattern. It calls `close()` in reverse order of resource declaration.
>
> Crucially, it handles **Suppressed Exceptions**: if the `try` block throws an exception AND `close()` also throws an exception, the primary exception is thrown to the caller while the closing exception is attached to it via `Throwable.addSuppressed()`, preventing exception masking."

---

### 9. Serialization through Inheritance (Parent vs Child)
#### 🎙️ 60-Second Verbal Script
> "In Java serialization:
>
> 1. **Parent implements `Serializable`, Child extends Parent**: The Child class is **automatically Serializable** by inheritance. All fields in both parent and child are serialized.
> 2. **Child implements `Serializable`, Parent DOES NOT**: The Child can be serialized, but during deserialization, the JVM requires the non-serializable Parent class to have an **accessible no-arg constructor**. The parent's fields are initialized via its no-arg constructor, NOT restored from the serialized stream."

---

### 10. `==` vs `.equals()` vs `hashCode()`
#### 🎙️ 60-Second Verbal Script
> "1. **`==` operator**: Compares **memory references** for objects (whether both variables point to the exact same memory address in the heap) and primitive values.
> 2. **`.equals()` method**: Defined in `java.lang.Object` (where default behavior is `==`), but overridden by classes like `String` or `User` to compare **logical equivalence / content equality**.
> 3. **The `equals()` and `hashCode()` Contract**: If two objects are equal according to `equals()`, they **must produce the exact same integer `hashCode()`**. If overridden incorrectly, hash-based collections (`HashMap`, `HashSet`) will fail to find existing keys."

---

### 11. 💻 Live Coding Task: Find Frequency of Duplicate Numbers using Streams

```java
package round1;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FindDuplicateFrequencies {

    /**
     * Finds duplicate numbers and their occurrence frequencies from a list of integers.
     */
    public static Map<Integer, Long> findDuplicateFrequencies(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return Collections.emptyMap();
        }

        return numbers.stream()
                .filter(Objects::nonNull)
                // Group by number and count occurrences
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ))
                // Filter only entries that appear more than once (duplicates)
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    public static void main(String[] args) {
        List<Integer> list = List.of(1, 2, 3, 2, 4, 5, 3, 3, 6, 1, null, 7, 8, 2);

        Map<Integer, Long> duplicates = findDuplicateFrequencies(list);

        System.out.println("=== Duplicate Numbers & Frequencies ===");
        duplicates.forEach((num, count) -> 
            System.out.printf("Number: %d -> Appears %d times%n", num, count)
        );
        // Output:
        // Number: 1 -> Appears 2 times
        // Number: 2 -> Appears 3 times
        // Number: 3 -> Appears 3 times
    }
}
```
