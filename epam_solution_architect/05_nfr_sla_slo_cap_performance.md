# 05: NFRs, SLA/SLO/SLI, CAP/PACELC, HA/DR & Performance Tuning (Basic → Advanced)

> JD §3: *"Define, evaluate, enforce strict NFRs; SLAs vs SLOs; CAP theorem in distributed data stores and multi-region systems; highly available, fault-tolerant systems; architecture-level performance tuning."*

| Level | Questions |
|---|---|
| Basic | B1–B9 |
| Medium | M1–M12 |
| Advanced | A1–A8 |

---

## Basic

### B1. What are NFRs? Name the ones you always define.
🎤 "Non-functional requirements describe **how well** the system works rather than what it does. My standard list: **performance** (latency percentiles, throughput), **scalability**, **availability**, **reliability/recoverability** (RTO, RPO), **consistency**, **security & compliance**, **maintainability**, **observability**, **operability**, **cost**, **portability**, plus usability and accessibility for front ends. Each gets a number, a measurement method and an owner."

### B2. SLA vs SLO vs SLI?
🎤 "**SLI** is the measurement, like the proportion of requests under 300 ms. **SLO** is the internal target for that measurement, say 99.9% over 28 days. **SLA** is the contractual promise to a customer with penalties, and it's looser than the SLO, say 99.5%, so we get warning before breaching it. The gap is our safety margin. The SLO also defines the **error budget**."

### B3. What does "four nines" mean in downtime?
🎤 "Per year: 99% ≈ 3.65 days, 99.9% ≈ 8.8 hours, 99.95% ≈ 4.4 hours, **99.99% ≈ 52.6 minutes**, 99.999% ≈ 5.3 minutes. Per month, 99.9% ≈ 43 minutes. Also remember **serial dependencies multiply**: three services at 99.9% in a synchronous chain give about 99.7%. That's an argument for async decoupling and fewer hops."

### B4. RTO vs RPO?
🎤 "**RPO** is how much data you can afford to lose (time since the last recoverable point). **RTO** is how long until service is restored. RPO drives the replication and backup strategy; RTO drives the failover strategy. Booking had RPO under a minute (synchronous standby or continuous WAL archiving) and RTO under 30 minutes. Analytics could tolerate RPO of hours."

### B5. What does the CAP theorem say?
🎤 "In a distributed data store, when a **network partition** happens you must choose **consistency** (every read sees the latest write, or errors) or **availability** (every request gets a non-error response, possibly stale). Partition tolerance isn't optional in real networks, so the real choice is CP vs AP **during partitions**, and it can differ per data type."

### B6. Horizontal vs vertical scaling?
🎤 "Vertical means a bigger machine: simple, but limited and a single point of failure. Horizontal means more instances: needs **stateless services**, load balancing, and partitioning for state. Stateless app tiers scale horizontally easily. Databases are the hard part: read replicas, partitioning, sharding, or managed distributed databases."

### B7. Why P95/P99 rather than average latency?
🎤 "Averages hide the tail. If P99 is 4 seconds, 1 in 100 requests is terrible. And a page that makes 50 backend calls hits that tail on most page loads. Users feel the tail. I set SLOs on P95/P99 and test at peak load, which is how we reported ticketing's checkout improvement: P99 4.2 s to 240 ms."

### B8. Availability vs reliability vs resilience?
🎤 "**Availability**: the proportion of time the service is usable. **Reliability**: it performs correctly over time (low failure rate). **Resilience**: the ability to absorb failures and recover gracefully. You build availability mostly *through* resilience: redundancy, isolation, fast recovery."

### B9. What is graceful degradation?
🎤 "When a dependency fails, serve a reduced but useful experience instead of an error. Examples: cached search results, recommendations hidden, scheduler jobs deferred via Kafka instead of failing, an AI copilot falling back to plain runbook search. Decide the degraded modes in design, not during the incident."

---

## Medium

### M1. How do you make NFRs testable and enforce them? ✅
🎤 (~1.5 min) "I write each one as a **quality-attribute scenario**: source, stimulus, environment, response, measure. For example: 'At 3,500 booking requests per second during the morning peak, 99% of checkouts complete in under 1 second, with zero double-bookings.' Then each NFR gets a **verification method**: load tests (JMeter or Gatling at 1.5× peak), chaos experiments, DR drills, security scans, static-analysis gates. A **traceability matrix** in the SAD maps NFR → architectural tactic → test → production SLI. Enforcement is automated wherever possible: performance budgets in CI, **architecture fitness functions** (ArchUnit dependency rules, API breaking-change checks), and SLO dashboards with burn-rate alerts. Non-negotiable NFRs block release. Others raise a risk entry in the SARR."

