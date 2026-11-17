# Master 10-Day Full-Stack to AI Solution Architect Study Curriculum

**Candidate:** Balasubramanian Shanmugham (Bala)  
**Target Roles:** Senior Full-Stack Engineer, Staff Technical Architect, Enterprise AI Solution Architect  
**Scope:** 100% self-contained roadmap using strictly the local repository files. No external tutorials or video courses needed.  
**Daily Rhythm:** 4 Core Focus Blocks (Morning, Midday, Afternoon, Evening) + 1 Nightly Simulation Sprint (10–12 hours/day).

---

## High-Level 10-Day Progression Map

```text
[PHASE 1: Full-Stack & Core Mastery]
  Day 1: Java Concurrency, JVM Internals, GC & Algorithmic Problem Solving (DSA)
  Day 2: Modern React 18/19, Next.js, Fiber, State Management & Streaming Frontends
  Day 3: Enterprise Spring Boot 3.x, Spring Security (OAuth2/OIDC/PKCE), AOP & REST Design
────────────────────────────────────────────────────────────────────────────────────────────
[PHASE 2: Distributed Systems & Data Architecture]
  Day 4: High-Throughput Event-Driven Systems, Apache Kafka Deep Dive & Sagas
  Day 5: Distributed System Design Case Studies, Polyglot Persistence & Caching
────────────────────────────────────────────────────────────────────────────────────────────
[PHASE 3: Cloud-Native, Reliability & Governance]
  Day 6: Kubernetes / OpenShift, Envoy Sidecars, Temporal Durable Workflows & Schedulers
  Day 7: Architecture Governance: ADRs, SRS, SBOM (CycloneDX), Zero-Trust & Capacity Sizing
────────────────────────────────────────────────────────────────────────────────────────────
[PHASE 4: Generative AI, RAG & Agentic Solution Architecture]
  Day 8: LLM Inference Mechanics, Transformer Attention, KV Cache, PagedAttention & SSE
  Day 9: Production RAG, Vector Math (Cosine/Dot), Vector DBs (HNSW), Hybrid RRF & Re-Ranking
  Day 10: Agentic Platforms (LangGraph, MCP, ZeroMem, HITL), Guardrails, FinOps & Leadership
```

---

## 10-Day Master Schedule Table

