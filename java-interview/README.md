# 🎯 Java & Spring Boot Full-Stack Senior Interview Mastery Hub

This repository contains battle-tested, high-impact answers, 60-second verbal scripts, JVM bytecode mechanics, and runnable production code snippets tailored for Senior/Lead Java Backend Developer interviews (Human & AI avatar technical rounds).

---

## 🧭 The 4-Pillar Structure for Every Interview Question
1. **🎙️ 60-Second Verbal Script**: Exact conversational pitch designed to be spoken smoothly in ~60 seconds to an interviewer or AI avatar.
2. **🧠 Key Technical Bullets & Memory Architecture**: Concise mental checklists and comparison tables.
3. **💻 Production-Grade Working Code & Visual Diagrams**: Copy-paste runnable code and architecture flows.
4. **⚡ Drill-Down Traps & Follow-Up Mastery**: Clear answers to edge-case questions, memory leaks, and concurrency bugs.

---

## 📚 Master Knowledge Modules Index

### 1. 🔹 [Java Core, Stream API & Modern Java 8 / 17 / 21](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/01_java_core_and_streams/README.md)
- GC Evolution (Java 8 vs 17 vs 21 Generational ZGC)
- Stream API Internals, Lazy Evaluation & `ReferencePipeline`
- Stream Reusability & `IllegalStateException`
- Concurrency & Parallel Streams with Non-Thread-Safe Collections
- Types of Thread Pools (`Executors` vs bounded `ThreadPoolExecutor`)
- Operator Overloading & Compiler Overriding Rules (Parameters vs Names)
- Try-with-Resources, `AutoCloseable` & Suppressed Exceptions
- Serialization through Inheritance & `==` vs `.equals()` Contract
- 💻 **Live Coding**: Find Frequency of Duplicate Numbers with Stream API

---

### 2. 🔹 [JVM Internals, Memory Management & Production Troubleshooting](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/02_jvm_internals_and_troubleshooting/README.md)
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

### 3. 🔹 [Spring Boot Core, Cache, Async & @Transactional](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/03_spring_boot_and_transactional/README.md)
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

### 4. 🔹 [GoF Design Patterns & Enterprise Microservices Architecture](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/04_design_patterns/README.md)
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

### 5. 🔹 [Microservices Architecture, Kafka & Distributed Systems](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/05_microservices_kafka_distributed_systems/README.md)
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

### 6. 🔹 [PostgreSQL, Concurrency, SQL Joins & Query Execution Plans](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/06_databases_postgresql_and_sql/README.md)
- How PostgreSQL Handles Concurrency: **MVCC Internals (`xmin`/`xmax`, Vacuuming)**
- PostgreSQL Transaction Isolation Levels (Read Committed, Repeatable Read, SSI)
- Primary Key vs Unique Key & **The NULL Trap**
- Types of SQL Joins
- 💻 **Practical SQL Task**: `RIGHT OUTER JOIN` Live Interview Query, Explanation & Deep Analysis
- SQL Execution Plans: Scan Types (Seq Scan, Index Only Scan) & Join Algorithms (Hash/Merge/Nested Loop)
- Query Optimization with `EXPLAIN (ANALYZE, BUFFERS)`

---

### 7. 🔹 [Cloud (AWS), Kubernetes, DevOps & Unit Testing (Mockito)](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/07_cloud_devops_k8s_and_testing/README.md)
- Amazon S3 Object Size Limits (5TB max, 5GB single PUT, Multipart Upload)
- Incident Response: What to Do When a Production Deployment Pipeline Fails Midway
- Where to Check CI/CD Pipeline Logs (GitHub Actions, Jenkins, GitLab)
- Kubernetes Pod Log Diagnostics (`kubectl logs`, `describe`, `get events`)
- Centralized Production Logging (ELK/EFK Stack, Grafana Loki, AWS CloudWatch)
- Mockito `@Mock` vs `@Spy` (Partial Mocking with Runnable Code)

---

## 🏛️ Chronological Interview Rounds

| Round | Directory | Highlights |
|---|---|---|
| **Round 1** | [round-1/](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-1/README.md) | PriorityQueue, SOLID, Strategy Pattern, `@SpringBootApplication`, PUT vs PATCH, REST, Stream Max Salary, CI/CD, AWS, Kubernetes, Kafka Schema Registry, Records/Sealed |
| **Round 2** | [round-2/](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-2/README.md) | Static Method Hiding, Spring Boot Startup Lifecycle, `final`/`finally`/`finalize`, GC Internals, Minimum Meeting Rooms Greedy |
| **Client Round** | [round-3-client/](file:///Volumes/Workspace/bala/interview-prep/leetcode/java-interview/round-3-client/README.md) | Class Loaders, Thread Creation Paradigms, Polymorphism, `JpaRepository` vs `CrudRepository`, `@RestController` vs `@Controller`, `@ControllerAdvice` vs `@RestControllerAdvice` |
