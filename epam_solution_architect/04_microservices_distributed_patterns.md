# 04: Microservices & Distributed Systems Patterns (Basic → Advanced)

> JD §2: *"Distributed, service-oriented, event-driven applications; modularization and decomposition; Circuit Breaker; Saga; choreography vs orchestration; 2PC/3PC trade-offs; CQRS; modernization."*
> STAR stories for these live in [01](01_jd_expectations_star_stories.md) (S7–S13). This file is the **question bank**.

| Level | Questions |
|---|---|
| Basic | B1–B10 |
| Medium | M1–M14 |
| Advanced | A1–A10 |

---

## Basic

### B1. Monolith vs modular monolith vs microservices?
🎤 "A **monolith** is one deployable with coupled internals. A **modular monolith** is one deployable with strictly enforced module boundaries (separate packages and schemas, ArchUnit rules). **Microservices** are independently deployable services, each owning its data. Microservices buy independent scaling, deployment and team autonomy, and cost you network failures, eventual consistency, distributed debugging and heavier operations. I recommend a modular monolith when the domain is still unclear or the team is small, and microservices when teams and scaling profiles genuinely diverge, as with Search vs Booking in ticketing."

### B2. What is a circuit breaker?
🎤 "A proxy around a remote call with three states. **Closed**: calls flow and failures are counted. **Open**: once the failure or slow-call rate crosses a threshold, calls fail fast without hitting the dependency. **Half-open**: after a wait, a few trial calls go through; success closes it, failure re-opens it. It stops one failing dependency from exhausting threads and cascading. In the scheduler, an open breaker also switched the job to the Kafka channel."

### B3. What is the Saga pattern?
🎤 "A way to keep data consistent across services without a distributed transaction. A business transaction is a **sequence of local transactions**, each publishing an event or command. If a step fails, earlier steps are undone with **compensating transactions**, for example release the seat if payment fails. It gives eventual consistency, so you design explicit intermediate states like PENDING."

### B4. What is CQRS?
🎤 "Command Query Responsibility Segregation: separate the **write model**, which enforces invariants, from the **read model**, which is optimized for queries, often in different stores, synced by events. It's useful when read and write loads or shapes differ a lot. The cost is eventual consistency between the two."

### B5. What is idempotency and why does it matter in distributed systems?
🎤 "An operation is idempotent if doing it twice has the same effect as doing it once. Networks time out and brokers redeliver, so retries are inevitable. Without idempotency, retries mean double charges or duplicate jobs. Implement it with idempotency keys, unique constraints, processed-message tables, or naturally idempotent operations like 'set status = CONFIRMED'."

### B6. What does an API gateway do?
🎤 "It's a single entry point for clients: routing, authentication (JWT validation), rate limiting, TLS termination, request aggregation, versioning and analytics. Keep business logic out of it. Examples: Spring Cloud Gateway, AWS API Gateway, Kong, Apigee."

### B7. What is a service mesh, and do you need one?
🎤 "A mesh (Istio, Linkerd, OpenShift Service Mesh) puts a sidecar proxy beside each service for mTLS, retries, timeouts, traffic shifting and telemetry, without code changes. You need it once you have many services, a zero-trust requirement, or polyglot stacks. For a handful of Java services, Resilience4j plus platform mTLS may be enough. Never configure retries in both the mesh and the application."

### B8. Synchronous vs asynchronous communication: how do you choose?
🎤 "Synchronous (REST, gRPC) when the caller **needs the answer now** to continue, like 'is this seat available?'. Asynchronous (Kafka, RabbitMQ) when the caller only needs to know the request was accepted, or when others react to a fact, like 'booking confirmed, so send an invoice and notify'. Async gives temporal decoupling and resilience. Sync is simpler to reason about. Long sync chains multiply latency and failure probability."

### B9. What is the strangler fig pattern?
🎤 "Incrementally replace a legacy system. Put a façade or gateway in front, route one capability at a time to the new implementation, keep data in sync during the transition, and retire the old code once traffic has moved. It avoids big-bang rewrites, and each step is reversible."