| Day | Primary Focus Area | Target Milestone | Primary Local Study Links |
| :---: | :--- | :--- | :--- |
| **Day 1** | **Java Internals, Concurrency & DSA** | Master JVM memory, thread contention, and 8 core DSA implementations. | [twenty_four_master_dsa_backend_system_design.md](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#part-i-data-structures--algorithms-dsa)<br>[volume_01_java_jvm_concurrency.md](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_01_java_jvm_concurrency.md) |
| **Day 2** | **React 18/19, Next.js & Frontend Streaming** | Master Fiber reconciliation, Zustand atomic state, and SSE streaming token UX. | [tringapps_ai_architect_50_master_prep.md (Q2, Q46)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q2-how-do-you-design-the-presentation-layer-in-reactjs-and-typescript-for-high-performance-ai-applications)<br>[ALL_200_REACTJS_QUESTIONS_MASTER.md](file:///Users/balashan/Workspace/dev/github/leetcode/reactjs/ALL_200_REACTJS_QUESTIONS_MASTER.md) |
| **Day 3** | **Spring Boot 3.x, OAuth2, AOP & API Contracts** | Master OAuth2 PKCE, Spring AOP CGLIB proxies, `@Transactional` traps & OpenAPI. | [twenty_four_master_dsa_backend_system_design.md (Q9–Q16)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#part-ii-java-backend--internals)<br>[tringapps_ai_architect_50_master_prep.md (Q1, Q3, Q5)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-1-enterprise-layered-architecture-identity--security-q1q6) |
| **Day 4** | **Kafka Event-Driven Sagas & Concurrency** | Master distributed Sagas, compensating transactions, idempotent ledgers & EOS. | [epam_technical_architect_50_master_qa.md (Q33–Q40)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-5-distributed-systems-scalability--event-driven-architecture)<br>[volume_02_distributed_ticketing_sagas.md](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_02_distributed_ticketing_sagas.md) |
| **Day 5** | **Distributed System Design & Polyglot DBs** | Master Payments, Chat, Rate Limiting, Video Streaming, and URL Shortener. | [twenty_four_master_dsa_backend_system_design.md (Q17–Q24)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#part-iii-system-design-architecture)<br>[ALL_SYSTEM_DESIGN_MASTER.md](file:///Users/balashan/Workspace/dev/github/leetcode/system-design/ALL_SYSTEM_DESIGN_MASTER.md) |
| **Day 6** | **Cloud-Native, OpenShift, Temporal & Schedulers** | Master Kubernetes pod topologies, Envoy sidecars, Temporal durable workflows & Quartz. | [tringapps_ai_architect_50_master_prep.md (Q6, Q9)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-2-observability-traceability-temporal--architecture-docs-q7q12)<br>[volume_03_job_scheduling_concurrency_quartz.md](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_03_job_scheduling_concurrency_quartz.md) |
| **Day 7** | **Architecture Governance, Security & Sizing** | Master ADRs, SRS (IEEE 830), CycloneDX SBOM, GDPR crypto-shredding & 1M DAU math. | [tringapps_ai_architect_50_master_prep.md (Q10–Q17, Q47)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-3-capacity-planning-volumetrics--back-of-the-envelope-estimations-q13q17)<br>[epam_technical_architect_50_master_qa.md (Q41–Q50)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-6-cloud-native-reliability-security--epam-delivery) |
| **Day 8** | **LLM Internals, KV Cache & PagedAttention** | Master Pre-fill vs Decode, KV Cache VRAM explosion, vLLM PagedAttention & GQA. | [tringapps_ai_architect_50_master_prep.md (Q18–Q23)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-4-generative-ai-llm-internals-kv-cache--pagedattention-q18q23)<br>[ALL_100_PYTHON_AI_FULLSTACK_QUESTIONS_MASTER.md](file:///Users/balashan/Workspace/dev/github/leetcode/python/ALL_100_PYTHON_AI_FULLSTACK_QUESTIONS_MASTER.md) |
| **Day 9** | **Advanced RAG, Vector DBs & Hybrid Search** | Master Layout chunking, Cosine/Dot/L2 math, Weaviate vs pgvector, HNSW, RRF & Re-Ranking. | [tringapps_ai_architect_50_master_prep.md (Q24–Q31)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-5-advanced-rag-vector-databases--semantic-search-q24q31)<br>[epam_technical_architect_50_master_qa.md (Q9–Q20)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-2-generative-ai-rag--vector-search-architectures) |
| **Day 10** | **Agentic Platforms, FinOps & Leadership** | Master LangGraph state graphs, MCP, ZeroMem, NeMo Guardrails, Ragas evals & Vision. | [tringapps_ai_architect_50_master_prep.md (Q32–Q44, Q49–Q50)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-6-agentic-systems-frameworks--tool-calling-q32q38)<br>[epam_technical_architect_50_master_qa.md (Q1–Q8, Q21–Q30)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-1-engineering-leadership-vision--operating-model) |

---

## Detailed Daily Breakdown (Morning to Night Routine)

### DAY 1: Java Concurrency, JVM Internals & Live Coding Algorithms
* **07:30 – 10:00 [Morning Deep Dive: Memory & Concurrency]**
  * *Concepts:* JVM memory model (Heap, Thread Stack frames, Metaspace), Object header mark word, Weak Generational Hypothesis, G1GC vs ZGC concurrent marking.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q9 & Q10)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#9-hashmap-vs-concurrenthashmap-vs-hashtable)
    - [`volume_01_java_jvm_concurrency.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_01_java_jvm_concurrency.md)
  * *Verbal Anchor:* Be ready to explain how `ConcurrentHashMap` uses CAS for empty bucket heads and `synchronized(head)` with TreeBins.
* **10:30 – 13:00 [Midday Coding Sprint: Core DSA 1–4]**
  * *Coding Tasks:* Implement and rehearse from memory:
    1. $O(1)$ LRU Cache (HashMap + Doubly Linked List with dummy head/tail).
    2. Continuous Median Finder (Two Heaps: Max-Heap + Min-Heap).
    3. Word Ladder (Bidirectional BFS graph traversal with char mutations).
    4. Merge K Sorted Lists (Min-Heap PriorityQueue of size $K$).
  * *Files to Code:*
    - [`twenty_four_master_dsa_backend_system_design.md` (DSA 1–4)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#part-i-data-structures--algorithms-dsa)
* **14:30 – 17:30 [Afternoon Coding Sprint: Core DSA 5–8]**
  * *Coding Tasks:*
    5. Detect Cycle in Directed Graph (DFS with 3-state coloring: White/Gray/Black).
    6. Maximum Subarray Sum (Kadane's algorithm with index tracking).
    7. Kth Largest in Stream (Fixed-size Min-Heap gatekeeper).
    8. Task Scheduler with Cooling Interval (Greedy frequency formula).
  * *Files to Code:*
    - [`twenty_four_master_dsa_backend_system_design.md` (DSA 5–8)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#5-detect-cycle-in-a-directed-graph)
* **18:30 – 20:30 [Evening Architecture Anchor: Morgan Stanley Ingestion Engine]**
  * *Project Story:* How you sustained 7,000 interactions/sec across 6 servers using lock-free ring buffers (Disruptor pattern), adaptive JDBC micro-batching, and table partitioning.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q38)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q38-how-did-you-optimize-java-concurrency-and-database-partitioning-to-achieve-7000-interactionssec-at-morgan-stanley-crm)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "Explain Java Deadlock detection using `ThreadMXBean` and how ordered locking prevents it."

---

### DAY 2: Modern React 18/19, Next.js, Fiber & Streaming Frontends
* **07:30 – 10:00 [Morning Deep Dive: React Internals & Compiler]**
  * *Concepts:* React Fiber architecture (workInProgress vs current tree, double buffering, time-slicing), React 19 compiler memoization, Server Components (RSC) vs Client Components.
  * *Files to Read:*
    - [`01_react19_core_compiler_and_new_features.md`](file:///Users/balashan/Workspace/dev/github/leetcode/reactjs/01_react19_core_compiler_and_new_features.md)
    - [`02_fiber_architecture_reconciliation_and_concurrent_mode.md`](file:///Users/balashan/Workspace/dev/github/leetcode/reactjs/02_fiber_architecture_reconciliation_and_concurrent_mode.md)
* **10:30 – 13:00 [Midday Sprint: State Management & Hooks]**
  * *Concepts:* State hierarchy: local state (`useState`) vs global shared context (`useContext`) vs atomic domain state (**Zustand**) vs server cache (**TanStack Query**).
  * *Files to Read:*
    - [`04_state_management_server_state_and_optimistic_updates.md`](file:///Users/balashan/Workspace/dev/github/leetcode/reactjs/04_state_management_server_state_and_optimistic_updates.md)
    - [`tringapps_ai_architect_50_master_prep.md` (Q2)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q2-how-do-you-design-the-presentation-layer-in-reactjs-and-typescript-for-high-performance-ai-applications)
* **14:30 – 17:30 [Afternoon Architecture: Real-Time AI Canvas & Streaming Tokens]**
  * *Concepts:* Parsing `ReadableStream` over HTTP Server-Sent Events (SSE) with `TextDecoderStream`, avoiding Context re-render lag, throttled Markdown AST parsing (debounced 50ms), canvas artifacts panel.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q23, Q46)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q23-how-do-you-architect-robust-streaming-llm-responses-from-a-spring-boot-backend-to-a-reactjs-frontend-using-server-sent-events-sse)
    - [`volume_06_react_nextjs_frontend_architecture.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_06_react_nextjs_frontend_architecture.md)
* **18:30 – 20:30 [Evening Drill: Web Performance & Security]**
  * *Concepts:* Core Web Vitals (LCP, FID/INP, CLS), code-splitting with dynamic imports, preventing XSS with `DOMPurify` when rendering LLM markdown.
  * *Files to Read:*
    - [`06_performance_optimization_memory_and_profiling.md`](file:///Users/balashan/Workspace/dev/github/leetcode/reactjs/06_performance_optimization_memory_and_profiling.md)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "When do you choose React Hooks with Context vs Zustand or Redux Toolkit in high-frequency real-time web applications?" ([Q49 in EPAM guide](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q49-when-do-you-choose-react-hooks-with-context-versus-zustand-or-redux-toolkit-for-complex-real-time-front-ends)).

---

### DAY 3: Enterprise Spring Boot 3.x, OAuth2/OIDC, AOP & API Contracts
* **07:30 – 10:00 [Morning Deep Dive: Spring Core & AOP Proxies]**
  * *Concepts:* `ApplicationContext` bean lifecycle, BeanPostProcessors, CGLIB dynamic subclass proxies, self-invocation proxy bypass, custom aspects for AI token metrics.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q12)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#12-how-spring-boot-dependency-injection-works-under-the-hood)
    - [`tringapps_ai_architect_50_master_prep.md` (Q5)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q5-how-does-spring-aop-work-under-the-hood-and-how-do-you-leverage-it-for-cross-cutting-enterprise-concerns-in-ai-architectures)
* **10:30 – 13:00 [Midday Sprint: Identity, OAuth 2.0 & Spring Security]**
  * *Concepts:* Authorization Code Flow with PKCE, Client Credentials for microservices, stateless JWT signature verification using Nimbus JOSE+JWT, JWKS caching.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q1)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-1-enterprise-layered-architecture-identity--security-q1q6)
    - [`09_senior_spring_boot_and_security`](file:///Users/balashan/Workspace/dev/github/leetcode/java-interview/09_senior_spring_boot_and_security/README.md)
* **14:30 – 17:30 [Afternoon Architecture: @Transactional Pitfalls & Clean Architecture]**
  * *Concepts:* Checked exception rollbacks (`rollbackFor = Exception.class`), propagation pitfalls (`REQUIRES_NEW` connection pool exhaustion), Hexagonal Architecture (Ports and Adapters).
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q14)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#14-transactions-in-spring-boot-transactional)
    - [`tringapps_ai_architect_50_master_prep.md` (Q3)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q3-how-do-you-design-the-application-layer-in-spring-boot-to-decouple-business-logic-from-ai-frameworks-and-transport-protocols)
* **18:30 – 20:30 [Evening Drill: Contract-First API Governance (getConsenso)]**
  * *Concepts:* OpenAPI 3.0 contract-first development, automated stub generation (`openapi-generator-maven-plugin`), CI breaking change validation, Postel's law, HTTP Sunset headers.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q16)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#16-designing-scalable-rest-apis)
    - [`epam_technical_architect_50_master_qa.md` (Q7, Q8)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-1-engineering-leadership-vision--operating-model)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "What are the three classic pitfalls of Spring's `@Transactional` annotation and how do you resolve them?"

---

### DAY 4: High-Throughput Event-Driven Systems & Apache Kafka Sagas
* **07:30 – 10:00 [Morning Deep Dive: Kafka Internals]**
  * *Concepts:* Partitions, offset commits, consumer group rebalancing, partition key hashing (murmur2), at-least-once delivery vs exactly-once semantics (EOS), transactional coordinators.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q13)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#13-rest-vs-kafka--rabbitmq-in-microservices)
    - [`20_kafka_advanced_production_interview_mastery`](file:///Users/balashan/Workspace/dev/github/leetcode/java-interview/20_kafka_advanced_production_interview_mastery/README.md)
* **10:30 – 13:00 [Midday Sprint: The Train Ticketing Saga Pattern (1M Users/Day)]**
  * *Concepts:* Why 2PC/XA fails under 3,500 req/sec; choreographed Saga on Kafka; Redis provisional seat reservation (7-minute TTL); asynchronous payment confirmation.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q33)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q33-how-did-you-design-the-high-volume-train-ticketing-booking-flow-serving-1m-daily-users-with-an-event-driven-saga-on-kafka)
    - [`volume_02_distributed_ticketing_sagas.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_02_distributed_ticketing_sagas.md)
* **14:30 – 17:30 [Afternoon Architecture: Compensating Transactions & Poison-Pill DLQs]**
  * *Concepts:* Compensating transaction consumers; Quartz TTL background sweeps for abandoned holds; non-blocking retry topics (`orders-retry-5s`, `orders-retry-30s`, `orders-retry-2m`); Dead Letter Queues (DLQ).
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q34, Q37)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q34-how-do-you-handle-compensating-transactions-payment-drop-offs-and-provisional-seat-releases-in-distributed-sagas)
    - [`volume_09_kafka_event_driven_microservices.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_09_kafka_event_driven_microservices.md)
