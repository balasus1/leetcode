# 06: Databases, DB Choices, Kafka, RabbitMQ & Messaging (Basic → Advanced)

> JD: *"Event-driven architectures (Kafka, RabbitMQ); CAP in distributed data stores; transaction management."* Resume: PostgreSQL, Redis, Kafka, RabbitMQ, Spark/Hadoop (HBase, Hive), DB partitioning, pessimistic locking.
> Existing deep dives: [java-interview/20_kafka_advanced_production_interview_mastery](../java-interview/20_kafka_advanced_production_interview_mastery), [system-design/07_messaging_streaming_and_event_driven_architectures.md](../system-design/07_messaging_streaming_and_event_driven_architectures.md).

| Section | Questions |
|---|---|
| Databases: basic | DB-B1–B7 |
| Databases: medium/advanced | DB-M1–M10 |
| DB choices per project | DB-C1 |
| Kafka | K-B1–B6, K-M1–M10, K-A1–A4 |
| RabbitMQ & broker choice | R-B1–B4, R-M1–M5 |

---

## Databases: Basic

### DB-B1. SQL vs NoSQL: how do you decide?
🎤 "Start from **access patterns and consistency needs**, not fashion. Relational (Postgres) for transactional integrity, complex queries and joins, and evolving query needs. That's my default for systems of record like bookings and scheduler state. NoSQL by type: **key-value** (Redis, DynamoDB) for simple lookups at massive scale; **document** (MongoDB) for flexible aggregates; **wide-column** (Cassandra, HBase) for huge write-heavy, time-series-like data; **search** (OpenSearch) for full-text and faceting; **graph** (Neo4j, Neptune) for relationship traversal. Many systems are polyglot."

### DB-B2. ACID vs BASE?
🎤 "**ACID**: atomicity, consistency, isolation, durability. That's classic RDBMS transactions. **BASE**: basically available, soft state, eventually consistent, which is typical of distributed NoSQL that chooses availability. In microservices we get ACID **within** a service's database and BASE **across** services via Sagas."

### DB-B3. What index types do you use?
🎤 "**B-tree** is the default, for equality and range. **Composite** indexes, where order matters: equality columns first, then range. **Partial** indexes (`WHERE status='SCHEDULED'`), great for queue-like tables. **Covering** indexes with `INCLUDE`. **GIN** for JSONB, arrays and full-text. **BRIN** for huge append-only, time-ordered tables. **Hash** for equality-only. Every index slows writes, so index for real query patterns verified with EXPLAIN."

### DB-B4. Transaction isolation levels and anomalies?
🎤 "**Read uncommitted** allows dirty reads (Postgres treats it as read committed). **Read committed** (Postgres default) prevents dirty reads but allows non-repeatable reads and phantoms. **Repeatable read** (snapshot isolation in Postgres) can still allow write skew. **Serializable** (SSI in Postgres) prevents all anomalies, but transactions may abort and need retries. I usually stay on read committed and protect specific invariants with row locks, constraints or optimistic versions."

### DB-B5. Normalization vs denormalization?
🎤 "Normalize the write model to avoid update anomalies. Denormalize **read models** for performance, such as a search projection or reporting tables, accepting duplication that you keep in sync via events or materialized views. That's CQRS in practice."

### DB-B6. What is connection pooling and how do you size the pool?
🎤 "Opening DB connections is expensive, so a pool (HikariCP) reuses them. Bigger isn't better: a database has limited cores and too many concurrent connections cause context switching and lock contention. A starting formula is about `cores × 2 + effective spindles` per DB, divided across all app instances. With 20 pods × 50 connections you'd overwhelm Postgres, so use PgBouncer or RDS Proxy. Watch `pending` and `active` metrics."

### DB-B7. Optimistic vs pessimistic locking (concept)?
🎤 "**Optimistic**: assume no conflict, detect it at write time with a version column, and retry. Good for low contention. **Pessimistic**: lock the row up front (`SELECT … FOR UPDATE`) so others wait. Good for high contention on must-not-conflict transitions, like the scheduler's job state changes, provided you hold the lock briefly."

---

