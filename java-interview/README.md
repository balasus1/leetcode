# 🎯 Senior Java Backend & Distributed Systems Interview Mastery Hub

This repository contains battle-tested, high-impact answers, 60-second verbal scripts, JVM bytecode mechanics, system architecture blueprints, concurrency traps, and runnable production code snippets tailored for Senior/Lead/Architect Java Backend & Full-Stack Developer interviews (Human & AI avatar technical rounds).

---

## 📅 Senior Java Backend 1-Day Timetable & Study Protocol

| Session / Time | Focus Domain | Modules Covered | Study Outcome & Key Goals |
|---|---|---|---|
| **🌅 Session 1**<br>`08:30 - 10:30` | **Core Java, JVM & Concurrency** | [Module 01](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/01_java_core_and_streams/README.md), [Module 02](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/02_jvm_internals_and_troubleshooting/README.md),<br>[Module 08](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/08_senior_backend_core_and_modern_java/README.md), [Module 14](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/14_senior_thread_safety_and_memory_traps/README.md) | • Master G1 vs ZGC sub-ms pauses, Escape Analysis, Metaspace vs PermGen<br>• Practice 60s pitch for Virtual Threads ($M:N$ Loom unmounting)<br>• Review JMM happens-before, `volatile` double-checked locking, and `ThreadLocal` leaks |
| **☀️ Session 2**<br>`11:00 - 13:00` | **Spring Boot, Security & Transactions** | [Module 03](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/03_spring_boot_and_transactional/README.md), [Module 09](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/09_senior_spring_boot_and_security/README.md),<br>[Module 15](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/15_spring_bean_scopes_java_returns_and_custom_file_handling/README.md) | • Drill `@Transactional` AOP proxy mechanics, self-invocation trap, and rollback rules<br>• Explain JWT + Refresh Token filter flow & RBAC `@PreAuthorize`<br>• Resolve the Prototype-in-Singleton injection trap (`@Lookup`/`ObjectProvider`) |
| **🌆 Session 3**<br>`14:30 - 16:30` | **Microservices, Kafka & PostgreSQL** | [Module 05](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/05_microservices_kafka_distributed_systems/README.md), [Module 06](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/06_databases_postgresql_and_sql/README.md),<br>[Module 10](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/10_senior_microservices_and_resilience/README.md), [Module 13](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/13_architect_concurrency_and_distributed_failures/README.md) | • Explain OpenTelemetry Distributed Tracing (`traceparent` propagation)<br>• Kafka ISR, Consumer Lag RCA, and Retry Storm mitigation (Full Jitter)<br>• PostgreSQL MVCC (`xmin`/`xmax`), `RIGHT OUTER JOIN` task & `EXPLAIN ANALYZE` |
| **🌙 Session 4**<br>`17:00 - 19:00` | **Live Coding, K8s & Verbal Drills** | [Module 11](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/11_senior_algorithms_and_coding_tasks/README.md), [Module 12](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/12_devops_k8s_docker_and_sql_deepdive/README.md),<br>[Module 16](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/16_senior_live_coding_and_algorithmic_patterns/README.md), [Rounds 1-3](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-1/README.md) | • Write **LRU Cache ($O(1)$)**, Sliding Window, and Kadane's from scratch<br>• Review K8s StatefulSets vs Deployments and zero-downtime rolling updates<br>• Speak 60-second scripts aloud for simulated AI & human interview avatars |

---

## 🧭 The 4-Pillar Structure for Every Interview Question
1. **🎙️ 60-Second Verbal Script**: Exact conversational pitch designed to be spoken smoothly in ~60 seconds to an interviewer or AI avatar.
2. **🧠 Key Technical Bullets & Memory Architecture**: Concise mental checklists, JMM happens-before rules, and comparison tables.
3. **💻 Production-Grade Working Code & Visual Diagrams**: Copy-paste runnable code and architecture flows.
4. **⚡ Drill-Down Traps & Follow-Up Mastery**: Clear answers to edge-case questions, memory leaks, and concurrency bugs.

