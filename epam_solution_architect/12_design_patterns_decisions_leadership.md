# 12: Design Patterns (GoF + Enterprise + Cloud), Decisions Made vs Better, Leadership & Behavioural

> JD: *"Establish coding standards, architecture guidelines… mentoring… resolve critical technical challenges."* Plus EPAM's consultative and behavioural rounds.

| Section | Content |
|---|---|
| §1 GoF patterns applied in your projects | P1–P14 |
| §2 Enterprise, cloud & distributed patterns | Table + EP1–EP5 |
| §3 Decisions made, and how they could be better | Per-project retrospective table + spoken answer |
| §4 Leadership & behavioural (STAR) | L1–L14 |
| §5 Questions to ask EPAM | — |

---

## §1. GoF patterns, each tied to a real use

### P1. Strategy ✅
🎤 "A family of interchangeable algorithms behind one interface, chosen at runtime. In the scheduler, the **channel selection**: a `DispatchStrategy` with `RestDispatch` and `KafkaDispatch`, chosen based on the circuit-breaker state. Also job-type executors (fire-and-forget, recurring, chained). It removes `if/else` chains, and each strategy is testable alone."

```java
public interface DispatchStrategy { DispatchResult dispatch(JobCommand cmd); }

@Component class RestDispatch  implements DispatchStrategy { /* RestClient + Resilience4j */ }
@Component class KafkaDispatch implements DispatchStrategy { /* outbox → Kafka */ }

@Component @RequiredArgsConstructor
class ChannelSelector {
    private final RestDispatch rest; private final KafkaDispatch kafka;
    private final CircuitBreakerRegistry breakers;
    DispatchResult dispatch(JobCommand cmd) {
        var state = breakers.circuitBreaker(cmd.targetService()).getState();
        DispatchStrategy s = (state == CircuitBreaker.State.OPEN) ? kafka : rest;
        return s.dispatch(cmd);
    }
}
```

### P2. State ✅
🎤 "An object's behaviour changes with its internal state. **Job lifecycle** (SCHEDULED, RUNNING, PAUSED, STOPPED, DELETED) and **booking lifecycle** (PENDING, CONFIRMED, FAILED, EXPIRED). I implement it as an enum with an allowed-transition map, so invalid transitions (resume a DELETED job) fail with a 409. With sealed types and pattern matching, the compiler checks that every case is handled."

```java
public enum JobState {
    SCHEDULED, RUNNING, PAUSED, STOPPED, DELETED;
    private static final Map<JobState, Set<JobState>> ALLOWED = Map.of(
        SCHEDULED, EnumSet.of(RUNNING, PAUSED, DELETED),
        RUNNING,   EnumSet.of(SCHEDULED, PAUSED, STOPPED),
        PAUSED,    EnumSet.of(SCHEDULED, STOPPED, DELETED),
        STOPPED,   EnumSet.of(SCHEDULED, DELETED),
        DELETED,   EnumSet.noneOf(JobState.class));
    public JobState transitionTo(JobState next) {
        if (!ALLOWED.get(this).contains(next)) throw new InvalidTransitionException(this, next);
        return next;
    }
}
```

### P3. Template Method ✅
🎤 "A base class defines the algorithm skeleton, and subclasses fill in steps. The **job execution lifecycle**: `before()` (lock and mark RUNNING), `execute()` (job-specific), `after()` (record result, audit, emit event), `onError()`. Every job type gets consistent auditing and metrics. In modern Spring I often prefer composition (a `JobExecutor` plus hooks) over inheritance, but it's the same idea."

### P4. Factory / Abstract Factory ✅
🎤 "Encapsulate object creation. In **getConsenso**, stub generation for Java, TypeScript, Go and Kotlin: a `StubGeneratorFactory` returns the right generator for a language, and each family of generators produces consistent client, server and model artifacts (Abstract Factory). New languages plug in without touching callers. In Spring, a map of beans keyed by type (`Map<String, StubGenerator>`) is an idiomatic factory."

### P5. Adapter ✅
🎤 "Convert one interface into another clients expect. **Payment gateway adapters** behind `PaymentPort`; the **Factiva adapter** in the AML platform; LLM provider adapters (Bedrock, Vertex, vLLM) behind a `GenAiPort`. It's the code-level face of the anti-corruption layer."

