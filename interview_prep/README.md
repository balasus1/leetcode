# Interview Preparation Master Repository

This directory contains comprehensive, master interview preparation guides tailored specifically for **Balasubramanian Shanmugham** (16+ years IT, 8+ years as Technical Architect) targeting premier **AI Architect, Solutions Architect, and Engineering Leadership** positions.

## Primary Master Guides
0. 🗓️ 🏆 **[`TEN_DAY_MASTER_AI_ARCHITECT_STUDY_PLAN.md`](./TEN_DAY_MASTER_AI_ARCHITECT_STUDY_PLAN.md)**: **The Complete 10-Day Full-Stack to Enterprise AI Solution Architect Master Curriculum**
   - Hour-by-hour structured daily schedule (07:30 to 22:30) with morning deep dives, midday coding sprints, afternoon architecture design, and nightly spoken drills.
   - 100% self-contained roadmap with direct clickable links to all DSA algorithms, React 18/19, Spring Boot 3, Kafka Sagas, System Design, OpenShift/Temporal, Capacity math, KV Cache/PagedAttention, Vector DBs/Hybrid RRF, LangGraph/MCP, and FinOps.

1. 🌟 👉 **[`tringapps_ai_architect_50_master_prep.md`](./tringapps_ai_architect_50_master_prep.md)**: **50 High-Yield AI Architect Interview Questions & Spoken Responses** tailored specifically for the **tringapps AI Architect** role:
   - Progressive complexity from enterprise fundamentals (OAuth/OIDC, React, Spring Boot, DB, Spring AOP, Sidecars, Temporal, ADRs, SRS, SBOM) to cutting-edge AI architecture (KV Cache, PagedAttention vLLM, GQA, HNSW Vector DBs, Hybrid Search RRF, LangGraph, MCP, ZeroMem, Guardrails, Hallucination defense, Back-of-the-envelope calculations, and tringapps enterprise scenarios).
   - Each question includes an architectural model/diagram, open-source tooling, and an elaborate 1–2 minute candidate spoken response (~180–250 words).

2. 🏢 🎯 **[`naukri_360_solution_architect_84_master_qa.md`](./naukri_360_solution_architect_84_master_qa.md)**: **84 High-Yield Solution Architect Interview Questions & Responses** from the Naukri360 Solution Architect Question Bank:
   - Tailored specifically to Balasubramanian Shanmugham's production architecture (16+ years IT, 8+ years as Technical Architect).
   - Deep dive into Production RAG pipelines (orchestration, caching, retries, per-user rate limiting), Kubernetes / OpenShift microservices, Kafka event-driven Sagas, distributed Quartz job scheduling, pessimistic locking, JVM ergonomics & GC tuning, Spark streaming lakehouse architectures (HBase + Hive), OAuth2/JWT RBAC, and React 18/19 SSR optimizations cutting HTTP requests by 80%.
   - Full question-by-question table of contents with direct anchor links.

3. 🤖 🚀 **[`production-rag/`](./production-rag/)**: **Production RAG Interview Master Suite (60 High-Yield Questions & Spoken Responses)**
   - Complete 4-tier production RAG interview curriculum (freeCodeCamp / Paulo Dichone + enterprise production patterns) targeting Senior AI Engineer & Enterprise AI Solution Architect roles:
     - 📋 [`00-cheatsheet.md`](./production-rag/00-cheatsheet.md): 60-question one-liner cheatsheet (3 key terms each), end-to-end tool stack, design-decision trade-offs matrix, and 8-stage 60–90 second architect answer frame.
     - 🟢 [`01-basic.md`](./production-rag/01-basic.md): Questions B1–B15 covering foundational concepts.
     - 🟡 [`02-intermediate.md`](./production-rag/02-intermediate.md): Questions I1–I15 covering building & debugging (including LangSmith trace debugging stories).
     - 🟠 [`03-advanced.md`](./production-rag/03-advanced.md): Questions A1–A15 covering production operations, formulas, and security.
     - 🔴 [`04-architect.md`](./production-rag/04-architect.md): Questions S1–S15 covering architecture & system design (with Mermaid flowcharts and STAR incident post-mortem).

