# 01: STAR Stories Mapped to Every EPAM JD Expectation

> Each JD responsibility gets a **spoken answer (1.5–2 min)**, a **STAR table**, and a **"What I'd do better"** line. The last item matters: EPAM panels reward self-critique, because it signals architectural maturity.
>
> **Legend:** ✅ = backed by a resume bullet · 🟡 = reasonable extension of a resume project. **Confirm it's true before you claim it**, or reword it as "how I would".
>
> **Canonical numbers** (keep these identical across every answer; they match [interview_prep/epam_technical_architect_50_master_qa.md](../interview_prep/epam_technical_architect_50_master_qa.md)):
>
> | Project | Numbers |
> |---|---|
> | Train ticketing | ~1M users/day, peak ~3,500 booking req/s, Redis 7-min seat hold, checkout P99 4.2 s → 240 ms, 99.99% inventory accuracy |
> | Job scheduler | Quartz on OpenShift, P99 state transition 680 ms → 210 ms, Redis metadata TTL 10 min, ~80% DB reads removed, 99.99% uptime |
> | Morgan Stanley CRM | 12M interaction records, 6 servers, 7,000 interactions/s, 4× batch-write throughput, micro-batch 500 rows / 50 ms |
> | getConsenso | 850 signed-up users, 50k+ monthly visitors, OpenAPI scanning, Java/TS/Go/Kotlin stubs, tenant-aware GDPR audit |

## Index

| # | JD expectation | Story |
|---|---|---|
| S1 | End-to-end architecture & solution blueprints | Centralized Job Scheduler blueprint |
| S2 | Translate business requirements into technical solutions | Train ticketing passenger flow |
| S3 | Enterprise-grade Java application design | Morgan Stanley CRM ingestion engine |
| S4 | Architecture discussions, technology selection, technical governance | Whiteboarding, RFPs, ADRs (Quartz vs alternatives) |
| S5 | C4 / 4+1 architectural modelling | Scheduler modelled in C4 + 4+1 |
| S6 | HLD, LLD, SAD, SARR, deployment/integration/security architectures | Ticketing document set |
| S7 | Distributed / event-driven design, modularization, decomposition | Monolith → microservices pathway |
| S8 | Circuit breaker | Scheduler dual-channel HTTP + Kafka |
| S9 | Saga | Ticketing booking Saga |
| S10 | Choreography vs orchestration | Booking (choreography) vs scheduler chains (orchestration) |
| S11 | 2PC / 3PC trade-offs | Why XA was rejected for payment + inventory |
| S12 | CQRS | Seat search read model vs booking write model |
| S13 | Application modernization | Legacy assessment → AI-enabled architecture |
| S14 | NFR definition & enforcement | NFR catalogue for ticketing |
| S15 | SLA vs SLO | Scheduler SLOs and the client SLA |
| S16 | CAP theorem | Seat inventory (CP) vs live train tracking (AP) |
| S17 | HA, fault tolerance, performance tuning | End-to-end diagnosis: Lighthouse → JMeter → EXPLAIN ANALYZE → JVM |
| S18 | REST, event-driven, streaming integration | Kafka → Spark → Hadoop/AWS lakehouse; socket streaming |
| S19 | API management, governance, EIP | getConsenso |
| S20 | Third-party / SaaS integration | Payment gateways, Factiva, GitHub/Jira/Linear |
| S21 | Security & compliance | AML platform, PKCS#11/JCA, GDPR audit |
| S22 | IAM, OAuth2, OIDC, JWT, SSO | OAuth2/JWT + RBAC on Kafka microservices |
| S23 | Hands-on coding, POCs, framework components | Circuit-breaker SDK, scheduler core, RAG agent |
| S24 | Production-grade AI (MLOps/LLMOps, serving, secure integration) | RAG Document Q&A agent → Scheduler Copilot |
| S25 | Mentoring, standards, code reviews, design assessments | Remote teams, OpenHands quality gates |
| S26 | Resolve critical challenges / bottlenecks / emerging tech adoption | 4× CRM batch writes |

---

## S1. End-to-end application architecture & solution blueprints ✅

**Likely prompt:** *"Walk me through an end-to-end solution blueprint you owned."*

### 🎤 Spoken answer (~2 min)
"The most recent blueprint I owned end-to-end is our **centralized job scheduler** at Bahwan CyberTek. Before it existed, every product team ran its own cron jobs and ad-hoc schedulers. There was duplicate execution after failovers, no audit, and nobody could answer 'which jobs failed last night and why?'.

I started with the **business capabilities**: three job archetypes (fire-and-forget, recurring cron, and chained multi-step jobs), plus operations like pause, resume, stop and delete, with full audit. Then I wrote the **NFRs** up front: 99.99% scheduler availability, no duplicate execution across pods, state-transition P99 under 250 ms, and safe behaviour when downstream services are slow.

The blueprint had five layers. A **React admin console** and REST APIs, contract-first in OpenAPI. A **scheduling core** on Spring Boot with Quartz in clustered mode on PostgreSQL, deployed across OpenShift pods. A **state machine** for job lifecycle with pessimistic locking only at the state-acquisition step. An **integration layer** with a dual channel: synchronous REST normally, switching at runtime to Kafka when a circuit breaker opens. And **cross-cutting concerns**: OAuth2/JWT with RBAC, Redis caching of job metadata, and OpenTelemetry into Prometheus and Grafana.