### P6. Decorator ✅
🎤 "Add behaviour by wrapping without changing the class. **Resilience4j decorators**: `Decorators.ofSupplier(call).withCircuitBreaker(cb).withRetry(retry).withBulkhead(bh)`. Also caching or metrics wrappers around repositories and clients. Java I/O streams are the classic example."

### P7. Proxy ✅
🎤 "A stand-in controlling access to an object. **Spring AOP** is proxy-based: `@Transactional`, `@Cacheable`, security, and my custom tenant and audit aspects work through JDK or CGLIB proxies. Also lazy-loading proxies in Hibernate, and client-side stubs. Knowing it's a proxy explains the self-invocation pitfall."

### P8. Chain of Responsibility ✅
🎤 "Pass a request along a chain of handlers. The **Spring Security filter chain**; servlet filters; and in getConsenso the **contract validation pipeline**: parse → lint rules → breaking-change check → security rules → reviewer policy, where each handler can stop the chain or annotate results. The AI governance sidecar uses an ordered chain of governance steps too."

### P9. Observer / Publish-Subscribe ✅
🎤 "Notify dependants of changes. In-process, Spring `ApplicationEventPublisher` and `@TransactionalEventListener`. Across services, Kafka topics (BookingConfirmed → Invoicing, Notifications), and Redis pub/sub for cache invalidation across pods in the scheduler."

### P10. Builder
🎤 "Construct complex objects step by step, with readable, immutable results: request objects, test data builders, `ProblemDetail`, AWS SDK requests, Lombok's `@Builder` or record builders. It avoids telescoping constructors."

### P11. Command ✅
🎤 "Encapsulate a request as an object. **Scheduler job commands** (`TriggerJob`, `PauseJob`) are serialized to Kafka, queued, retried, audited and replayed. That's command plus message. Also undo and redo, and CQRS commands."

### P12. Singleton (and why to be careful)
🎤 "One instance per JVM. In Spring, beans are singletons by default, managed by the container, which is the right way. Avoid hand-rolled static singletons: hidden global state, hard to test, and classloader issues. If needed, use an enum singleton or the holder idiom. Remember Spring singletons must be stateless or thread-safe."

### P13. Facade ✅
🎤 "A simplified interface over a complex subsystem. The **booking façade API** hides inventory, pricing and payment orchestration from the front end, and the **strangler façade** in modernizations routes to legacy or new implementations transparently."

### P14. Specification / Repository
🎤 "**Repository** abstracts persistence behind collection-like interfaces (Spring Data). **Specification** composes query criteria as reusable predicates (`JpaSpecificationExecutor`), useful for complex filters in the admin console (status AND team AND date range) without combinatorial query methods."

---

## §2. Enterprise, cloud & distributed patterns

| Pattern | Problem | Where I used / would use it |
|---|---|---|
| **Circuit breaker** | Cascading failures | Scheduler downstream calls (Resilience4j), getConsenso SDK module |
| **Bulkhead** | One dependency exhausting shared resources | Thread pools per downstream; copilot isolated from scheduler |
| **Retry with backoff + jitter** | Transient failures | Idempotent calls only, single retry owner |
| **Timeout** | Hung calls | Every outbound call |
| **Saga** | Distributed transactions | Ticketing booking (choreography), refunds (orchestration) |
| **Transactional outbox / inbox** | Dual writes, duplicates | Booking + scheduler events |
| **CQRS** | Divergent read/write loads | Seat search vs booking |
| **Event sourcing** | Full history, audit | AML case history / ledgers (selectively) |
| **Strangler fig** | Incremental legacy replacement | Monolith → microservices migrations |
| **Anti-corruption layer** | Legacy or third-party model leakage | Payment gateways, Factiva, SaaS tools |
| **API gateway / BFF** | Client-specific aggregation, edge concerns | Spring Cloud Gateway; BFF for SPA token handling |
| **Sidecar / Ambassador** | Cross-cutting concerns outside app code | Service mesh proxies; AI governance sidecar |
| **Claim check** | Large payloads on brokers | Documents in S3, reference in Kafka |
| **Competing consumers** | Parallel work processing | Kafka consumer groups, RabbitMQ workers |
| **Leader election / distributed lock** | One active worker | Quartz clustered locks; fencing tokens |
| **Cache-aside / L1-L2** | Read latency, DB load | Scheduler metadata (Caffeine + Redis) |
| **Rate limiter / throttling** | Overload, fairness | Per-user limits in RAG agent; gateway quotas |
| **Health endpoint monitoring** | Detect and route around failures | K8s readiness/liveness |
| **Idempotency key** | Safe retries | Payments, job triggers |
| **Database per service** | Coupling via schema | All microservices designs |
| **Externalized configuration** | Env-specific config | ConfigMaps, Secrets Manager, AppConfig |
| **Hexagonal architecture** | Testability, swappable adapters | Service internal structure |

