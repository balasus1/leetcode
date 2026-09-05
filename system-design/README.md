# 🏛️ System Design & Distributed Systems Engineering Master Repository

A comprehensive, production-grade knowledge base designed to clear Staff / Principal / FAANG System Design rounds.

---

## 📚 Modules & Deep-Dives

| Module | Title | Core Focus Areas |
| :--- | :--- | :--- |
| **[Master Guide](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/ALL_SYSTEM_DESIGN_MASTER.md)** | **Complete Master Document** | Unified document containing all 11 modules and case studies. |
| **[Part 00](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/00_capacity_planning_and_cost_estimation_framework.md)** | Capacity Planning & Cost Estimation | 6-step sizing pipeline, Little's Law, memory 80/20, egress pricing, cloud cost models. |
| **[Part 01](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/01_url_shortener_service.md)** | Global URL Shortener Service (TinyURL) | Base62 encoding, KGS counter, multi-region routing, Redis caching, 100B URLs. |
| **[Part 02](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/02_scalability_load_balancing_and_consistent_hashing.md)** | Scalability & Load Balancing | L4 vs L7, Direct Server Return (DSR), Maglev, P2C algorithm, Consistent Hashing with Vnodes. |
| **[Part 03](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/03_reliability_fault_tolerance_and_resilience_patterns.md)** | Reliability & Resilience | SLA/SLO math, Circuit Breakers, Exponential Backoff + Jitter, Bulkheads, Redis Lua Sliding Window. |
| **[Part 04](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/04_databases_storage_engines_and_partitioning.md)** | Databases & Storage Engines | B+Tree vs LSM-Tree, Composite index leftmost prefix, Sharding strategies, Quorum $R+W>N$. |
| **[Part 05](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/05_distributed_systems_cap_pacelc_consensus_and_transactions.md)** | Distributed Systems, CAP & Consensus | CAP proof, PACELC, Linearizability vs Eventual, Raft consensus, Saga Orchestration, Outbox CDC, Snowflake IDs. |
| **[Part 06](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/06_caching_architectures_eviction_and_recency.md)** | Caching Architectures & Eviction | Multi-tier topology, Write-Through vs Write-Behind vs Aside, LRU/LFU/ARC/TinyLFU, XFetch stampede, Bloom filters. |
| **[Part 07](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/07_messaging_streaming_and_event_driven_architectures.md)** | Messaging & Event Streams | RabbitMQ vs Kafka, zero-copy `sendfile`, consumer groups, Exactly-Once Processing, Event Sourcing, CQRS. |
| **[Part 08](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/08_cloud_native_kubernetes_and_storage_infrastructure.md)** | Cloud-Native K8s & Object Storage | K8s control plane, Envoy sidecars, S3 Erasure Coding, Presigned URLs, Google Spanner TrueTime, BigQuery. |
| **[Part 09](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/09_graph_systems_algorithms_and_search_engines.md)** | Graph Systems & Search Engines | Index-Free Adjacency, BFS/DFS 2nd-degree friends, Dijkstra, PageRank, Elasticsearch Inverted Index, BM25. |
| **[Part 10](file:///Volumes/Workspace/dev/github/personal/balasus1/leetcode/system-design/10_top_faang_system_design_case_studies.md)** | Top FAANG Case Studies | WhatsApp Real-Time Chat, Twitter Hybrid Newsfeed, Uber H3 Hexagonal Dispatch, YouTube Transcoding. |

---

## 🎯 Quick Reference: Decision Cheatsheet

### 1. When to choose what Database:
- **Relational (PostgreSQL / MySQL)**: Financial ledgers, complex multi-table SQL queries, ACID requirement, dataset fits on single primary + replicas ($< 2	ext{TB}$).
- **Key-Value / Wide-Column (DynamoDB / Cassandra)**: Massive horizontal write scale ($> 100	ext{k}$ QPS), predictable key-based queries, multi-region active-active.
- **Distributed NewSQL (Google Spanner / CockroachDB)**: Global relational database requiring strong consistency without manual sharding.
- **Document Store (MongoDB)**: Unstructured polymorphic JSON schemas, rapid prototyping.

### 2. When to choose what Messaging System:
- **AWS SQS / RabbitMQ**: Task/Job queues where messages are discarded after execution, complex per-message routing, delayed delivery.
- **Apache Kafka / AWS Kinesis / GCP Pub/Sub**: High-throughput event streaming, event sourcing, CDC pipelines, message replayability, multi-consumer subscriptions.

### 3. When to choose what Real-Time Protocol:
- **Server-Sent Events (SSE)**: Unidirectional server-to-client streaming (e.g. LLM token streaming, stock prices).
- **WebSockets**: Bi-directional full-duplex communication (e.g. chat applications, collaborative whiteboards, multiplayer gaming).
- **gRPC**: Low-latency internal microservice communication over HTTP/2 with Protocol Buffers.