### M2. Error budgets and burn-rate alerting?
🎤 (~1.5 min) "With a 99.9% SLO over 30 days, the error budget is 0.1%, about 43 minutes of 'bad'. **Burn rate** is how fast you're consuming it: a burn rate of 1 uses exactly the budget over the window, while 14.4 would use 2% of the monthly budget in one hour. I use **multi-window, multi-burn-rate alerts**: page on a fast burn (for example 14.4× over 1 hour *and* 5 minutes), ticket on a slow burn (for example 1× over 3 days). That cut alert noise significantly versus static thresholds. Policy: if the budget is exhausted, feature releases pause and reliability work takes priority. That's agreed in advance with the product owner."

```yaml
- alert: SchedulerTriggerSLOFastBurn      # SLO: 99.9% triggers fired within 5s
  expr: |
    (sum(rate(trigger_late_total[1h])) / sum(rate(trigger_total[1h]))) > (14.4 * 0.001)
    and
    (sum(rate(trigger_late_total[5m])) / sum(rate(trigger_total[5m]))) > (14.4 * 0.001)
  labels: { severity: page }
```

### M3. Explain PACELC and why it's more practical than CAP.
🎤 (~1 min) "PACELC: if there's a **P**artition, choose **A** or **C**; **E**lse (normal operation), choose **L**atency or **C**onsistency. Partitions are rare, but the latency vs consistency trade-off is constant. DynamoDB and Cassandra default to PA/EL (eventually consistent reads, with strong reads optional at higher latency). Spanner and single-primary Postgres are PC/EC. In ticketing, search was EL (cached, fast, slightly stale) and booking was EC (paid the latency to read the primary)."

### M4. Consistency models you design with?
🎤 (~1 min) "**Strong / linearizable**: reads see the latest write (seat holds). **Sequential/causal**: respects cause and effect (a reply never appears before the post). **Read-your-writes**: a user sees their own changes, needed after 'pause job' in the admin console. **Monotonic reads**: never go back in time. **Eventual**: converges eventually (search projections, notifications). I specify the required model **per use case** in the HLD, because 'eventual everywhere' and 'strong everywhere' are both wrong."

### M5. Designing HA within a region on Kubernetes/OpenShift or AWS ✅
🎤 (~1.5 min) "**Compute**: at least 3 replicas spread across **availability zones** with topology spread constraints, **PodDisruptionBudgets**, readiness and liveness probes, graceful shutdown, and HPA. **State**: managed or replicated databases with automatic failover (Aurora Multi-AZ or Postgres with Patroni), Redis with replicas or cluster mode, Kafka with replication factor 3, `min.insync.replicas=2` and rack awareness across AZs. **Stateless services** so any pod can serve any request. **No single points of failure**: load balancers across zones, multiple NAT gateways. **Isolation**: bulkheads and circuit breakers. And **test it**: kill pods and nodes, fail over the database in game days. The scheduler ran clustered Quartz across OpenShift pods. Losing a pod meant another node picked up its triggers via the DB locks, which is how we kept 99.99%."

### M6. DR strategies, and how you choose?
🎤 (~1 min)

| Strategy | RTO / RPO | Cost | Use |
|---|---|---|---|
| Backup & restore | hours / hours | $ | Internal tools, analytics |
| Pilot light | tens of min / minutes | $$ | Core data replicated, compute off |
| Warm standby | minutes / seconds–minutes | $$$ | Booking, scheduler |
| Active-active multi-region | ~0 / ~0 (per data model) | $$$$ | Global, revenue-critical, with conflict strategy |

"Choose by the business cost of downtime and data loss. And **drill it**: an untested DR plan is a hope, not a plan."

### M7. Capacity planning: do the math for the ticketing peak.
🎤 (~1.5 min) "About 1M users a day. The peak is concentrated in a morning window: around 3,500 booking requests per second at peak, and search at roughly 10–20× that. If one booking pod handles around 250 req/s at our P99 target (measured by load test, not assumed), we need 3,500 / 250 = 14 pods, ×1.5 headroom ≈ 21, spread over 3 AZs, so 7 per AZ. Database writes: each booking is around 3–4 writes including outbox, so roughly 12–14k writes/s at peak. That's why holds went to Redis and only confirmations hit Postgres. Kafka: around 4 events per booking ≈ 14k msgs/s; at ~1 KB each that's about 14 MB/s, which is modest. Partitions are sized for consumer parallelism (for example 24–48), not raw throughput. Then I validate with a load test at 1.5× and keep a scaling runbook."