---

## 📚 Master Knowledge Modules Index

### 🔹 [01. Java Core, Stream API & Modern Java 8 / 17 / 21](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/01_java_core_and_streams/README.md)
- GC Evolution (Java 8 vs 17 vs 21 Generational ZGC)
- Stream API Internals, Lazy Evaluation & `ReferencePipeline`
- Stream Reusability & `IllegalStateException`
- Concurrency & Parallel Streams with Non-Thread-Safe Collections
- Types of Thread Pools (`Executors` vs bounded `ThreadPoolExecutor`)
- Operator Overloading & Compiler Overriding Rules
- Try-with-Resources, `AutoCloseable` & Suppressed Exceptions
- Serialization through Inheritance & `==` vs `.equals()` Contract
- 💻 **Live Coding**: Find Frequency of Duplicate Numbers with Stream API

---

### 🔹 [02. JVM Internals, Memory Management & Production Troubleshooting](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/02_jvm_internals_and_troubleshooting/README.md)
- Heap vs Stack Memory & Java Memory Model (JMM)
- ClassLoader Lifecycle (Loading, Linking [Verification/Prep/Resolution], Initialization)
- Garbage Collection Deep-Dive: G1 GC vs ZGC vs Serial GC
- Full GC Triggers & Production Memory Leak Detection (Sawtooth patterns)
- Heap Dump Generation & Eclipse MAT Dominator Tree Analysis
- JIT Compiler (Tiered Compilation C1/C2) & Escape Analysis (Scalar Replacement)
- Metaspace vs PermGen
- Troubleshooting `OutOfMemoryError` (Heap, Metaspace, GC Overhead, Direct Buffer)
- Step-by-Step High CPU Troubleshooting (`top -H`, `jstack`, Hex TID matching)
- Production JVM Profilers (JFR/JMC, Async-Profiler, Arthas)

---

### 🔹 [03. Spring Boot Core, Cache, Async & @Transactional](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/03_spring_boot_and_transactional/README.md)
- Where and How to Use `@Async` with custom ThreadPools
- Spring Cache Architecture (`@Cacheable`, `@CachePut`, `@CacheEvict`, Redis TTLs)
- `@Autowired` vs `@Qualifier` & Multiple Bean Resolution (`@Primary`)
- Bean Definition Overriding & `BeanFactory` vs `ApplicationContext`
- Spring Boot Actuator Monitoring Endpoints
- `ResponseEntity<T>` vs Plain DTOs
- `@Transactional` Internals: Spring AOP Dynamic Proxies (CGLIB/JDK)
- The **Self-Invocation Problem** & Why `@Transactional` Fails
- Rollback Rules (`rollbackFor = Exception.class`)
- Transaction Propagation (`REQUIRED` vs `REQUIRES_NEW`, `NESTED` savepoints)
- Transaction Isolation Levels (Dirty Reads, Non-Repeatable Reads, Phantom Reads)
- ⚠️ **Critical Trap**: Why External API Calls Should NEVER be Placed Inside `@Transactional`

---

### 🔹 [04. GoF Design Patterns & Enterprise Microservices Architecture](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/04_design_patterns/README.md)
- Factory Method vs Abstract Factory Pattern
- Builder Pattern for Immutable Thread-Safe Objects
- Thread-Safe Singletons (Bill Pugh vs Double-Checked Locking with `volatile` vs Enum)
- Strategy Pattern vs Template Method Pattern
- Observer Pattern in Event-Driven Microservices
- Adapter Pattern vs Decorator Pattern vs Proxy Pattern
- Dependency Injection & Inversion of Control (IoC)
- Core Design Patterns used inside Spring Framework
- Microservices Patterns: **Transactional Outbox Pattern**, **Saga Pattern**, **Circuit Breaker**, **CQRS**

---

