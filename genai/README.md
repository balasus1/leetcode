# GenAI & Agentic AI Technical Architect — Interview Pack

Target roles:
- **LTM: GCP GenAI & Agentic AI Architect** (12 questions): [01_ltm_gcp_genai_agentic_architect.md](01_ltm_gcp_genai_agentic_architect.md)
- **Cognizant: Generative AI Architect** (12 questions): [02_cognizant_genai_architect.md](02_cognizant_genai_architect.md)

Supporting material:
- [00_anchor_project_scheduler_copilot.md](00_anchor_project_scheduler_copilot.md): the **one story** every answer comes back to. It covers the Centralized Job Scheduler, the GenAI copilot built on top of it, the architecture, where each piece is stored on AWS (with GCP equivalents), encryption, multi-tenancy and cost.
- [03_cross_cutting_cheatsheet.md](03_cross_cutting_cheatsheet.md): model comparison tables ("why not X?"), guardrail layers, the eval metric glossary, the Spring AOP patterns, a numbers card, and a 60-second elevator pitch.
- [04_aws_specific_genai_architecture.md](04_aws_specific_genai_architecture.md): **AWS-only edition**. It has the AWS reference architecture, a service-by-service why/why-not, data placement with KMS/IAM/bucket policies, Bedrock Converse + Guardrails + Knowledge Base code, Terraform guardrails, Bedrock model and cost levers, an **AWS talking point for each of the 24 questions**, and an AWS rewrite of LTM Q12.
- [05_sidecar_pattern_ai_governance.md](05_sidecar_pattern_ai_governance.md): **Sidecar pattern for retrofitting GenAI + AI governance** into the scheduler and your other projects (ticketing, CRM, AML, lakehouse, getConsenso). It covers the use-case registry, the governance chain, **Spring Security** token exchange, **Temporal** multi-agent workflows, **Spring Batch** for re-embedding and evals, **OTel/ADOT + Micrometer**, K8s native-sidecar manifests, a phased retrofit plan, and 6 extra spoken Q&As.

---

## How to use this pack

1. **Learn the anchor project first** (00). Every one of the 24 answers points back to it, so the interviewer hears one consistent and believable system, not 24 unrelated textbook answers.
2. **Each question follows the same layout:**
   - 🎤 **Spoken answer (1.5–2 min)**: written to be said out loud, in first person. About 260–320 words at a calm pace.
   - ⭐ **STAR**: the incident or delivery story, kept short so you can expand it if asked.
   - 📊 **Numbers to quote**: all taken from the numbers card in 03, so they stay consistent.
   - 💻 **Code**: Java 21 / Spring Boot 3 / LangChain4j / Resilience4j (with Python LangGraph where the question asks for it).
   - ⚖️ **Why not…?**: the comparison or trade-off table that shows architect-level judgement.
   - 🎯 **Likely follow-ups**: one-line answers.
3. **AWS-focused panel?** Add the matching AWS line from 04 §5 to each answer. **Asked about brownfield or governance?** Use 05.
4. **Say the "anchor phrase" first.** Each answer opens with one sentence you can say in 5 seconds that frames the rest.

## ⚠️ Honesty rule (read this)

The scheduler facts come from your resume: Quartz on OpenShift, fire-and-forget, recurring and chained jobs, pessimistic locking on pause/resume/stop/delete, cache-TTL tuning for P99, circuit breakers, and dual-channel HTTP + Kafka. The RAG Q&A agent, getConsenso tenant audit, ZeroMem and OpenHands quality gates also come from the resume.

The **GenAI "Scheduler Copilot" layer and its metrics are a defensible design template** that links your scheduler work to your RAG and agentic experience. **Before any interview, replace every number with one you can personally defend**, or say "approximately" or "in our pilot". Interviewers dig into numbers. A smaller real number beats a big invented one.

## Repo map

```
genai/
├── README.md                                  ← you are here
├── 00_anchor_project_scheduler_copilot.md     ← the system story + AWS/GCP placement
├── 01_ltm_gcp_genai_agentic_architect.md      ← LTM 12 Q&A
├── 02_cognizant_genai_architect.md            ← Cognizant 12 Q&A
├── 03_cross_cutting_cheatsheet.md             ← comparisons, guardrails, evals, numbers card
├── 04_aws_specific_genai_architecture.md      ← AWS-only edition + AWS line for all 24 Qs
└── 05_sidecar_pattern_ai_governance.md        ← sidecar retrofit: Temporal, Spring Batch, OTel, Micrometer, Spring Security
```
