# 11: Production-Grade AI: MLOps/LLMOps, Model Serving & Secure Enterprise AI (Basic → Advanced)

> JD §6: *"Proven experience deploying and scaling AI in production (beyond POCs), including MLOps/LLMOps pipelines, model serving inference engines, and secure enterprise AI integrations."*
> Resume: production RAG Document Q&A agent (event-driven, step observability, retries, throttling, per-user rate limits), AI-driven solutions and agentic platforms, LangChain4j, ZeroMem (MCP server), OpenHands agents, Agentic AI Solution Architect (BeSA).
> **Deep dives (24 GenAI architect Q&As, AWS edition, sidecar governance):** [genai/README.md](../genai/README.md). This file covers the EPAM-flavoured questions and links there for depth.

| Level | Questions |
|---|---|
| Basic | B1–B8 |
| Medium | M1–M10 |
| Advanced | A1–A6 |

---

## Basic

### B1. Traditional ML vs LLMs: when do you use which?
🎤 "**Traditional ML** (gradient boosting, time-series models) for structured or tabular prediction: forecasting, classification, anomaly detection. It's cheaper, explainable and easy to evaluate. **LLMs** for unstructured language tasks: summarization, extraction, Q&A over documents, classification of free text, and agents that use tools. Many solutions combine them, for example a forecasting model predicts an SLA breach and an LLM explains it in plain English."

### B2. What is RAG and why use it?
🎤 "Retrieval-Augmented Generation: retrieve relevant passages from your own data (via vector and keyword search), put them in the prompt, and have the model answer **grounded in that context, with citations**. It gives current, private knowledge without retraining, reduces hallucinations, and allows access control at retrieval time. My RAG Document Q&A agent does this for PDFs."

### B3. What are embeddings and vector databases?
🎤 "An embedding is a numeric vector representing meaning; similar texts have nearby vectors. A vector store (pgvector, OpenSearch k-NN, Pinecone, Vertex Vector Search) indexes them for approximate nearest-neighbour search (HNSW, IVF). In practice I combine vector search with BM25 keyword search (hybrid), because exact identifiers like error codes don't embed well."

### B4. What causes hallucinations, and how do you reduce them?
🎤 "The model generates plausible text without grounding. Mitigations: RAG with good retrieval, instructions to answer only from context and say 'insufficient evidence', a structured output with **citations validated in code**, lower temperature, reranking for relevant context, an evaluation of faithfulness in CI, and human review for high-stakes outputs."

### B5. Prompt engineering vs fine-tuning vs RAG?
🎤 "**Prompting**: cheapest, first resort. **RAG**: adds knowledge that changes, with citations and access control. **Fine-tuning**: changes behaviour, style or format, or makes a smaller model good at a narrow task. It doesn't reliably add facts, and needs data, training pipelines and re-training. Order: prompt → RAG → fine-tune only when evals show a gap the others can't close."

### B6. What is an AI agent?
🎤 "An LLM that, in a loop, decides which **tools** to call (APIs, searches, code), observes results and continues until the task is done. In production you bound it: typed tools, permissions, step and token budgets, explicit state, human approval for risky actions, and full tracing."

### B7. What's MCP?
🎤 "The **Model Context Protocol**: an open standard for exposing tools, resources and prompts to AI agents through a common client-server protocol, so tools are written once and used by any MCP-capable agent. I shipped an MCP server for **ZeroMem**, my open-source agent-memory orchestrator, so agents can store and recall memories in the user's own Postgres, with only hashes retained for audit."

### B8. MLOps vs LLMOps?
🎤 "**MLOps**: data versioning, feature pipelines, training, experiment tracking, model registry, deployment, drift monitoring, retraining. **LLMOps** adds prompt and model versioning, retrieval pipeline management (chunking, embeddings, indexes), **evaluation with golden sets and LLM-as-judge**, guardrails, token cost and latency management, provider routing and fallbacks, and tracing of multi-step chains and agents. The shared core is reproducibility, automated quality gates, safe rollout and monitoring."

---

## Medium

