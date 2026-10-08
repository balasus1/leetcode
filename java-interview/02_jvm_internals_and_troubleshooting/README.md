# 02. JVM Internals, Memory Management & Production Troubleshooting Mastery

Deep-dive architectural scripts, diagrams, diagnostic CLI commands, and root-cause analysis (RCA) techniques for Senior Java Engineers.

---

## 📑 Topics Index
1. [Heap vs Stack Memory](#1-heap-vs-stack-memory)
2. [JVM Memory Model (JMM) Architecture](#2-jvm-memory-model-jmm-architecture)
3. [Class Loading Lifecycle: Loading, Linking & Initialization](#3-class-loading-lifecycle-loading-linking--initialization)
4. [Garbage Collection Deep-Dive: G1 GC vs ZGC vs Serial GC](#4-garbage-collection-deep-dive-g1-gc-vs-zgc-vs-serial-gc)
5. [What Triggers a Full GC?](#5-what-triggers-a-full-gc)
6. [Identifying Memory Leaks in Production](#6-identifying-memory-leaks-in-production)
7. [Heap Dumps: Generation & Analysis (Eclipse MAT)](#7-heap-dumps-generation--analysis-eclipse-mat)
8. [JIT Compiler & Tiered Compilation (C1 / C2)](#8-jit-compiler--tiered-compilation-c1--c2)
9. [Escape Analysis & Scalar Replacement](#9-escape-analysis--scalar-replacement)
10. [Metaspace vs PermGen](#10-metaspace-vs-permgen)
11. [Troubleshooting `OutOfMemoryError` (OOM Root Causes)](#11-troubleshooting-outofmemoryerror-oom-root-causes)
12. [Troubleshooting High CPU Usage in Production (Step-by-Step)](#12-troubleshooting-high-cpu-usage-in-production-step-by-step)
13. [JVM Performance Analysis Toolset (JFR, Async-Profiler, Arthas)](#13-jvm-performance-analysis-toolset-jfr-async-profiler-arthas)

---

### 1. Heap vs Stack Memory
#### 🎙️ 60-Second Verbal Script
> "In the JVM runtime data area, **Heap and Stack** serve completely distinct memory lifecycles:
>
> - **Stack Memory**: Each thread has its own private Stack. It stores short-lived primitive local variables and object reference pointers within **Stack Frames** for active method calls. Memory allocation is strictly LIFO (Last-In-First-Out), extremely fast, and automatically reclaimed as methods return. Stack overflow triggers `StackOverflowError`.
> - **Heap Memory**: Shared across all threads in the JVM. It stores all instantiated **Objects, Arrays, and their instance variables**. Memory is dynamically allocated at runtime via `new` and is managed automatically by the **Garbage Collector**. If the heap runs out of memory, it throws `OutOfMemoryError: Java heap space`."

---

### 2. JVM Memory Model (JMM) Architecture
#### 🎙️ 60-Second Verbal Script
> "The JVM runtime memory is partitioned into 5 key regions:
> 1. **Heap**: Shared region storing all object instances, divided into Young (Eden, S0, S1) and Old generations.
> 2. **Metaspace (Off-Heap)**: Stores class metadata, bytecode, method structures, and the runtime constant pool.
> 3. **JVM Stacks**: Thread-private stacks containing call frames and local variable arrays.
> 4. **Program Counter (PC) Registers**: Thread-private register holding the memory address of the current bytecode instruction being executed.
> 5. **Native Method Stacks**: Thread-private memory for JNI (C/C++) native library executions.
>
> The **Java Memory Model (JMM)** also defines the specification for thread synchronization, memory visibility via `volatile`, and the **Happens-Before** relationship."

---

### 3. Class Loading Lifecycle: Loading, Linking & Initialization
#### 🎙️ 60-Second Verbal Script
> "The JVM Class Loading lifecycle consists of three distinct phases:
>
> 1. **Loading**: Finds the binary `.class` bytecode byte-stream from disk/network and creates a `java.lang.Class` object in Metaspace.
> 2. **Linking**: Subdivided into:
>    - **Verification**: Bytecode verifier ensures the code conforms to JVM specifications and is structurally safe (no stack corruption).
>    - **Preparation**: Allocates memory for static variables and initializes them to **default zero values** (e.g. `null`, `0`, `false`).
>    - **Resolution**: Transforms symbolic references in the constant pool into direct memory references.
> 3. **Initialization**: The JVM executes the class's static initializers (`static {}` blocks) and assigns user-defined initial values to static variables (`<clinit>` method)."

---

### 4. Garbage Collection Deep-Dive: G1 GC vs ZGC vs Serial GC
#### 🎙️ 60-Second Verbal Script
> "Modern JVMs offer specialized garbage collectors for different workloads:
>
> 1. **Serial GC (`-XX:+UseSerialGC`)**: Single-threaded Mark-Copy & Mark-Sweep-Compact. Has significant STW pauses; only used for tiny single-core scripts.
> 2. **G1 GC (Default in Java 9-21)**: Partitions the heap into equal-sized regions (~2048 regions). It predicts pause times and prioritizes collecting regions with the most garbage ('Garbage-First') within a target pause time (e.g. `-XX:MaxGCPauseMillis=200`).
> 3. **ZGC / Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational`)**: A concurrent, region-based, colored-pointer collector using load barriers. Performs almost all marking, relocation, and compaction **concurrently with application threads**, guaranteeing **sub-millisecond STW pause times** (< 1ms) even on 16TB heaps."

---

### 5. What Triggers a Full GC?
#### 🎙️ 60-Second Verbal Script
> "A **Full GC** stops all application threads (STW) to collect both Young and Old generations (and Metaspace). It is triggered by:
> 1. **Old Generation Exhaustion**: Old Gen is full because promotion of live objects from Young Gen failed (**Promotion Failure / Concurrent Mode Failure**).
> 2. **Metaspace Exhaustion**: Metaspace reaches its configured `MaxMetaspaceSize` threshold, forcing GC to unload unused classloaders.
> 3. **Explicit `System.gc()`**: Invocation by code or third-party libraries (can be disabled via `-XX:+DisableExplicitGC`).
> 4. **Humongous Object Allocation**: Large objects exceeding 50% of G1 region size failing contiguous region allocation."

---

### 6. Identifying Memory Leaks in Production
#### 🎙️ 60-Second Verbal Script
> "In Java, a memory leak occurs when unneeded objects remain reachable from active **GC Roots**, preventing garbage collection.
>
> Common production causes include:
> 1. Unbounded in-memory collections / caches (missing LRU or TTL).
> 2. Static collections holding entity references indefinitely.
> 3. Unclosed resources (database connections, streams, thread locals not cleaned in thread pools).
> 4. Lingering event listeners and observer registrations.
>
> We identify memory leaks by observing a **sawtooth GC pattern** in Prometheus/Grafana where heap usage baseline steadily creeps up after each Full GC until an OOM occurs."

---

### 7. Heap Dumps: Generation & Analysis (Eclipse MAT)
#### 🎙️ 60-Second Verbal Script
> "A **Heap Dump (HPROF file)** is a snapshot of all objects residing in the JVM heap at a specific moment in time.
>
> **Generation**:
> - Automatic on crash: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/oom.hprof`
> - On-demand in live container: `jcmd <PID> GC.heap_dump /tmp/dump.hprof`
>
> **Analysis with Eclipse MAT**:
> 1. Inspect the **Leak Suspects Report** which highlights 'Big Object' retained sizes.
> 2. Check the **Dominator Tree** to find which top-level root objects retain the largest chunk of memory.
> 3. Inspect **Incoming and Outgoing References** to trace back to the retaining GC Root."

---

### 8. JIT Compiler & Tiered Compilation (C1 / C2)
#### 🎙️ 60-Second Verbal Script
> "The **Just-In-Time (JIT) Compiler** boosts Java performance by compiling frequently executed bytecode ('hot methods') directly into native machine code at runtime.
>
> Java uses **Tiered Compilation (`-XX:+TieredCompilation`)**:
> - **Tier 0**: Interpreter parses bytecode without compilation.
> - **Tiers 1-3 (C1 Client Compiler)**: Quickly compiles bytecode into optimized native code with profiling instrumentation, optimizing startup latency.
> - **Tier 4 (C2 Server Compiler)**: Analyzes profiling data collected by C1 to perform aggressive, deep optimizations (method inlining, loop unrolling, escape analysis, dead-code elimination) for maximum peak throughput."

---

### 9. Escape Analysis & Scalar Replacement
#### 🎙️ 60-Second Verbal Script
> "**Escape Analysis** is an advanced JIT compiler optimization technique that analyzes whether an object allocated inside a method escapes the method scope or thread boundaries.
>
> If the JIT proves an object does NOT escape:
> 1. **Scalar Replacement**: The object is not allocated on the heap at all; its primitive fields (scalars) are broken down and stored directly in CPU registers or stack frames, eliminating GC allocation overhead.
> 2. **Lock Elimination**: Synchronization locks on non-escaping objects (e.g. `StringBuffer` in a local method) are stripped away completely."

---

### 10. Metaspace vs PermGen
#### 🎙️ 60-Second Verbal Script
> "| Feature | PermGen (Java 7 and earlier) | Metaspace (Java 8+) |
> |---|---|---|
> | **Location** | Part of contiguous JVM Heap | Native OS Off-Heap Memory |
> | **Default Size**| Fixed maximum size (~64MB-128MB) | Unbounded (grows to available system RAM) |
> | **Risk** | Frequent `OOM: PermGen space` | Rarely throws OOM unless class leak occurs |
> | **Tuning Flag** | `-XX:MaxPermSize=256m` | `-XX:MaxMetaspaceSize=512m` |"

---

### 11. Troubleshooting `OutOfMemoryError` (OOM Root Causes)
#### 🎙️ 60-Second Verbal Script
> "When diagnosing an `OutOfMemoryError`, first inspect the error message detail:
> 1. **`Java heap space`**: Heap is exhausted. Either heap size is undersized (`-Xmx`) or a true memory leak exists. Analyze HPROF heap dump via Eclipse MAT.
> 2. **`Metaspace`**: Too many dynamic classes generated (e.g. CGLIB, ByteBuddy, Spring reflection proxies, uncleaned dynamic classloaders).
> 3. **`GC overhead limit exceeded`**: JVM spent >98% of total CPU time doing GC and reclaimed <2% of heap.
> 4. **`Direct buffer memory`**: Off-heap native memory exhausted (e.g. Netty or DirectByteBuffers not released).
> 5. **`Unable to create new native thread`**: OS process limit (`ulimit -u`) reached or native memory exhausted."

---

### 12. Troubleshooting High CPU Usage in Production (Step-by-Step)
#### 🎙️ 60-Second Verbal Script
> "To isolate high CPU usage in a Linux/Kubernetes production environment:
>
> 1. **Find Java Process PID**: Run `top` and identify the high-CPU Java PID (e.g., `PID 1234`).
> 2. **Find Consuming Thread TID**: Run `top -H -p 1234` to list all individual threads. Note the thread ID with high `%CPU` (e.g., `TID 1289`).
> 3. **Convert TID to Hex**: Run `printf "%x\n" 1289` (e.g., outputs `0x509`).
> 4. **Capture Thread Dump & Grep Hex TID**:
>    ```bash
>    jcmd 1234 Thread.print > threaddump.txt
>    grep -A 25 -i "nid=0x509" threaddump.txt
>    ```
> 5. **Root-Cause**: Inspect the stack trace for infinite loops, intensive regex matching, hashmap concurrency thrashing, or high GC threads (`VM Thread` / `G1 Conc#0`)."

---

### 13. JVM Performance Analysis Toolset
#### 🎙️ 60-Second Verbal Script
> "In modern production environments, our primary JVM diagnostic tools are:
> 1. **JDK Flight Recorder (JFR) & JDK Mission Control (JMC)**: Low-overhead (<1%) kernel-level continuous event recorder.
> 2. **Async-Profiler**: Non-intrusive sampling profiler that avoids JVM safepoint bias to generate Flame Graphs for CPU and memory allocations.
> 3. **Arthas (Alibaba)**: Interactive live CLI tool to watch method execution arguments, return values, and trace latency in running containers without restarting.
> 4. **Prometheus + Micrometer + Grafana**: Continuous monitoring of JVM metrics (`jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`)."