* **18:30 – 20:30 [Evening Drill: Real-Time CDC & Vector Synchronization]**
  * *Concepts:* Debezium Change Data Capture (CDC) over PostgreSQL WAL; streaming document inserts/updates/deletes into Kafka; real-time upsert into Weaviate/pgvector.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q48)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q48-how-do-you-design-real-time-event-driven-data-pipelines-using-apache-kafka-to-keep-vector-databases-synchronized-with-streaming-enterprise-databases)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "How do you handle compensating transactions when a payment fails or times out in a Kafka Saga?"

---

### DAY 5: Distributed System Design & Polyglot Data Architecture
* **07:30 – 10:00 [Morning Deep Dive: Polyglot Persistence & Caching Tiers]**
  * *Concepts:* Relational ACID (PostgreSQL) vs Key-Value (Redis) vs Document (MongoDB) vs Wide-Column (Cassandra). Two-tier caching (L1 Caffeine + L2 Redis Pub/Sub invalidation), cache stampedes (`SETNX` mutex).
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q15)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#15-sql-vs-nosql-selection-matrix-and-polyglot-persistence)
    - [`epam_technical_architect_50_master_qa.md` (Q39)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q39-how-do-you-design-a-two-tier-caching-architecture-l1-caffeine--l2-redis-while-preventing-cache-stampedes)