I documented it with C4 diagrams, an HLD and LLD, and ADRs for each major choice, like Quartz over Spring `@Scheduled`, and Kafka as the fallback channel. I also built the first vertical slice myself as a reference implementation.

The result: P99 state transitions went from 680 to 210 milliseconds, uptime stayed at 99.99%, and teams onboarded jobs through configuration instead of writing their own schedulers."

| STAR | |
|---|---|
| **S** | Fragmented per-team schedulers; duplicate runs after failover; no central audit |
| **T** | Design and lead a shared, multi-team scheduling platform |
| **A** | Capability map → NFRs → layered blueprint (UI, core, state machine, dual-channel integration, cross-cutting) → C4/HLD/LLD/ADRs → hands-on reference slice |
| **R** | P99 **680 → 210 ms**, **99.99%** uptime, central audit, config-based onboarding |

**What I'd do better:** "I'd introduce a durable workflow engine such as **Temporal** for the chained, long-running jobs from day one. We hand-built checkpointing and retries that Temporal gives you out of the box. I'd also have defined SLOs per job tier earlier, instead of a single platform SLO."

---

## S2. Translating business requirements into scalable technical solutions ✅

**Likely prompt:** *"Give an example where you converted fuzzy business needs into a concrete architecture."*

### 🎤 Spoken answer (~2 min)
"For the train ticketing platform, the business ask was simple to say and hard to build: 'Passengers must be able to book during the morning rush without double-booking, and payments must never lose a seat or charge without a ticket.'

I turned that into **measurable requirements** with the product owner. About a million users a day, peaks of around 3,500 booking requests a second, checkout under one second at P99, **zero double-bookings**, and seat holds released automatically if payment doesn't complete.

Then I **mapped the user journey to bounded contexts**: Search, Seat Inventory, Booking, Payment, Invoicing and Notifications, each with its own data. The critical design decision came from the 'never double-book, never lose a seat' requirement. The original design used database row locks across the whole checkout, which collapsed under peak load. I replaced that with a **provisional seat hold in Redis with a 7-minute TTL**, and an **event-driven Saga on Kafka**: SeatReserved, then payment, then either BookingConfirmed or a compensating SeatReleased. A background expiry job freed abandoned holds.

I validated the design with the business using **failure scenarios rather than diagrams**: 'what happens if the bank times out after debiting?'. That's how we added payment reconciliation and idempotent payment callbacks.

The outcome was zero double-bookings, 99.99% inventory accuracy, and checkout P99 down from 4.2 seconds to 240 milliseconds."

| STAR | |
|---|---|
| **S** | Morning-rush booking failures; lock contention; P99 4.2 s |
| **T** | Convert "no double-booking, no lost seats" into an architecture |
| **A** | Quantified NFRs → bounded contexts → Redis TTL holds + Kafka Saga + compensations → failure-scenario walkthroughs with business |
| **R** | **0** double-bookings, **99.99%** accuracy, P99 **4.2 s → 240 ms** |

**What I'd do better:** "I'd add a **virtual waiting room** (queue-based admission) for extreme peaks such as festival booking openings, rather than relying only on scaling. I'd also publish the event contracts to a schema registry from day one; we added it later after a breaking change."

---

## S3. Designing enterprise-grade Java applications ✅

**Likely prompt:** *"Tell me about the most demanding Java system you designed."*

### 🎤 Spoken answer (~2 min)
"At Genpact on the Morgan Stanley CRM platform, I worked on the ingestion engine that auto-logged client interactions (emails, SMS, phone calls and chat) into the CRM. Volume was around 12 million interaction records across six servers, and during market volatility the inbound rate spiked past what the engine could write.

The bottleneck was clear from profiling: **row-by-row synchronous JDBC inserts** plus blocking queues meant threads spent most of their time waiting on locks and database round trips, and memory pressure built up during peaks.

I redesigned the pipeline using core Java concurrency. **Bounded, non-blocking ring buffers** (Disruptor-style) replaced synchronized blocking queues, with dedicated consumer threads. **Adaptive micro-batching** flushed to the database when a batch reached 500 records or 50 milliseconds passed, whichever came first, using JDBC batch writes. **Database partitioning** spread writes and kept indexes small. And because it was regulated capital-markets data, every record had to be **audit-ready**: idempotent writes keyed by interaction ID, so retries never duplicated data.

The result was a **4× batch-write throughput improvement**, sustaining about **7,000 interactions per second** in real time, with zero production defects over that period.

What this taught me as an architect is to **measure before you redesign**. The fix wasn't more servers. It was removing coordination: fewer locks, fewer round trips, and bounded memory."

| STAR | |
|---|---|
| **S** | Ingestion spikes during market volatility; row-by-row inserts; lock contention |
| **T** | Sustain real-time ingestion on the same 6 servers |
| **A** | Profiling → ring buffers → adaptive micro-batching (500 / 50 ms) → DB partitioning → idempotent writes |
| **R** | **4×** throughput, **7,000/s**, **0** production defects |