### M1. What makes an AI system production-grade rather than a POC? ✅
🎤 (~2 min) "Eight things, most of which I built into my RAG agent and the scheduler copilot design. **One, contracts**: typed outputs (JSON schemas) validated in code, so downstream systems don't parse free text. **Two, evaluation**: a golden dataset and automated gates (retrieval recall, faithfulness, schema validity, safety tests) on every prompt, model or retriever change. **Three, observability**: per-step traces (retrieval scores, tokens, cost, latency, guardrail actions), and quality SLIs, not just uptime. **Four, resilience**: timeouts, retries only for transient errors, circuit breakers, rate limiting (my RAG agent has per-user limits and throttling), fallbacks and degraded modes. **Five, security**: PII redaction before embedding, tenant-scoped retrieval, private endpoints, prompt-injection defences, least-privilege tools. **Six, cost control**: model routing, caching, context budgets. **Seven, safe delivery**: versioned prompts, canary rollout with automatic rollback. **Eight, governance**: an approved-model list, a use-case inventory, audit, a kill switch. A POC has a prompt and a demo. Production has all of the above."

### M2. Model serving / inference engines: what are the options and how do you choose?
🎤 (~1.5 min)

| Option | What it is | Choose when |
|---|---|---|
| **Managed APIs**: Amazon Bedrock, Azure OpenAI, Vertex AI | Serverless access to frontier and open models, IAM, private endpoints | Default for enterprises: fastest, compliant, no GPU ops |
| **SageMaker / Vertex endpoints** | Managed hosting of your own or open models on chosen GPUs | Custom or fine-tuned models, steady volume, VPC control |
| **vLLM** | High-throughput open-source LLM server (PagedAttention, continuous batching), OpenAI-compatible API | Self-hosting open models at scale on Kubernetes |
| **Hugging Face TGI** | LLM inference server with similar optimizations | Hugging Face ecosystem |
| **NVIDIA Triton / TensorRT-LLM** | Multi-framework server; optimized kernels | Max GPU efficiency, mixed model types (vision, embeddings, LLM) |
| **TorchServe / KServe / Seldon / BentoML** | Model serving frameworks; KServe for Kubernetes-native serverless inference | Classic ML models, multi-model platforms |
| **Ollama / llama.cpp** | Local, quantized models | Dev, edge, air-gapped small workloads |

"My decision factors: **data residency and compliance**, volume (managed is cheaper below steady high utilization), latency targets, model choice (closed frontier vs open weights), and team ops capability. Typical enterprise answer: managed APIs (Bedrock) as primary, a self-hosted vLLM on EKS for an open-weight model as fallback or for sovereignty, both behind one internal interface."

### M3. Key inference optimizations?
🎤 (~1 min) "**Continuous batching** (vLLM, TGI) to keep GPUs busy; **KV-cache management** (PagedAttention); **quantization** (INT8/INT4, AWQ, GPTQ) to cut memory and cost with small quality loss, validated by evals; **speculative decoding**; **prompt caching** of static prefixes (provider-side); **streaming** for time-to-first-token; right-sizing context (fewer, better chunks); **model routing** (small model for easy queries). Autoscale on queue depth or tokens per second rather than CPU, and keep warm capacity because GPU cold starts are slow."

### M4. Describe your production RAG agent architecture ✅
🎤 (~2 min) "It answers questions over uploaded PDFs. **Ingestion is event-driven**: an upload stores the file in object storage and emits an event. Workers extract text, chunk it structure-aware with overlap, embed it and upsert into the vector store with metadata (document ID, owner, page). Each step is **retryable and idempotent** (content-hash IDs), with failures to a dead-letter queue. **Query**: authenticate the user, apply **per-user rate limiting** and throttling, retrieve with metadata filters so users only see their own documents, rerank, then generate with an instruction to cite pages and to say when evidence is insufficient. **Step-level observability**: every stage emits timings, token counts and retrieval scores, so I can see whether a bad answer came from retrieval or generation. **Resilience**: timeouts and retries with backoff on the LLM provider, and degradation to showing the top passages if generation fails. It's containerized and deployed as a demo. At architecture level, I scaled the same patterns up for the scheduler copilot: hybrid search, golden-set evals in CI, a model router, and tenant isolation."

