# 04. Explain How Garbage Collection Works Internally

## 🎙️ 60-Second Verbal Script (For Interviewer & AI)
> "Garbage Collection (GC) in the JVM is an automatic memory management process that identifies and reclaims heap memory occupied by unreferenced objects. It is built on two core pillars: **Reachability Analysis (GC Root Tracing)** and the **Weak Generational Hypothesis** (which states that most objects die shortly after creation).
>
> 1. **Reachability Analysis**: Starting from **GC Roots** (Thread stack local variables, static variables, JNI references), GC traverses the object reference graph. Any object unreachable from GC Roots is marked as garbage.
> 2. **Generational Heap Architecture**: The JVM heap is partitioned into:
>    - **Young Generation (Eden + Survivor Spaces S0/S1)**: New objects are allocated in Eden. Minor GC quickly collects short-lived objects using copying algorithms. Objects surviving multiple cycles (aging threshold, e.g. 15) are promoted to Tenured space.
>    - **Old / Tenured Generation**: Holds long-lived objects (e.g. Spring singletons, connection pools). Major/Full GC runs here.
> 3. **Modern GC Collectors**: Modern production JVMs (Java 17/21) use **G1GC** (Region-based collector balancing throughput and latency) or **ZGC / Shenandoah** (ultra-low-latency collectors with sub-millisecond Stop-The-World pause times regardless of heap size)."

---

## 🧠 JVM Heap Memory Layout & GC Phases

```
┌────────────────────────────────────────────────────────────────────────┐
│                              JVM Heap                                  │
├──────────────────────────────────┬─────────────────────────────────────┤
│         Young Generation         │       Old / Tenured Generation      │
│ ┌──────────────┬───────┬───────┐ │ ┌─────────────────────────────────┐ │
│ │     Eden     │  S0   │  S1   │ │ │     Long-lived objects,         │ │
│ │  (New Allocs)│ (From)│ (To)  │ │ │     Promoted Spring Beans       │ │
│ └──────────────┴───────┴───────┘ │ └─────────────────────────────────┘ │
└──────────────────────────────────┴─────────────────────────────────────┘
                                   │
                                   ▼ Non-Heap
                    ┌─────────────────────────────┐
                    │      Metaspace (Off-Heap)   │
                    │  Class Metadata & Bytecode  │
                    └─────────────────────────────┘
```

### The 3 Core GC Algorithmic Phases:
1. **Mark**: Traverse GC roots to find all live objects.
2. **Sweep**: Reclaim memory of all unmarked (dead) objects.
3. **Compact**: Move surviving objects together to eliminate memory fragmentation.

---

## 💻 Modern JVM GC Comparison

| Garbage Collector | Algorithm | Best For | Typical Pause Time | Flag |
|---|---|---|---|---|
| **Serial GC** | Mark-Copy & Mark-Sweep-Compact | Single-threaded CLI tools | High pause times | `-XX:+UseSerialGC` |
| **Parallel GC** | Multi-threaded Mark-Sweep-Compact | High-throughput batch processing | 100ms - Seconds | `-XX:+UseParallelGC` |
| **G1 GC (Default in Java 9-21+)**| Region-based partitioned heap | General enterprise web apps | 10ms - 200ms (Configurable) | `-XX:+UseG1GC` |
| **ZGC (Ultra Low Latency)**| Colored Pointers + Load Barriers | Large heaps (16GB - 16TB), Financial APIs | **< 1 millisecond** | `-XX:+UseZGC` |

---

## ⚡ Drill-Down Traps & Follow-Up Questions

### 1. "What constitutes a GC Root in Java?"
**Answer:**
> "GC Roots include:
> 1. Local variables and parameters on active Thread Call Stacks.
> 2. Static fields of loaded classes in Metaspace.
> 3. JNI (Java Native Interface) Global and Local references.
> 4. Active Thread objects themselves."

### 2. "Why does Reference Counting fail in Java, and why is Tracing preferred?"
**Answer:**
> "Reference counting cannot detect **Cyclic References** (e.g. Object A points to Object B, and Object B points to Object A, but neither is reachable from the application). GC Root Tracing starts strictly from external roots, correctly identifying disconnected cyclic graphs as garbage."
