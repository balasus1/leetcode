# EPAM Solution Architect (Chennai): Complete Interview Preparation Pack

> Role: **hands-on Java Solution/Application Architect** covering enterprise apps, digital platforms and **production-grade AI**.
> This pack maps **every line of the job description** and **every resume project** to STAR stories and to Basic → Medium → Advanced questions with spoken answers (30–60 s for basic, 1–2 min for medium and advanced), code, trade-off tables and "what I'd do better" reflections.

## Files

| # | File | What it covers | Approx. Qs |
|---|---|---|---|
| 01 | [01_jd_expectations_star_stories.md](01_jd_expectations_star_stories.md) | **26 STAR stories**, one per JD expectation, each with "what I'd do better" | 26 |
| 02 | [02_java_core_concurrency_jvm.md](02_java_core_concurrency_jvm.md) | Core Java, collections, concurrency, JMM, virtual threads, GC, JVM in containers, refactoring | 38 |
| 03 | [03_spring_boot_spring_cloud.md](03_spring_boot_spring_cloud.md) | Spring Boot, `@Transactional`, AOP, JPA, caching, testing, Spring Cloud (Gateway, Config, Resilience4j, Stream, Contract), starters, multi-tenancy | 37 |
| 04 | [04_microservices_distributed_patterns.md](04_microservices_distributed_patterns.md) | Decomposition, Circuit Breaker, **Saga**, **choreography vs orchestration**, **2PC/3PC**, **CQRS**, outbox, event sourcing, locking, modernization | 34 |
| 05 | [05_nfr_sla_slo_cap_performance.md](05_nfr_sla_slo_cap_performance.md) | NFRs, **SLA vs SLO vs SLI**, error budgets, **CAP/PACELC**, HA, DR, capacity math, caching, load testing, perf tuning | 29 |
| 06 | [06_data_messaging_kafka_rabbitmq.md](06_data_messaging_kafka_rabbitmq.md) | SQL/NoSQL, indexes, isolation, partitioning, **DB choices per project**, Redis, **Kafka** (basic → advanced), **RabbitMQ**, broker decision matrix | 47 |
| 07 | [07_api_integration_security.md](07_api_integration_security.md) | REST, API governance, webhooks, EIPs, third-party/SaaS integration, **OAuth2, OIDC, JWT, SSO, IAM**, Spring Security, IdP choice, compliance, **auth libraries cheat sheet** | 50 |
| 08 | [08_aws_cloud_native_devops.md](08_aws_cloud_native_devops.md) | AWS compute/data/messaging choices, HA 3-tier, EKS vs ROSA vs ECS, autoscaling, cost, Terraform, GitHub Actions OIDC, K8s/OpenShift, DR, migration plan | 31 |
| 09 | [09_reactjs_frontend_architecture.md](09_reactjs_frontend_architecture.md) | React fundamentals, hooks, state management, performance & Core Web Vitals, testing, real-time UI, security, micro-frontends, Next.js/RSC | 29 |
| 10 | [10_architecture_modelling_governance.md](10_architecture_modelling_governance.md) | **C4, 4+1**, HLD/LLD, **SAD, SARR**, deployment/integration/security docs, tech selection matrix, governance, ADR sample, **full C4 + 4+1 of the scheduler** | 21 |
| 11 | [11_production_ai_llmops.md](11_production_ai_llmops.md) | MLOps/LLMOps, **model serving engines** (Bedrock, vLLM, TGI, Triton, KServe), RAG, evals, secure AI, agents | 24 |
| 12 | [12_design_patterns_decisions_leadership.md](12_design_patterns_decisions_leadership.md) | GoF patterns tied to projects, enterprise/cloud patterns, **decisions made vs better** table, leadership & behavioural STAR, questions for EPAM | 34 |
| 13 | [13_saga_2pc_temporal_outbox_cdc_implementation.md](13_saga_2pc_temporal_outbox_cdc_implementation.md) | **Implementation deep dive**: Saga (choreography + Temporal), 2PC/XA in Spring Boot, dual-write, outbox **poller vs Debezium CDC**, TCC, **Quartz → Temporal feasibility & migration**, other OSS tools, testing, failure traps | 24 |

**Total: ~420 questions/stories.** For deeper GenAI coverage: [../genai/](../genai/README.md). Earlier EPAM 50 Q&A: [../interview_prep/epam_technical_architect_50_master_qa.md](../interview_prep/epam_technical_architect_50_master_qa.md).

---

## JD → coverage matrix (nothing missed)