**What I'd do better:** "Today I'd put **Kafka** in front of the writers for durability and replay. An in-memory buffer loses data if a JVM crashes before flush. We mitigated that with source re-reads, but a log is cleaner. And a small correction for anyone copying the config: for PostgreSQL the batch flag is `reWriteBatchedInserts=true`; `rewriteBatchedStatements` is the MySQL equivalent."

---

## S4. Driving architecture discussions, technology selection & technical governance ✅

**Likely prompt:** *"How do you run technology selection and keep governance light but effective?"*

### 🎤 Spoken answer (~2 min)
"At Bahwan CyberTek I lead whiteboarding sessions, design reviews, scoping and RFP or pre-sales work with business stakeholders. The outputs are ADRs and SRS documents that drive the technology decisions.

My selection process has four steps. **First, frame the decision with drivers**: the NFRs and constraints that actually matter. For the scheduler that was clustering without duplicates, persistence of triggers, misfire handling, team skills, and running on OpenShift. **Second, shortlist two or three options and score them** on a weighted matrix. For scheduling we compared Spring `@Scheduled`, Quartz in clustered mode, and a workflow engine. **Third, time-box a spike.** I build the riskiest slice myself, usually in two or three days, so the decision is based on evidence, not opinion. **Fourth, record an ADR**: context, options, decision, consequences, and the conditions under which we'd revisit it.

We chose Quartz clustered on PostgreSQL. It met the clustering and misfire requirements, the team knew it, and it needed no new infrastructure. The ADR explicitly said 'revisit if we need long-running stateful workflows', which is exactly what happened later with the AI agent workflows.

For governance, I keep it **lightweight and automated**: architecture principles on one page, ADRs in the repo, a design review only for changes that cross service boundaries or touch NFRs, and **fitness functions in CI**, such as contract tests, dependency rules and performance budgets, so standards are enforced by the pipeline, not by meetings."

| STAR | |
|---|---|
| **S** | Multiple teams, competing opinions on scheduler technology |
| **T** | Make a defensible, durable decision quickly |
| **A** | Decision drivers → weighted matrix → hands-on spike → ADR with revisit triggers → automated fitness functions |
| **R** | Decision in **~1 week**; ADR's revisit trigger correctly anticipated the later Temporal adoption |

**What I'd do better:** "Involve operations/SRE in the scoring earlier. Operability was under-weighted in the first matrix."

---

## S5. Architectural modelling with C4 and 4+1 ✅🟡

**Likely prompt:** *"How do you communicate architecture to different audiences?"*

### 🎤 Spoken answer (~1.5 min)
"I use **C4** as my default because it maps cleanly to audiences. For the scheduler: a **System Context** diagram for business stakeholders, showing the scheduler, its users (ops engineers, product teams), and external systems (downstream microservices, Kafka, IdP, monitoring). A **Container** diagram for architects and DevOps: React console, scheduler API, Quartz workers, PostgreSQL, Redis, Kafka, the observability stack. A **Component** diagram for the core team: trigger manager, state machine, channel selector, circuit breaker, audit publisher. And **Code** level only where it adds value, like the state machine.

I complement C4 with **4+1 views** when the panel or the client's enterprise architects expect it. The **logical** view is the domain model and components. The **process** view shows concurrency: how clustered pods acquire triggers with row locks. The **development** view is the module structure and build. The **physical** view is OpenShift deployment across nodes and zones. And the **+1, scenarios**, ties them together; 'a pod dies mid-job' was our key scenario.

I keep diagrams as code with Structurizr DSL or Mermaid in the repo, so they're versioned with the system and reviewed in pull requests, not left to rot in a slide deck."

**What I'd do better:** "Generate container diagrams partly from deployment manifests to detect drift automatically."

> Full C4 + 4+1 walkthrough with diagrams-as-code: see [10_architecture_modelling_governance.md](10_architecture_modelling_governance.md).

---

## S6. Architecture artifacts: HLD, LLD, SAD, SARR, deployment / integration / security architectures ✅🟡

**Likely prompt:** *"What artifacts do you produce, and what goes in each?"*

### 🎤 Spoken answer (~2 min)
"For the ticketing platform I produced a document set, each piece with a different audience.

The **SRS** captured functional and non-functional requirements, signed off with the business. The **Solution Architecture Document** was the master document: business context, architecture drivers, C4 views, key decisions with ADR links, the NFR traceability matrix, risks, and the roadmap. The **HLD** described services, their responsibilities, data ownership, sync and async interactions, and technology choices. The **LLD** went per service: API contracts in OpenAPI, event schemas, data models, sequence diagrams for the booking Saga including failure paths, and the state machines.

Then three specialised views. The **deployment architecture** covered clusters, namespaces, zones, scaling policies, and CI/CD with rollback. The **integration architecture** listed every interface with protocol, pattern, SLA, retry and idempotency policy: payment gateways, SMS and email providers. The **security architecture** covered the trust boundaries, OAuth2 flows, token lifetimes, encryption at rest and in transit, secrets management, and the threat model.

Finally the **Solution Architecture Review Report (SARR)**. That's the output of a formal review against the requirements and principles: findings, risks with severity, waivers, and actions with owners. It's what the client's architecture board signs off before go-live.

The rule I follow is that each document must answer **one audience's questions**, and anything that changes often lives as code next to the system."

