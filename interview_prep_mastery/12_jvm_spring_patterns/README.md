# ☕ Module 12: JVM Internals, Spring @Transactional & Enterprise Design Patterns

A complete, battle-tested interview master curriculum covering **45 essential senior & staff engineering questions** across JVM mechanics, Spring transactional internals, and GoF / Microservices design patterns.

---

## 🧭 Master Quick Navigation

| Section | Guide | Key Coverage |
| :--- | :--- | :--- |
| **Part 1: JVM Internals (Q1–Q15)** | [01_JVM_INTERNALS.md](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/01_JVM_INTERNALS.md) | Heap vs Stack, Runtime Data Areas, ClassLoader Lifecycle & Delegation, GC Generational Hypothesis, G1GC vs ZGC vs Serial GC, Full GC Triggers, Memory Leak Detection, Heap Dumps (.hprof), JIT Tiered Compilation (C1/C2), Escape Analysis & Scalar Replacement, Metaspace vs PermGen, OOM Troubleshooting, High CPU Triage (`top -H`, `jstack`), Production Profilers (`async-profiler`, JFR). |
| **Part 2: Spring @Transactional (Q16–Q30)** | [02_SPRING_TRANSACTIONAL.md](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/02_SPRING_TRANSACTIONAL.md) | AOP Proxy Interception, 5 Silent Transaction Failures, Checked vs Unchecked Rollback Rules, `REQUIRED` vs `REQUIRES_NEW`, 7 Propagation Types, ACID Isolation Levels & Anomaly Matrix (Dirty, Non-repeatable, Phantom Reads), API Calls inside Transactions & HikariCP Starvation, Nested Transactions & Savepoints, `TransactionSynchronizationManager`, Self-Invocation Problem, CGLIB vs JDK Dynamic Proxies. |
| **Part 3: Java Design Patterns (Q31–Q45)** | [03_JAVA_DESIGN_PATTERNS.md](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/12_jvm_spring_patterns/03_JAVA_DESIGN_PATTERNS.md) | Factory & Abstract Factory, Builder for Immutability, 3 Thread-Safe Singletons (Bill Pugh, DCL with volatile, Enum), Strategy Pattern, Observer & Distributed Event Systems (Kafka), Adapter vs Decorator, Proxy Pattern (Virtual, Protection, Smart), Template Method vs Strategy, Dependency Injection / IoC, Spring Core Patterns, Microservices Distributed Patterns (Saga, Outbox, Circuit Breaker, API Gateway, CQRS, Sidecar). |

---

## 💻 Zero-Dependency Executable Java Companion Suites

All topics are accompanied by runnable, zero-dependency Java test suites located in [`interview_prep_mastery/_12_jvm_spring_patterns`](file:///Volumes/Workspace/bala/interview-prep/leetcode/interview_prep_mastery/_12_jvm_spring_patterns):

1. **`JvmInternalsDemos.java`**: Demonstrates ClassLoader hierarchy inspection, Escape Analysis scalar replacement, OutOfMemory diagnostic hook, and simulated memory retention.
2. **`SpringTransactionalSimulator.java`**: Simulates Spring Dynamic Proxies, `@Transactional` interception, self-invocation bypass demonstration, Propagation (`REQUIRED` vs `REQUIRES_NEW`), and Checked vs Unchecked rollback behavior.
3. **`DesignPatternsMasterDemos.java`**: Validates Bill Pugh & Double-Checked Singletons, Immutable Builder, Factory & Abstract Factory, Strategy, Observer, Adapter vs Decorator, and Template Method.

---

## ⚡ How to Compile & Run

```bash
# Compile all modules including Module 12
javac -d out interview_prep_mastery/*.java \
             interview_prep_mastery/*/*.java

# Run the Master Test Runner (runs 100% of all DSA + Module 12 suites)
java -ea -cp out interview_prep_mastery.MasterTestRunner
```