| JD section / line | STAR (01) | Question bank |
|---|---|---|
| **1. Solution Architecture & Design** | | |
| End-to-end architecture & blueprints | S1 | 10-M1, 10-A1 |
| Translate business requirements | S2 | 05-M1, 10-A5 |
| Enterprise-grade Java applications | S3 | 02 (all), 03 (all) |
| Architecture discussions, technology selection, governance | S4 | 10-M4, 10-M6, 10-M9 |
| C4 model / 4+1 views | S5 | 10-B1, 10-B2, 10-A1, 10-A2 |
| HLD & LLD | S6 | 10-B3, 10-A4 |
| SAD & SARR | S6 | 10-B5, 10-B6, 10-M1, 10-M2 |
| Deployment, integration, security architectures | S6 | 10-M3, 07-API-A1, 08-M1 |
| **2. Application Architecture & Distributed Systems** | | |
| Distributed / SOA / event-driven; modularization; decomposition | S7 | 04-M1, 04-M2, 04-M14, 04-A9 |
| Circuit Breaker | S8 | 04-B2, 03-SC5, 06-K-A1 |
| Saga | S9 | 04-B3, 04-M4, 04-A1, 04-A2, 13-D1, 13-D2, 13-D8 |
| Choreography vs orchestration | S10 | 04-M3, 13-D0, 13-D15 |
| 2PC / 3PC trade-offs | S11 | 04-M6, 04-M7, 04-M8, 13-D3, 13-D4 |
| CQRS | S12 | 04-B4, 04-M9, 04-A3 |
| Modernization approaches | S13 | 04-A4, 02-M14, 02-A8, 08-A1 |
| **3. Reliability, Performance & Scalability** | | |
| NFRs (scalability, availability, maintainability, recoverability) | S14 | 05-B1, 05-M1, 05-A3 |
| SLAs vs SLOs | S15 | 05-B2, 05-M2 |
| CAP theorem, multi-region | S16 | 05-B5, 05-M3, 05-M4, 05-A1 |
| HA, fault tolerance, performance tuning | S17 | 05-M5, 05-M6, 05-A2, 02-A3, 03-A5, 06-DB-M2 |
| **4. Integration & API Strategy** | | |
| RESTful APIs, event-driven, streaming | S18 | 07-API-B1–B8, 06-K-*, 06-DB-M9 |
| API management, governance, EIPs | S19 | 07-API-M1, 07-API-M2, 07-INT-M1 |
| Enterprise / third-party / SaaS integration | S20 | 07-INT-M2, 07-INT-M3, 07-API-M4 |
| **5. Security & Compliance** | | |
| Enterprise security standards & compliance | S21 | 07-SEC-M10, 07-SEC-A3, 07-SEC-M11 |
| AuthN/AuthZ frameworks | S22 | 07-SEC-B1, 07-SEC-B7, 07-SEC-M6 |
| IAM, OAuth2, OIDC, JWT, SSO | S22 | 07-SEC-B2–B6, 07-SEC-M1–M5, 07-SEC-M9, 07-SEC-A1, §LIB |
| **6. Production AI & Technical Leadership** | | |
| Hands-on coding, POCs, framework components | S23 | 03-A1, 02-A1, 12-L12 |
| Production AI: MLOps/LLMOps, model serving, secure AI | S24 | 11 (all), ../genai |
| Mentoring | S25 | 12-L2 |
| Coding standards, reviews, design assessments | S25 | 12-L9, 10-M2 |
| Resolve bottlenecks; adopt emerging tech | S26 | 02-A3, 05-A2, 12-L6 |
| **Profile** | | |
| Spring Cloud | — | 03-SC1–SC8 |
| Kafka, RabbitMQ | S9, S18 | 06-K-*, 06-R-* |
| Cloud (AWS) at scale | — | 08 (all), ../genai/04 |
| 12+ yrs, 4–5+ as architect | — | 12-L1 |

## Resume → coverage matrix