**What I'd do better:** "Replace long Word documents with a lean SAD and living docs-as-code; on earlier projects our LLDs drifted from reality within two sprints."

---

## S7. Distributed / service-oriented / event-driven design, modularization & decomposition ✅

**Likely prompt:** *"How do you decide how to split a system into microservices?"*

### 🎤 Spoken answer (~2 min)
"I've designed migration pathways from monoliths to microservices. My rule is: **decompose by business capability and data ownership, not by technical layer**.

I start with **event storming** with domain experts to find the bounded contexts. Then for each candidate service I check four things. Does it own its data? Can it be deployed independently? Does it have a different scaling or change profile? Can a single team own it?

In the ticketing platform, Search had a very different profile from Booking: 100 times the read traffic, tolerant of slight staleness, cache-friendly. So it became its own service with its own read-optimized store. Booking and Seat Inventory needed strong consistency for a seat, so they were carefully bounded and communicated through events. Notifications were naturally asynchronous.

For legacy systems I use the **strangler-fig pattern**: put a façade or gateway in front, carve out one capability at a time behind it, synchronize data through change-data-capture or events, and retire the old path once traffic has moved. An **anti-corruption layer** protects the new model from legacy concepts.

I'm also honest that sometimes the right answer is a **modular monolith**: clear module boundaries enforced with ArchUnit, one deployable. It's ideal when the team is small or the domain isn't stable yet. Microservices buy independent deployment and scaling at the cost of distributed complexity, so you need the operational maturity to pay that cost."

**What I'd do better:** "On one migration we split by entity (customer service, order service) early on, which created chatty synchronous calls, a distributed monolith. We later merged services along capability lines. I now validate boundaries by counting cross-service calls per user journey before committing."

---

## S8. Circuit Breaker to prevent cascading failures ✅

**Likely prompt:** *"Tell me how you used a circuit breaker in production."*

### 🎤 Spoken answer (~2 min)
"In the scheduler, jobs trigger downstream microservices: billing runs, report generation, data syncs. During batch windows, if a downstream service slowed down, scheduler threads blocked waiting on it. Thread pools filled, and the **scheduler itself** started missing triggers for completely unrelated jobs. That's a classic cascading failure.

I added **Resilience4j circuit breakers** per downstream service, using a count-based sliding window with thresholds on both **failure rate** and **slow-call rate**. Slow calls matter as much as errors. When the breaker opens, calls fail fast instead of tying up threads. After the wait duration it goes **half-open**, lets a few trial calls through, and closes again if they succeed.

The key design addition was the **dual channel**. Instead of failing the job when the breaker opens, the scheduler switches at runtime from synchronous REST to **publishing the job command on Kafka**. The downstream service consumes it when it recovers, so the work is deferred, not lost. Commands carry an **idempotency key**, so if a REST call timed out but actually succeeded, the Kafka redelivery doesn't run the job twice. We also put **bulkheads** around each downstream service's thread pool, so one slow dependency can't consume all the threads.

The result: downstream outages stopped cascading into the scheduler, uptime held at 99.99%, and no jobs were lost during incidents."

| STAR | |
|---|---|
| **S** | Slow downstream services exhausted scheduler threads → missed unrelated triggers |
| **T** | Contain failures and avoid losing jobs |
| **A** | Per-dependency Resilience4j breakers (failure + slow-call), bulkheads, runtime REST → Kafka switch, idempotency keys |
| **R** | **0** cascading incidents afterwards, **0** lost jobs, **99.99%** uptime |

**What I'd do better:** "Dual-channel adds two code paths to test. Today I'd make the integration **async-first** (outbox + Kafka) for jobs that don't need an immediate answer, and keep REST only for the few that do."

---

## S9. Saga for distributed transactions ✅

### 🎤 Spoken answer (~2 min)
"The booking flow spans Seat Inventory, Payment, Booking and Invoicing, each with its own database, so there's no single ACID transaction. I implemented a **Saga**: a sequence of local transactions where each step publishes an event, and each step has a **compensating action** if a later step fails.

The happy path: Booking creates a PENDING booking and a provisional seat hold in Redis with a 7-minute TTL, then publishes `SeatReserved`. Payment consumes it and calls the bank gateway. On success, it publishes `PaymentSucceeded`, Booking confirms, and Invoicing and Notifications react.

The failure paths are where the design work is. If payment fails, a dedicated consumer publishes `SeatReleased` and the seat returns to inventory. If the user abandons checkout, the **TTL expiry plus a background sweeper** frees the hold. If the bank **times out**, we don't guess. We mark the payment UNKNOWN and run **reconciliation** against the gateway before compensating, because releasing a seat that was actually paid for is the worst outcome.

Three properties made it reliable. **Idempotent consumers** keyed by booking ID, because Kafka redelivers. The **transactional outbox**, so a state change and its event can't diverge. And **semantic locking**: the PENDING state stops anyone else from booking that seat while the Saga is in flight.

The result was zero double-bookings and 99.99% inventory accuracy at peak."

| STAR | |
|---|---|
| **S** | Cross-service booking with no global transaction; lost seats and double-bookings under failure |
| **T** | Guarantee consistency across 4 services |
| **A** | Kafka Saga, compensations, TTL + sweeper, UNKNOWN-state reconciliation, idempotent consumers, outbox, semantic locks |
| **R** | **0** double-bookings, **99.99%** inventory accuracy |