### B10. What is an anti-corruption layer?
🎤 "A translation layer between your model and an external or legacy model, so their concepts and quirks don't leak into your domain. I used one with payment gateways and Factiva, each with its own adapter mapping to our own `PaymentResult` or `ScreeningResult`."

---

## Medium

### M1. How do you decompose a system into microservices?
🎤 (~1.5 min) "By **business capability and bounded context**, found through event storming with domain experts, not by technical layer or entity. For each candidate service I check: does it **own its data**? Can it deploy independently? Does it have a distinct **scaling or change profile**? Can **one team own** it? Then I validate by walking the key user journeys and counting synchronous cross-service calls. If 'book a seat' needs six synchronous hops, the boundaries are wrong. In ticketing: Search (read-heavy, cacheable), Inventory and Booking (consistency-critical), Payment (external integration and PCI scope), Invoicing, and Notifications (async). Smells of bad decomposition: shared databases, services that must deploy together, and chatty CRUD services named after tables."

### M2. Why is "database per service" important, and how do you query across services?
🎤 (~1 min) "A shared database couples services at the schema level. One team's migration breaks another, and you lose independent deployment. So each service owns its data and exposes it through APIs or events. For cross-service queries use **API composition** (a BFF calls several services: simple, but latency adds up), **CQRS read models** (subscribe to events and build a denormalized view: fast, eventually consistent), or a data platform for analytics. Never reach into another service's tables."

### M3. Choreography vs orchestration: trade-offs?
🎤 (~1.5 min)

| | Choreography | Orchestration |
|---|---|---|
| Control | Each service reacts to events | A central orchestrator issues commands |
| Coupling | Loose; services know events, not each other | Services coupled to the orchestrator's commands |
| Visibility | Implicit; needs tracing and a Saga monitor | Explicit; flow is in one place |
| Change | Adding a step touches several services | Change the orchestrator |
| Failure handling | Distributed compensation logic | Centralized retries, timeouts, compensation |
| Risk | Cyclic event dependencies, "event spaghetti" | Orchestrator becomes a god service or bottleneck |
| Fit | Short, stable flows across teams | Long, complex, changing flows, human steps, timers |

"Ticketing booking was choreographed: four steps, different teams, high throughput. The scheduler's chained jobs are orchestrated: the sequence *is* the product. The refund flow I'd orchestrate today with Temporal or Camunda. Hybrid is common: orchestration inside a bounded context, events between contexts."

### M4. How do you design compensations in a Saga? What can't be compensated?
🎤 (~1.5 min) "Each forward step gets a semantic inverse: hold seat ↔ release seat, charge ↔ refund, create booking ↔ cancel booking. Compensations must be **idempotent and retryable**, because they can be redelivered too, and they're business actions, not database rollbacks. A refund is a new transaction, visible to the customer. Some actions **can't be undone**, like sending an email or SMS, or calling an irreversible external API. So you **order steps by reversibility**: do the compensable steps first, the irreversible ones (notifications) last, after the pivot transaction. Use **semantic locks** (a PENDING status) so others don't act on half-finished state, and the **UNKNOWN state plus reconciliation** for ambiguous outcomes like a payment timeout. Never compensate blindly."

### M5. Explain the transactional outbox pattern, and the inbox pattern.
🎤 (~1 min) "**Outbox**: write the business change and the event row in **one local transaction**. A relay (Debezium CDC reading the WAL, or a poller) publishes outbox rows to Kafka and marks them sent. That solves the dual-write problem, at-least-once. **Inbox**: the consumer records processed message IDs in its own DB in the same transaction as the side effect (`INSERT … ON CONFLICT DO NOTHING`), so duplicates are ignored. Outbox + inbox together give effectively-once processing."

### M6. How does two-phase commit work, and why is it usually avoided in microservices?
🎤 (~1.5 min) "A coordinator runs **phase 1, prepare**: each participant writes its changes durably, holds locks and votes yes or no. In **phase 2, commit or abort**: if all voted yes, the coordinator tells everyone to commit, otherwise to roll back. It gives atomicity across resources. The problems: it's **blocking**. If the coordinator fails after prepare, participants stay in doubt, **holding locks** until it recovers. Latency is at least two round trips with locks held. It couples availability (all participants must be up). And many modern resources (Kafka, REST APIs, SaaS, most NoSQL stores) **don't support XA**. At 3,500 booking requests per second, locks held across a network were exactly what we were removing. So we used a Saga, and documented the 2PC rejection in an ADR."