### M8. Caching strategies and their failure modes?
🎤 (~1.5 min) "**Cache-aside** (app reads cache, falls back to DB, populates): the most common, and what the scheduler used with Redis. **Read-through/write-through** via a cache provider. **Write-behind** (fast writes, risk of loss). **Refresh-ahead**. Failure modes: **stampede** when a hot key expires (mitigate with single-flight, `refreshAfterWrite`, jittered TTLs); **stale data** (TTL plus event-driven invalidation, which is what we did with Redis pub/sub); **cache penetration** from requests for non-existent keys (negative caching, bloom filters); **avalanche** from a mass expiry or cache node loss (jitter, replicas, a circuit breaker to the DB); and **cold start** after deploys (warm-up). Also **L1 + L2** with Caffeine and Redis, and the CDN and browser caches for static schedule data in ticketing."

### M9. Database scaling options in order?
🎤 (~1 min) "**One**, fix queries and indexes (EXPLAIN ANALYZE): cheapest and biggest win. **Two**, connection pooling and right-sizing (PgBouncer, Hikari). **Three**, caching. **Four**, read replicas for read-heavy traffic (watch replication lag; read-your-writes). **Five**, partitioning (time- or key-based) to keep indexes small and enable partition pruning, as in the CRM engine. **Six**, vertical scaling. **Seven**, sharding or a distributed database: the most complex, so it's last. Also **CQRS** to move reads to a fit-for-purpose store."

### M10. How do you run a load test that actually tells you something? ✅
🎤 (~1.5 min) "**Workload model from production**: request mix, think times, data distributions (hot trains), peak shape. **Environment** close to production in size and config, with realistic data volumes, since an empty DB lies. **Test types**: baseline, load at expected peak, **stress** to find the breaking point, **soak** for 8+ hours to find leaks and pool exhaustion, and **spike** tests. **Observe everything at once**: client-side percentiles plus server metrics (CPU, GC, pools, DB waits, Kafka lag). **Change one thing at a time** and compare runs. JMeter was my tool, with Lighthouse for the front end. Gatling and k6 are good alternatives with code-based scenarios that live in the repo. Run a smaller performance smoke test in CI to catch regressions."

### M11. Frontend performance as an NFR ✅
🎤 (~1 min) "I use **Core Web Vitals**: LCP under 2.5 s, INP under 200 ms, CLS under 0.1, measured with **Lighthouse** in CI and real-user monitoring in production. Tactics: code splitting and lazy routes, image optimization, caching and a CDN, SSR or static generation where SEO and first paint matter (Next.js), avoiding render-blocking requests, memoization only where profiling shows re-render cost, and virtualized long lists. In ticketing, Lighthouse showed the booking dashboard bundle was too big. Splitting it was part of the end-to-end fix."

### M12. Observability: logs, metrics, traces. What's your standard?
🎤 (~1 min) "**Metrics** (Micrometer → Prometheus) for SLIs and alerting: RED (rate, errors, duration) for services, USE (utilization, saturation, errors) for resources. **Traces** (OpenTelemetry) to find where time goes across services. **Logs** as structured JSON with traceId, tenant and user (hashed), centralized in Loki or ELK. Dashboards per service with golden signals, alerts on SLO burn, and runbooks linked from alerts. That's the stack I ran on getConsenso: Prometheus, Loki, Grafana."

---

## Advanced

### A1. Multi-region design: what's CP, what's AP? ✅
🎤 (~2 min) "First, **data classification by consistency need**. Using ticketing as the example. **Seat inventory**: must not double-sell, so CP. Options: **single-writer region per train or route** (home-region ownership), with other regions forwarding writes there, or a consensus-based distributed SQL database (Spanner, CockroachDB, Aurora DSQL-style) that pays cross-region latency on writes. During a partition, the non-owner region rejects or queues bookings for that train: available for search, not for booking. **Schedules, train positions, search**: AP. Replicate asynchronously, serve locally, accept seconds of staleness. **User profiles and preferences**: AP with last-writer-wins or merge, since conflicts are rare and cheap. **Payments**: anchored to the region of the payment provider, idempotent, with reconciliation.

Then the **traffic layer**: latency-based or geo DNS (Route 53), health checks, regional failover runbooks. **Data layer**: Aurora Global Database or DynamoDB global tables for the AP data, accepting their conflict semantics. And **test failover** regularly. The architect's job is to make these trade-offs **explicit per data set** in the SAD, rather than claiming the whole system is 'highly available and consistent'."