### EP1. Which pattern do you see misused most?
🎤 (~1 min) "**Retries.** Teams add retries at the client, gateway and mesh, with no jitter, on non-idempotent operations. It turns a small blip into a retry storm and duplicate side effects. The fix: one retry owner per hop, idempotency keys, retry budgets, and a circuit breaker outside the retry. Second place goes to **microservices themselves**: split by entity, ending up as a distributed monolith."

### EP2. Sidecar vs library for cross-cutting concerns?
🎤 (~45 s) "Library: no extra hop, but one per language and redeploys for upgrades. Sidecar: language-agnostic and independently upgraded, at the cost of per-pod resources and a local hop. I use libraries (a Spring Boot starter) for Java-only estates, and sidecars (mesh, an AI governance sidecar) for polyglot estates or when enforcement must be outside the app's control."

### EP3. Hexagonal vs layered vs clean architecture?
🎤 (~45 s) "Layered (controller → service → repository) is simple, but the domain often leaks framework concerns. Hexagonal and Clean put the **domain at the centre** with ports, and adapters outside; dependencies point inward. I use hexagonal for services with real domain logic, enforced by ArchUnit, and keep simple CRUD services layered. Don't over-engineer."

### EP4. DDD tactical patterns you use?
🎤 (~45 s) "**Aggregates** as consistency boundaries (Booking with its passengers; modify via the root only), **value objects** (Money, SeatId, as records), **domain events** (BookingConfirmed), **repositories** per aggregate, **domain services** for logic spanning aggregates. Strategically: bounded contexts and context maps (customer/supplier, ACL, published language)."

### EP5. How do you choose between patterns in a design review?
🎤 (~45 s) "Start from the quality attributes and failure scenarios, not the pattern catalogue. Ask which problem we actually have, what's the simplest thing that solves it, and what it costs to operate. Patterns are answers. If you can't name the question, don't apply the pattern."

---

## §3. Decisions made, and how they could be better (the "self-critique" round)

| Project | Decision made | Why it was right then | What I'd do better now |
|---|---|---|---|
| Job scheduler | **Quartz clustered on PostgreSQL** | Persistence, misfires, clustering, team skills, no new infra | Durable workflow engine (**Temporal**) for long-running chained/agent workflows; per-tier SLOs |
| Job scheduler | **Pessimistic locks on state transitions** | Correctness for concurrent pause/delete | `FOR UPDATE SKIP LOCKED` for worker claiming + optimistic `@Version` for admin edits; lock timeouts from day one |
| Job scheduler | **Dual channel REST + Kafka** | Keep critical jobs flowing during outages | **Async-first via outbox** for jobs not needing immediate answers; fewer code paths to test |
| Ticketing | **Choreographed Kafka Saga** | Few steps, cross-team, high throughput | **Orchestrate** refund/cancellation flows; Saga monitor read model from day one |
| Ticketing | **Redis TTL holds** | Removed DB lock contention at 3,500 req/s | Add **virtual waiting room** for festival peaks; schema registry from day one |
| Ticketing | **Java socket real-time push** | Low latency, full control, CDN + caching | Managed **WebSocket/SSE** gateway or MQTT, less custom connection code |
| Lakehouse | **Spark → HBase/Hive on Hadoop**, later AWS | Mature stack, team skills, volume | **Iceberg on S3** + Spark/Flink + Athena/Trino; decouple storage/compute |
| Morgan Stanley CRM | **In-memory ring buffers + micro-batching** | 4× throughput on fixed hardware | **Kafka in front** for durability and replay; partition by client ID |
| getConsenso | **Node/Next.js, few services, self-managed observability** | Solo founder speed; low cost | Postgres **RLS** earlier; managed observability to save ops time |
| RAG agent | **Fixed chunking + vector-only** initially | Fast to ship | Structure-aware chunking + **hybrid** search + golden set from day one |
| General | **Word/Visio LLDs** | Client expectation | **Docs-as-code**, diagrams-as-code, lean SAD |

### 🎤 "Tell me about an architecture decision you regret, or would change." (~2 min)
"The one I reflect on most is the **dual-channel REST plus Kafka** design in the scheduler. The decision was sound: during downstream outages, jobs failed and cascaded. Adding a circuit breaker with a runtime switch to Kafka kept jobs flowing, and uptime held at 99.99%.