**What I'd do better:** "Choreography got hard to follow once we added refunds and partial cancellations. For the refund flow I'd use an **orchestrated** Saga (Temporal or a state-machine orchestrator) so the whole flow is visible in one place."

---

## S10. Choreography vs orchestration ✅

### 🎤 Spoken answer (~1.5 min)
"I've used both, and I choose based on **how many steps there are and who needs to see the whole flow**.

The ticketing booking Saga was **choreographed**. Each service reacted to events on Kafka; no central coordinator. That fitted because there were few steps, the services were owned by different teams, and we wanted loose coupling and high throughput. The downside is that the flow is implicit. To answer 'where is booking 123 stuck?' you need distributed tracing and an event log, and adding a step means touching several services.

The scheduler's **chained jobs are orchestrated**. The scheduler is the orchestrator: it knows step 1, 2, 3, persists progress, retries, and decides what runs next. That fits because the sequence is the product. Users configure it, want to see it, and want to pause or resume it.

My rule of thumb: choreography for **short, stable flows across team boundaries**; orchestration for **long, complex or frequently changing flows**, or anywhere you need visibility, timeouts and human steps. Many systems end up hybrid: an orchestrator inside a bounded context, events between contexts."

**What I'd do better:** "Add a 'Saga monitor' read model (built from events) from day one for choreographed flows. Ops needed it within weeks of go-live."

---

## S11. Evaluating 2PC / 3PC ✅🟡

### 🎤 Spoken answer (~1.5 min)
"During the ticketing design, one proposal was to wrap the seat inventory update and the payment record in an **XA two-phase commit**. I evaluated it and rejected it, and I documented why in an ADR.

2PC gives atomicity across resources: a coordinator asks every participant to **prepare**, and if everyone votes yes, tells them to **commit**. The problems at our scale were these. Participants **hold locks** between prepare and commit, which is exactly the contention we were trying to remove at 3,500 requests a second. The coordinator is a **blocking single point of failure**: if it dies after prepare, participants sit in doubt holding locks. And **the payment gateway and Kafka don't take part in XA anyway**, so it couldn't cover the real boundary.

**3PC** adds a pre-commit phase so participants can time out and make progress, but it assumes bounded network delays. Under real network partitions it can still produce inconsistency, which is why it's mostly academic and rarely implemented in mainstream products.

So we used a Saga with compensations and idempotency, accepting brief **eventual consistency** with explicit states like PENDING.

Where would I still use 2PC? Inside a **single trusted boundary with low throughput**, for example a legacy app server writing to one database and one JMS queue, where XA is supported and correctness matters more than scale. Even there, the **outbox pattern** usually achieves the same thing with less risk."

---

## S12. CQRS ✅🟡

### 🎤 Spoken answer (~1.5 min)
"In ticketing, reads and writes had completely different shapes. **Seat search and availability** was about a hundred times the traffic of bookings, needed sub-100-millisecond responses, and could tolerate a second or two of staleness. **Booking writes** needed strict correctness on a single seat.

So we separated them, which is CQRS. The **command side** was the booking and inventory services on PostgreSQL, enforcing invariants. Every change published an event. The **query side** was a denormalized availability projection, built by consumers of those events and cached in Redis, with CDN and client caching for static schedule data, as on my resume.

The trade-off is **eventual consistency**: a seat can show as available in search for a moment after it's held. We handled that in the UX and the domain. The final check happens on the command side at hold time, so the worst case is a 'seat just taken, pick another' message, never a double-booking.

I'm careful not to over-apply CQRS. It adds moving parts: projections, replays, lag monitoring. I use it where **read and write loads or models genuinely diverge**, not by default. And CQRS doesn't require event sourcing. We used ordinary state-based tables on the write side."

**What I'd do better:** "Expose projection lag as a first-class SLO and show 'availability as of N seconds ago' in the UI during peaks."

---

## S13. Application modernization ✅

### 🎤 Spoken answer (~2 min)
"I assess legacy systems and recommend modernization strategies, including paths to AI-enabled architectures. I use a structured approach rather than defaulting to 'rewrite in microservices'.

**Step one is assessment** across four lenses. Business value and change frequency: which capabilities change monthly versus never? Technical health: code quality, test coverage, coupling, outdated frameworks. Operational pain: incidents, slow deployments, scaling limits. And risk: security, end-of-life dependencies, key-person knowledge.

**Step two is choosing a strategy per capability** using the 6R/7R framework: retain, retire, rehost, replatform, refactor, re-architect or replace with SaaS. Not everything deserves a rewrite. In one assessment we **replatformed** stable batch modules into containers on OpenShift as-is, **re-architected** the high-change booking capability into services, and **retired** two reports nobody used.

**Step three is incremental execution**: strangler fig behind a gateway, characterization tests before touching legacy code, CDC or events to sync data during the transition, and feature flags to switch traffic gradually with rollback.

For **AI enablement**, the modernization goal is often just to expose clean APIs and events and a good data foundation, so AI features like RAG assistants or agents can plug in safely. Our scheduler copilot was only possible because the scheduler already had clean APIs, audit events and structured telemetry."