## Databases: Medium / Advanced

### DB-M1. How did you use pessimistic locking in the scheduler, and what would you change? ✅
🎤 (~1.5 min) "Job state transitions (pause, resume, stop, delete) must be serialized. Two operators or two pods acting on the same job at once could resurrect a deleted job or double-trigger it. So transitions do `SELECT … FOR UPDATE` on the job row, validate the transition against the state machine, update, and commit. The issue was that the lock was originally held across trigger evaluation and validation, so connections queued up. I narrowed it to just the atomic transition and cached metadata in Redis: P99 went from 680 to 210 ms, lock waits dropped around 75%. **What I'd change**: for worker claiming, `FOR UPDATE SKIP LOCKED` so pods never wait on each other; for admin edits to definitions, **optimistic `@Version`**; and lock timeouts everywhere. Pessimistic locking stays only for the genuine conflict hotspots."

### DB-M2. How do you read an EXPLAIN ANALYZE plan? ✅
🎤 (~1.5 min) "I run `EXPLAIN (ANALYZE, BUFFERS)` on the real query with production-like data. I look at: **estimated vs actual rows** (big gaps mean stale statistics, so `ANALYZE`, or correlated columns need extended statistics); **Seq Scan** on large tables with selective filters (missing or unusable index, for example a function on the column or a type mismatch); **Nested Loop** with huge outer row counts (should be a hash join; often a misestimate); **Sort** or **Hash** spilling to disk (`work_mem`); **Buffers: shared read** vs hit (I/O vs cache); and **rows removed by filter** (index not selective enough). In ticketing the dashboard query was a seq scan on bookings filtered by status and date range. A composite index `(status, travel_date)` plus removing an ORM N+1 fixed it. I always confirm with before-and-after timings under load, not one cold run."

### DB-M3. Table partitioning in PostgreSQL: when and how? ✅
🎤 (~1.5 min) "Partitioning splits a big table into child tables by **range** (time), **list** (tenant or region) or **hash**. Benefits: **partition pruning** (queries touch only relevant partitions), smaller indexes that fit in memory, cheap data retention (`DROP` or `DETACH` an old partition instead of a massive `DELETE`), and parallel maintenance. In the CRM engine, partitioning interaction tables helped sustain the high write rate and kept indexes small, part of the 4× improvement. Cautions: the partition key must appear in queries for pruning; unique constraints must include the partition key; too many partitions hurt planning time. Automate partition creation with `pg_partman` or a scheduled job."

```sql
CREATE TABLE interactions (
  interaction_id text NOT NULL, client_id text NOT NULL, channel text NOT NULL,
  occurred_at timestamptz NOT NULL, payload jsonb,
  PRIMARY KEY (interaction_id, occurred_at)
) PARTITION BY RANGE (occurred_at);
CREATE TABLE interactions_2026_11 PARTITION OF interactions
  FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE INDEX ON interactions_2026_11 (client_id, occurred_at DESC);
```

### DB-M4. Sharding: strategies and pitfalls?
🎤 (~1 min) "Split data across database instances by a **shard key**: hash-based (even distribution, hard range queries), range-based (good for time, but hot spots), directory or lookup-based (flexible, but the directory is an extra component). Pitfalls: cross-shard joins and transactions, rebalancing, hot keys, and operational complexity. Choose a key aligned with the main access pattern (tenant ID for SaaS). Prefer partitioning, read replicas, or a managed distributed database before hand-rolled sharding."

### DB-M5. Redis: what do you use it for, and what are the risks? ✅
🎤 (~1.5 min) "**Caching** (scheduler job metadata, L2 cache), **provisional seat holds** with TTL in ticketing (`SET NX PX`), **rate limiting** (token buckets via Lua or Bucket4j, as in my RAG agent's per-user limits), **pub/sub** for cache invalidation, **distributed locks** carefully with fencing, **sessions**, and **leaderboards/streams**. Risks: it's in memory, so plan for eviction policies (`volatile-ttl` vs `allkeys-lru`) and `maxmemory`. Persistence (RDB snapshots and AOF) and replication are asynchronous, so a failover can lose recent writes. That's why seat holds were *accelerators* backed by the booking DB as the source of truth, and confirmation re-validated. Avoid big keys and hot keys, and `KEYS *` in production. Use Redis Cluster or ElastiCache for HA."

