# Interview Preparation: EPAM Technical Architect Master Question Bank

This directory contains the comprehensive, 50-question master interview preparation guide tailored specifically for **Balasubramanian Shanmugham** (16+ years IT, 8+ years as Technical Architect) targeting the **Technical Architect / Solutions Architect** role at **EPAM Systems**.

## Primary Master Guide
👉 **[`epam_technical_architect_50_master_qa.md`](./epam_technical_architect_50_master_qa.md)**: Contains 50 detailed interview questions structured for maximum verbal clarity and impact:
- **🎙️ Elegant Spoken Anchor (Say Aloud in 5 Seconds)**: Immediate executive one-liner to take command of the room instantly.
- **🧒 12-Year-Old Intuitive Mental Model**: Relatable, non-technical everyday metaphors (restaurants, traffic lights, airport gates, relay races) that eliminate textbook dryness.
- **🗣️ Candidate Spoken Response (60–90 Seconds)**: Natural, first-person conversational cadence with clear problem-action-result structure (~140–200 words).
- **Embedded STAR Analysis**: Explicit Situation, Task, Action, and Result mapping.
- **Concrete Computable Numbers**: Real production scale metrics (1M users/day, 7K ops/sec, 12M records, 6 servers, 4x speedup, 850 users, 50k visitors, P99 from 680ms to 210ms, 99.99% accuracy).
- **Zero Omissions**: Covers every technology, pattern, project, and certification from the candidate's resume and coaching prompts.

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

## Related

- **EPAM Solution Architect pack (JD-mapped, ~400 Q&As + 26 STAR stories):** [../epam_solution_architect/README.md](../epam_solution_architect/README.md)
- **GenAI & Agentic AI Architect pack:** [../genai/README.md](../genai/README.md)