**What I'd do better:** "Quantify the business case up front (cost of delay, incident cost) so modernization isn't the first thing cut when budgets tighten."

---

## S14. Defining & enforcing NFRs ✅🟡

### 🎤 Spoken answer (~1.5 min)
"I treat NFRs as **testable requirements with numbers and owners**, not adjectives. For ticketing, the NFR catalogue had entries like these. **Scalability**: 3,500 booking requests a second at peak, linear scaling to 2× with no redesign. **Performance**: checkout P99 under one second, search P95 under 200 milliseconds. **Availability**: 99.95% for booking, measured monthly. **Recoverability**: RPO under one minute, RTO under 30 minutes. **Consistency**: zero double-bookings. **Security**: OAuth2, PCI-scope minimisation, encryption in transit and at rest. **Maintainability**: deploy any service independently, under 30 minutes from merge to production.

Each NFR had **a way to verify it**: load tests in JMeter at 1.5× peak before every major release, chaos drills for availability, DR drills for RTO and RPO, static analysis and coverage gates for maintainability. I mapped each NFR to the architectural tactics that satisfy it in a **traceability matrix** inside the SAD. For example, 'zero double-bookings' maps to Redis TTL holds, the Saga, and idempotent consumers.

Enforcement is automated wherever possible: performance budgets in CI, SLO dashboards in production, and an architecture review whenever a change touches an NFR."

---

## S15. SLAs vs SLOs ✅🟡

### 🎤 Spoken answer (~1.5 min)
"An **SLI** is the measurement, for example the percentage of job triggers fired within 5 seconds of their scheduled time. An **SLO** is our internal target for that SLI, say 99.95% over 30 days. An **SLA** is the external, contractual promise, usually looser, say 99.9%, with consequences such as service credits if we miss it.

For the scheduler we defined SLIs on what users actually feel: trigger punctuality, state-transition latency (P99 under 250 ms), API availability, and job completion success excluding downstream business failures. The SLOs were tighter than the SLA so we had a buffer, and the gap between them gave us an **error budget**.

The error budget drives behaviour. If we burned it fast, we paused feature releases and prioritised reliability. If we had plenty left, we could take more delivery risk. Alerting was based on **burn rate**, not raw thresholds, which cut alert noise considerably.

A common mistake is making SLA and SLO the same number. Then you have no early warning before you breach the contract."

---

## S16. CAP theorem in practice ✅

### 🎤 Spoken answer (~1.5 min)
"CAP says that during a **network partition** a distributed store must choose between consistency and availability. In the same ticketing platform I made **opposite choices for different data**.

**Seat inventory chose CP.** If a partition occurs, it's better to reject or delay a booking than to sell the same seat twice. Holds went through a strongly consistent path (the primary Redis and PostgreSQL), and if we couldn't confirm the hold, we returned 'try again' rather than guessing.

**Live train movement and schedule data chose AP.** Showing a train position that's 10 seconds stale is far better than showing nothing. That data flowed through the socket-based streaming layer with client and server caches and CDN clustering, so it stayed available even when parts of the backend were degraded.

I also use **PACELC** in discussions, because partitions are rare but latency trade-offs happen all the time. Even without a partition, you choose between latency and consistency. Search used cached, slightly stale data for low latency; booking paid the latency cost for consistency."

---

## S17. High availability, fault tolerance & performance tuning ✅

### 🎤 Spoken answer (~2 min)
"I diagnose production performance end-to-end with **Lighthouse, JMeter, EXPLAIN ANALYZE and JVM profiling**. Here's how one case played out.

Users reported a slow booking dashboard during peaks. **Lighthouse** showed the frontend was part of it: a large JavaScript bundle and render-blocking calls. We fixed that with code splitting and lazy loading. But the API was also slow, so I reproduced peak load with **JMeter** at about 1.5× production traffic and watched latency percentiles alongside thread and connection-pool metrics.

The API P99 climbed with load while CPU stayed moderate, which usually means **waiting, not working**. A **JVM thread dump and profiler** showed many threads blocked on database connections. **EXPLAIN ANALYZE** on the top query showed a sequential scan on a large bookings table, because a filter on status plus date had no suitable index, and an N+1 pattern from the ORM.

The fixes were a composite index on the filter columns, a fetch-join to remove the N+1, a right-sized connection pool, and caching of reference data. Then I re-ran the same JMeter profile to prove the improvement. That's important: no tuning claim without before-and-after numbers.

For **high availability** I design for failure: multiple replicas across zones, readiness probes with graceful shutdown, stateless services, database replicas with automated failover, and circuit breakers and timeouts at every dependency."

---

## S18. RESTful, event-driven & streaming architectures ✅

### 🎤 Spoken answer (~1.5 min)
"I've built all three styles and chose between them by interaction needs.

**REST** for request/response where the caller needs an answer now, contract-first in OpenAPI: the scheduler APIs and the booking APIs.

**Event-driven** on Kafka where services should react without coupling: the booking Saga and the scheduler's async channel.

**Streaming** for continuous data. At Bahwan CyberTek I designed **Apache Spark ELT pipelines streaming Kafka data into a Hadoop lakehouse** with HBase and Hive, later moved to AWS, with partitioning strategies on RDDs and DataFrames and tuned Spark SQL jobs for high-volume processing. We monitored health with Ambari, Grafana and CloudWatch. For live train movement I designed **Java socket-based real-time push** with client and server caching and CDN clustering.