### M7. What does 3PC add, and why isn't it used much?
🎤 (~1 min) "3PC splits commit into **can-commit → pre-commit → do-commit**. After pre-commit, participants know everyone voted yes, so if the coordinator disappears they can **time out and commit** rather than block. But it assumes a **synchronous network with bounded delays and no partitions**. Under a real partition, two sides can decide differently, so it isn't safe in practice. Production systems use consensus (Paxos, Raft) for agreement, or avoid distributed atomic commits with Sagas. Know it to explain the trade-off; don't propose it."

### M8. When *is* 2PC acceptable?
🎤 (~45 s) "Within a single trusted boundary, at modest throughput, where all resources support XA and correctness outweighs latency. For example, a legacy app server writing to one relational database and one JMS queue, or within a distributed SQL database that runs consensus-based commit internally (Spanner, CockroachDB). Even then, the outbox is often simpler."

### M9. Event sourcing vs CQRS: are they the same?
🎤 (~1 min) "No. **Event sourcing** stores state as an append-only log of events and derives current state by replay. You get a full audit history, temporal queries and the ability to rebuild projections, at the cost of complexity, schema evolution of events, and snapshots. **CQRS** just separates read and write models. They pair well (events feed read models), but you can do CQRS with ordinary state tables, which is what we did in ticketing. I'd use event sourcing for audit-centric domains like ledgers or AML case histories, not as a default."

### M10. Timeouts, retries, bulkheads, rate limiting: how do they fit together?
🎤 (~1.5 min) "They're layers. **Timeouts** come first, because without them nothing else works. Derive them from the end-to-end latency budget. **Retries**: only idempotent operations, exponential backoff with **jitter**, a small attempt count, and **one retry owner per hop**, plus a retry budget to avoid retry storms. **Circuit breaker** outside the retry, so retries stop when a dependency is clearly down. **Bulkhead**: separate thread pools or semaphores per dependency, so one slow service can't starve the rest. **Rate limiter** to protect yourself and respect partner quotas. **Fallback**: a degraded but useful response. In the scheduler the fallback was the Kafka channel, not an error."

### M11. Distributed locking: how and when?
🎤 (~1.5 min) "First ask whether you can avoid it with idempotency, partitioning by key (a Kafka partition gives single-writer per key), or DB constraints. If you need it: a **DB row lock** (`SELECT … FOR UPDATE`, or Quartz's `QRTZ_LOCKS`) is simple and correct if you already have the DB, and it's what the scheduler used. **Redis `SET NX PX`** is fast but needs care. Redlock is debated because process pauses or clock drift can let two holders believe they own the lock. **ZooKeeper/etcd** with leases are consensus-backed. Whatever you use, add **fencing tokens**: a monotonically increasing token checked by the protected resource, so a paused ex-holder can't write. And keep the critical section short with a TTL."

### M12. How do you handle duplicate and out-of-order events?
🎤 (~1 min) "**Duplicates**: idempotent consumers (inbox table, unique keys, upserts). **Ordering**: Kafka guarantees order only **within a partition**, so key by the entity (booking ID, job ID) so one entity's events stay ordered. Across entities, design for reordering: include a **version or sequence number** in events and ignore stale ones (`UPDATE … WHERE version < :incoming`). For state machines, reject invalid transitions, for example PaymentSucceeded arriving for a booking already CANCELLED, which triggers a refund rather than a confirm."

### M13. How do you trace a request across microservices?
🎤 (~45 s) "Use OpenTelemetry W3C trace context propagated through HTTP headers and Kafka record headers. Micrometer Tracing in Spring Boot 3 instruments clients, servers and listeners automatically. Correlate logs via `traceId` in MDC. For choreographed Sagas, add a business correlation ID (booking ID) to events and build a Saga monitor view from them."

### M14. What is a distributed monolith, and how do you spot one?
🎤 (~1 min) "Services that are deployed separately but must change, deploy and fail together. Symptoms: lockstep releases, shared databases or shared domain libraries, long synchronous call chains, one service's outage taking down everything, and a change requiring PRs in five repos. Fixes: realign boundaries to capabilities, move to async events, merge services that always change together, and introduce consumer-driven contracts."