### M5. How do you evaluate an LLM application?
🎤 (~1.5 min) "Separate **retrieval** and **generation** metrics. Retrieval: recall@k and MRR against labelled evidence. Generation: faithfulness (claims supported by context), answer relevance, correctness against references, plus schema validity and citation correctness. Build a **golden set from real data** (historical tickets or incidents with SME-approved answers), including negative cases where the right answer is a refusal. Use **LLM-as-judge**, but calibrate it against human labels and use a different model family than the generator. Gate CI on absolute floors **and** per-slice regressions. In production, sample traces for judging, collect user feedback, and feed failures back into the golden set."

> Full treatment: [genai/02_cognizant_genai_architect.md §C11](../genai/02_cognizant_genai_architect.md).

### M6. CI/CD for AI: what quality gates?
🎤 (~1 min) "Ordered cheapest first: build, unit tests and scans → prompt-template snapshot and schema tests → offline eval on the golden set (thresholds and regression deltas) → safety and red-team suite (prompt injection, PII extraction, tool escalation) plus a cross-tenant leak test → cost and latency budgets → canary deployment with online metrics and automatic rollback via feature flags. Prompts and models are versioned artifacts, just like code."

### M7. Integrating AI into a Java/Spring estate?
🎤 (~1 min) "**LangChain4j** or **Spring AI**: both give a common chat and embedding model interface across providers (Bedrock, Azure OpenAI, Vertex, Ollama), structured output to Java records, tool calling, and RAG building blocks. They integrate naturally with Spring Security, Resilience4j, Micrometer and AOP. I keep provider SDKs behind our own port, use AOP for tenant binding, audit and metrics around AI calls, and treat the AI service like any other dependency with timeouts and breakers. For Python-first agent orchestration (LangGraph), I run it as a separate service behind a contract."

```java
interface TriageAssistant {
    @SystemMessage("Answer only from the provided context. Cite chunk ids. If insufficient, say INSUFFICIENT_EVIDENCE.")
    TriageAnswer triage(@UserMessage String question);     // JSON → Java record
}
TriageAssistant assistant = AiServices.builder(TriageAssistant.class)
        .chatModel(chatModel)                               // Bedrock / Azure OpenAI / Vertex via adapter
        .retrievalAugmentor(retrievalAugmentor)             // hybrid retriever + reranker, tenant filter injected
        .build();
```

### M8. Securing enterprise AI integrations?
🎤 (~1 min) "Private connectivity to models (PrivateLink / Private Service Connect); contracts with no training on our data and no retention; **PII redaction or tokenization** before prompts and embeddings; tenant isolation in retrieval, caches and logs; **guardrails** for input (prompt injection, topic) and output (PII, toxicity, schema, grounding); agents using **delegated user permissions** with a policy engine and human approval for destructive actions; audit with hashed or encrypted prompts; an approved-model inventory enforced by IAM. I implemented these as a governance **sidecar** pattern, detailed in [genai/05](../genai/05_sidecar_pattern_ai_governance.md)."

### M9. Cost management for LLM workloads?
🎤 (~1 min) "Measure cost per request and per tenant first. Then: route easy traffic to small models, use exact and semantic caching (tenant- and version-scoped), provider prompt caching for static prefixes, trim context (6 good chunks instead of 20), cap output tokens with structured outputs, use batch inference for offline jobs (roughly half the price on Bedrock), and set budgets with alerts per tenant and use case. Treat cost as an SLO alongside latency."

### M10. MLOps for a classic model (forecasting) in production?
🎤 (~1 min) "Versioned data and features with the same transformation code for training and serving; **rolling-origin backtesting** for time series; promotion gates (beat the champion by X%, no regression on critical slices like month-end); a model registry with aliases (champion, challenger); shadow then canary deployment; drift monitoring (PSI on features, accuracy once ground truth arrives); rollback by alias flip; scheduled retraining. Details in [genai/01 Q6](../genai/01_ltm_gcp_genai_agentic_architect.md)."

---

## Advanced