* **10:30 – 13:00 [Midday Design Sprint: Core System Design Architectures]**
  * *Architectures to Master:*
    1. **Design a Payment System:** Double-entry ledger (Debit + Credit = 0), idempotency keys, state machine, reconciliation.
    2. **Design an API Rate Limiter:** Token Bucket algorithm in Redis using atomic Lua scripts (`HMGET`, `HMSET`).
    3. **Design a Chat Application:** Persistent WebSockets, Redis presence service, Cassandra message store.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q17, Q21, Q22)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#17-design-a-chat-application-whatsapp--slack)
    - [`volume_08_payments_webhooks_ledgers.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_08_payments_webhooks_ledgers.md)
* **14:30 – 17:30 [Afternoon Design Sprint: High-Scale Media & Logistics]**
  * *Architectures to Master:*
    4. **Scale a Video Streaming Platform:** HLS/DASH chunking, CDN edge caching, adaptive bitrate (ABR), HTTP 206 partial content.
    5. **Design a URL Shortener:** 7-char Base62 encoding, ZooKeeper range allocation, Redis 301 vs 302 caching.
    6. **Design a Ride-Hailing App:** Uber H3 hexagonal geospatial sharding, Redis `GEOADD`/`GEORADIUS`.
  * *Files to Read:*
    - [`twenty_four_master_dsa_backend_system_design.md` (Q18, Q20, Q24)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/twenty_four_master_dsa_backend_system_design.md#18-design-a-url-shortener-tinyurl)
    - [`volume_07_media_streaming_large_files.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_07_media_streaming_large_files.md)
