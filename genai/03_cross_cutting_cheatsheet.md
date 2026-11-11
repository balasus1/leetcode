# Cross-Cutting Cheatsheet: Comparisons, Guardrails, Evals, AOP, Security, Numbers

Use this to **survive follow-ups**: "why not model X?", "what guardrails exactly?", "where is the data encrypted?", "give me the numbers again".

---

## 1. 60-second elevator pitch (opening "tell me about yourself")

"I'm a Technical Architect with 16+ years in IT and about ten years hands-on full-stack with Java, Spring Boot and React. For the last few years I've focused on GenAI and agentic systems built on solid distributed-systems foundations.

My latest work is a centralized, multi-tenant job scheduler on OpenShift. It runs about 1.2 million job executions a day with circuit breakers, dual-channel HTTP-plus-Kafka failover, and pessimistic locking that brought state-transition P99 from 680 milliseconds to about 210. On top of it I architected a GenAI copilot: hybrid RAG for failure triage, natural-language job operations, and a guard-railed remediation agent with on-behalf-of permissions and human approval for destructive actions. MTTR for failed jobs went from about 47 minutes to 12, at around six-tenths of a cent per query.

Outside work, I built getConsenso, a contract-first API governance SaaS with 850 users, plus a production RAG document agent and ZeroMem, an open-source agent-memory orchestrator with an MCP server. I like owning the hard decisions (isolation, evals, cost, security) and writing them down as ADRs."

---

## 2. Model comparison: "why this model, why not that one?"

> Talk in **tiers and criteria**, not marketing names. Model versions change every quarter, but your selection *method* doesn't. Re-check current model names and pricing before the interview.

| Tier | Examples (2026 families) | Use in Scheduler Copilot | Why not for everything |
|---|---|---|---|
| **Frontier** | Claude Opus/Sonnet class, Gemini Pro class, GPT frontier class | Remediation planning, multi-hop impact analysis, low-confidence retrievals, payment-tagged jobs (~30%) | 3–5× cost and slower. No measurable gain on simple lookup questions |
| **Small/fast** | Claude Haiku class, Gemini Flash class, GPT mini class | Error explanation, runbook lookup, classification, query rewriting (~70%) | Faithfulness dropped to 0.81 on multi-hop questions |
| **Open-weight, self-hosted** | Llama / Mistral / Qwen class on vLLM | Fallback, air-gapped tenants, PII classification at high volume | GPU ops burden. Below frontier on reasoning. Worth it only at high steady volume or for sovereignty |
| **Embeddings** | Titan / Cohere embed (Bedrock), Vertex text-embedding, open BGE/E5 | 1024-d, multilingual not required | Higher dims gave +0.5% recall for +50% memory |
| **Reranker** | Cohere Rerank, BGE-reranker (cross-encoder) | Top-20 → top-6 | LLM-as-reranker: +800 ms, higher cost |

**Selection method (say this):** "I shortlist 3 candidates per tier, run our 600-item golden set plus the red-team suite, then plot quality vs. cost vs. P95 latency. I pick the cheapest model that meets the quality floor for each route. I also check the non-functionals: regional availability for data residency, private endpoints, zero data retention, context window, structured-output support and rate limits."

| Criterion | Weight | Notes |
|---|---|---|
| Quality on *our* eval | 35% | Never public benchmarks alone |
| Cost per successful answer | 20% | Include retries and repair calls |
| Latency (TTFT + P95) | 15% | |
| Security/compliance (residency, PrivateLink/PSC, no training on data) | 15% | Hard gate for regulated tenants |
| Structured output / tool-calling reliability | 10% | Schema-pass rate on our contract |
| Ecosystem / portability | 5% | Available on both Bedrock and Vertex is a plus |

### RAG vs fine-tuning vs long context

| Approach | Use when | Why not as the default here |
|---|---|---|
| **RAG** ✅ | Knowledge changes daily (runbooks, incidents), citations needed, tenant isolation | — |
| Fine-tuning | Stable style/format, narrow classification, cost reduction at huge volume | Knowledge goes stale. Retraining needed per tenant. No citations. Harder isolation |
| Long context ("stuff it all") | One-off analysis of a few big docs | 10–30× tokens, slower, "lost in the middle", tenant mixing risk |
| Agents + tools | Need **live** state (job status, logs) | Combined with RAG, not instead of it |