The architectural point is matching the semantics. REST for commands needing immediate confirmation. Events for facts others may react to. Streams for continuous, high-volume data where you process windows and aggregates rather than individual requests."

**What I'd do better:** "For live train tracking today I'd use managed **WebSockets/SSE** behind an API gateway, or MQTT for device feeds, instead of raw Java sockets: less custom connection management. For the lakehouse, an open table format like **Apache Iceberg on S3** instead of HBase/Hive."

---

## S19. API management, governance & enterprise integration patterns ✅

### 🎤 Spoken answer (~2 min)
"API governance is close to my heart. I built **getConsenso**, a contract-first API governance SaaS, as founder and sole engineer.

The problem it solves is API drift: teams change APIs, consumers break, and breaking changes are found in QA or production. getConsenso scans **OpenAPI specs and GitHub repositories**, runs **automated contract validation and linting**, runs **reviewer approval workflows** before a contract change is accepted, and generates **client and server stubs in Java, TypeScript, Go and Kotlin**, so implementation follows the contract. It integrates with **GitHub, Jira and Linear**.

From that work, the governance standards I recommend for an enterprise are these. Contract-first design. A **style guide enforced by linting**, not documentation: naming, pagination, error format using RFC 9457 problem details, and versioning. **Breaking-change detection in CI.** A **deprecation policy** with sunset headers. And a central **API catalogue**. At runtime, an **API gateway** handles authentication, rate limits, quotas and analytics.

For integration I use **enterprise integration patterns** deliberately: content-based routing, message translators at boundaries, idempotent receivers, dead-letter channels, claim check for large payloads, and anti-corruption layers for legacy systems.

getConsenso grew to 850 signed-up users and over 50,000 monthly visitors. I iterated it from early feedback into a team-first model with developer, reviewer and admin roles."

---

## S20. Integrating with enterprise systems, third-party platforms & SaaS ✅

### 🎤 Spoken answer (~1.5 min)
"Three examples across my career. In the ticketing platform, **payment gateways**, where the challenges were unreliable responses and money correctness. We used idempotency keys on every payment request, signed callbacks, timeouts with an UNKNOWN state and reconciliation, and circuit breakers per gateway. On the AML platform at Detica, I built the **Watchlist and Scenario Manager** modules and integrated **Factiva** for sanctions and adverse-media screening, a regulated workflow where every external result had to be traceable for auditors. In getConsenso, **GitHub, Jira and Linear** integrations via OAuth apps and webhooks: verify webhook signatures, process asynchronously, de-duplicate deliveries, respect rate limits with backoff.

My standard checklist for any third-party integration: an **adapter or anti-corruption layer** so their model doesn't leak into ours; **timeouts, retries with jitter and circuit breakers**; **idempotency**; **contract tests** against their sandbox; **secrets in a vault**; and **observability** per partner, so we can show them evidence when their side fails."

---

## S21. Enterprise security standards & compliance ✅

### 🎤 Spoken answer (~1.5 min)
"Security and compliance have run through my career. At the **IBM Java Technology Centre**, I implemented security API integration for IBM Tivoli using **PKCS#11 and JCA/JCE** cryptographic APIs on z/OS. That's hardware security modules and the Java cryptography architecture, where key handling mistakes are unforgivable. At **Detica**, I built modules of a credit-card **anti-money-laundering** platform, a regulated domain with strict audit trails. At Morgan Stanley, data had to be **audit-ready**. And in getConsenso I designed **tenant-aware audit logging** with `tenant_id` and `api_key_id` for GDPR.

My approach for any solution is to identify the applicable frameworks early (GDPR, PCI-DSS for card data, SOC 2, ISO 27001 controls), then design controls into the architecture. That means data classification, encryption at rest with KMS and in transit with TLS, least-privilege IAM, secrets management, centralized audit logging, threat modelling with STRIDE, dependency and container scanning in CI, and evidence collection for auditors. **Compliance is easier when it's built into the pipeline, not bolted on before an audit.**"

---

## S22. Authentication & authorization: IAM, OAuth2, OIDC, JWT, SSO ✅🟡

### 🎤 Spoken answer (~2 min)
"I built event-driven services on Kafka with **OAuth2/JWT security and RBAC**. Here's how I structure an auth framework.

**Identity** comes from a central IdP, such as Keycloak, Okta, Azure AD or Cognito, using **OIDC** for user login and **SSO** across applications. For the React SPA we use the **Authorization Code flow with PKCE**. Ideally a **backend-for-frontend** holds tokens in httpOnly cookies, so tokens never sit in browser storage.

**APIs** are OAuth2 **resource servers** in Spring Security. They validate the JWT signature against the IdP's JWKS, check issuer, audience and expiry, and map scopes and roles to authorities. Access tokens are short-lived (5 to 15 minutes), with refresh-token rotation.

**Authorization** is layered: coarse **RBAC** with roles like operator and admin at the gateway and the endpoint, then fine-grained checks in the domain with `@PreAuthorize`, for example 'can this user pause jobs owned by this team?'. For multi-tenant systems, the tenant comes from a token claim and is enforced in the data layer, as I did with `tenant_id` audit in getConsenso.