* **18:30 – 20:30 [Evening Architecture: Database Query Tuning & Partitioning]**
  * *Concepts:* PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)`, partial indexing, partition pruning on range/hash tables.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q44)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q44-how-do-you-optimize-high-volume-postgresql-databases-using-explain-analyze-partial-indexing-and-table-partitioning)
    - [`04_databases_storage_engines_and_partitioning.md`](file:///Users/balashan/Workspace/dev/github/leetcode/system-design/04_databases_storage_engines_and_partitioning.md)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "Walk me through how you design an idempotent payment processing endpoint with a double-entry ledger."

---

### DAY 6: Cloud-Native, Kubernetes/OpenShift, Temporal & Schedulers
* **07:30 – 10:00 [Morning Deep Dive: Kubernetes / OpenShift Topologies]**
  * *Concepts:* HorizontalPodAutoscaler (HPA on CPU & Kafka lag), zero-downtime rolling updates (`maxSurge: 25%`, `maxUnavailable: 0%`), liveness/readiness/startup probes, graceful shutdown draining.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q41)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q41-how-do-you-configure-kubernetesopenshift-pod-topologies-autoscaling-hpa-and-zero-downtime-rolling-deployments)
    - [`volume_03_kubernetes_eks_architecture.md`](file:///Users/balashan/Workspace/dev/github/leetcode/cloud_architect_interview_100/volume_03_kubernetes_eks_architecture.md)
* **10:30 – 13:00 [Midday Sprint: Distributed Schedulers & Clustered Quartz]**
  * *Concepts:* Spring `@Scheduled` vs Quartz JDBC clustered locking, overlap prevention (`@DisallowConcurrentExecution`), misfire recovery, IST half-hour offset trap (`UTC+05:30`), REST + Kafka dual-channel failover.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q40, Q48)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q40-how-did-you-design-the-distributed-job-scheduler-on-openshift-with-dual-channel-rest--kafka-runtime-failover)
    - [`volume_03_job_scheduling_concurrency_quartz.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_03_job_scheduling_concurrency_quartz.md)