---

## Advanced

### A1. Design the ticketing booking Saga end-to-end, including its failure matrix ✅
🎤 (~2 min) "Services: Booking, Inventory, Payment, Invoicing, Notification. Kafka topics keyed by `bookingId`. Outbox in every producer, inbox in every consumer.

Happy path: `POST /bookings` with an idempotency key creates a PENDING booking. Inventory places a Redis hold with a 7-minute TTL (`SET seat:{train}:{seat} {bookingId} NX PX 420000`) and records the hold, then publishes `SeatHeld`. Payment calls the gateway with idempotency key = bookingId, then publishes `PaymentSucceeded`. Booking moves to CONFIRMED and publishes `BookingConfirmed`. Invoicing and Notification consume it. Notification is last because it's irreversible."

| Failure | Detection | Handling |
|---|---|---|
| Seat already held | `SET NX` fails | Immediate 409 "choose another seat"; no Saga started |
| Payment declined | Gateway response | `PaymentFailed` → Inventory releases hold → Booking FAILED |
| Payment timeout | No response in 30 s | Payment UNKNOWN → reconciliation job queries gateway → success or fail path; **never release blindly** |
| User abandons | Hold TTL expires | Sweeper publishes `HoldExpired` → Booking EXPIRED |
| Payment succeeds after hold expired | Late `PaymentSucceeded` | Try re-hold; if seat taken → auto-refund + apology notification |
| Duplicate events | Inbox table | Ignored |
| Consumer crash mid-step | Offset not committed | Redelivered; idempotent handlers |
| Invoicing down | Lag grows | Booking still CONFIRMED; invoice generated on recovery (not on critical path) |

"Result: zero double-bookings and 99.99% inventory accuracy at about 3,500 req/s, with checkout P99 from 4.2 s to 240 ms."

### A2. Implement an orchestrated Saga in Java (Temporal or a hand-rolled orchestrator)
🎤 (~1.5 min) "For flows like refunds or partial cancellations I prefer orchestration. With Temporal, the workflow code *is* the Saga. Activities are retried automatically, compensations are registered as you go, and state survives crashes."

```java
public class RefundWorkflowImpl implements RefundWorkflow {
    private final RefundActivities acts = Workflow.newActivityStub(RefundActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build()).build());

    @Override
    public RefundResult refund(RefundRequest r) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        try {
            acts.markBookingRefundPending(r.bookingId());
            saga.addCompensation(acts::restoreBookingConfirmed, r.bookingId());

            acts.releaseSeat(r.bookingId());
            saga.addCompensation(acts::reholdSeat, r.bookingId());

            String refundId = acts.refundPayment(r.bookingId(), r.amount());   // idempotency key = bookingId
            acts.markBookingRefunded(r.bookingId(), refundId);
            acts.notifyCustomer(r.bookingId());                              // irreversible → last
            return RefundResult.ok(refundId);
        } catch (ActivityFailure e) {
            saga.compensate();
            return RefundResult.failed(e.getMessage());
        }
    }
}
```

### A3. Implement CQRS with Kafka projections: consistency and rebuilds
🎤 (~1.5 min) "Write side: services write to Postgres plus an outbox, and events go to Kafka keyed by aggregate ID. Read side: a projector consumes events and upserts a denormalized view (availability per train and date in Redis or OpenSearch), **idempotently**, with version checks so out-of-order events don't regress state. **Rebuild**: because topics are retained or compacted, I can create a new projection by replaying from offset zero into a new index, then switch an alias. Zero downtime, and great for fixing projection bugs. **Consistency UX**: show 'as of' timestamps, re-validate on the command side, and offer read-your-writes for the user who just acted (route that user's next read to the write model, or return the new state in the command response). **Monitor projection lag** as an SLO."