But in hindsight it doubled our integration surface. Every downstream service had to support both a REST endpoint and a Kafka consumer, with the same idempotency semantics. Our test matrix doubled. And subtle issues appeared: a REST call that timed out but actually succeeded, followed by the Kafka redelivery, which is why idempotency keys became mandatory.

If I designed it today, I'd ask which jobs **actually need a synchronous answer**. For most, like report generation or data syncs, they don't. I'd make those **async-first** through the transactional outbox and Kafka from the start, and keep synchronous REST only for the few jobs where the caller needs an immediate result. Same resilience, half the code paths.

What I learned is to question whether a requirement is really 'synchronous' before designing around it, and I now put a 'revisit when' trigger in every ADR, so decisions get re-examined when the context changes."

---

## §4. Leadership & behavioural (STAR, 1–2 min each)

### L1. Tell me about yourself (90 s) ✅
🎤 "I'm a Technical Architect with 16+ years in IT, including about 10 years of hands-on full-stack delivery on Java, Spring Boot and React, and more than 8 years at Bahwan CyberTek in architecture roles. I design secure, scalable enterprise systems on-prem and in the cloud: contract-first REST APIs, event-driven microservices on Kafka, resilience patterns and observability, while staying hands-on in code and reviews.

Three examples. A **train ticketing platform** serving about a million users a day, where I designed the passenger flow and moved booking to an event-driven Saga on Kafka, eliminating double-bookings and cutting checkout P99 from 4.2 seconds to 240 milliseconds. A **centralized job scheduler** on OpenShift with circuit breakers and dual-channel failover, which cut state-transition P99 from 680 to 210 milliseconds. And earlier, at Genpact for Morgan Stanley, an ingestion engine sustaining 7,000 interactions per second with a 4× batch-write improvement.

I also build products: **getConsenso**, a contract-first API governance SaaS with 850 users, plus a production RAG agent and ZeroMem, an open-source agent-memory MCP server. Most recently I've focused on taking GenAI from POC to production with evals, guardrails and governance. That combination of hands-on Java architecture and production AI is why this Solution Architect role at EPAM fits well."

### L2. Mentoring a senior developer into an architect role ✅🟡
🎤 (~1.5 min) "**S**: A strong senior developer on the scheduler team wanted to grow into architecture but had never owned cross-team decisions. **T**: Develop them without slowing delivery. **A**: I gave them ownership of the chained-jobs design. They wrote the ADR, ran the design review, and presented trade-offs to the stakeholders. I coached beforehand with questions ('what happens if a pod dies mid-chain?'), reviewed their drafts, and backed them in the meeting rather than taking over. I paired with them on one hard problem (lock contention) so they saw the diagnosis method. **R**: Within two quarters they led design reviews independently, and their chained-jobs design shipped. The team's dependency on me for decisions dropped, which is how I measure mentoring success."

### L3. Disagreement with a stakeholder or client ✅
🎤 (~1.5 min) "**S**: In ticketing, a stakeholder pushed for wrapping inventory and payment in a distributed transaction (XA), because it 'guarantees consistency'. **T**: Avoid a design that wouldn't scale, without dismissing a legitimate concern. **A**: I reframed it around shared criteria: zero double-bookings, checkout under 1 second at 3,500 req/s, and payment gateway constraints. I showed that XA held locks across network calls and that the external gateway couldn't take part anyway. Then I walked the failure scenarios of the Saga alternative, including payment timeouts with reconciliation, which addressed the real worry. **R**: We agreed on the Saga, recorded in an ADR with the stakeholder's concerns as explicit acceptance tests. Zero double-bookings in production, and they became an advocate for the design."

### L4. A production crisis you led ✅
🎤 (~1.5 min) "**S**: During a batch window, the scheduler started missing triggers for unrelated jobs because one downstream service had slowed. **T**: Restore scheduling fast, then prevent recurrence. **A**: I stabilised first by pausing the jobs that targeted the slow service, which freed threads. Then thread dumps showed workers blocked on that service with no effective timeout. Short term, we tightened timeouts. Structurally, I introduced per-dependency circuit breakers and bulkheads plus the Kafka fallback channel with idempotency keys, and wrote a blameless post-incident review. **R**: Triggers recovered within the hour, and after the fixes, later downstream outages didn't cascade. Uptime held at 99.99%."