---

## 3. Guardrails: defence in depth

| Layer | Control | Implementation |
|---|---|---|
| **Input** | Authentication, tenant binding | JWT + `@TenantScoped` aspect |
| | Rate limit / quota | Bucket4j per tenant/user; global token bucket |
| | Prompt-injection detection | Classifier (Bedrock Guardrails / Model Armor) + heuristics |
| | PII detection / redaction | Presidio/Comprehend (AWS), Sensitive Data Protection (GCP) |
| | Topic allow-list | "Scheduler/ops topics only" classifier |
| **Context** | Untrusted-content delimiting | `<context>` tags + "treat as data" instruction |
| | Classification ceiling | Chunks above the user's clearance are filtered at retrieval |
| **Model** | System prompt hardening, low temperature | Versioned prompts, temp 0–0.2 |
| **Output** | Schema validation | JSON schema + one repair retry |
| | Grounding/citation check | Every claim → retrieved chunk ID |
| | PII + cross-tenant identifier scan | Regex + NER + tenant ID dictionary |
| | Toxicity / policy | Provider guardrail + custom rules |
| **Action** | Tool policy | OPA/Cedar via `@AgentTool` aspect, OBO token |
| | HITL | Plan-hash-bound approval for destructive tier |
| | Budgets | Step ≤ 12, tokens ≤ 20k, replans ≤ 2 |
| **Post** | Audit | Hash-chained, encrypted content, Object Lock |
| | Monitoring | Policy-denial spikes alert (possible injection) |

**Soundbite:** "Prompts are not a security boundary. Every guardrail that matters is enforced in code, outside the model."

---

## 4. Spring AOP patterns used across the answers

| Annotation | Advice | Purpose | Answer refs |
|---|---|---|---|
| `@TenantScoped` | `@Around` | Bind tenant from JWT → context, MDC and span; fail if absent | LTM Q11, C12 |
| `@AgentTool(tier, action)` | `@Around` | Policy check (OPA), audit, structured refusal back to the model | LTM Q5 |
| `@AiObserved(operation)` | `@Around` | Tokens, cost, latency, outcome metrics + OTel GenAI attributes | C4 |
| `@AiAudited` | `@AfterReturning` | Hash-chained audit event, encrypted content | C12 |
| `@Guarded` | `@Around` | Input guardrails before, output guardrails after | 00 §2 |
| Resilience4j `@CircuitBreaker` `@Retry` `@Bulkhead` `@RateLimiter` | (AOP-based) | Dependency protection | LTM Q2, C7, C10 |

**Why AOP?** These are cross-cutting concerns. If every service method had to remember tenant binding, audit and metrics, someone would forget one. Aspects make the secure path the default path.
**AOP gotchas to mention:** self-invocation bypasses the proxy, so call through the bean or split classes. Aspect ordering matters, so use `@Order`: tenant binding → guardrails → observability → audit. Final/private methods aren't proxied.

```java
@Aspect @Order(1) class TenantScopeAspect { /* bind tenant first */ }
@Aspect @Order(2) class GuardrailAspect   { /* needs tenant for policies */ }
@Aspect @Order(3) class AiObservabilityAspect { }
@Aspect @Order(4) class AiAuditAspect     { /* runs last, sees final result */ }
```

---

## 5. Security: data at rest, in transit, in use

