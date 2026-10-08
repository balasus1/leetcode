# 🎯 Senior Java Backend & Distributed Systems Interview Mastery Hub

This repository contains battle-tested, high-impact answers, 60-second verbal scripts, JVM bytecode mechanics, system architecture blueprints, concurrency traps, and runnable production code snippets tailored for Senior/Lead/Architect Java Backend Developer interviews (Human & AI avatar technical rounds).

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
- Where to Check CI/CD Pipeline Logs (GitHub Actions, Jenkins, GitLab)
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
- Concurrency Decision Matrix: `synchronized` vs `ReentrantLock` vs `ConcurrentHashMap` vs Atomics
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
- `ThreadLocal` Leaks in Tomcat Thread Pools
- Designing a Read-Heavy Shared Cache (`StampedLock` Optimistic Reads)
- 🔥 **Production Incident RCA**: Users Receiving Another User's Data

---

## 🏛️ Chronological Interview Rounds
- 📁 [Round 1 Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-1/README.md)
- 📁 [Round 2 Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-2/README.md)
- 📁 [Client Round Archive](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-3-client/README.md)