* **14:30 – 17:30 [Afternoon Architecture: Temporal Durable Execution vs Kafka Sagas]**
  * *Concepts:* Temporal architecture (Workflows vs Activities), event sourcing, deterministic replay, human approval suspension (`Workflow.await`), when to choose Temporal vs Kafka.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q9)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q9-what-is-temporal-how-does-durable-execution-work-and-when-do-you-choose-temporal-over-kafka-sagas-for-ai-workflows)
* **18:30 – 20:30 [Evening Drill: Sidecars & Service Mesh (Envoy/Istio)]**
  * *Concepts:* Envoy proxy sidecars for mTLS, automatic cert rotation, AI egress sidecar for token rate limiting & key management, FluentBit logging sidecars.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q6)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q6-how-do-you-implement-the-sidecar-pattern-in-kubernetesopenshift-for-microservices-and-ai-proxies)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "What is Temporal, and why is it superior to Kafka Sagas for long-running human-in-the-loop AI workflows?"

---

### DAY 7: Architecture Governance, Security, SBOM & Capacity Sizing
* **07:30 – 10:00 [Morning Deep Dive: Architectural Artifacts (ADR, SRS, SBOM)]**
  * *Concepts:* Architecture Decision Records (6 sections), Software Requirements Specification (IEEE 830 standard with probabilistic AI NFRs), CycloneDX / SPDX SBOM automation in CI/CD via Trivy.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q10, Q11, Q12)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q10-how-does-an-enterprise-technical-architect-define-and-structure-a-software-requirements-specification-srs-for-mission-critical-ai-systems)
* **10:30 – 13:00 [Midday Sprint: Zero-Trust Security & GDPR Crypto-Shredding]**
  * *Concepts:* Zero-Trust perimeter, mTLS, OAuth2/OIDC stateless JWT verification, tenant-aware audit logging, GDPR Article 17 "Right to be Forgotten" via AWS KMS crypto-shredding.
  * *Files to Read:*
    - [`epam_technical_architect_50_master_qa.md` (Q46, Q47)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q46-how-do-you-architect-a-zero-trust-security-perimeter-across-spring-boot-microservices-using-oauth2oidc-and-stateless-jwts)
    - [`volume_04_multitenancy_security_cryptography.md`](file:///Users/balashan/Workspace/dev/github/leetcode/staff_interview_100/volume_04_multitenancy_security_cryptography.md)
* **14:30 – 17:30 [Afternoon Architecture: Capacity Sizing & Back-of-the-Envelope Math]**
  * *Calculations to Master:*
    1. Sizing a 1M DAU platform (10M requests $\to$ 350 peak RPS $\to$ 10 Spring Boot pods).
    2. Sizing a 10M chunk Vector DB ($10\text{M} \times 6\text{ KB} = 60\text{ GB} + 15\text{ GB HNSW} \to 100\text{ GB RAM}$).
    3. GPU Memory Bandwidth bound: 70B model = 35GB read per token $\to$ continuous batching (vLLM).
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q13, Q14, Q15)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-3-capacity-planning-volumetrics--back-of-the-envelope-estimations-q13q17)
    - [`11_system_design_estimations_and_infra_sizing_playbook.md`](file:///Users/balashan/Workspace/dev/github/leetcode/system-design/11_system_design_estimations_and_infra_sizing_playbook.md)
* **18:30 – 20:30 [Evening Drill: FinOps Unit Economics (Cloud APIs vs Self-Hosted EKS)]**
  * *Concepts:* Break-even volume math: < 40k queries/day $\to$ commercial APIs ($0.75¢/query); > 50k queries/day $\to$ self-hosted AWS `g5.12xlarge` with vLLM ($9k fixed vs $22k variable).
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q17)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q17-how-do-you-build-a-finops-unit-economics-model-comparing-cloud-hosted-api-models-openaianthropic-vs-self-hosted-open-source-models-on-aws-eks)
    - [`volume_10_finops_cost_optimization_executive_leadership.md`](file:///Users/balashan/Workspace/dev/github/leetcode/cloud_architect_interview_100/volume_10_finops_cost_optimization_executive_leadership.md)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "Perform a live back-of-the-envelope calculation to size a vector database storing 10 million document chunks."

---

### DAY 8: LLM Inference Mechanics, Transformer Attention, KV Cache & SSE
* **07:30 – 10:00 [Morning Deep Dive: Pre-Fill vs Autoregressive Decoding]**
  * *Concepts:* Pre-fill phase (parallel, compute/FLOPs-bound, Time-to-First-Token) vs Autoregressive decoding phase (sequential token-by-token, memory-bandwidth bound, streaming throughput).
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q18)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q18-can-you-explain-the-mechanics-of-llm-inference-the-pre-fill-prompt-processing-phase-vs-the-autoregressive-decoding-phase)
    - [`ALL_100_PYTHON_AI_FULLSTACK_QUESTIONS_MASTER.md`](file:///Users/balashan/Workspace/dev/github/leetcode/python/ALL_100_PYTHON_AI_FULLSTACK_QUESTIONS_MASTER.md)
* **10:30 – 13:00 [Midday Sprint: The KV Cache Crisis & PagedAttention (vLLM)]**
  * *Concepts:* Self-attention quadratic computation without caching; KV Cache formula ($2 \times \text{layers} \times \text{heads} \times \text{head\_dim} \times \text{tokens} \times \text{bytes}$); contiguous allocation fragmentation; vLLM virtual memory paging (16-token page tables, prefix caching).
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q19)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q19-what-is-the-kv-cache-key-value-cache-in-llms-why-does-it-consume-massive-gpu-memory-and-how-does-pagedattention-in-vllm-solve-memory-fragmentation)
* **14:30 – 17:30 [Afternoon Architecture: Attention Evolution & Quantization]**
  * *Concepts:* MHA (Multi-Head) vs MQA (Multi-Query single head) vs GQA (Grouped-Query Attention in Llama 3); FP16 vs INT8 vs INT4 (AWQ vs GPTQ); running vLLM and Ollama behind Envoy.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q20, Q21)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q20-explain-the-architectural-evolution-of-attention-mechanisms-multi-head-attention-mha-vs-multi-query-attention-mqa-vs-grouped-query-attention-gqa)