### 🔹 [05. Microservices Architecture, Kafka & Distributed Systems](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/05_microservices_kafka_distributed_systems/README.md)
- Synchronous (REST/gRPC) vs Asynchronous (Kafka/SQS) Communication
- 💻 Code Implementation: Async Communication with Spring Kafka
- **Distributed Tracing**: Trace ID & Span ID Propagation (OpenTelemetry, W3C `traceparent`)
- Kafka Partitions Sizing, Ordering Guarantees & Limits
- Troubleshooting Stuck Kafka Consumers & Consumer Lag in Production
- Kafka Fault Tolerance: Replication Factor, Leader/Follower, In-Sync Replicas (ISR)
- API Gateway Architecture & Core Responsibilities
- Eureka Service Discovery & Client-Side Load Balancing
- Enterprise Rule Engines (Drools / Easy Rules)

---

### 🔹 [06. PostgreSQL, Concurrency, SQL Joins & Query Execution Plans](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/06_databases_postgresql_and_sql/README.md)
- How PostgreSQL Handles Concurrency: **MVCC Internals (`xmin`/`xmax`, Vacuuming)**
- PostgreSQL Transaction Isolation Levels (Read Committed, Repeatable Read, SSI)
- Primary Key vs Unique Key & **The NULL Trap**
- Types of SQL Joins
- 💻 **Practical SQL Task**: `RIGHT OUTER JOIN` Live Interview Query, Explanation & Deep Analysis
- SQL Execution Plans: Scan Types (Seq Scan, Index Only Scan) & Join Algorithms (Hash/Merge/Nested Loop)
- Query Optimization with `EXPLAIN (ANALYZE, BUFFERS)`

---

### 🔹 [07. Cloud (AWS), Kubernetes, DevOps & Unit Testing (Mockito)](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/07_cloud_devops_k8s_and_testing/README.md)
- Amazon S3 Object Size Limits (5TB max, 5GB single PUT, Multipart Upload)
- Incident Response: What to Do When a Production Deployment Pipeline Fails Midway
- Where to Check CI/CD Pipeline Logs (GitHub Actions Workflows)
- Kubernetes Pod Log Diagnostics (`kubectl logs`, `describe`, `get events`)
- Centralized Production Logging (ELK/EFK Stack, Grafana Loki, AWS CloudWatch)
- Mockito `@Mock` vs `@Spy` (Partial Mocking with Runnable Code)

---

### 🔹 [08. Modern Java 17/21, Concurrency, Virtual Threads & Immutability](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/08_senior_backend_core_and_modern_java/README.md)
- Sealed Classes & Exhaustive Compiler Pattern Matching
- Virtual Threads (Java 21 Project Loom) & Carrier Thread Unmounting
- `Optional.map()` vs `Optional.flatMap()`
- Garbage Collection: Serial vs Parallel vs G1 vs ZGC
- `var` Local Variable Type Inference Best Practices
- `CopyOnWriteArrayList` vs `ArrayList`
- `CompletableFuture` for Non-Blocking Asynchronous Pipelines
- Pattern Matching for `switch` & Guarded Patterns (`when`)
- Text Blocks (`"""`) for JSON/XML/SQL
- Enforcing Object Immutability & Defensive Copying

---

### 🔹 [09. Spring Boot Architecture, Security (JWT/RBAC) & Cloud Integrations](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/09_senior_spring_boot_and_security/README.md)
- 4-Tier Clean Layered Architecture (Controller $\to$ Service $\to$ Repository)
- Complete Spring Security 6 / JWT Authentication & Refresh Token Flow
- Authorization with RBAC & `@PreAuthorize`
- Multi-Environment Configuration (`application.yml` Profiles)
- `@Configuration` vs `@Bean` & `proxyBeanMethods` CGLIB mechanics
- Embedded Server Management (Tomcat/Jetty auto-detection)
- `@ControllerAdvice` vs `@ExceptionHandler`
- Integrating Spring Boot with AWS Services (S3 Pre-signed URLs, RDS Aurora)

---