### DB-M6. Zero-downtime schema migrations?
🎤 (~1 min) "**Expand–contract** with Flyway or Liquibase, as covered in the Spring answer: add nullable columns, dual-write, backfill in batches, switch reads, drop later. Also create indexes `CONCURRENTLY` in Postgres, avoid long `ALTER TABLE` locks on hot tables (add a column with no default rewrite, add constraints `NOT VALID` then validate separately), and set `lock_timeout` on migrations so they fail rather than block production traffic."

### DB-M7. Read replicas: what problems do they introduce?
🎤 (~45 s) "**Replication lag**, so stale reads. A user who just paused a job might not see it. Solutions: route that user's reads to the primary for a short window (read-your-writes), use session-based stickiness, or read critical data from the primary. Also failover promotion, and long queries on replicas conflicting with vacuum (`hot_standby_feedback`)."

### DB-M8. OpenSearch/Elasticsearch: when, and how do you keep it in sync?
🎤 (~1 min) "When you need full-text search, fuzzy matching, faceting, geo or log analytics, which relational indexes do poorly. It's never the system of record. Sync through **CDC or events** (Debezium → Kafka → indexer), idempotent upserts by ID, versioning to drop stale updates, and a full reindex path via an alias swap. Monitor indexing lag."

### DB-M9. Hadoop lakehouse (HBase/Hive) on your resume: what would you do today? ✅
🎤 (~1.5 min) "We built **Spark ELT pipelines streaming Kafka data into a Hadoop lakehouse**. HBase served low-latency key lookups, and Hive served SQL analytics over partitioned data, later moved to AWS. We partitioned RDDs and DataFrames on high-cardinality keys to avoid skew, tuned shuffle partitions and broadcast joins, and monitored with Ambari, Grafana and CloudWatch. **Today** I'd use an **open table format (Apache Iceberg, or Delta/Hudi) on S3** with Spark or Flink, a Glue or REST catalog, and Athena, Trino or Redshift Spectrum for queries. That gives ACID tables on object storage, schema evolution, time travel, and separate storage and compute, without operating HDFS or HBase. For low-latency serving, DynamoDB or a purpose-built store fed from the same stream."

### DB-M10. How would you store time-series or audit data at scale?
🎤 (~1 min) "Append-only, partitioned by time, with lifecycle tiering. Hot data in Postgres partitions or TimescaleDB, or OpenSearch for search; cold data in S3 Parquet or Iceberg queried by Athena; WORM requirements via **S3 Object Lock**. For tamper evidence, hash-chain records per tenant. For GDPR erasure, use **crypto-shredding** with per-tenant keys rather than deleting from immutable stores."

---

## DB-C1. "Which databases did you choose, and why?" (per-project decision table) ✅

| Project | Store | Why chosen | Alternatives rejected & why | Would revisit |
|---|---|---|---|---|
| Job scheduler | **PostgreSQL** (Quartz JDBC store + job/audit tables) | ACID state transitions, row locks for clustering, team skills | MongoDB (weak multi-doc transactions at the time), Redis-only (durability) | `SKIP LOCKED` claiming; partition run history |
| Job scheduler | **Redis** | Metadata cache (~10-min TTL), pub/sub invalidation | Caffeine-only (no cross-pod invalidation) | Fine as is |
| Train ticketing | **PostgreSQL** per service | Bookings and payments need ACID | Cassandra (no transactions, harder invariants) | Distributed SQL for multi-region |
| Train ticketing | **Redis** | Seat holds with TTL, `SET NX` atomic claim, caches | DB row locks (contention at 3,500 req/s) | Keep, with DB re-validation |
| Train ticketing | Read-model cache + CDN | Search and schedules (read-heavy, stale-tolerant) | Querying booking DB directly | Formal CQRS projections |
| Morgan Stanley CRM | **Relational DB with partitioning** | Audit-ready records, batch writes, partition pruning | NoSQL (compliance tooling and joins needed) | Kafka in front for durability |
| Analytics | **HBase + Hive** on Hadoop → AWS | Low-latency lookups + SQL analytics | — | Iceberg on S3 + Athena/Trino |
| getConsenso | **PostgreSQL + Redis** | Multi-tenant relational data, tenant-aware audit; Redis for caching and rate limits | Firestore/Mongo (relational queries across specs, reviews, orgs) | Postgres RLS for tenant isolation |
| ZeroMem | **User's own Postgres** (pgvector) | Data stays with the user; only hashes retained for audit | Hosted vector DB (data leaves the customer) | — |
| RAG agent / GenAI | **pgvector or OpenSearch** + S3 | Hybrid search, metadata filters, tenant scoping | Dedicated SaaS vector DB (data residency) | — |