* **18:30 – 20:30 [Evening Drill: Server-Sent Events (SSE) Streaming Pipeline]**
  * *Concepts:* Spring WebFlux reactive `Flux<ServerSentEvent<String>>`, keepalive pings (`: ping\n\n`), browser `fetch` `ReadableStream`, `TextDecoderStream`, React Zustand integration, `AbortController`.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q23)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q23-how-do-you-architect-robust-streaming-llm-responses-from-a-spring-boot-backend-to-a-reactjs-frontend-using-server-sent-events-sse)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "What is the KV Cache in LLMs, why does it consume massive GPU memory, and how does PagedAttention solve memory fragmentation?"

---

### DAY 9: Production RAG, Vector Math, HNSW & Hybrid Search
* **07:30 – 10:00 [Morning Deep Dive: Layout-Aware Ingestion & Vector Math]**
  * *Concepts:* Unstructured.io / LlamaParse layout extraction, table HTML preservation, recursive chunking (500 tokens, 10% overlap), Cosine Similarity vs Dot Product vs Euclidean Distance (L2) unit normalization math.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q24, Q25)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-5-advanced-rag-vector-databases--semantic-search-q24q31)
    - [`epam_technical_architect_50_master_qa.md` (Q9)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q9-explain-the-mechanics-of-cosine-similarity-vs-dot-product-vs-euclidean-distance-in-vector-embeddings-and-how-you-choose-among-them)
* **10:30 – 13:00 [Midday Sprint: Vector Databases & HNSW Indexing]**
  * *Concepts:* Pinecone vs FAISS vs Weaviate vs pgvector vs Azure AI Search; HNSW multi-layer graph skip-lists ($O(\log N)$) vs IVF Voronoi centroids.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q26, Q27)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q26-how-do-you-evaluate-and-select-a-vector-database-pinecone-vs-faiss-vs-weaviate-vs-azure-ai-search-vs-postgresql-pgvector)
* **14:30 – 17:30 [Afternoon Architecture: Hybrid Search & Cross-Encoder Re-Ranking]**
  * *Concepts:* BM25 sparse lexical matching combined with dense HNSW vectors; Reciprocal Rank Fusion formula: `Score = sum(1 / (60 + rank))`; Two-stage funnel: Top-20 retrieved in 30ms, BGE-Reranker cross-encoder scores top 3 in 40ms.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q28, Q29)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q28-how-do-you-implement-hybrid-search-bm25--dense-vectors-with-reciprocal-rank-fusion-rrf-to-eliminate-semantic-search-blind-spots)
    - [`epam_technical_architect_50_master_qa.md` (Q12, Q13)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q12-what-is-hybrid-search-bm25--dense-vector-and-why-is-reciprocal-rank-fusion-rrf-essential-in-production)