| Resume item | Where it's used |
|---|---|
| Whiteboarding, design reviews, RFPs, pre-sales, ADRs, SRS | 01-S4, 10-M4, 10-A3, 10-A5, 12-L10 |
| AI-driven solutions, agentic platforms, legacy assessment, modernization | 01-S13, 01-S24, 04-A4, 11 |
| Monolith → microservices / cloud-native / AI-enabled migrations | 01-S7, 04-A4, 08-A1 |
| RAG in production | 11-M4, 11-A5, 01-S24 |
| Kafka + resilience (CB, retry, bulkhead) + OAuth2/JWT/RBAC | 01-S8, 03-SC5, 07-SEC-M2, 07-SEC-M12 |
| L1/L2 caching | 03-M5, 05-M8 |
| Lighthouse, JMeter, EXPLAIN ANALYZE, JVM profiling | 01-S17, 02-A3, 05-M10, 06-DB-M2, 09-M4 |
| Remote cross-geography teams 8+ yrs | 01-S25, 12-L5 |
| Quartz scheduler on OpenShift; pessimistic locking; TTLs; P99 | 01-S1, 03-M4, 03-M10, 06-DB-M1, 05-A2, 10-A1 |
| Circuit breakers + dual channel HTTP/Kafka | 01-S8, 06-K-A1, 12-§3 |
| Train ticketing 1M/day, Saga on Kafka, seat holds | 01-S2, 01-S9, 04-A1, 04-A10 |
| Java socket real-time + CDN | 01-S16, 01-S18, 09-M9 |
| Spark ELT → Hadoop (HBase/Hive) → AWS | 01-S18, 06-DB-M9, 06-K-M7 |
| Morgan Stanley CRM: 12M records, 7k/s, 4× | 01-S3, 02-A1, 06-K-A2 |
| Capital markets / audit-ready data | 01-S21, 07-SEC-M10 |
| AML Watchlist/Scenario Manager + Factiva | 01-S20, 07-INT-M2 |
| IBM Tivoli PKCS#11, JCA/JCE on z/OS | 07-SEC-M11 |
| getConsenso (contract-first, stubs, GDPR audit, CB SDK, 850 users) | 01-S19, 07-API-M1, 07-API-M4, 12-P4 |
| OpenHands agents, security & quality gates | 11-A6, 12-L9, 08-B10 |
| RAG Document Q&A agent | 11-M4 |
| System Design Studio (Next.js, React Flow, Mermaid, PostHog) | 09-A4, 09-M3 |
| ZeroMem (MCP, user's own Postgres) | 11-B7, 06-DB-C1 |
| Skills: React, Next.js, TS, Tailwind, SCSS, Node, FastAPI | 09 (all) |
| Skills: Docker, K8s, Harbor, GitHub Actions, Prometheus, Grafana, OTel | 08 (all), 05-M12 |
| Skills: RabbitMQ, Redis, AWS, LangChain4j | 06-R-*, 06-DB-M5, 08, 11-M7 |

---

## ⚠️ Honesty legend & consistency rules

- ✅ = backed by a resume bullet. 🟡 = reasonable extension. **Confirm it's true before claiming it**, or phrase it as "how I'd approach it".
- **Canonical numbers** (identical across this pack, the genai pack and the earlier EPAM 50 Q&A):
  - Train ticketing: ~1M users/day, peak ~3,500 booking req/s, 7-min Redis hold, checkout P99 **4.2 s → 240 ms**, 99.99% inventory accuracy
  - Scheduler: P99 state transition **680 ms → 210 ms**, ~10-min Redis TTL, ~80% DB reads removed, 99.99% uptime
  - Morgan Stanley CRM: 12M records, 6 servers, **7,000/s**, **4×** throughput, micro-batch 500 / 50 ms
  - getConsenso: 850 users, 50k+ monthly visitors
- **RabbitMQ** is on the resume's skills list but not tied to a project bullet. Speak to it as "where it fits better than Kafka" (06-R-B4) unless you have a real project to cite.
- **Correction to the earlier EPAM 50 Q&A (Q4):** for PostgreSQL the JDBC batch flag is `reWriteBatchedInserts=true`. `rewriteBatchedStatements=true` is the MySQL Connector/J flag.

---

## 10-day study plan

| Day | Focus | Files |
|---|---|---|
| 1 | Pitch + all 26 STAR stories (say them aloud, time them) | 01, 12-L1 |
| 2 | Java core + concurrency + JVM | 02 |
| 3 | Spring Boot + Spring Cloud | 03 |
| 4 | Distributed patterns: Saga, CQRS, 2PC/3PC, choreography/orchestration | 04 |
| 5 | NFRs, SLO/SLA, CAP, HA/DR, perf tuning | 05 |
| 6 | Databases, Kafka, RabbitMQ | 06 |
| 7 | APIs, integration, security (OAuth2/OIDC/JWT/SSO/IAM) | 07 |
| 8 | AWS + DevOps + React | 08, 09 |
| 9 | C4/4+1, SAD/SARR, governance; draw the scheduler C4 from memory | 10 |
| 10 | Production AI + patterns + "decisions I'd change" + mock interview | 11, 12, ../genai |

**Mock-interview drill:** pick one random question per file, answer aloud in under 2 minutes, then check against the written answer for: a number, a trade-off, a project reference, and a "better next time".