### A4. Modernization assessment: monolith to microservices or cloud-native. Walk me through it ✅
🎤 (~2 min) "**One, discovery**: business capability map, change frequency per module (from git history), incident data, dependency and coupling analysis (static analysis, runtime call graphs), data model and ownership, and NFR gaps. **Two, decide per capability** with the **7Rs**: retain, retire, rehost, replatform (containerize as-is onto OpenShift or EKS), refactor, re-architect, replace with SaaS. **Three, target architecture and transition architectures**: never jump straight to the end state; define the intermediate states that run in production. **Four, platform first**: CI/CD, observability, security baseline, and an API gateway that becomes the strangler façade. **Five, carve out the highest value-to-risk capability first**: introduce an anti-corruption layer, sync data via CDC (Debezium) or events, use dual-run or shadow traffic to compare outputs, then switch with feature flags. **Six, measure**: deployment frequency, lead time, change-failure rate, MTTR (DORA), cost and incident trends. That's how I've designed migration pathways from monolith to microservices, cloud-native and AI-enabled architectures."

### A5. How do you guarantee "exactly-once" in an event-driven system?
🎤 (~1.5 min) "End-to-end exactly-once **delivery** doesn't exist across arbitrary systems. What you can achieve is **effectively-once processing**: at-least-once delivery plus idempotent effects. Within Kafka, the idempotent producer (`enable.idempotence=true`) prevents broker-side duplicates, and **Kafka transactions** (`read_committed` consumers plus `sendOffsetsToTransaction`) give exactly-once for consume-transform-produce **inside Kafka**. As soon as the side effect is a database or an external API, you need the **inbox/idempotency key**, or an atomic DB write of state plus offset. I explain this distinction in design reviews, because 'we enabled EOS' often hides a non-idempotent DB write."

### A6. Designing for partial failure: what happens when Kafka itself is down?
🎤 (~1 min) "Producers using the **outbox** keep committing business transactions; the outbox grows and drains when Kafka recovers. Nothing is lost and the user path stays up. Producers sending directly need a local buffer or must fail the request. Consumers just pause. Monitor outbox age as an SLI. That's another reason the scheduler's REST → Kafka fallback was written to the outbox rather than sent directly."

### A7. Microservice versioning and backward compatibility across services and events
🎤 (~1 min) "APIs: additive changes only within a version; tolerant readers (ignore unknown fields); a new major version (`/v2`) for breaking changes with a deprecation window and `Sunset` headers; breaking-change detection in CI (the getConsenso approach). Events: a schema registry (Avro or Protobuf) with **BACKWARD** or **FULL** compatibility enforced, never reusing field numbers or names with new meanings, and upcasters for old events. Consumers deploy before producers start emitting new fields."

### A8. When would you NOT use microservices? Decisions that could have been better ✅
🎤 (~1.5 min) "I'd not use them for an early-stage product with unclear domain boundaries, a team under about 10 engineers, no CI/CD or observability maturity, or strong cross-entity transactional needs. getConsenso, for example, is deliberately a small set of Node/Next.js services, not dozens, because I'm one engineer. On reflection: in one migration we split by entity too early and got a chatty distributed monolith, and later merged services. Choreography in the booking Saga became hard to follow as refunds and partial cancellations were added; orchestration would have been clearer there. And dual-channel REST + Kafka doubled the test matrix; async-first via outbox would be simpler."

### A9. Service granularity: how small is too small?
🎤 (~45 s) "Too small when a single business change spans several services, when services can't do anything useful without synchronous calls to siblings, or when operational overhead outweighs the benefit. A good heuristic: one service per bounded context or sub-capability, owned by one team, deployable on its own, with a meaningful API rather than CRUD on a table."

### A10. How do you design a highly concurrent reservation system (seats, inventory, slots) generically?
🎤 (~1.5 min) "The pattern is **reserve, confirm, release**. The reservation is a short-lived **atomic claim** on the contended unit: Redis `SET NX PX`, or a DB conditional update `UPDATE seat SET held_by=?, held_until=? WHERE id=? AND (held_by IS NULL OR held_until < now())`. Check affected rows to know you won. **Partition by the contended key** (train-date) to spread hot spots. **Confirm** via a Saga after payment. **Release** via TTL plus a sweeper. Put an **admission queue or virtual waiting room** in front for flash peaks. The source of truth stays in the DB and the cache only accelerates. Make it observable: holds created, expired, confirmed, and conflicts."