* **18:30 – 20:30 [Evening Drill: Advanced RAG Patterns & Vector ACLs]**
  * *Concepts:* HyDE (Hypothetical Document Embeddings), Corrective RAG (CRAG), Self-RAG; chunk-level pre-filtering (`FILTER tenant_id == 'acme'`) to prevent post-filter recall drop-offs.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q30, Q31)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q30-what-are-advanced-rag-patterns-like-self-rag-corrective-rag-crag-and-hypothetical-document-embeddings-hyde)
* **21:00 – 22:30 [Nightly Active Recall & Spoken Drill]**
  * Rehearse aloud: "Why is Hybrid Search with Reciprocal Rank Fusion (RRF) essential in production, and how does cross-encoder re-ranking optimize accuracy?"

---

### DAY 10: Agentic Platforms, Guardrails, FinOps & Leadership
* **07:30 – 10:00 [Morning Deep Dive: LangGraph, MCP & ZeroMem]**
  * *Concepts:* ReAct loops; LangGraph directed stateful graphs (state schemas, nodes, conditional edges, checkpointing); Model Context Protocol (MCP) JSON-RPC standard; ZeroMem local PostgreSQL agent memory with SHA-256 audit hashes.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q32–Q36)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-6-agentic-systems-frameworks--tool-calling-q32q38)
    - [`epam_technical_architect_50_master_qa.md` (Q26, Q27)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-4-agentic-systems-tool-calling--context-governance)
* **10:30 – 13:00 [Midday Sprint: AI Reliability, Guardrails & Evaluation Harnesses]**
  * *Concepts:* Eliminating hallucinations (grounding, temperature 0.0); automated Ragas CI/CD evals (Faithfulness, Relevance, Precision, Recall); NeMo Guardrails / Llama-Guard; Presidio PII redaction; OWASP Top 10 for LLMs.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q39, Q40, Q42, Q43)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#module-7-ai-reliability-hallucination-defense--guardrails-q39q44)
    - [`epam_technical_architect_50_master_qa.md` (Q14, Q15)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#q14-how-do-you-defend-enterprise-rag-systems-against-indirect-prompt-injection-and-document-embedded-jailbreaks)
* **14:30 – 17:30 [Afternoon Architecture: FinOps, Dynamic Routing & Semantic Caching]**
  * *Concepts:* Redis Semantic Cache (cosine $\ge 0.95$, sub-20ms, 35% cost reduction); Dynamic Model Routing (SLMs for classification vs Frontier LLMs); Resilience4j multi-LLM circuit breaker failover.
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q41, Q44)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q41-how-do-you-architect-an-in-memory-semantic-cache-using-redis-vector-search-to-slash-llm-costs-and-deliver-sub-20ms-responses)
    - [`epam_technical_architect_50_master_qa.md` (Q20, Q21, Q25)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-3-llm-cost-optimization-finops--multi-model-governance)
* **18:30 – 20:30 [Evening Executive Sprint: Leadership, Roadmap & tringapps Scenario]**
  * *Pitches to Rehearse:*
    1. Q1: "Setting technical vision and roadmap for scaling AI B2C products."
    2. Q2: "Leading 10+ person teams and building second-line leaders."
    3. Q3: "Scaling to millions of users while maintaining reliability and cost."
    4. Q49 & Q50: "Designing an AI Customer Support Assistant under GDPR" and "Why you are the ideal AI Architect for tringapps from Day 1."
  * *Files to Read:*
    - [`tringapps_ai_architect_50_master_prep.md` (Q49, Q50)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/tringapps_ai_architect_50_master_prep.md#q49-scenario-design-an-ai-powered-customer-support--ticketing-assistant-for-an-enterprise-client-with-strict-gdpr-compliance-and-a-sub-second-sla-how-do-you-present-the-end-to-end-architecture-to-tringapps-stakeholders)
    - [`epam_technical_architect_50_master_qa.md` (Q1–Q4)](file:///Users/balashan/Workspace/dev/github/leetcode/interview_prep/epam_technical_architect_50_master_qa.md#section-1-engineering-leadership-vision--operating-model)
* **21:00 – 22:30 [Final Pre-Interview Polish]**
  * Complete mental walk-through of the entire full-stack to AI architecture stack. You are 100% prepared.