**Service-to-service** uses the client-credentials grant or mTLS inside the mesh. When a service acts on behalf of a user, **token exchange** preserves the user's identity instead of using an all-powerful service account. On Kafka, producers and consumers authenticate with SASL/OAuth or mTLS, with topic-level ACLs.

On AWS, **IAM roles for service accounts** give pods cloud permissions without static keys."

---

## S23. Hands-on coding, POCs & core framework components ✅

### 🎤 Spoken answer (~1.5 min)
"I stay hands-on in code and reviews while owning the design decisions. A few concrete examples. In getConsenso I wrote a **circuit-breaker module for the contract SDK** with full Vitest coverage, plus the contract-validation engine and stub generators. For the scheduler I built the **reference vertical slice**: the state machine with lock-scoped transitions and the channel selector for REST/Kafka failover, which the team then extended. I built a **production RAG Document Q&A agent** with event-driven orchestration, step-level observability, retries, throttling and per-user rate limiting, containerised and deployed. And **ZeroMem**, an open-source agent-memory orchestrator with an MCP server.

How I use coding as an architect: **POCs to retire the biggest risk first**, not to show off. **Reference implementations** that encode the standards (logging, error handling, security, tests), so teams copy good patterns. **Pairing on the hardest bugs**, like deadlocks or performance cliffs. And code reviews focused on design and failure handling, not formatting, which is the linter's job."

---

## S24. Production-grade AI: MLOps/LLMOps, model serving, secure enterprise AI ✅🟡

### 🎤 Spoken answer (~2 min)
"I have hands-on experience with **RAG in production**, beyond POCs. My **RAG Document Q&A agent** processes PDFs with event-driven orchestration, step-level observability, retries, throttling and per-user rate limiting. I've also designed AI solutions and agentic platforms at Bahwan CyberTek, most recently a GenAI copilot on top of the job scheduler for failure triage and guarded remediation.

What makes something **production-grade** rather than a POC comes down to six things. **First, an eval-gated LLMOps pipeline**: prompts and models are versioned artifacts, a golden dataset gates every change in CI on retrieval recall, faithfulness and schema validity, and rollout is canary with automatic rollback. **Second, serving**: managed endpoints like Bedrock or Vertex, or self-hosted engines like vLLM for open models, behind a provider abstraction with fallbacks, plus routing between small and large models for cost. **Third, security**: PII redaction before embedding, tenant-isolated retrieval, private endpoints, prompt-injection guardrails, and agents acting with the user's permissions, never a super-user. **Fourth, observability**: tokens, cost, latency, retrieval scores and guardrail actions on every trace. **Fifth, resilience**: timeouts, circuit breakers, rate limits and degraded modes. **Sixth, governance**: an inventory of use cases, an approved-model list, audit and a kill switch.

That's the difference between a demo and something an enterprise can run."

> Deep dives: [genai/](../genai/README.md) (24 GenAI architect Q&As, AWS edition, sidecar governance) and [11_production_ai_llmops.md](11_production_ai_llmops.md).

---

## S25. Mentoring, coding standards, code reviews, design assessments ✅

### 🎤 Spoken answer (~1.5 min)
"I've worked in fully remote, cross-geography teams for more than 8 years, so I lean on **written standards and automation** more than meetings.

**Standards**: a short engineering handbook covering error handling, logging, API style, testing pyramid and security basics, plus reference implementations that teams can copy.

**Automation as the first reviewer**: in my OpenHands agent work I added security gates (dependency scanning, input-validation checks) and quality gates (Jest and JUnit 5 coverage thresholds) before merge. I apply the same to teams. Formatting, lint, coverage, vulnerability scans and architecture rules (ArchUnit) all run in CI, so human reviewers focus on design, failure handling, naming and tests.

**Code reviews**: I ask questions rather than dictate ('what happens if this call times out?'), and I explain the *why* so people learn the principle.

**Mentoring** senior developers into architects: I have them write ADRs and lead design reviews while I coach, and I gradually hand over ownership of decisions. **Design assessments** follow a lightweight checklist against NFRs, security, operability and cost, with findings rated by severity.

The measure of success is when the team makes good decisions without me in the room."

---

## S26. Resolving critical challenges & eliminating bottlenecks ✅

### 🎤 Spoken answer (~1.5 min)
"My clearest example is the Morgan Stanley CRM ingestion engine, where I improved batch-write performance by 4× and sustained 7,000 interactions per second. My approach to any critical bottleneck has five steps.

**One, stabilise first**: shed load or scale temporarily so the business keeps running. **Two, measure, don't guess**: metrics, thread dumps, profilers and query plans, to find where time is spent. In CRM it was lock contention and round trips, not CPU. **Three, fix the constraint, not the symptom**: we removed coordination with micro-batching, lock-free buffers and partitioning rather than adding servers. **Four, prove it**: rerun the same load profile and compare percentiles. **Five, prevent recurrence**: add the metric, the alert, and a performance test to CI.

On **emerging technology adoption**, I follow the same evidence-first approach. When we introduced GenAI capabilities, we started with a read-only, eval-gated use case before allowing any agent to take actions."