### 🔹 [10. Microservices Resilience, Rate Limiting & RCA Playbook](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/10_senior_microservices_and_resilience/README.md)
- Rate Limiting: Token Bucket vs Leaky Bucket vs Sliding Window Counter with Redis
- Circuit Breaker Pattern (Resilience4j CLOSED $\to$ OPEN $\to$ HALF_OPEN)
- API Gateway (North-South) vs Service Mesh (East-West Istio/Envoy)
- Handling Eventual Consistency (Saga Pattern, Transactional Outbox, Idempotent Consumers)
- Senior Engineer Playbook: Step-by-step Troubleshooting of Slow APIs in Production

---

### 🔹 [11. Senior Coding Tasks & Algorithmic Solutions](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/11_senior_algorithms_and_coding_tasks/README.md)
- 💻 **Palindrome Number without String Conversion** ($O(\log N)$ time, $O(1)$ space)
- 💻 **Sort a Stack using Another Stack** ($O(N^2)$ time, $O(N)$ space)
- 💻 **Maximum Sum Subarray (Kadane's Algorithm)** ($O(N)$ linear time)
- 💻 **Remove Duplicate Integers using Two Pointers** ($O(1)$ auxiliary in-place)
- 💻 **Count All Palindromic Substrings** (Expand Around Center, $O(N^2)$ time, $O(1)$ space)
- 💻 **Complete 3-Tier REST API**: Controller, Service, Repository with Path/Query Params, Pagination & Sorting

---

### 🔹 [12. Kubernetes, Docker, DevOps & Advanced SQL Deep-Dive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/12_devops_k8s_docker_and_sql_deepdive/README.md)
- Kubernetes StatefulSets vs Deployments
- Kubernetes Zero-Downtime Rolling Updates (`maxSurge`, `maxUnavailable`, readiness probes)
- Docker Volumes vs Bind Mounts vs tmpfs
- SQL Query: 2nd Highest Salary in a Department (`DENSE_RANK()`)
- Complex SQL Query with WHERE, GROUP BY, HAVING, ORDER BY & Database Execution Order

---

### 🔹 [13. Senior Architect: Concurrency, High Load & Distributed Failures](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/13_architect_concurrency_and_distributed_failures/README.md)
- Concurrency Decision Matrix: `synchronized` vs `ReentrantLock` vs `ConcurrentHashMap` vs Atomics/`LongAdder`
- Production JVM Incident: High CPU & Long GC Pauses RCA
- Asynchronous Orchestration: 5 Downstream Services with a 2-Second SLA
- `HashMap` vs `ConcurrentHashMap`: Internal Failure Mechanics under Concurrency
- Mitigating Retry Storms & Cascading Failures (Exponential Backoff with Full Jitter, Circuit Breakers)

---

### 🔹 [14. Thread Safety, Memory Traps & Concurrency Deep-Dive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/14_senior_thread_safety_and_memory_traps/README.md)
- Determining Thread Safety in a Java Class
- Immutable Classes with Mutable Internal Fields (Defensive Copying)
- The Synchronized Method Fallacy & Compound Operation Races
- Reference Escape: Returning Internal Mutable Collections
- Why `Collections.unmodifiableList()` is NOT Inherently Thread-Safe
- Iterating Over Synchronized Collections & `ConcurrentModificationException`
- Check-Then-Act TOCTOU Races & `computeIfAbsent()` Atomicity
- Why Double-Checked Locking REQUIRES `volatile` (Instruction Reordering)
- Safe Publication & Partially Initialized Object Escape
- `ThreadLocal` Pollution in Tomcat Thread Pools & Cross-User Data Leaks
- Designing a Read-Heavy Shared Cache (`StampedLock` Optimistic Reads)
- 🔥 **Production Incident RCA**: Users Receiving Another User's Data

---

### 🔹 [15. Spring Bean Scopes, Java Return Mechanics & Custom File Handling](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/15_spring_bean_scopes_java_returns_and_custom_file_handling/README.md)
- All 6 Spring Bean Scopes (`singleton`, `prototype`, `request`, `session`, `application`, `websocket`)
- The **Prototype-in-Singleton Injection Trap** & Resolution via `@Lookup` / `ObjectProvider`
- Java 8 Stream: Calculating Frequency of All Elements in a List
- Mechanics of `return` in Java & The `finally` Block Override Trap
- Custom File Handling: High-Throughput Memory-Efficient Streaming (`StreamingResponseBody`) vs Static Files

---

### 🔹 [16. Senior Live Problem Solving & Must-Know Algorithmic Patterns](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/16_senior_live_coding_and_algorithmic_patterns/README.md)
- 💻 **LRU Cache Implementation**: $O(1)$ Doubly Linked List + HashMap (Custom node & synchronization)
- 💻 **Sliding Window Pattern**: Longest Substring Without Repeating Characters ($O(N)$ index map)
- 💻 **Intervals Pattern**: Merge Overlapping Intervals ($O(N \log N)$ sorting & in-place merge)
- 💻 **QuickSelect / Top-K Pattern**: $K$-th Largest Element in $O(N)$ Average Time
- 💻 **Graph / Cycle Detection**: Microservice Circular Dependency Detection (Topological Sort / Kahn's Algorithm)
- 💻 **Dynamic Programming**: Coin Change (Minimum Coins in $O(\text{amount} \times N)$)

---

### 🔹 [17. 30 Scenario-Based Senior Java Backend Interview Deep-Dives](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/17_thirty_scenario_based_interview_deep_dives/README.md)
- ⚡ **Java & Concurrency**: HashMap concurrency pitfalls, duplicate payment race conditions, thread pool spikes, 30s downstream REST call optimization, record consistency.
- 🍃 **Spring Boot & JPA**: Slow startup diagnosis (`/actuator/startup`), circular dependencies, `LazyInitializationException` & OSIV, `@Transactional` rollback anomalies, `OutOfMemoryError` troubleshooting.
- 🌐 **Microservices**: Cascading failure mitigation (Resilience4j), 2-phase idempotency tables, slow service isolation (Bulkheads), W3C distributed tracing, REST vs gRPC vs Kafka.
- 📬 **Apache Kafka**: Consumer message deduplication, partition traffic skew/salting, Poison Pill handling & DLQ architecture, customer message ordering guarantees.
- 🗄️ **Database & SQL**: 50ms-to-10s query regression RCA, 100% DB CPU incident response, concurrent row update isolation, 100M+ row table optimization (Partitioning/Keyset), Optimistic vs Pessimistic locking for inventory flash sales.
- ☁️ **System Design & AWS**: 100k req/min scaling blueprint, distributed sliding window rate limiting, Redis crash fail-open behavior, zero-credential EC2-to-S3 IAM Instance Profiles, Canary deployment rollback & APM root-cause analysis.

---

### 🔹 [18. Senior Core Java, Spring Boot, Kafka, Code Quality & WebClient Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/18_senior_core_java_spring_kafka_and_code_quality_interview_guide/README.md)
- ☕ **Core Java (8 vs 17)**: Records, Sealed Classes, Pattern Matching, Stream API internals, Default/Static methods, sorting Person by age, `map()` vs `flatMap()`.
- 🍃 **Spring Boot & Microservices**: Auto-Configuration internals (`@ConditionalOnClass`), `@SpringBootApplication`, `@ComponentScan`, Circuit Breaker state machines, and dynamic Service Discovery registration/routing flows.
- 📬 **Apache Kafka**: Spring Kafka architecture, Consumer Groups, partition assignment strategies (`CooperativeStickyAssignor`), and multi-group broadcast mechanics.
- 🛡️ **Code Quality & Modern JVM**: JaCoCo Maven bytecode instrumentation, SonarQube Quality Gates, and Java 8 to 17 performance (ZGC/Compact Strings) & security (JEP 403 encapsulation) rationale.
- 🌐 **API Communication**: WebSocket full-duplex TCP framing, Spring WebFlux `WebClient` reactive Netty event loops, and `WebClient` vs `RestTemplate` concurrency benchmarks.

---

### 🔹 [19. 20 Modern Production Scenarios & Backend Interview Deep-Dives (2026 Edition)](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/19_twenty_2026_modern_production_scenarios_and_interview_deep_dives/README.md)
- ☕ **Core Java & JVM**: Floating-point drift (IEEE 754 & `BigDecimal.compareTo()`), `ForkJoinPool.commonPool()` HTTP exhaustion, Native Off-Heap OOM-Kills, UTC vs IST timezone drift.
- 🍃 **Spring Boot Internals**: Shared mutable lists in Singleton beans, Prototype-in-Singleton `@Lookup` trap, `@Async` unbounded queue memory spikes, Jackson recursion, Kubernetes 502 deployment race conditions.
- 🗄️ **Database Mechanics**: Deep `OFFSET` scan degradation vs Keyset pagination, `READ COMMITTED` non-repeatable reads, massive delete replication lag & partitioning.
- 🌐 **Distributed Systems**: Kafka Poison Pill DLQ recovery, `max.poll.interval.ms` rebalance eviction, compound failure mathematics ($0.999^{50}$), Transactional Outbox pattern, Redis `KEYS *` event loop freeze, ShedLock distributed scheduling, and Expand-Contract API evolution.

---

## 🏛️ Chronological Interview Rounds
- 📁 [Round 1 Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-1/README.md)
- 📁 [Round 2 Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-2/README.md)
- 📁 [Client Round Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-3-client/README.md)

---

## 🌐 Cross-Domain Ecosystem Tables (In this Repository)

### 🟢 1. Node.js Senior Backend & Microservices Mastery Table ([`../nodejs/`](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/README.md))

| Module # | Document Link | Core Topics & Architectural Highlights | 1-Day Timetable Block |
|---|---|---|---|
| **01** | [Core Architecture & Internals](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/01_core_architecture_and_internals.md) | V8 engine, Libuv architecture, Call stack, Node runtime vs Browser | `09:00 - 09:45` (Morning) |
| **02** | [Event Loop & Async Programming](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/02_event_loop_and_async_programming.md) | 6 Event Loop phases (Timers, I/O, Poll, Check, Close), `process.nextTick` vs `setImmediate`, Microtask queue | `09:45 - 10:30` (Morning) |
| **03** | [Streams, Buffers & I/O](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/03_streams_buffers_and_io.md) | Readable, Writable, Duplex, Transform streams, Backpressure handling, Buffer memory allocation | `10:45 - 11:30` (Midday) |
| **04** | [Concurrency, Workers & Clustering](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/04_concurrency_workers_and_clustering.md) | Cluster module (IPC, Master-Worker), `worker_threads`, `SharedArrayBuffer`, Child processes | `11:30 - 12:15` (Midday) |
| **05** | [Memory Management & V8 GC](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/05_memory_management_and_gc.md) | V8 Generational GC (Scavenge, Mark-Sweep-Compact), Heap snapshot memory leak analysis via Chrome DevTools | `13:30 - 14:15` (Afternoon) |
| **06** | [Networking, Web & Realtime](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/06_networking_web_and_realtime.md) | HTTP/2, HTTP/3, WebSockets (`ws`), Server-Sent Events (SSE), TCP socket programming | `14:15 - 15:00` (Afternoon) |
| **07** | [Databases, Caching & Messaging](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/07_databases_caching_and_messaging.md) | Connection pooling (pg/mysql2), Redis caching strategies, Kafka / RabbitMQ consumer backpressure | `15:15 - 16:00` (Afternoon) |
| **08** | [Security, Auth & Cryptography](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/08_security_auth_and_cryptography.md) | JWT authentication, crypto module, Rate limiting, Helmet, Prototype pollution prevention | `16:00 - 16:45` (Late Afternoon) |
| **09** | [Production Scale & Modern Features](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/09_production_scale_observability_and_modern_features.md) | OpenTelemetry distributed tracing, PM2 process management, Graceful shutdown, AsyncLocalStorage | `17:00 - 17:45` (Evening) |
| **10** | [Advanced Caching & Recency](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/10_caching_strategies_and_recency_mechanisms.md) | Multi-tier L1/L2 caches, Cache stampede (Singleflight / Mutex), Eviction policies (LRU/LFU/W-TinyLFU) | `17:45 - 18:30` (Evening) |
| **Master** | [All 200 Node.js Master Q&A](file:///Volumes/Workspace/bala/interview-prep/leetcode/nodejs/ALL_200_NODEJS_QUESTIONS_MASTER.md) | Comprehensive 200-question interview bank with complete answers | *Quick Revision Reference* |

---

### ⚛️ 2. React.js 19 Senior Frontend Mastery Table ([`../reactjs/`](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/README.md))

| Module # | Document Link | Core Topics & Architectural Highlights | 1-Day Timetable Block |
|---|---|---|---|
| **01** | [React 19 Core & Compiler](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/01_react19_core_compiler_and_new_features.md) | React 19 Compiler (Forget), `use()`, Server Actions, `useActionState()`, `useOptimistic()`, `<form>` actions | `09:00 - 09:45` (Morning) |
| **02** | [Fiber Architecture & Concurrency](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/02_fiber_architecture_reconciliation_and_concurrent_mode.md) | Fiber node tree, Double buffering, Render vs Commit phases, Time-slicing, `useTransition()`, `useDeferredValue()` | `09:45 - 10:30` (Morning) |
| **03** | [Hooks Internals & Custom Hooks](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/03_hooks_internals_and_advanced_custom_hooks.md) | Hook dispatcher linked-list, `useEffect` vs `useLayoutEffect` vs `useInsertionEffect`, closures & stale state | `10:45 - 11:30` (Midday) |
| **04** | [State Management & Server State](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/04_state_management_server_state_and_optimistic_updates.md) | TanStack Query v5 cache lifecycle, Zustand vs Redux Toolkit, optimistic UI rollbacks | `11:30 - 12:15` (Midday) |
| **05** | [Rendering: SSR, RSC & Streaming](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/05_rendering_patterns_ssr_rsc_streaming_and_ppr.md) | React Server Components (RSC) vs Client Components, Selective hydration, Suspense streaming, Partial Prerendering (PPR) | `13:30 - 14:15` (Afternoon) |
| **06** | [Performance & Memory Profiling](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/06_performance_optimization_memory_and_profiling.md) | React DevTools Profiler (Flamegraph, Ranked charts), Chrome Performance panel, Garbage collection & DOM leaks | `14:15 - 15:00` (Afternoon) |
| **07** | [Code Splitting & Microfrontends](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/07_code_splitting_bundling_and_microfrontends.md) | Dynamic `import()`, `React.lazy()`, Webpack Module Federation, Island architecture | `15:15 - 16:00` (Afternoon) |
| **08** | [Design Patterns & Security](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/08_design_patterns_architecture_and_security.md) | Compound Components, Render Props, HOCs, XSS mitigation (`dangerouslySetInnerHTML`), CSRF protection | `16:00 - 16:45` (Late Afternoon) |
| **09** | [Production Scaling & Testing](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/09_production_scaling_testing_and_enterprise_saas.md) | Vitest, React Testing Library (RTL), Playwright E2E, Error Boundaries with Sentry error monitoring | `17:00 - 17:45` (Evening) |
| **10** | [Rerender & Media Optimization](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/10_rerender_elimination_and_media_optimization.md) | Rerender elimination patterns (State colocation, Children as props, `React.memo`), Next/Image & virtual scrolling | `17:45 - 18:30` (Evening) |
| **11** | [API Fetching & Pagination](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/11_api_fetching_refetch_elimination_and_pagination.md) | Infinite scrolling, Cursor vs Offset pagination, Stale-While-Revalidate (SWR), Deduplication | `18:30 - 19:15` (Evening) |
| **Master** | [All 200 React.js Master Q&A](file:///Volumes/Workspace/bala/interview-prep/leetcode/reactjs/ALL_200_REACTJS_QUESTIONS_MASTER.md) | Comprehensive 200-question interview bank with complete answers | *Quick Revision Reference* |

---

### 🏗️ 3. System Design & Distributed Systems Mastery Table ([`../system-design/`](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/README.md))

| Module # | Document Link | Core Topics & Architectural Highlights | 1-Day Timetable Block |
|---|---|---|---|
| **00** | [Capacity Planning & Cost Estimation](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/00_capacity_planning_and_cost_estimation_framework.md) | QPS/RPS calculations, Peak vs Average multiplier ($3\times$), Storage/Bandwidth/RAM 80/20 rule, Cloud infrastructure cost estimation | `08:30 - 09:15` (Morning) |
| **01** | [URL Shortener System Design (TinyURL)](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/01_url_shortener_service.md) | Base62 encoding, Key Generation Service (KGS), Distributed Snowflake ID generation, 301 vs 302 redirects | `09:15 - 10:15` (Morning) |
| **02** | [Scalability & Load Balancing](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/02_scalability_load_balancing_and_consistent_hashing.md) | Layer 4 vs Layer 7 load balancers, Consistent Hashing ring with virtual nodes (vnodes), Hotspot mitigation | `10:30 - 11:30` (Midday) |
| **03** | [Reliability & Resilience Patterns](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/03_reliability_fault_tolerance_and_resilience_patterns.md) | Circuit Breaker, Exponential Backoff + Jitter, Bulkhead pattern, Rate limiting (Token Bucket/Leaky Bucket) | `11:30 - 12:30` (Midday) |
| **04** | [Databases, Engines & Partitioning](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/04_databases_storage_engines_and_partitioning.md) | B-Tree (RDBMS) vs LSM-Tree (Cassandra/RocksDB), Horizontal Sharding, Range vs Hash vs Directory partitioning | `13:30 - 14:30` (Afternoon) |
| **05** | [Distributed Consensus & Transactions](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/05_distributed_systems_cap_pacelc_consensus_and_transactions.md) | CAP theorem, PACELC theorem, Raft & Paxos consensus, 2-Phase Commit (2PC) vs Saga Pattern (Orchestration/Choreography) | `14:30 - 15:30` (Afternoon) |
| **06** | [Caching Architectures & Eviction](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/06_caching_architectures_eviction_and_recency.md) | Cache-Aside, Read-Through, Write-Through, Write-Behind, Cache Stampede / Thundering Herd mitigation, W-TinyLFU / Caffeine | `15:45 - 16:30` (Afternoon) |
| **07** | [Messaging & Event Streaming](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/07_messaging_streaming_and_event_driven_architectures.md) | Apache Kafka vs RabbitMQ vs AWS SQS/SNS, Partitioning, Consumer group rebalances, Transactional Outbox Pattern | `16:30 - 17:15` (Late Afternoon) |
| **08** | [Cloud Native & Kubernetes Infrastructure](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/08_cloud_native_kubernetes_and_storage_infrastructure.md) | Kubernetes Pods, Ingress Controllers, HPA autoscaling, Service mesh (Istio/Envoy), Object storage (S3) vs Block (EBS) | `17:15 - 18:00` (Evening) |
| **09** | [Graph Systems & Search Engines](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/09_graph_systems_algorithms_and_search_engines.md) | Elasticsearch inverted index, BM25 text relevance ranking, Neo4j graph traversal for social feeds | `18:00 - 18:45` (Evening) |
| **10** | [FAANG System Design Case Studies](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/10_top_faang_system_design_case_studies.md) | WhatsApp/Messenger real-time chat, Netflix video streaming & CDN, Uber proximity location service (Geohash/H3) | `19:00 - 20:00` (Night) |
| **11** | [Infra Sizing Playbook & Formulas](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/11_system_design_estimations_and_infra_sizing_playbook.md) | Step-by-step whiteboard capacity estimation math cheatsheet and quick reference guide | *Quick Calculation Reference* |
| **Master** | [All System Design Master Guide](file:///Volumes/Workspace/bala/interview-prep/leetcode/system-design/ALL_SYSTEM_DESIGN_MASTER.md) | Complete encyclopedia of all system design questions and blueprints | *Master Reference Guide* |