🎤 (~1 min summary) "My pattern is Postgres as the system of record, Redis for speed and short-lived coordination, and purpose-built stores for search and analytics, fed by events. Each choice is in an ADR with the access patterns that justified it and the trigger to revisit."

---

## Kafka

### K-B1. Explain Kafka's core concepts.
🎤 "Kafka is a distributed, append-only **log**. **Topics** split into **partitions**, which are the unit of parallelism and ordering. Each record has an **offset**. Producers write to partitions, chosen by key hash. **Consumer groups** share partitions, with each partition consumed by one member of a group, so max parallelism = partition count. Brokers replicate partitions (leader plus followers, the ISR). Data is retained by time or size, or **compacted** by key, so consumers can replay."

### K-B2. How does Kafka guarantee ordering?
🎤 "Only **within a partition**. So key by the entity whose events must stay ordered (booking ID, job ID). With retries, keep ordering by using the idempotent producer (`enable.idempotence=true`, which allows up to 5 in-flight requests with ordering preserved). Across partitions there is no global order. Design consumers to tolerate it."

### K-B3. Delivery semantics?
🎤 "**At-most-once**: commit the offset before processing, so you may lose messages. **At-least-once**: process, then commit, so you may get duplicates. This is the usual default, paired with idempotent consumers. **Exactly-once**: idempotent producer plus transactions for consume-transform-produce within Kafka. External side effects still need idempotency."

### K-B4. Durability settings you always set?
🎤 "Producer: `acks=all`, `enable.idempotence=true`, sensible `delivery.timeout.ms`. Topic: `replication.factor=3`, `min.insync.replicas=2`, `unclean.leader.election.enable=false`. Brokers spread across AZs with rack awareness. That way, losing one broker loses no acknowledged data and stays writable."

### K-B5. What is a consumer group rebalance?
🎤 "When members join or leave, or partitions change, partitions are reassigned. Eager rebalancing stops everyone. **Cooperative sticky** assignment (incremental) moves only the affected partitions. Also use **static membership** (`group.instance.id`) so rolling restarts don't trigger rebalances, and keep processing within `max.poll.interval.ms`, or the consumer is kicked out and rebalance storms follow."

### K-B6. Log compaction: what is it for?
🎤 "Kafka keeps at least the **latest value per key**, deleting older ones, and a null value (tombstone) deletes the key. Good for changelog or state topics, like the latest job definition or document version, so a new consumer can rebuild current state by replay."

### K-M1. How do you handle errors, retries and dead-letter topics? ✅
🎤 (~1.5 min) "Classify errors. **Transient** (downstream timeout): retry with backoff. **Poison pill / deterministic** (bad schema, validation failure): don't retry forever. Send to a **DLT** with headers carrying the exception, original topic, partition and offset. In Spring Kafka, `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries` and `DeadLetterPublishingRecoverer` for blocking retries. Or **non-blocking retry topics** (`@RetryableTopic`: retry-1s, retry-10s, retry-1m, then DLT), so one slow message doesn't block the partition, at the cost of ordering for that key. Then have a DLT **runbook and replay tool**, alerts on DLT growth, and idempotent consumers so replays are safe."