### A2. End-to-end performance tuning case: from symptom to proof ✅
🎤 (~2 min) "The scheduler case, step by step. **Symptom**: state-transition API P99 of 680 ms during batch windows; CPU fine. **Hypothesis**: contention, not compute. **Evidence**: thread dumps showed request threads waiting on Hikari connections; DB-side `pg_stat_activity` showed sessions waiting on row locks; `pg_locks` confirmed `FOR UPDATE` holds lasting the whole trigger evaluation and validation. Repeated reads of job metadata also showed up as the top queries in `pg_stat_statements`. **Fixes**: one, narrow the lock to the atomic state acquisition (claim, transition, commit), with validation and downstream calls outside the transaction. Two, cache job definitions and cron metadata in Redis with a ~10-minute TTL plus pub/sub invalidation, which removed ~80% of reads. Three, a lock timeout so waits fail fast. **Proof**: re-ran the identical JMeter profile: P99 680 → 210 ms, lock waits down ~75%, about 3× concurrent execution capacity. **Prevention**: dashboards on lock wait time and pool pending, and a performance test in the release pipeline."

### A3. Define NFRs as architecture fitness functions in CI
🎤 (~1.5 min) "A fitness function is an automated check that an architectural characteristic still holds. Examples I use: **dependency rules** (ArchUnit: domain doesn't depend on Spring; no cycles between modules); **API compatibility** (OpenAPI diff fails on breaking changes); **performance budgets** (Gatling smoke test: P95 under X at Y rps on every merge to main; Lighthouse budget for bundle size and LCP); **security** (dependency CVE threshold, container image scan, secret scan); **resilience** (every outbound client has a timeout, checked by a custom ArchUnit rule or lint); **operability** (every service exposes health and metrics endpoints). The NFRs stop being a document and become part of the build."

```java
@ArchTest
static final ArchRule domain_is_framework_free =
    noClasses().that().resideInAPackage("..domain..")
               .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");
```

### A4. Design for 10× growth: what changes?
🎤 (~1.5 min) "I look for the **first thing that breaks**, using the capacity model. Usually: the **database write path** (partitioning, moving hot contended data to purpose-built stores, CQRS); **synchronous fan-out** (move to async, cache, collapse requests); **single-partition hot keys** (re-key, split hot entities); **operational limits** like connection counts, Kafka partition counts, and cloud quotas; and **cost**, since linear cost growth may be unacceptable. I'd also revisit **organizational scaling**: team boundaries aligned to services, a platform team, paved roads. I present it as a phased plan with triggers: 'at 2× we add read replicas; at 5× we partition bookings by date'. Don't build for 10× on day one, but don't block it."

### A5. Chaos engineering: how do you introduce it safely?
🎤 (~1 min) "Start with a **steady-state hypothesis** tied to SLIs. Run experiments in staging first, then in production with a small **blast radius** and an abort switch. Typical experiments: kill pods or nodes, inject latency into a dependency (to verify breakers and timeouts), fail over the DB, take a broker down, expire certificates, exhaust disk. Tools: Chaos Mesh, Litmus, AWS FIS. Every finding becomes a fix plus a regression experiment. The scheduler's dual-channel failover was validated by injecting downstream latency and checking that jobs moved to Kafka without loss."

### A6. How do you balance consistency, latency and cost in a data-store decision?
🎤 (~1 min) "I write the access patterns first (queries per second, read/write ratio, item size, consistency need, latency target, growth), then evaluate candidates on a matrix: fit to access patterns, consistency model, operational burden, cost at 1× and 10×, team skills, and ecosystem. The decision and its triggers go in an ADR. Often the answer is polyglot: Postgres for transactional truth, Redis for holds and caches, a search engine for discovery, object storage plus a table format for analytics."

### A7. What is tail-latency amplification and how do you fight it?
🎤 (~1 min) "If a request fans out to N services, the slowest one dominates. With 100 sub-calls, even a 1% slow rate per call makes most requests slow. Tactics: reduce fan-out (aggregate data, CQRS read models), **hedged requests** (send a second request after the P95 delay to another replica, only for idempotent reads), timeouts with partial results, request coalescing, and keeping hot paths in memory."

### A8. NFR decisions that could have been better ✅
🎤 (~1 min) "**One SLO for the whole scheduler** was too coarse. A payment batch job and a nightly report don't deserve the same objective; per-tier SLOs would have focused effort better. **Load tests with synthetic uniform data** missed the hot-train skew in ticketing; we added production-shaped data after an incident. And **DR drills were annual**. Quarterly, smaller drills would have surfaced runbook drift earlier."