### A1. Design an enterprise RAG platform serving many teams and tenants
🎤 (~2 min) "**Ingestion plane**: connectors (S3, SharePoint, Confluence, databases via CDC) → classification and PII redaction → structure-aware chunking → embedding → hybrid index, all event-driven on Kafka with idempotent, content-hash-based incremental updates and DLQs. Each chunk carries tenant, source, ACL and classification metadata and the embedding model ID. **Query plane**: gateway with authentication (OIDC), tenant and ACL-filtered hybrid retrieval, reranking, a model router, guardrails, structured output with citations, and streaming. **LLMOps plane**: a prompt registry, a golden-set eval service, CI gates, canary rollouts, and a tracing and quality dashboard. **Governance**: a use-case registry, approved models, audit, cost showback. **Isolation**: pooled indexes with mandatory filters for most tenants, dedicated indexes or accounts for regulated ones. **Platform contract**: teams consume `/rag/query` and `/rag/ingest` APIs, or an SDK, rather than building their own. Scale: separate scaling of ingestion workers (on Kafka lag) from query pods (on RPS), with GPUs only where self-hosted models are used."

### A2. Scaling self-hosted LLM inference on Kubernetes
🎤 (~1.5 min) "Use **vLLM** (or TGI/Triton) deployments on GPU node pools, with Karpenter provisioning GPU instances, node taints so only inference pods land there, and the model weights pre-loaded from a cache (container image layers or a shared volume) to cut cold start. **Autoscale on queue depth or in-flight requests and token throughput** (KEDA on custom metrics), not CPU. Keep minimum replicas warm. A routing layer handles multiple models and LoRA adapters, applies per-tenant rate limits, and does request batching. Quantize to fit models on smaller GPUs where evals allow. Monitor time-to-first-token, tokens per second, GPU utilization and memory, queue wait. Do capacity planning by tokens per second per GPU from load tests. Use KServe for serverless scale-to-zero on low-traffic models."

### A3. Agentic AI in production: how do you keep it safe? ✅
🎤 (~1.5 min) "Model the agent as an **explicit state machine** (or a durable workflow, such as Temporal or LangGraph with checkpoints), not an open-ended loop. Use **typed tools with risk tiers**; only the tools for the current state are exposed. Agents act **on behalf of the user** (token exchange), so they can't exceed the user's permissions, and a **policy engine** decides every tool call, not the model. **Human approval** for destructive actions, bound to the exact plan hash. **Budgets** (steps, tokens, replans) and loop detection. **Idempotency keys** on every mutation. Full traces and audit. Red-team tests for prompt injection in CI. That's the design of the scheduler remediation agent in the genai pack, with about 38% of failures auto-remediated as the design target and zero unauthorized actions."

### A4. How do you move an organization from AI POCs to production?
🎤 (~1.5 min) "Most POCs die because of missing foundations, not models. I'd set up: **one, a platform** (shared AI gateway or sidecar for security, routing and observability; shared RAG ingestion; an eval service), so each team doesn't rebuild them. **Two, a governance path**: a use-case intake with risk tiering, and data, security and legal checklists that are fast for low-risk cases. **Three, evaluation as the definition of done**: no golden set, no production. **Four, start with read-only, high-value use cases**, then move to agents with human-in-the-loop, then bounded autonomy. **Five, measure business outcomes** (MTTR, handling time, deflection), not just usage. **Six, enable teams** with reference implementations and starters. That's how the copilot plan was phased: triage (read-only), then natural-language operations with confirmation, then guarded remediation."

### A5. AI decisions that could be better ✅
🎤 (~1 min) "In my RAG agent, I started with fixed-size chunking and vector-only search. Answers about exact identifiers were weak until I added structure-aware chunking and hybrid BM25 search. Early evaluation was a handful of manual test questions; a proper golden set from day one would have caught regressions earlier. And I logged full prompts initially for debugging, which isn't acceptable with real user data; now it's hashed and encrypted content with sampled, access-controlled traces."

### A6. What's your view on AI-assisted software delivery (your OpenHands work)? ✅
🎤 (~1 min) "I orchestrated self-hosted AI engineering agents from functional requirements to tested code, deployed on Vercel, Railway and a VPS. The key was **spec-driven skills**: agents first produced API contracts, data models and acceptance criteria, and only then generated code. **Security gates** (dependency scanning, input-validation checks) and **quality gates** (Jest and JUnit 5 coverage thresholds) ran before merge. The lesson: AI accelerates delivery only inside a strong engineering system: contracts, tests, gates and human review of design decisions. That's the same governance mindset I'd bring to EPAM client delivery."