| State | Control (AWS) | Control (GCP) |
|---|---|---|
| **At rest** | KMS CMKs: S3 SSE-KMS, Aurora/RDS, OpenSearch, DynamoDB, EBS, MSK, ElastiCache. Per-tenant CMK for regulated tenants. Key rotation yearly | Cloud KMS CMEK on GCS, AlloyDB, Vector Search, Firestore, Pub/Sub. Cloud HSM for bank-grade tenants |
| **In transit** | TLS 1.2+ everywhere. mTLS in the service mesh. PrivateLink to Bedrock. VPC endpoints for S3/DynamoDB. MSK TLS + IAM auth | TLS. mTLS (Anthos/Istio). Private Service Connect to Vertex. VPC-SC perimeter |
| **In use** | Redact/tokenize PII before prompts. Nitro Enclaves for highly sensitive processing (optional) | Same. Confidential VMs/GKE nodes (optional) |
| **Secrets** | Secrets Manager with 30-day rotation; IRSA, no static keys | Secret Manager; Workload Identity |
| **Identity** | OAuth2/OIDC, token exchange (OBO), least-privilege IAM roles per service | Same with Workload Identity Federation |
| **Provider terms** | Bedrock: no training on customer data; regional inference | Vertex: no training on customer data; data residency controls |

**Common trap:** "Service-account key files on disk." Never. Use workload identity (IRSA / GCP Workload Identity), short-lived tokens, and secret scanning in CI (gitleaks).

---

## 6. Eval metric glossary (one line each)

| Metric | Meaning |
|---|---|
| Recall@k | Share of expected evidence found in the top-k retrieved |
| MRR | Mean of 1/rank of the first relevant chunk |
| nDCG | Ranking quality weighted by position |
| Faithfulness / groundedness | Share of answer claims supported by retrieved context |
| Answer relevance | Does the answer address the question? |
| Context precision | Share of retrieved chunks that were actually relevant |
| Citation correctness | Cited chunk IDs exist *and* support the claim |
| Refusal correctness | Says "insufficient evidence" when it should |
| Schema pass rate | Outputs valid against the JSON contract |
| Judge agreement (Cohen's κ) | LLM-judge vs human agreement; ≥ 0.7 is acceptable |
| Task success rate (agents) | Remediation verified successful / attempts |
| Steps / tokens per task | Efficiency + loop detection |
| Policy-denial rate | Rising → injection attempts or a prompt regression |

---

## 7. Numbers card (memorize; keep consistent)

| Area | Number |
|---|---|
| Experience | 16+ yrs IT, ~10 yrs hands-on full-stack |
| Scheduler scale | ~40 tenants, ~18k job definitions, ~1.2M executions/day, peak ~350 triggers/s |
| State-transition P99 | 680 ms → **210 ms** |
| MTTR failed jobs | 47 → **12 min** |
| Auto-remediated | **~38%** of failures |
| Recall@10 | 0.71 → **0.89** |
| Faithfulness | 0.84 → **0.93** |
| Hallucinated-step rate | 6.1% → **1.4%** |
| Cost per query | $0.021 → **$0.006** (−71%) |
| Routing split | 70% small / 30% frontier |
| Semantic cache hit | **31%** |
| P95 / TTFT | **2.4 s / 0.7 s**; retrieval P95 **230 ms** |
| Index freshness | 24 h → **< 2 min** |
| Re-index cost | **−90%** (content-hash dedupe) |
| Golden set | **600** items; judge κ **0.78** on 150 human labels |
| Red-team suite | **~150** prompts, zero-tolerance |
| Agent budgets | 12 steps, 20k tokens, 2 replans; median 4 steps, P99 9 |
| Forecasting | SLA breaches 23 → **6/month**; idle compute **−18%** |
| Availability (copilot) | **99.95%** through 3 provider incidents |
| Earlier career anchors | 7,000 interactions/s, 4× batch write (Morgan Stanley CRM); ~1M users/day train platform; 850 users / 50k monthly visitors (getConsenso) |

> ⚠️ Replace any number you can't personally defend. Saying "approximately" or "in our pilot" is fine. Inventing figures is not.

---

## 8. Questions to ask the interviewer (architect-level)

1. "Where are you today on the RAG → agents journey: read-only assistants, or agents with write access?"
2. "How do you evaluate GenAI changes before production? Is there a golden dataset owned by the business?"
3. "What's the stance on model providers: single cloud (Vertex) or multi-provider with abstraction?"
4. "What are the hardest non-functionals for your clients: residency, isolation, audit, cost?"
5. "How is success measured for this role in the first 90 days?"