```java
@RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 10),
                exclude = {ValidationException.class},              // poison → straight to DLT
                dltStrategy = DltStrategy.FAIL_ON_ERROR)
@KafkaListener(topics = "payment.events", groupId = "booking")
public void onPayment(PaymentEvent e) { bookingService.apply(e); }   // idempotent

@DltHandler
public void dlt(PaymentEvent e, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String err) {
    alerting.dlt("payment.events", e.bookingId(), err);
}
```

### K-M2. How do you design idempotent consumers? ✅
🎤 (~1 min) "Each event carries a unique `eventId` and the entity ID. In the **same DB transaction** as the side effect, insert the eventId into a `processed_events` table with a unique constraint (`ON CONFLICT DO NOTHING`), and if it was already there, skip. Or make the operation naturally idempotent: an upsert with a version check, or 'set status to CONFIRMED where status = PENDING'. Commit Kafka offsets after the DB commit. Duplicates are then harmless."

### K-M3. How many partitions should a topic have?
🎤 (~1 min) "Enough for **peak consumer parallelism** with headroom: if one consumer instance handles 1,000 msgs/s and the peak is 14,000/s, I need at least 14 consumers, so 24–48 partitions. Also consider producer throughput per partition and key distribution (skewed keys create hot partitions). Increasing partitions later **breaks key→partition mapping**, which matters for ordering, so plan ahead. Don't go extreme either: thousands of partitions per broker add recovery time and metadata overhead."

### K-M4. Schema evolution with a schema registry?
🎤 (~1 min) "Use Avro or Protobuf with a schema registry (Confluent or AWS Glue). Set a compatibility mode: **BACKWARD** (new consumers read old data, the default), **FORWARD**, or **FULL**. Rules: add fields with defaults, never change a field's type or meaning, never reuse Protobuf tag numbers, deprecate rather than delete. CI checks compatibility before a producer deploys. In ticketing we added the registry after a breaking change. Lesson learned: day one next time."

### K-M5. Kafka transactions and exactly-once: how do they work?
🎤 (~1 min) "The producer gets a `transactional.id`. It begins a transaction, sends output records, calls `sendOffsetsToTransaction` with the consumer's offsets, and commits. Consumers with `isolation.level=read_committed` see only committed records. So consume-transform-produce is atomic within Kafka. The overhead is extra latency and coordination. It doesn't cover DB writes or HTTP calls, which still need the outbox or idempotency."

### K-M6. CDC with Debezium: why and how?
🎤 (~1 min) "Debezium reads the database transaction log (Postgres WAL via logical replication) and emits change events to Kafka. Uses: the **outbox relay** (the outbox event router), syncing search indexes or caches, and strangler migrations where legacy DB changes feed new services. Benefits: no dual writes, low latency, ordering per row. Watch replication-slot lag (WAL growth if the connector stops) and schema changes."

### K-M7. Kafka Streams vs Flink vs Spark Structured Streaming? ✅
🎤 (~1 min) "**Kafka Streams** is a library inside your Java service: great for per-key stateful processing and joins between Kafka topics, with no separate cluster. **Flink** is a dedicated engine with true event-time processing, large state, complex windowing and low latency. **Spark Structured Streaming** is micro-batch (and continuous), strongest when you also do batch and ML on the same platform, which is why we used Spark for the Kafka → lakehouse ELT. I pick Kafka Streams for service-level enrichment, Flink for complex real-time analytics, Spark for unified batch and streaming."

### K-M8. How do you monitor Kafka?
🎤 (~45 s) "**Consumer lag** per group and partition is the most important SLI (Burrow, or exporter metrics, on Prometheus). Plus under-replicated partitions, ISR shrinks, request latency, disk usage, controller status, producer error and retry rates, and rebalance frequency. Alert on lag growth rate, not just absolute lag."

### K-M9. Kafka security?
🎤 (~45 s) "TLS encryption in transit; authentication via **mTLS** or **SASL** (SCRAM, OAUTHBEARER, or IAM on MSK); **ACLs** per topic and consumer group with least privilege; encryption at rest via disk or KMS on MSK; schema-registry auth; no PII in keys; and field-level encryption for sensitive payloads."