4. 👉 **[`epam_technical_architect_50_master_qa.md`](./epam_technical_architect_50_master_qa.md)**: 50 detailed interview questions structured for Staff Architect & Engineering Manager rounds (AI vision, 10+ person team leadership, distributed Sagas, Kafka EOS, P99 tuning, FinOps).

5. 👉 **[`twenty_four_master_dsa_backend_system_design.md`](./twenty_four_master_dsa_backend_system_design.md)**: 24 targeted interview modules across DSA (1-8), Java Backend (9-16), and System Design (17-24) with Mermaid/ASCII architecture diagrams and working Java code.

---

## 50 Questions Structure & Coverage Matrix

| Section | Topic Domain | Questions | Key Production Systems Covered |
| :--- | :--- | :--- | :--- |
| **Section 1** | Architectural Leadership & Flagship Projects | Q1 – Q5 | Train Ticketing (1M/day), Job Scheduler (OpenShift), Morgan Stanley CRM (7k ops/sec), getConsenso SaaS |
| **Section 2** | Domain-Driven Design, Microservices & APIs | Q6 – Q10 | Bounded Contexts, OpenAPI Contract-First, Backward Compatibility, gRPC/REST/GraphQL, OpenShift / Docker |
| **Section 3** | Event-Driven Architecture, Kafka & Sagas | Q11 – Q16 | Kafka Saga Pattern, Compensating Transactions, Partition Ordering, Idempotency, EOS, DLQ, Spark ELT Lakehouse |
| **Section 4** | Concurrency, Performance & Scalability | Q17 – Q22 | Disruptor Lock-Free Buffers, P99 Profiling, Pessimistic DB Locking, L1/L2 Caching (Caffeine+Redis), JVM ZGC/G1GC, PostgreSQL `EXPLAIN ANALYZE` |
| **Section 5** | Fault Tolerance, Resilience & Streaming | Q23 – Q27 | Resilience4j Circuit Breakers, Dual-Channel REST+Kafka Failover, Bulkheads, Token Bucket, WebSockets Live Tracking, CAP Theorem, K8s Probes |
| **Section 6** | Enterprise Security, Cryptography & GDPR | Q28 – Q32 | AuthN vs AuthZ, Zero-Trust OAuth2/JWT, RBAC `@PreAuthorize`, PKCS#11 Hardware HSMs (IBM Tivoli/z/OS), Detica AML Factiva, GDPR Crypto-Shredding |
| **Section 7** | Spring Boot Internals & Core Java Depth | Q33 – Q38 | IoC & Bean Lifecycle, `@Transactional` Pitfalls (Self-Invocation/Rollback), `map` vs `flatMap`, Sealed Classes/Interfaces, `Optional` Patterns, Deadlock Diagnostics (`ThreadMXBean`) |
| **Section 8** | Schedulers, Distributed Locking & Temporal | Q39 – Q42 | Quartz vs Spring `@Scheduled`, Overlap Prevention (`@DisallowConcurrentExecution`), Misfire Handling, IST Half-Hour Offset Trap (UTC+05:30), Redis Lua Rate Limiting |
| **Section 9** | Generative AI, RAG & Agentic Systems | Q43 – Q47 | Production RAG PDF Q&A, Vector Re-Ranking, LLM Guardrails (Injection/PII), `ZeroMem` PostgreSQL MCP Server, OpenHands AI Coding Agents, Semantic Caching |
| **Section 10** | Frontend Architecture, Governance & EPAM Fit | Q48 – Q50 | React Hooks/Context vs Zustand/Redux, Architecture Decision Records (ADRs) & Technical Debt, EPAM Remote Governance & Logistics |

---

## Logistical Profile Summary for EPAM Recruiter
- **Experience:** 16+ Years Total in Enterprise IT (8+ Years as Technical Architect)
- **Current Notice Period:** 60 Days (negotiable transition buffer)
- **Target Compensation:** 45 – 55 LPA Fixed (aligned with senior architect band)
- **Time-Zone Flexibility:** 2–3 Hours Daily Overlap with CET (Central European Time) and EST (US East Coast)
- **Target Practice:** Enterprise Java, Cloud-Native Platform Engineering & Generative AI Solutions