### L5. Delivering with a remote, cross-geography team ✅
🎤 (~1 min) "I've worked fully remote across time zones for 8+ years. My practices: **written-first** (ADRs, design docs, decision logs, so no decision lives only in a call), **overlap hours** reserved for discussions that need real-time interaction, **async reviews** with clear SLAs, recorded demos, and automation in CI so quality doesn't depend on being awake at the same time. With Jira, Teams and Slack, structured documentation kept code quality and alignment consistent."

### L6. Introducing a new technology to a skeptical team ✅
🎤 (~1 min) "When introducing GenAI capabilities, people worried about hallucinations and security. I proposed a **read-only, eval-gated** first use case (failure triage), with citations and a measurable target. We built a golden set from real incidents, so the team could see the quality numbers, and kept humans in control. Only after trust was earned did we design guarded actions. Evidence beats enthusiasm."

### L7. A time you were wrong ✅
🎤 (~1 min) "Early in the ticketing migration I pushed to split services by entity. It produced chatty synchronous calls between Customer, Order and Seat services. Latency and coupling grew. I owned it in the retrospective, we measured cross-service calls per journey, and we merged services along capability lines. I now validate boundaries against user journeys before committing."

### L8. Balancing tech debt and delivery ✅
🎤 (~1 min) "I make debt **visible and costed**: a debt register with impact (incidents, slowed delivery), tied to business outcomes. I reserve a fixed capacity share, around 15–20%, and attach refactoring to features that touch the affected code. And I use ADRs to record intentional debt with a revisit trigger. For example, scheduler lock contention was prioritised when we showed it threatened the batch SLA."

### L9. Raising engineering standards across teams ✅
🎤 (~1 min) "Rather than policing, I **automated standards**: a shared Spring Boot starter (logging, error handling, security, metrics), CI gates for coverage, scanning, ArchUnit rules and API breaking changes, and code-review checklists focused on failure handling. In my OpenHands work I added security and quality gates before merge. Adoption was high because the paved road was faster than going around it."

### L10. Pre-sales / RFP success ✅
🎤 (~1 min) "I've led whiteboarding, scoping, RFPs and pre-sales with business stakeholders. What works: understand the evaluation criteria, propose a phased plan that de-risks (discovery, MVP, increments), show a credible architecture with NFR answers, be explicit about assumptions, and bring evidence (reference implementations, prior metrics). I turn the client's business goals into measurable success criteria in the proposal."

### L11. Handling an underperforming team member
🎤 (~1 min) "Understand first: is it skills, clarity, motivation or personal circumstances? Set clear, specific expectations, pair on work, give regular feedback with examples, and agree a short improvement plan with support. Involve the manager early and document. Most cases improve with clarity and support. If not, escalate fairly."

### L12. How do you stay hands-on as an architect? ✅
🎤 (~1 min) "I build the risky POCs and reference slices myself, review PRs on critical components, pair on the hardest bugs, and keep shipping my own products (getConsenso, the RAG agent, System Design Studio, ZeroMem). It keeps my estimates honest and my designs implementable. EPAM's description of 'not a purely theoretical architecture role' is exactly how I work."

### L13. Why EPAM? ✅
🎤 (~1 min) "Three reasons. EPAM's **engineering-first culture** values architects who code, which matches how I work. The **breadth of clients and industries**, including financial services, travel and hi-tech, maps to my background in capital markets, AML, ticketing and SaaS. And EPAM is investing heavily in **AI-enabled engineering**, where I can contribute production RAG, agent governance and LLMOps experience, not just POCs. I want to apply that across client engagements and help teams move from AI experiments to production systems."

### L14. Where do you see yourself in 3–5 years?
🎤 (~45 s) "Leading architecture for large client programmes, combining distributed systems and production AI, mentoring the next set of architects, and contributing reusable accelerators (reference architectures, governance frameworks) that make EPAM teams faster and safer across engagements."

---

## §5. Questions to ask the EPAM panel
1. "What does the client's current architecture look like, and what's the biggest constraint you want this role to solve in the first 90 days?"
2. "How is the split between hands-on engineering and architecture governance expected to look week to week?"
3. "Where is the client on AI: POCs, early production, or scaling? What's blocking production today?"
4. "Which architecture artifacts and review processes does the client's enterprise architecture board require (SAD, SARR, specific templates)?"
5. "How do EPAM architects share accelerators and reference architectures across accounts?"