### K-M10. Kafka consumer processing slower than production: what do you do?
🎤 (~1 min) "Find the bottleneck: is it per-record processing time or downstream calls? Options: scale consumers up to the partition count (add partitions if needed, mindful of key mapping); batch processing (`batch listener`, bulk DB writes, like CRM micro-batching); async processing with care for offset ordering (Confluent parallel consumer, key-ordered); optimize the downstream (bulk APIs, caching); and offload slow work to another topic. Make sure processing fits within `max.poll.interval.ms` (reduce `max.poll.records`) to avoid rebalance storms."

### K-A1. Design the Kafka topology for the scheduler's dual channel ✅
🎤 (~1.5 min) "Topics: `scheduler.job-commands.{service}` (commands for a downstream service when its breaker is open, keyed by jobId, 12–24 partitions, 7-day retention); `scheduler.job-events` (JobStarted, JobCompleted, JobFailed for audit, observability and later the AI copilot); a compacted `scheduler.job-definitions` changelog for cache rebuilds; and DLTs per command topic. Commands are written via the **outbox**, so the job's state change and the command are atomic. Each command carries an **idempotency key** (jobId + fireTime), so if a REST call timed out but actually succeeded, the downstream ignores the Kafka duplicate. Downstream services expose the same handler for REST and Kafka, so business logic isn't duplicated. Completion comes back on `job-events`, which updates job state. Monitored by lag and outbox age."

### K-A2. 7,000 msgs/s with per-client ordering: design it on Kafka today ✅
🎤 (~1.5 min) "Topic `crm.interactions`, keyed by `clientId` so each client's interactions are ordered, 48 partitions for headroom (about 150 msgs/s per partition at peak). Producers (the email, SMS, voice and chat connectors) use `acks=all`, idempotence, `linger.ms=5–10` and `compression.type=lz4` for batching efficiency. Consumers are a group of writer instances using **batch listeners** with bulk upserts to partitioned tables (the same 500-record micro-batch idea), with idempotency on `interactionId`. Offsets are committed after the DB commit. Poison messages go to a DLT. Kafka gives the durability and replay the in-memory buffer lacked, while keeping the 4× batching gain. Lag is the scaling signal (KEDA)."

### K-A3. Kafka vs a database outbox poller vs CDC for event publishing: trade-offs?
🎤 (~45 s) "**Direct send**: simplest, but dual-write risk. **Outbox + poller**: simple and reliable, but polling latency and DB load. **Outbox + CDC (Debezium)**: low latency and no polling, but more infrastructure. I use a poller for small services and CDC at scale."

### K-A4. Kafka decisions that could be better ✅
🎤 (~45 s) "No schema registry from day one in ticketing, which caused a breaking-change incident. Partition counts sized for launch, then increased, which reshuffled key ordering during a live migration; I'd now size for 3× and plan the migration path. And early on we used blocking retries, so one poison message stalled a partition. It's non-blocking retry topics plus DLT now."

---

## RabbitMQ & broker choice

### R-B1. RabbitMQ core concepts?
🎤 "Producers publish to **exchanges**, which route to **queues** via **bindings**. Exchange types: **direct** (exact routing key), **topic** (pattern match like `booking.*.confirmed`), **fanout** (broadcast), **headers**. Consumers receive pushed messages and **ack** them; un-acked messages are redelivered. Messages are removed once acked, so it's a queue, not a replayable log."

### R-B2. Acks, prefetch and reliability in RabbitMQ?
🎤 "Use **manual acks** after processing, and `basic.nack`/requeue or dead-letter on failure. **Prefetch (QoS)** limits un-acked messages per consumer, which gives fair dispatch and backpressure. **Publisher confirms** so producers know the broker persisted the message. **Durable queues plus persistent messages**, and **quorum queues** (Raft-replicated) for HA instead of classic mirrored queues."

### R-B3. Dead-lettering and delayed retry in RabbitMQ?
🎤 "A queue with `x-dead-letter-exchange` sends rejected or expired messages to a DLX. For delayed retries: per-queue TTL retry queues that dead-letter back to the work queue (1 s → 10 s → 60 s), or the delayed-message plugin. Track the retry count in headers (`x-death`) and park messages after N attempts."

### R-B4. Where does RabbitMQ fit better than Kafka?
🎤 "**Task and work queues** with per-message acknowledgement, competing consumers, **priority queues**, per-message TTL, **complex routing**, and **request/reply (RPC)**, where messages are consumed and gone. For example, notification dispatch (email, SMS, push workers), image or PDF processing jobs, or commands to workers. Kafka fits event streams, replay, high throughput, event sourcing and stream processing."

### R-M1. Kafka vs RabbitMQ vs SQS/SNS vs EventBridge vs ActiveMQ: decision matrix

| Need | Kafka / MSK | RabbitMQ / Amazon MQ | SQS (+SNS) | EventBridge | ActiveMQ/JMS |
|---|---|---|---|---|---|
| Replay / retention | ✅ log | ❌ (streams plugin partially) | ❌ | Archive & replay | ❌ |
| Ordering | Per partition | Per queue (single consumer) | FIFO queues (per group) | ❌ | Per queue |
| Throughput | Very high | High | Very high (managed) | Moderate | Moderate |
| Routing flexibility | Topic/key | ✅ exchanges | SNS filters | ✅ rules/patterns | Selectors |
| Per-message ack / priority | ❌ (offsets) | ✅ | ✅ (visibility timeout) | n/a | ✅ |
| Stream processing | ✅ Streams/Flink | ❌ | ❌ | ❌ | ❌ |
| Ops burden | High (MSK reduces) | Medium | None | None | Medium |
| Best for | Event backbone, CDC, Sagas, analytics | Work queues, RPC, routing | Serverless decoupling, buffering | SaaS/AWS event integration | Legacy JMS/XA |

🎤 (~1 min) "In the systems I built, **Kafka** was the event backbone (Sagas, the scheduler's async channel, streaming into the lakehouse) because we needed replay, ordering per key and throughput. **RabbitMQ** fits work-queue style needs like notification dispatch with priorities and per-message retries. On AWS, **SQS** for simple decoupling with no ops, **SNS** for fan-out, and **EventBridge** for integrating SaaS and AWS events with rule-based routing. Often it's both: Kafka between domains, a queue in front of a pool of workers."

### R-M2. How do you guarantee ordering in RabbitMQ?
🎤 (~45 s) "Order is preserved per queue with a single consumer. With competing consumers, or after requeue, order breaks. Use the **consistent-hash exchange** to shard by key into multiple queues, each with one consumer. Single active consumer (`x-single-active-consumer`) gives failover without reordering. Or design for idempotent, version-checked processing."

### R-M3. Spring AMQP essentials?
🎤 (~45 s) "`RabbitTemplate` with publisher confirms and returns; `@RabbitListener` with manual ack mode, prefetch and a concurrency range; `RetryInterceptorBuilder` with a `RejectAndDontRequeueRecoverer` to dead-letter after N attempts; declarative `Queue`/`Exchange`/`Binding` beans; and `Jackson2JsonMessageConverter`."

```java
@Bean Queue notifications() {
    return QueueBuilder.durable("notifications").quorum()
            .deadLetterExchange("notifications.dlx").build();
}
@RabbitListener(queues = "notifications", concurrency = "4-16")
public void send(NotificationCommand cmd) { dispatcher.send(cmd); }   // idempotent by cmd.id()
```

### R-M4. Messaging anti-patterns you've seen?
🎤 (~1 min) "Using a message broker as a database, with huge messages instead of the **claim-check** pattern (store the payload in S3 and send a reference). Unbounded retries that hide poison messages. Not monitoring DLQs. Shared topics with mixed event types and no schema. Synchronous request-reply over Kafka for user-facing calls. And consumers that aren't idempotent 'because the broker is exactly-once'."

### R-M5. When would you pick SQS over Kafka on AWS?
🎤 (~45 s) "When I need simple point-to-point buffering between services or Lambdas, with no replay, no ordering beyond FIFO groups, and no operational burden: background jobs, absorbing traffic spikes, fan-out via SNS→SQS. Kafka (MSK) when I need an event log consumed by many independent consumers, replay, stream processing, or CDC."
