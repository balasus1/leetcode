# LTM: GCP GenAI & Agentic AI Architect: Top 12 Questions

> Anchor system for every answer: **Centralized Job Scheduler + Scheduler Copilot**. See [00_anchor_project_scheduler_copilot.md](00_anchor_project_scheduler_copilot.md).
> The role is GCP-flavoured. Say "on AWS we used X; the GCP equivalent I'd use is Y" naturally. The mapping table is in the anchor doc §3.

| # | Question | Core theme |
|---|---|---|
| 1 | Production incident + observability | Silent embedding-version mismatch |
| 2 | RAG pipeline with evaluation, retries, step observability | Pipeline as a product |
| 3 | GraphRAG for enterprise knowledge + governance | Job-dependency knowledge graph |
| 4 | Architecture principles for a GenAI platform | Contracts, isolation, evals, cost SLOs |
| 5 | Agentic workflows with safe tools + end-to-end permissions | On-behalf-of tokens, policy engine, HITL |
| 6 | MLOps for forecasting models | Job-runtime / SLA-breach forecasting |
| 7 | CI/CD with AI quality gates | Eval, schema, safety, tenant-canary gates |
| 8 | RAG latency vs cost vs quality | Router, cache, context budget |
| 9 | Multi-agent without conflict or infinite loops | Supervisor, single-writer state, budgets |
| 10 | Integrating GenAI with enterprise services (APIs + events) | Outbox, idempotency, dual channel |
| 11 | Multi-tenant isolation for retrieval and outputs | Framework-injected filters, RLS, canaries |
| 12 | GenAI on GCP with portability + provider abstraction | Ports & adapters, fallback chain |

---

## Q1. Describe a production incident you diagnosed and how your observability helped fix GenAI or pipeline failures.

### 🎤 Spoken answer (~2 min)

> **Anchor:** "The scariest GenAI incidents are the ones that don't throw errors, and step-level observability is the only thing that catches them."

"Let me give you a real one from our Scheduler Copilot, the GenAI layer I designed on top of our centralized job scheduler. It does RAG-based failure triage for about 1.2 million job runs a day.

During a month-end close window, our on-call engineers started saying the copilot's answers felt 'generic'. There were no 5xx errors and no exceptions, and every health check was green. But we had built step-level tracing into every RAG stage: retrieval, rerank, generation and output validation. Each stage emits its own OpenTelemetry span with AI-specific attributes.

The dashboard told the story in about five minutes. The **median top-1 retrieval similarity** had dropped from 0.82 to 0.61. Our **'insufficient evidence' rate** jumped from 4% to 19%. And because the router escalates low-confidence queries to the bigger model, our **cost per query tripled** at the same time.

Retrieval was clearly the broken step, not the LLM. We drilled into one trace and compared the embedding model ID stamped on the indexed chunks with the one used at query time. They didn't match. A platform upgrade had moved the ingestion pipeline to a newer embedding model version, but the query path still used the old one. Both produced 1024-dimension vectors, so nothing failed. The math just became meaningless.

We did three things. First, the immediate fix: we pinned the query path to the matching version and **recovered within 40 minutes**. Second, the structural fix: every vector now carries `embedding_model_id`, and the retriever **refuses to query** an index whose model ID doesn't match. A loud failure beats a silent one. Third, re-indexing is now **blue-green behind an index alias**, gated by our golden-set recall eval before the alias swap.

The lesson I took away: in GenAI, 'up' doesn't mean 'correct'. You need quality SLIs, not just availability SLIs."

### ⭐ STAR
| | |
|---|---|
| **S** | Month-end close, peak load (~350 triggers/s), copilot answers degraded, no errors raised |
| **T** | Find root cause fast; failed jobs were piling up and MTTR was creeping back toward the pre-copilot ~47 min |
| **A** | Traced per-step spans → retrieval similarity drift → embedding model version mismatch → pinned version → added version guard + blue-green re-index with eval gate |
| **R** | Recovered in **40 min**; similarity back to 0.82; "insufficient evidence" back to ~4%; cost spike reversed; **0 repeats** in the following releases |

### 📊 Numbers to quote
- Top-1 similarity median **0.82 → 0.61** (detection signal)
- Insufficient-evidence rate **4% → 19%**
- Cost/query **3×** during the incident (router escalations)
- Time to detect: **~5 min** from the alert; time to recover: **40 min**

### 💻 Code: step-level span + embedding-version guard (Java, OpenTelemetry)

```java
@Component
@RequiredArgsConstructor
public class TracedRetriever {

    private static final AttributeKey<Double> TOP1 = AttributeKey.doubleKey("rag.retrieval.top1_score");
    private static final AttributeKey<Long>   HITS = AttributeKey.longKey("rag.retrieval.hits");
    private static final AttributeKey<String> EMB  = AttributeKey.stringKey("rag.embedding.model_id");

    private final Tracer tracer;                      // injected OpenTelemetry tracer
    private final EmbeddingModel embeddingModel;      // LangChain4j
    private final EmbeddingStore<TextSegment> store;
    private final IndexMetadataService indexMeta;     // reads model id stamped on the index
    private final MeterRegistry meters;

    @Value("${copilot.embedding.model-id}") String queryModelId;

    public List<EmbeddingMatch<TextSegment>> retrieve(String tenantId, String question) {
        Span span = tracer.spanBuilder("rag.retrieve").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            // 1. Loud failure beats silent drift
            String indexModelId = indexMeta.embeddingModelId(tenantId);
            if (!indexModelId.equals(queryModelId)) {
                meters.counter("rag.embedding.mismatch", "tenant", tenantId).increment();
                throw new EmbeddingVersionMismatchException(indexModelId, queryModelId);
            }

            Embedding q = embeddingModel.embed(question).content();
            var result = store.search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(q)
                    .maxResults(40)
                    .filter(metadataKey("tenant_id").isEqualTo(tenantId))
                    .build());

            var matches = result.matches();
            double top1 = matches.isEmpty() ? 0.0 : matches.get(0).score();
            span.setAttribute(TOP1, top1);
            span.setAttribute(HITS, (long) matches.size());
            span.setAttribute(EMB, queryModelId);
            // the drift SLI: alert when the p50 of this drops > 15% vs the 7-day baseline
            meters.summary("rag.retrieval.top1_score", "tenant", tenantId).record(top1);
            return matches;
        } catch (RuntimeException e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

Prometheus alert on quality drift (not just errors):
```yaml
- alert: RagRetrievalQualityDrift
  expr: |
    histogram_quantile(0.5, sum by (le) (rate(rag_retrieval_top1_score_bucket[15m])))
      < 0.85 * histogram_quantile(0.5, sum by (le) (rate(rag_retrieval_top1_score_bucket[7d])))
  for: 10m
  labels: { severity: page }
  annotations: { summary: "Retrieval similarity dropped >15% vs 7-day baseline" }
```

### ⚖️ Why not…?
| Alternative | Why we didn't rely on it |
|---|---|
| Only HTTP 5xx / latency alerts | Incident had **zero errors**; quality regressions are silent |
| LLM-as-judge on 100% of traffic | Too expensive at volume; we judge a **2% sample** + cheap proxy SLIs (top-1 score, citation rate, no-answer rate) |
| Re-index in place | No rollback; blue-green + alias gives an **instant rollback** |
| Vendor black-box observability only | We need tenant-tagged spans in **our** Grafana next to scheduler metrics, so OTel is portable to Cloud Trace on GCP |

### 🎯 Likely follow-ups
- *"How would you see this on GCP?"* OTel → Cloud Trace + Cloud Monitoring. Same span attributes, and alert policies on the custom metric.
- *"What other silent failures do you watch?"* Chunker changes (avg chunk length drift), prompt-version drift, rising refusal rate, and a falling citation coverage rate.
- *"How do you avoid alert fatigue?"* Burn-rate alerts on quality SLOs, not raw thresholds. Pages only for user-impacting drift.

---

## Q2. Describe your approach to building RAG pipelines with strong evaluation, retries, and step observability.

### 🎤 Spoken answer (~2 min)

> **Anchor:** "I treat RAG as a data pipeline with an output contract, not as a prompt with some documents attached."

"I build RAG in two halves: an **offline indexing pipeline** and an **online query pipeline**. Each has its own contracts, retries and metrics.

On the **indexing side** in our Scheduler Copilot, the sources are runbooks, past incident RCAs and summarized job-failure logs. Every change arrives as a Kafka event. The pipeline classifies the data, **redacts PII before embedding** (that point is non-negotiable), chunks it structure-aware at around 400 tokens with overlap, stamps metadata like `tenant_id`, `source`, `version` and `embedding_model_id`, and upserts idempotently using a content hash. Failures go to a dead-letter topic, so a broken document never blocks the rest.

On the **query side**, I run **hybrid retrieval**: BM25 for exact things like job IDs and error codes, plus vectors for semantic meaning. I fuse them with reciprocal rank fusion and rerank down to six chunks. Hybrid alone moved our Recall@10 from 0.71 to 0.89, because error codes like `ORA-00060` simply don't embed well.

The **output contract** is strict JSON: root cause, evidence with chunk IDs, next steps and confidence. If the JSON fails validation, I do **one repair retry** with the validation error fed back to the model, then degrade gracefully to 'here are the top runbook sections'. I never retry blindly. Retries on the LLM call are only for transient errors (429, 503, timeouts) with exponential backoff and jitter, all behind a circuit breaker.

For **evaluation**, I keep a golden set of about 600 real questions built from historical incidents. On every change we measure retrieval (recall@k, MRR) and generation (faithfulness, answer relevance, citation correctness) separately. That separation is the key point: when quality drops, I know immediately whether to fix retrieval or the prompt.

And every stage emits its own span, so a single trace shows exactly where the time and quality went."

### ⭐ STAR
| | |
|---|---|
| **S** | Ops engineers were spending ~47 min per failed job searching Confluence runbooks and old tickets |
| **T** | Build a triage assistant that is trustworthy enough for on-call to act on |
| **A** | Event-driven indexing with PII redaction → hybrid retrieval + rerank → JSON contract with citation check → golden-set eval gate in CI → per-step OTel spans |
| **R** | Recall@10 **0.71 → 0.89**; faithfulness **0.93**; MTTR **47 → 12 min**; P95 **2.4 s** |

### 💻 Code: LangChain4j hybrid RAG with output contract + repair retry

```java
// The output contract: the LLM must fill this. LangChain4j maps JSON → record.
public record TriageAnswer(
        String rootCause,
        List<Evidence> evidence,     // every claim must cite a chunk
        List<String> nextSteps,
        double confidence) {
    public record Evidence(String chunkId, String quote) {}
}

interface TriageAssistant {
    @SystemMessage(fromResource = "prompts/triage/v7.txt")   // versioned prompt
    TriageAnswer triage(@UserMessage String question);
}

@Configuration
class RagConfig {

    @Bean
    TriageAssistant triageAssistant(ChatModel chatModel,
                                    HybridContentRetriever retriever) {
        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .contentAggregator(new ReRankingContentAggregator(
                        CohereScoringModel.builder()           // cross-encoder reranker
                                .apiKey(System.getenv("COHERE_API_KEY"))
                                .modelName("rerank-v3.5").build(),
                        ReRankingContentAggregator.DEFAULT_QUERY_SELECTOR,
                        0.35))                                  // min rerank score
                .build();

        return AiServices.builder(TriageAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .build();
    }
}

/** BM25 (OpenSearch) + vector, fused with Reciprocal Rank Fusion. */
@Component
@RequiredArgsConstructor
class HybridContentRetriever implements ContentRetriever {
    private final KeywordSearchClient bm25;          // thin OpenSearch client
    private final TracedRetriever vector;            // from Q1
    private static final int K = 60;                 // RRF constant

    @Override
    public List<Content> retrieve(Query query) {
        String tenant = TenantContext.require();
        var kw  = bm25.search(tenant, query.text(), 40);
        var vec = vector.retrieve(tenant, query.text());

        Map<String, Double> score = new HashMap<>();
        Map<String, TextSegment> seg = new HashMap<>();
        for (int i = 0; i < kw.size(); i++) {
            score.merge(kw.get(i).id(), 1.0 / (K + i + 1), Double::sum);
            seg.put(kw.get(i).id(), kw.get(i).segment());
        }
        for (int i = 0; i < vec.size(); i++) {
            score.merge(vec.get(i).embeddingId(), 1.0 / (K + i + 1), Double::sum);
            seg.put(vec.get(i).embeddingId(), vec.get(i).embedded());
        }
        return score.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(20)                                   // reranker trims to 6
                .map(e -> Content.from(seg.get(e.getKey())))
                .toList();
    }
}

/** Retry policy: transient → backoff; contract violation → ONE repair; then degrade. */
@Service
@RequiredArgsConstructor
class TriageService {
    private final TriageAssistant assistant;
    private final CitationValidator citations;
    private final RunbookFallback fallback;

    @Retry(name = "llm")                       // 429/503/timeout only (see yaml)
    @CircuitBreaker(name = "llm", fallbackMethod = "degraded")
    public TriageAnswer answer(String q) {
        TriageAnswer a = assistant.triage(q);
        var problems = citations.validate(a);  // every chunkId must exist in retrieved set
        if (problems.isEmpty()) return a;
        a = assistant.triage(q + "\n\nYour previous answer was invalid: " + problems
                + ". Only cite retrieved chunk ids. Return valid JSON.");
        if (!citations.validate(a).isEmpty()) throw new UngroundedAnswerException(problems);
        return a;
    }

    TriageAnswer degraded(String q, Throwable t) { return fallback.topRunbookSections(q); }
}
```

```yaml
resilience4j:
  retry:
    instances:
      llm:
        max-attempts: 3
        wait-duration: 400ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        enable-randomized-wait: true          # jitter → no thundering herd
        retry-exceptions: [java.util.concurrent.TimeoutException, com.acme.llm.TransientLlmException]
        ignore-exceptions: [com.acme.rag.UngroundedAnswerException]
  circuitbreaker:
    instances:
      llm:
        sliding-window-size: 50
        failure-rate-threshold: 40
        slow-call-duration-threshold: 6s
        wait-duration-in-open-state: 20s
```

### 📊 Eval scorecard (what the CI gate checks)
| Layer | Metric | Gate |
|---|---|---|
| Retrieval | Recall@10, MRR | ≥ 0.85, ≥ 0.70 |
| Generation | Faithfulness (claims supported by context) | ≥ 0.90 |
| Generation | Answer relevance | ≥ 0.85 |
| Contract | JSON schema pass rate | ≥ 99.5% |
| Citation | Citations that resolve to retrieved chunks | 100% |
| Safety | Red-team prompt-injection suite pass | 100% |
| Cost/latency | Avg input tokens, P95 | ≤ 3.5k, ≤ 2.5 s |

### ⚖️ Why not…?
| Choice | Picked | Why not the alternative |
|---|---|---|
| Chunking | Structure-aware (headings, steps), ~400 tokens, 15% overlap | Fixed 1k-token chunks split runbook steps in half and hurt precision. Semantic chunking cost 4× at ingest for a +1% gain |
| Retrieval | Hybrid BM25 + vector | Vector-only missed exact error codes and job IDs (recall 0.71) |
| Reranker | Cross-encoder on top-20 | LLM-as-reranker adds ~800 ms and costs more |
| Top-k to LLM | 6 | 20 chunks gave "lost in the middle" + 3× tokens with no faithfulness gain |
| Framework | LangChain4j | Spring AI was also viable. LangChain4j had more mature RAG building blocks (augmentor, reranking aggregator) when we started |

### 🎯 Likely follow-ups
- *"How do you build the golden set?"* Mine resolved incidents: question = alert text, expected = the RCA and runbook section an SME actually used. 600 items, refreshed monthly from production traces.
- *"How do you evaluate without labels?"* Faithfulness via LLM-as-judge calibrated against 150 human-graded items (κ ≈ 0.78).

---

## Q3. How would you implement GraphRAG for enterprise knowledge, including governance and auditability controls?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "Vector RAG answers 'what is similar'. GraphRAG answers 'what is connected'. Most enterprise questions are about connections."

"In the scheduler world, the most valuable questions are multi-hop. 'If the GL-close job fails tonight, which downstream reports miss their SLA, which teams own them, and is there a runbook for this failure class?' Vector search can't answer that, because the answer is spread across five documents linked by relationships.

So I'd model a **knowledge graph as a governed data product**. Nodes are Job, Dataset, Team, Runbook, FailureClass and Incident. Edges are `DEPENDS_ON`, `READS`, `WRITES`, `OWNED_BY`, `REMEDIATES` and `AFFECTED`. The key governance decision is **two classes of edges**. **Deterministic edges** come straight from the scheduler's chained-job metadata and our CMDB, with confidence 1.0. **Inferred edges** come from LLM extraction over RCAs and runbooks, like 'this runbook remediates deadlock failures'. Those carry a confidence score, the source chunk, the extractor prompt version, and a review status.

At query time the LLM never writes free-form Cypher against production. It picks from a **library of parameterized query templates**, such as 'downstream impact within N hops'. The framework then injects the tenant filter and a classification ceiling based on the user's clearance. The retrieved subgraph plus the linked text chunks become the context. The answer must cite node and edge IDs.

For **auditability**, every node and edge carries provenance: source system, ingested-at, extractor version and tenant. Every query logs which subgraph was retrieved. So when an auditor asks 'why did the copilot say report X is impacted?', we can replay the exact path. And inferred edges below 0.8 confidence go to an SME review queue. They're used for suggestions, never for automated actions.

The net effect in our pilot: impact-analysis questions went from about 50% correct with vector RAG to about 88% with graph-plus-vector hybrid."

### ⭐ STAR
| | |
|---|---|
| **S** | Chained jobs across 40 tenants with ~18k job definitions. Impact analysis was done by hand in spreadsheets |
| **T** | Answer "blast radius" questions reliably and auditably |
| **A** | KG from scheduler DAG metadata (deterministic) + LLM-extracted edges (inferred, reviewed) → parameterized Cypher templates → hybrid graph + vector context → cited answers with provenance |
| **R** | Impact-analysis accuracy **~50% → ~88%** on a 120-question eval; audit replay for every answer; **0** automated actions based on unreviewed inferred edges |

### 💻 Code: governed graph query (Neo4j/Neptune openCypher) + Java template execution

```cypher
// Template: downstream_impact  (LLM selects the template + fills $jobId, $hops; never raw Cypher)
MATCH path = (j:Job {id: $jobId, tenantId: $tenantId})<-[:DEPENDS_ON*1..3]-(down:Job)
WHERE all(r IN relationships(path) WHERE r.tenantId = $tenantId
                                     AND (r.kind = 'DETERMINISTIC' OR r.reviewStatus = 'APPROVED'))
  AND down.classification <= $clearance
OPTIONAL MATCH (down)-[:OWNED_BY]->(t:Team)
OPTIONAL MATCH (down)-[:WRITES]->(d:Dataset)<-[:READS]-(r:Report)
RETURN down.id AS job, t.name AS owner, collect(DISTINCT r.id) AS reports,
       [rel IN relationships(path) | {id: rel.id, source: rel.source,
                                      extractor: rel.extractorVersion}] AS provenance
LIMIT 200
```

```java
public enum GraphTemplate {
    DOWNSTREAM_IMPACT("cypher/downstream_impact.cypher", Set.of("jobId", "hops")),
    REMEDIATION_FOR_FAILURE("cypher/remediation_for_failure.cypher", Set.of("failureClass"));
    final String resource; final Set<String> allowedParams;
    GraphTemplate(String r, Set<String> p) { resource = r; allowedParams = p; }
}

@Service
@RequiredArgsConstructor
class GovernedGraphRetriever {
    private final Driver neo4j;
    private final AuditLog audit;

    @TenantScoped
    public GraphContext run(GraphTemplate t, Map<String, Object> llmParams, UserPrincipal user) {
        if (!t.allowedParams.containsAll(llmParams.keySet()))
            throw new PolicyViolation("LLM supplied unexpected params: " + llmParams.keySet());
        int hops = Math.min((int) llmParams.getOrDefault("hops", 2), 3);  // clamp blast radius

        Map<String, Object> params = new HashMap<>(llmParams);
        params.put("hops", hops);
        params.put("tenantId", TenantContext.require());     // framework-injected, never from LLM
        params.put("clearance", user.clearanceLevel());

        try (Session s = neo4j.session(SessionConfig.forDatabase("kg"))) {
            var rows = s.executeRead(tx -> tx.run(Templates.load(t.resource), params).list());
            audit.graphQuery(user, t, params, rows.size());   // replayable audit
            return GraphContext.from(rows);
        }
    }
}
```

Edge schema (governance fields):
```json
{ "id": "e-88121", "type": "REMEDIATES", "from": "rb-212", "to": "fc-DEADLOCK",
  "kind": "INFERRED", "confidence": 0.86, "reviewStatus": "APPROVED",
  "source": "s3://kb/acme/rca/2026-03-14.md#chunk-7", "extractorVersion": "kg-extract@v3",
  "tenantId": "acme", "classification": 2, "ingestedAt": "2026-03-15T02:10:00Z" }
```

### ⚖️ Why not…?
| Option | Verdict |
|---|---|
| Pure vector RAG | Fails multi-hop. Can't express "3 hops downstream" |
| Community-summary GraphRAG (LLM summarizes clusters globally) | Great for "themes across a corpus", but costly to rebuild and weak on precise lineage. We use it only for "summarize all failure patterns this quarter" |
| LLM writes Cypher (text-to-Cypher) freely | Injection risk, unbounded traversals, and tenant filter could be dropped. **Templates + parameter allow-lists** instead |
| Neo4j vs Amazon Neptune vs Spanner Graph (GCP) | Neptune/Spanner Graph for managed + IAM + KMS. Neo4j where the team needs GDS algorithms. The template layer keeps it portable (openCypher / GQL) |

### 🎯 Likely follow-ups
- *"How do you keep the graph fresh?"* The same Kafka change events as vector indexing. A job-definition change triggers an edge upsert, deterministic edges are rebuilt nightly from the scheduler DB, and drift is reported.
- *"Right to erasure?"* Tenant-scoped delete by `tenantId` plus per-tenant KMS key crypto-shredding for the raw chunks.

---

## Q4. What architecture principles guide your GenAI platform design for scalability, reliability, and maintainability over time?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "Seven principles, and every one exists because I've been burned without it."

"**One: contract-first.** The copilot exposes OpenAPI endpoints and Kafka event schemas with a schema registry. The LLM's output is also a contract: a typed JSON schema. That's what stops 'prompt spaghetti'. Teams integrate against the schema, not against prompt wording. It's the same discipline I built getConsenso around: contract validation before code.

**Two: models are replaceable, contracts are not.** All model calls go through a provider-abstraction port: generate, embed, rerank, moderate. Swapping Claude for Gemini is a config change plus an eval run, not a rewrite.

**Three: every change is eval-gated.** Prompts, models, chunkers and retrievers are versioned artifacts. Nothing merges without passing the golden set, and nothing reaches 100% traffic without a canary on online metrics.

**Four: isolation by default.** Tenant isolation at every layer, bulkheads between the copilot and the core scheduler, and separate thread pools so a slow LLM provider can never slow down job triggering. The scheduler is tier-0. The copilot is tier-2, and it must degrade, never cascade.

**Five: observability is a feature.** Every step is a span, and we track quality SLIs (faithfulness, citation rate, no-answer rate) alongside latency and errors.

**Six: cost is an SLO.** We budget cost per query the same way we budget latency, at $0.006 today. Routing, caching and context trimming are architecture decisions, not later optimizations.

**Seven: security and governance by design.** PII redaction before embedding, encryption with KMS, private endpoints for model traffic, policy-checked tools and tamper-evident audit.

And I record all of these as ADRs, so in two years someone knows *why* we chose hybrid retrieval or the template-only graph queries, not just *that* we did."

### ⭐ STAR (principle in action)
| | |
|---|---|
| **S** | Early pilot: the copilot shared the scheduler's Tomcat thread pool. During an LLM provider slowdown, scheduler API P99 climbed |
| **T** | Guarantee the GenAI layer can never degrade tier-0 scheduling |
| **A** | Applied principle 4: split into a separate deployment, a thread-pool bulkhead, a circuit breaker, and a "search-only" degraded mode |
| **R** | Scheduler P99 stayed at **~220 ms** through later provider incidents; copilot degraded gracefully with **0** scheduler impact |

### 💻 Code: the contract (OpenAPI excerpt) + the provider port

```yaml
# openapi/copilot-v1.yaml (excerpt)
paths:
  /v1/triage:
    post:
      operationId: triage
      security: [{ oauth2: [copilot.read] }]
      requestBody:
        content: { application/json: { schema: { $ref: '#/components/schemas/TriageRequest' } } }
      responses:
        '200': { content: { application/json: { schema: { $ref: '#/components/schemas/TriageAnswer' } } } }
        '202': { description: Accepted, long-running; poll /v1/tasks/{id} }
components:
  schemas:
    TriageAnswer:
      type: object
      required: [rootCause, evidence, nextSteps, confidence, promptVersion, model]
      properties:
        rootCause:     { type: string, maxLength: 1200 }
        evidence:      { type: array, minItems: 1, items: { $ref: '#/components/schemas/Evidence' } }
        nextSteps:     { type: array, items: { type: string } }
        confidence:    { type: number, minimum: 0, maximum: 1 }
        promptVersion: { type: string, example: triage@7.2.0 }
        model:         { type: string, example: small-tier }
```

```java
/** Port: business code depends on this, never on a vendor SDK. */
public interface GenAiPort {
    <T> T generate(PromptRef prompt, Map<String, Object> vars, Class<T> outputType, RoutingHints hints);
    float[] embed(String text, EmbeddingProfile profile);
    ModerationResult moderate(String text);
}
```

### ⚖️ Principles vs. anti-patterns
| Principle | Anti-pattern it prevents |
|---|---|
| Contract-first | Downstream parsing of free text; breaking changes on prompt edits |
| Replaceable models | Vendor lock-in; re-writes on every model generation |
| Eval-gated change | "Vibe-checked" prompt edits that silently regress |
| Isolation/bulkheads | GenAI outage cascading into core systems |
| Observability | Silent quality drift (see Q1) |
| Cost SLO | Surprise bills at month-end |
| Security by design | PII in vector stores, prompt-injection-driven tool misuse |

### 🎯 Likely follow-ups
- *"Which principle would you compromise first under deadline pressure?"* None of the safety ones. I'd compromise on breadth: ship one capability (triage, read-only) with all seven principles, rather than three capabilities with none.

---

## Q5. How do you design agentic workflows that safely use tools and enforce permissions end-to-end?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "The agent never has more power than the human it acts for, and the model never makes the authorization decision. Code does."

"Our remediation agent can pause, resume, re-run and re-parameterize jobs on a scheduler that runs payment and GL-close batches. So safety wasn't optional. I designed it in five layers.

**First, a typed tool registry with risk tiers.** Every tool has a JSON schema, a description, and a tier. Tier 0 is read-only, like get job status or fetch logs. Tier 1 is reversible writes, like pause or re-run a single idempotent step. Tier 2 is destructive or high blast radius, like delete, bulk stop, or anything that touches a payment-tagged job. The model only sees the tools allowed for the current step and role.

**Second, on-behalf-of identity.** The agent doesn't have a super-user service account. When a user triggers it, we do an OAuth2 token exchange and the tool calls carry *that user's* scopes. If an ops analyst can't delete jobs in the UI, the agent can't either. That kills the confused-deputy problem.

**Third, a policy engine outside the model.** A Spring AOP aspect intercepts every tool call and evaluates it against OPA/Cedar policies: role, tenant, job tags, time windows like 'no re-runs during the close freeze', and blast-radius limits like 'max 20 jobs per bulk action'. The LLM can *propose* anything. Code *decides*.

**Fourth, human-in-the-loop for tier 2.** The agent produces a **dry-run preview** ('this will pause 14 jobs; here are their IDs and downstream impact'), and a human approves it in Teams before execution. That approval is cryptographically bound to the exact plan hash, so the agent can't change the plan after approval.

**Fifth, idempotency and audit.** Every mutating call carries an idempotency key. Every proposal, decision and execution is audited.

The result: about 38% of failed jobs are auto-remediated, and we've had zero unauthorized actions. Policy denials are a monitored metric, because a spike usually means prompt injection or a bad prompt version."

### ⭐ STAR
| | |
|---|---|
| **S** | Ops wanted auto-remediation; security feared an LLM with write access to payment schedules |
| **T** | Enable autonomy for known-safe failure classes without expanding privilege |
| **A** | Tiered tool registry + OBO token exchange + OPA policy aspect + plan-hash-bound HITL + idempotency keys + audit |
| **R** | **~38%** of failures auto-remediated; **0** unauthorized actions; security sign-off in **2 review cycles** |

### 💻 Code: Spring AOP policy enforcement around LangChain4j tools

```java
@Retention(RUNTIME) @Target(METHOD)
public @interface AgentTool { RiskTier tier(); String action(); }

public enum RiskTier { READ, REVERSIBLE, DESTRUCTIVE }

@Component
@RequiredArgsConstructor
public class SchedulerTools {
    private final SchedulerClient scheduler;   // calls the existing scheduler REST API with the OBO token

    @Tool("Get current status and last 5 runs of a job")
    @AgentTool(tier = RiskTier.READ, action = "job:read")
    public JobStatus getJobStatus(@P("job id") String jobId) { return scheduler.status(jobId); }

    @Tool("Re-run a single failed step of a chained job. Idempotent.")
    @AgentTool(tier = RiskTier.REVERSIBLE, action = "job:rerun")
    public RunRef rerunStep(@P("job id") String jobId, @P("step name") String step) {
        return scheduler.rerun(jobId, step, IdempotencyKey.of(jobId, step, AgentRun.current().id()));
    }

    @Tool("Pause multiple jobs matching a tag. Requires human approval.")
    @AgentTool(tier = RiskTier.DESTRUCTIVE, action = "job:bulk-pause")
    public PlanPreview bulkPause(@P("tag") String tag) {
        return scheduler.previewBulkPause(tag);            // DRY RUN ONLY, never executes
    }
}

@Aspect
@Component
@RequiredArgsConstructor
public class ToolPolicyAspect {
    private final PolicyClient opa;           // OPA / Cedar sidecar
    private final AuditLog audit;
    private final MeterRegistry meters;

    @Around("@annotation(tool)")
    public Object enforce(ProceedingJoinPoint pjp, AgentTool tool) throws Throwable {
        var ctx = AgentRun.current();         // user, tenant, OBO scopes, step, approval
        var input = new PolicyInput(ctx.user(), ctx.tenantId(), tool.action(), tool.tier(),
                                    pjp.getArgs(), ctx.approvedPlanHash());
        Decision d = opa.evaluate("scheduler/agent/allow", input);

        audit.toolDecision(ctx, tool, pjp.getArgs(), d);
        if (!d.allowed()) {
            meters.counter("agent.policy.denied", "action", tool.action()).increment();
            // Return a structured refusal TO THE MODEL so it can re-plan, not an exception it can't see
            return ToolRefusal.of(d.reason());
        }
        return pjp.proceed();
    }
}
```

```rego
# policy/scheduler/agent.rego
package scheduler.agent
default allow := false

allow if { input.tier == "READ"; input.action in input.user.scopes }

allow if {
  input.tier == "REVERSIBLE"
  input.action in input.user.scopes
  not freeze_window
  not payment_tagged
}

allow if {                                   # destructive only with a matching human approval
  input.tier == "DESTRUCTIVE"
  input.action in input.user.scopes
  input.approvedPlanHash == crypto.sha256(json.marshal(input.args))
}

freeze_window if { time.clock(time.now_ns())[0] >= 22 }          # e.g. month-end close freeze
payment_tagged if { some a in input.args; contains(a, "PAYMENT") }
```

### ⚖️ Why not…?
| Alternative | Why not |
|---|---|
| Agent runs with a privileged service account | Confused deputy: any user can escalate through the agent |
| "Tell the model in the system prompt not to delete things" | Prompts are not a security boundary. Prompt injection from a log line can override them |
| HITL for everything | Approval fatigue means humans rubber-stamp. Tiering keeps HITL meaningful (~6% of actions) |
| Model-generated arbitrary HTTP calls | Unbounded surface. Typed tools wrap the **existing** scheduler APIs, which already enforce RBAC |

### 🎯 Likely follow-ups
- *"Indirect prompt injection via job logs?"* Logs are wrapped as untrusted data blocks and scanned by an injection classifier. Even if the model is fooled, the policy engine and OBO scopes still bound the damage.
- *"How do you test it?"* A red-team suite of 150 injection and escalation prompts in CI. The pass criterion is zero allowed destructive calls.

---

## Q6. Explain your MLOps design for productionizing forecasting models, including CI/CD, monitoring, and rollbacks.

### 🎤 Spoken answer (~2 min)

> **Anchor:** "A forecast is only as good as the decision it drives. Ours decided when to scale the scheduler and when to warn about SLA breaches."

"In the scheduler we had a very practical forecasting problem. Month-end and quarter-end loads caused jobs to miss their completion windows. We built two models. One forecasts **job runtime and queue depth per tenant** to pre-scale OpenShift worker pods. The other is an **SLA-breach classifier** that predicts, an hour ahead, which chained jobs will miss their window.

My MLOps design starts with **contracts**: a feature contract (lag features, calendar features like business-day-of-month, upstream volumes) and a data contract validated with schema checks before training. Training data is versioned. Every snapshot in S3 is immutable and referenced by the model card, so any model can be reproduced exactly.

The **CI/CD** pipeline runs on every merge and on a weekly retrain schedule, and our own scheduler triggers it. It does data validation, then **rolling-origin backtesting**, because a random train/test split leaks the future in time series. Then come the promotion gates: the new model must beat the champion on weighted MAPE by at least 2%, and must not regress on the month-end slice. Averages hide exactly the days we care about. Passing models go to the model registry as 'challenger'.

Deployment is **shadow first**. The challenger predicts alongside the champion for a week with no impact. Then canary for a subset of tenants, then promotion.

For **monitoring**, we track input drift with PSI on key features, prediction drift, and actual accuracy once ground truth lands, which is fast for runtime forecasts.

**Rollback** is a registry alias flip. Serving reads `models:/sla-breach@champion`, so rollback takes seconds and needs no redeploy.

The result: SLA breaches dropped from about 23 to 6 a month, and pre-scaling cut idle worker compute by around 18%."

### ⭐ STAR
| | |
|---|---|
| **S** | ~23 SLA breaches/month at period-end; static over-provisioning wasted compute |
| **T** | Predict load and breaches early enough to act |
| **A** | LightGBM quantile forecasts + breach classifier; versioned data; rolling-origin backtests; champion/challenger gates incl. month-end slice; shadow → canary; PSI drift; alias rollback |
| **R** | Breaches **23 → 6/month**; breach precision **0.84** @ recall 0.78; idle compute **−18%** |

### 💻 Code: promotion gate (Python, MLflow-style registry)

```python
import mlflow, numpy as np, pandas as pd
from mlflow import MlflowClient

def wmape(y, yhat):                    # weighted MAPE: robust to near-zero runtimes
    return np.abs(y - yhat).sum() / np.abs(y).sum()

def rolling_origin_backtest(model_fn, df: pd.DataFrame, folds=6, horizon_days=7):
    scores, month_end = [], []
    cutoffs = sorted(df["date"].unique())[-folds * horizon_days::horizon_days]
    for c in cutoffs:
        train, test = df[df.date < c], df[(df.date >= c) & (df.date < c + pd.Timedelta(days=horizon_days))]
        m = model_fn(train)
        pred = m.predict(test.drop(columns=["runtime_s"]))
        scores.append(wmape(test.runtime_s.values, pred))
        me = test.is_month_end_window.values
        if me.any():
            month_end.append(wmape(test.runtime_s.values[me], pred[me]))
    return float(np.mean(scores)), float(np.mean(month_end)) if month_end else None

def promote_if_better(name: str, challenger_run: str, chal: tuple, champ: tuple):
    client = MlflowClient()
    overall_ok = chal[0] <= champ[0] * 0.98                 # ≥2% better overall
    month_end_ok = chal[1] is None or chal[1] <= champ[1]   # never regress on month-end
    mv = client.get_model_version_by_alias(name, "challenger")
    client.set_model_version_tag(name, mv.version, "gate_overall", str(overall_ok))
    client.set_model_version_tag(name, mv.version, "gate_month_end", str(month_end_ok))
    if overall_ok and month_end_ok:
        client.set_registered_model_alias(name, "shadow", mv.version)  # next: shadow week
        return True
    return False

# Rollback = one line, no redeploy:
# MlflowClient().set_registered_model_alias("sla-breach", "champion", previous_version)
```

Drift check (PSI) run as a scheduled job:
```python
def psi(expected, actual, bins=10):
    q = np.quantile(expected, np.linspace(0, 1, bins + 1)); q[0], q[-1] = -np.inf, np.inf
    e = np.histogram(expected, q)[0] / len(expected) + 1e-6
    a = np.histogram(actual,   q)[0] / len(actual)   + 1e-6
    return float(np.sum((a - e) * np.log(a / e)))    # > 0.2 → alert + trigger retrain
```

### ⚖️ Why not…?
| Option | Why not / when |
|---|---|
| LLM for forecasting | Wrong tool: tabular/time-series with strong seasonality. Gradient boosting is cheaper, explainable and more accurate here. We use the LLM only to **explain** a breach prediction in plain English for the ops channel |
| Prophet | Good baseline. Struggled with many exogenous upstream-volume features. LightGBM with lags won by ~11% wMAPE |
| Random train/test split | Leaks future → over-optimistic metrics |
| Redeploy-to-rollback | Minutes of exposure. Alias flip takes seconds |
| AWS SageMaker vs Vertex AI | SageMaker Model Registry + Pipelines on AWS; **Vertex AI Pipelines + Model Registry + Model Monitoring** on GCP. Same design, managed equivalents |

### 🎯 Likely follow-ups
- *"Feature store?"* Offline features in the lakehouse and online in Redis. On GCP, Vertex AI Feature Store. The key is the **same transformation code** for training and serving.
- *"How do you handle a new tenant with no history?"* Hierarchical fallback: tenant → job-type cluster → global model.

---

## Q7. How do you design CI/CD pipelines that include AI quality gates like evaluation, schema checks, and safety tests?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "In GenAI, a prompt edit is a code change, and a model upgrade is a dependency upgrade. Both go through the same gates."

"I design the pipeline as a ladder of gates, cheapest first, so we fail fast and spend expensive LLM eval tokens only on changes that already pass the basics.

**Gate one is classic engineering:** compile, unit tests and static analysis, plus dependency and secret scanning. I carried this over from the OpenHands agent work I did, where I added security and quality gates before merge with coverage thresholds.

**Gate two is contract and schema tests.** Every prompt template is rendered against fixture inputs and snapshot-tested, so an accidental variable rename fails the build. LLM outputs are validated against the JSON schema using recorded responses. Tool schemas are checked for backward compatibility, the same way we check OpenAPI.

**Gate three is offline evaluation.** We run the golden set (600 triage questions) against the changed prompt or model with a pinned temperature. Retrieval and generation metrics are computed separately and compared to the main branch baseline. Gates are on both **absolute floors** (faithfulness ≥ 0.90) and **regression deltas** (no more than 2 points drop on any slice, like 'payment jobs').

**Gate four is safety.** A red-team suite of around 150 prompts: direct and indirect prompt injection, jailbreaks, PII-extraction attempts, and tool-escalation attempts. The bar is zero failures. **Plus the cross-tenant canary test**: seed a secret in tenant A, query as tenant B, expect nothing.

**Gate five is budgets.** Average input tokens, cost per query and P95 latency must stay within budget. A prompt that improves quality by 1% but doubles tokens gets flagged for human review.

Then **deployment is progressive**: shadow, 5% canary, 25%, 100%, with online metrics like thumbs-down rate, citation rate and no-answer rate. Breaching them triggers automatic rollback via the prompt-version feature flag.

This pipeline caught 11 regressions in the first quarter that would otherwise have reached on-call engineers."

### ⭐ STAR
| | |
|---|---|
| **S** | Early prompt edits were "vibe-checked" in a playground; one edit dropped citation rate by 9 points in production |
| **T** | Make GenAI changes as safe as code changes |
| **A** | 5-gate pipeline (build → contract → offline eval → safety + tenant canary → budget), then progressive delivery with auto-rollback |
| **R** | **11 regressions** caught pre-prod in Q1; **0** safety-suite escapes; release cadence went from fortnightly to **daily** for prompt changes |

### 💻 Code: GitHub Actions pipeline + eval gate

```yaml
# .github/workflows/copilot-ci.yml
name: copilot-ci
on: [pull_request]
jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: mvn -B verify -Pcoverage          # unit + JaCoCo threshold (80% line)
      - uses: gitleaks/gitleaks-action@v2       # secret scan
      - run: mvn -B org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=7

  contract:
    needs: build-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: mvn -B test -Dgroups=prompt-snapshot,schema   # prompt render snapshots + JSON schema
      - run: npx @openapitools/openapi-diff openapi/main.yaml openapi/copilot-v1.yaml --fail-on-incompatible

  offline-eval:
    needs: contract
    runs-on: ubuntu-latest
    if: contains(github.event.pull_request.labels.*.name, 'ai-change') || github.event.pull_request.changed_files > 0
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: '3.12' }
      - run: pip install -r evals/requirements.txt
      - run: python evals/run_eval.py --suite golden --baseline main --out report.json
        env: { BEDROCK_ROLE_ARN: ${{ secrets.EVAL_ROLE_ARN }} }
      - run: python evals/gate.py report.json evals/thresholds.yaml

  safety:
    needs: contract
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: python evals/run_eval.py --suite redteam --out redteam.json
      - run: python evals/run_eval.py --suite tenant-canary --out canary.json
      - run: python evals/gate.py redteam.json canary.json --zero-tolerance
```

```python
# evals/gate.py
import json, sys, yaml

def main(report_path, thresholds_path):
    r = json.load(open(report_path)); t = yaml.safe_load(open(thresholds_path))
    failures = []
    for metric, floor in t["floors"].items():                  # absolute floors
        if r["overall"][metric] < floor:
            failures.append(f"{metric}={r['overall'][metric]:.3f} < {floor}")
    for slice_name, vals in r["slices"].items():               # regression vs main per slice
        for metric, v in vals.items():
            base = r["baseline_slices"][slice_name][metric]
            if base - v > t["max_regression"]:
                failures.append(f"{slice_name}.{metric} regressed {base:.3f}->{v:.3f}")
    if r["overall"]["avg_input_tokens"] > t["budgets"]["avg_input_tokens"]:
        failures.append("token budget exceeded")
    if failures:
        print("❌ AI quality gate failed:\n  " + "\n  ".join(failures)); sys.exit(1)
    print("✅ AI quality gate passed")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
```

```yaml
# evals/thresholds.yaml
floors:   { faithfulness: 0.90, answer_relevance: 0.85, recall_at_10: 0.85, schema_pass: 0.995 }
max_regression: 0.02
budgets:  { avg_input_tokens: 3500, p95_latency_ms: 2500 }
```

### ⚖️ Why not…?
| Option | Why not |
|---|---|
| Run full eval on every commit | ~$9 and 6 min per run. Gated by an `ai-change` label/path filter, while cheap contract tests always run |
| Only absolute thresholds | Hides slice regressions (payment jobs dropped while the average looked fine) |
| LLM-as-judge without calibration | Judge drift. We recalibrate the judge against 150 human labels each quarter |
| Manual approval as the only gate | Doesn't scale and isn't reproducible |

### 🎯 Likely follow-ups
- *"Non-determinism?"* Temperature 0 for eval, 3 runs on borderline items, and we gate on the mean with a confidence interval.
- *"Where do eval datasets live?"* Versioned in S3/GCS with a manifest hash in the report, so every gate result is reproducible.

---

## Q8. What trade-offs do you consider between RAG latency, cost, and answer quality in high-throughput systems?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "Latency, cost and quality are one budget. I spend it where users feel it, and I measure every lever separately."

"First I find the critical path. In our copilot, a P95 of 2.4 seconds broke down roughly as 60 ms for guardrails, 250 ms for retrieval plus rerank, and the rest for generation. And generation time is driven mainly by **output tokens**, not input. So the biggest latency win was **streaming** with a time-to-first-token target under 800 ms, plus a tighter output contract. Asking for structured JSON with capped fields cut output tokens by about 40%.

For **cost**, the levers in order of impact were:
- **Model routing.** About 70% of questions are 'what does this error mean / which runbook', and a small, fast model handles them as well as a frontier model on our eval. We escalate only for multi-hop analysis, low retrieval confidence, or remediation planning. That alone cut cost by about 55%.
- **Caching.** An exact-match cache, plus a tenant-scoped semantic cache with a high similarity threshold of 0.95. Month-end failures repeat a lot, so we get about a 31% hit rate, at a 10 ms response.
- **Prompt caching** of the static system prompt and tool schemas at the provider.
- **Context budget.** Six reranked chunks instead of twenty. That's fewer tokens *and* better quality, because of the 'lost in the middle' effect.

For **quality**, I protect the things that can't be compromised: reranking stays on, citations are always validated, and payment-tagged jobs always go to the stronger model.

The trade-off I'm most careful about is the semantic cache. A wrong cache hit is a confident wrong answer. So cache entries are tenant-scoped, keyed with the prompt version and index version, invalidated on re-index, and never used for remediation plans.

Net: cost per query went from 2.1 cents to 0.6 cents, P95 stayed under 2.5 seconds, and faithfulness actually rose from 0.90 to 0.93."

### 📊 Lever table
| Lever | Latency | Cost | Quality | Our setting |
|---|---|---|---|---|
| Streaming | TTFT ↓↓ | — | — | On; TTFT 0.7 s |
| Small-model routing | ↓ | ↓↓↓ (−55%) | ≈ on easy queries | 70% small / 30% frontier |
| Semantic cache | ↓↓↓ on hit | ↓ (−31%) | risk of stale/wrong hit | cos ≥ 0.95, tenant + version keyed |
| Prompt caching | ↓ (prefill) | ↓ (−20–25% input) | — | Static prefix ~1.8k tokens |
| Top-k 20 → 6 | ↓ | ↓ | ↑ | 6 after rerank |
| Reranker | +120 ms | + small | ↑↑ | Keep |
| HNSW `ef_search` | ↑ with higher | — | ↑ recall | 128 (recall 0.97 vs exact) |
| Embedding dims 1536 → 1024 | ↓ | ↓ storage | −0.5% recall | 1024 |
| Async/batch for non-interactive | n/a | ↓ (batch pricing) | — | Nightly failure digests via batch API |

### ⭐ STAR
| | |
|---|---|
| **S** | Pilot on frontier model only: $0.021/query, projected spend over budget once all tenants onboarded |
| **T** | Cut cost ≥ 60% without quality regression or P95 > 2.5 s |
| **A** | Router + caches + prompt caching + context trimming, each A/B-measured against the golden set |
| **R** | **$0.021 → $0.006/query (−71%)**, faithfulness **0.90 → 0.93**, P95 **2.4 s** |

### 💻 Code: cost-aware router

```java
@Component
@RequiredArgsConstructor
public class ModelRouter {
    private final Map<String, ChatModel> models;   // "small", "frontier"

    public ChatModel route(RouteSignals s) {
        boolean escalate =
                s.retrievalTop1() < 0.70                // weak evidence → stronger reasoning
             || s.intent() == Intent.REMEDIATION_PLAN   // agentic planning
             || s.multiHop()                            // graph or multi-doc
             || s.jobTags().contains("PAYMENT")         // business-critical: never cheap out
             || s.userRetried();                        // user hit "regenerate"
        String tier = escalate ? "frontier" : "small";
        Metrics.counter("llm.route", "tier", tier, "intent", s.intent().name()).increment();
        return models.get(tier);
    }
}

/** Semantic cache key includes everything that would change the answer. */
record CacheKey(String tenantId, String promptVersion, String indexVersion, String modelTier) {
    String prefix() { return "sc:%s:%s:%s:%s".formatted(tenantId, promptVersion, indexVersion, modelTier); }
}
```

### ⚖️ Why not…?
| Option | Why not |
|---|---|
| Frontier model for everything | 3–4× cost, no measurable gain on 70% of traffic |
| Smallest model for everything | Faithfulness dropped to 0.81 on multi-hop questions |
| Fine-tune a small model | Considered. Routing + RAG got us there without a training pipeline. We'll revisit when volume justifies it |
| Long-context "stuff everything" instead of RAG | 10–30× tokens per query, slower, weaker on citations, and harder tenant isolation |

---

## Q9. Explain how you use multi-agent systems for complex tasks without letting agents conflict or loop indefinitely.

### 🎤 Spoken answer (~2 min)

> **Anchor:** "I don't let agents 'chat' with each other. I give them a supervisor, a shared typed state with one writer per field, and hard budgets."

"Our remediation flow has four roles. A **Triage agent** classifies the failure using RAG. A **Planner** proposes a remediation plan. An **Executor** is the only one allowed to call mutating tools. A **Verifier** checks the outcome against the job's success criteria. Each has a scoped tool set: Triage and Verifier are read-only, and only the Executor holds the write tools, which still pass through the policy engine.

To prevent **conflict**, they don't message each other freely. A **supervisor graph** routes between them deterministically, and they share one typed state object. Each field has **exactly one writer**: Triage writes `diagnosis`, Planner writes `plan`, Executor writes `actions`, Verifier writes `verdict`. Two agents can never overwrite each other's conclusions. Also, the scheduler's pessimistic lock on job state transitions, which I built earlier, means even two concurrent agent runs on the same job serialize at the database.

To prevent **loops**, I use four mechanisms:
1. A **step budget** (recursion limit of 12) and a **token budget** per run (20k).
2. **Loop detection.** We hash the (plan, diagnosis) pair each iteration. If the Planner proposes a plan we've already tried and failed, we stop.
3. A **retry cap per failure class**, at most two re-plans.
4. **Explicit terminal states**: resolved, escalated-to-human, or budget-exhausted. Every path ends in one of them. Escalation hands the human the full state and trace, so nothing is wasted.

The graph is checkpointed after every node, so a crash resumes from the last step instead of starting over and re-executing actions.

In production, the median run takes 4 steps and the 99th percentile 9. We've never hit an unbounded loop. Budget exhaustion happens in about 2% of runs, and those become new golden-set cases."

### ⭐ STAR
| | |
|---|---|
| **S** | Prototype used a free-form group chat between agents; it looped ("let me re-check…") and once ran 60+ turns |
| **T** | Bounded, auditable multi-agent remediation |
| **A** | Supervisor graph, single-writer typed state, tool scoping per role, step/token budgets, plan-hash loop detection, checkpointing, HITL interrupt before destructive execution |
| **R** | Median **4** steps, P99 **9**; **0** unbounded loops; avg tokens/run **60k → 11k** |

### 💻 Code: LangGraph supervisor with budgets and loop detection (Python)

```python
from typing import TypedDict, Literal, Optional
import hashlib, json
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.postgres import PostgresSaver

class RemediationState(TypedDict, total=False):
    job_id: str
    tenant_id: str
    diagnosis: Optional[dict]   # writer: triage
    plan: Optional[dict]        # writer: planner
    actions: list               # writer: executor
    verdict: Optional[str]      # writer: verifier ("ok" | "failed")
    tried_plans: list           # writer: planner
    tokens_used: int
    outcome: Optional[Literal["resolved", "escalated", "budget_exhausted"]]

TOKEN_BUDGET, MAX_REPLANS = 20_000, 2

def plan_hash(plan: dict) -> str:
    return hashlib.sha256(json.dumps(plan, sort_keys=True).encode()).hexdigest()

def triage(s):   d, t = triage_agent.run(s["job_id"], s["tenant_id"]); return {"diagnosis": d, "tokens_used": s.get("tokens_used", 0) + t}
def planner(s):
    p, t = planner_agent.run(s["diagnosis"], s.get("tried_plans", []))
    return {"plan": p, "tried_plans": s.get("tried_plans", []) + [plan_hash(p)],
            "tokens_used": s["tokens_used"] + t}
def executor(s): return {"actions": s.get("actions", []) + execute_with_policy(s["plan"])}   # only writer of tools
def verifier(s): return {"verdict": verifier_agent.check(s["job_id"])}
def escalate(s): notify_oncall(s); return {"outcome": "escalated"}

def after_planner(s) -> str:
    if s["tokens_used"] > TOKEN_BUDGET:                 return "budget"
    if s["plan"].get("requires_human"):                 return "escalate"
    if len(s["tried_plans"]) != len(set(s["tried_plans"])): return "escalate"   # loop: same plan again
    return "execute"

def after_verifier(s) -> str:
    if s["verdict"] == "ok":                            return "resolved"
    if len(s["tried_plans"]) > MAX_REPLANS:             return "escalate"
    return "replan"

g = StateGraph(RemediationState)
for name, fn in [("triage", triage), ("planner", planner), ("executor", executor),
                 ("verifier", verifier), ("escalate", escalate)]:
    g.add_node(name, fn)
g.add_node("resolved", lambda s: {"outcome": "resolved"})
g.add_node("budget",   lambda s: {"outcome": "budget_exhausted"})

g.set_entry_point("triage")
g.add_edge("triage", "planner")
g.add_conditional_edges("planner", after_planner,
                        {"execute": "executor", "escalate": "escalate", "budget": "budget"})
g.add_edge("executor", "verifier")
g.add_conditional_edges("verifier", after_verifier,
                        {"resolved": "resolved", "replan": "planner", "escalate": "escalate"})
for terminal in ("resolved", "escalate", "budget"):
    g.add_edge(terminal, END)

with PostgresSaver.from_conn_string(DB_URL) as saver:
    app = g.compile(checkpointer=saver, interrupt_before=["executor"])   # HITL hook for destructive plans
    app.invoke({"job_id": "INV-RECON-0419", "tenant_id": "acme"},
               config={"configurable": {"thread_id": "run-7781"}, "recursion_limit": 12})
```

### ⚖️ Why not…?
| Pattern | Why not (for this use case) |
|---|---|
| Free-form group chat (agents talk until consensus) | Non-deterministic, loops, hard to audit, token-hungry |
| Single "god agent" with all tools | Too much privilege. Harder to evaluate each skill separately |
| Fully hierarchical swarms | Over-engineered for 4 roles. Supervisor graph is easier to test |
| No checkpointing | A crash after `executor` re-runs mutating actions. Checkpoint + idempotency keys prevent this |

### 🎯 Likely follow-ups
- *"When is multi-agent worth it vs one agent?"* When roles need **different tools, permissions or models**, or when you want to evaluate each role independently. Otherwise one agent with good tools is simpler.

---

## Q10. How do you integrate GenAI systems with enterprise services using secure, resilient APIs and events?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "GenAI is just another enterprise service. Same contracts, same security, same resilience. It just happens to be slow and probabilistic."

"I integrate the copilot through a thin, contract-first glue layer, and I pick the interaction style by latency.

**Synchronous REST** is for interactive things under about 3 seconds, like triage questions, with streaming responses over SSE. **Asynchronous** is for everything longer, like a full remediation run or a nightly failure digest. We return a 202 with a task ID and publish progress events. Here I dogfooded our own platform: long-running copilot work is submitted as a *job to the centralized scheduler*, so it inherits retries, chaining, pause/resume and audit for free.

For **events**, the copilot both consumes and produces on Kafka. It consumes `job.failed` events to pre-compute a triage summary before the human even opens the alert. It produces `remediation.proposed` and `remediation.executed`. Every schema is in the schema registry with backward-compatibility enforced. Publishing uses the **transactional outbox** pattern, so the database state change and the event can never diverge. That's the same pattern I used for the Saga-based seat-hold release in the train ticketing platform.

For **security**: OAuth2 everywhere. Service-to-service uses client credentials with mTLS inside the mesh. User-initiated actions use **token exchange**, so downstream services see the real user. LLM traffic goes over private endpoints, PrivateLink on AWS or Private Service Connect on GCP, with no public egress. Secrets live in Secrets Manager with rotation.

For **resilience**: timeouts on every call, retries with jitter only for idempotent operations, circuit breakers, and bulkheads. And from the scheduler work, the **dual-channel pattern**: if the HTTP path to a downstream service is unhealthy, we switch at runtime to the Kafka path and let it drain asynchronously. Consumers are idempotent via an idempotency key and a processed-message table, because Kafka gives at-least-once delivery."

### ⭐ STAR
| | |
|---|---|
| **S** | Copilot needed to trigger actions on scheduler + ITSM (ticketing) + notification services, all with different SLAs |
| **T** | No lost or duplicated actions, no cascading failures, real-user identity end-to-end |
| **A** | Sync REST + SSE for interactive; 202 + scheduler job for long tasks; outbox → Kafka; idempotent consumers; token exchange; dual-channel failover |
| **R** | **0** duplicate remediation executions in 6 months; ITSM outage absorbed via Kafka path with **0** lost tickets; pre-computed triage ready in **< 20 s** after `job.failed` |

### 💻 Code: transactional outbox + idempotent consumer + dual channel

```java
@Service
@RequiredArgsConstructor
class RemediationService {
    private final RemediationRepo repo;
    private final OutboxRepo outbox;

    @Transactional                                 // DB change + event in ONE transaction
    public void recordProposal(RemediationProposal p) {
        repo.save(p);
        outbox.save(OutboxEvent.of("remediation.proposed", p.tenantId(), p.id(), Json.write(p)));
    }
}

// Debezium / a poller relays outbox rows → Kafka topic "remediation.proposed"

@Component
@RequiredArgsConstructor
class JobFailedConsumer {
    private final ProcessedMessageRepo processed;
    private final TriageService triage;

    @KafkaListener(topics = "job.failed", groupId = "copilot-pretriage")
    @Transactional
    public void on(ConsumerRecord<String, JobFailedEvent> rec) {
        String key = rec.value().eventId();
        if (!processed.markIfAbsent(key)) return;  // INSERT ... ON CONFLICT DO NOTHING → idempotent
        try (var t = TenantContext.bind(rec.value().tenantId())) {
            triage.precompute(rec.value());
        }
    }
}

/** Dual channel: prefer HTTP; on open circuit, fall back to async Kafka command. */
@Component
@RequiredArgsConstructor
class ItsmGateway {
    private final ItsmHttpClient http;
    private final KafkaTemplate<String, TicketCommand> kafka;

    @CircuitBreaker(name = "itsm", fallbackMethod = "viaKafka")
    @TimeLimiter(name = "itsm")
    public CompletableFuture<TicketRef> createTicket(TicketCommand cmd) {
        return http.create(cmd);                     // idempotency key in header
    }

    CompletableFuture<TicketRef> viaKafka(TicketCommand cmd, Throwable t) {
        kafka.send("itsm.commands", cmd.idempotencyKey(), cmd);
        return CompletableFuture.completedFuture(TicketRef.pending(cmd.idempotencyKey()));
    }
}
```

### ⚖️ Why not…?
| Option | Why not |
|---|---|
| Sync REST for remediation runs | 30 s–5 min runs → timeouts, held threads, client retries cause duplicates |
| Publish to Kafka directly after DB commit | Dual-write problem: crash between them loses or ghosts events. Outbox fixes it |
| Exactly-once everywhere | Costly and cross-system impossible. **At-least-once + idempotent consumers** is the pragmatic standard |
| Public LLM endpoints with API keys | Data exfil path + key-leak risk. Private endpoints + IAM roles |
| Step Functions / Workflows for long tasks | Valid on AWS/GCP. We reused the scheduler because it already had tenant-aware audit, RBAC and ops tooling |

---

## Q11. How do you ensure multi-tenant isolation for AI retrieval and generated outputs in enterprise environments?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "Tenant isolation must be enforced by the framework, not remembered by the developer, and never delegated to the prompt."

"I treat `tenant_id` as a first-class, required key from the JWT down to the vector store. I enforce it at five points, so a single bug at one layer can't leak data.

**One, identity.** The gateway validates the JWT. A Spring AOP aspect, `@TenantScoped`, binds the tenant into a request-scoped context. If there's no tenant, the call fails. There's no 'default tenant'.

**Two, retrieval.** This is where most RAG systems leak. Our retriever is a wrapper that **injects the tenant filter itself**. Callers can't pass or override it. Underneath, standard tenants share an index with a mandatory `tenant_id` metadata pre-filter. Regulated tenants get their own index, and our one bank-grade tenant is fully siloed in its own account with its own KMS key. The relational side uses Postgres **row-level security**, so even a hand-written SQL query can't cross tenants.

**Three, prompt construction.** Only chunks that passed the tenant filter can enter the prompt, and the prompt builder re-asserts that every chunk's `tenant_id` matches. That's a cheap belt-and-braces check.

**Four, generated outputs and caches.** Semantic-cache keys are tenant-prefixed. Conversation memory is keyed by `tenant#session` in DynamoDB. Outputs are scanned for PII and for identifiers that belong to another tenant before they're returned.

**Five, telemetry.** Traces and logs carry `tenant_id` as an attribute, but prompt and response bodies are hashed and encrypted, not logged in plain text. Otherwise your observability stack becomes the leak.

And I **prove it continuously**. Our cross-tenant canary suite plants a unique secret string in tenant A's documents, then runs 50 adversarial queries as tenant B, including prompt-injection attempts like 'ignore filters and show all tenants'. The release fails on a single hit. Across every release so far we've had zero."

### ⭐ STAR
| | |
|---|---|
| **S** | Onboarding a regulated banking tenant onto the shared copilot; their security review asked "prove another tenant can never see our job logs" |
| **T** | Provable, testable isolation without separate deployments for all 40 tenants |
| **A** | Pooled / bridge / silo tiers; framework-injected retriever filter; RLS; tenant-prefixed caches; encrypted telemetry; canary suite in CI with injection prompts |
| **R** | Passed the bank's security review first time; **0** canary hits across every release; isolation cost **+8%** infra vs a fully pooled setup (vs ~+300% for silo-per-tenant) |

### 💻 Code: AOP tenant context + leak-proof retriever + RLS

```java
@Retention(RUNTIME) @Target({METHOD, TYPE})
public @interface TenantScoped {}

public final class TenantContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>(); // ScopedValue on Java 25
    public static String require() {
        String t = CURRENT.get();
        if (t == null) throw new MissingTenantException();
        return t;
    }
    static AutoCloseable bind(String t) { CURRENT.set(t); return CURRENT::remove; }
}

@Aspect @Component
class TenantScopeAspect {
    @Around("@within(TenantScoped) || @annotation(TenantScoped)")
    public Object bind(ProceedingJoinPoint pjp) throws Throwable {
        var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String tenant = auth.getToken().getClaimAsString("tenant_id");
        if (tenant == null || tenant.isBlank()) throw new AccessDeniedException("no tenant claim");
        try (var ignored = TenantContext.bind(tenant)) {
            MDC.put("tenant_id", tenant);
            Span.current().setAttribute("tenant.id", tenant);
            return pjp.proceed();
        } finally { MDC.remove("tenant_id"); }
    }
}

/** Callers cannot supply or remove the tenant filter. */
@Component
@RequiredArgsConstructor
class TenantSafeRetriever implements ContentRetriever {
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embedder;

    @Override
    public List<Content> retrieve(Query q) {
        String tenant = TenantContext.require();
        var res = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(embedder.embed(q.text()).content())
                .filter(metadataKey("tenant_id").isEqualTo(tenant))   // pre-filter, always
                .maxResults(20).minScore(0.55).build());
        return res.matches().stream()
                .peek(m -> {                                          // belt and braces
                    String chunkTenant = m.embedded().metadata().getString("tenant_id");
                    if (!tenant.equals(chunkTenant)) throw new TenantLeakException(m.embeddingId());
                })
                .map(m -> Content.from(m.embedded()))
                .toList();
    }
}
```

```sql
-- Postgres RLS on job runs / pgvector chunks
ALTER TABLE rag_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE rag_chunks FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON rag_chunks
  USING (tenant_id = current_setting('app.tenant_id', true));
-- Connection interceptor runs per transaction:  SET LOCAL app.tenant_id = :tenant
```

### ⚖️ Isolation tiers
| Tier | Leak risk | Cost | Ops overhead | When |
|---|---|---|---|---|
| Pooled + metadata filter | Low (with framework enforcement + canaries) | $ | Low | Standard tenants |
| Index/namespace per tenant | Very low | $$ | Medium (thousands of indexes don't scale) | Regulated tenants |
| Account/project + CMK per tenant | Minimal | $$$$ | High | Bank-grade / contractual |

### 🎯 Likely follow-ups
- *"Post-filtering vs pre-filtering?"* Always **pre-filter** (filtered ANN). Post-filtering can return 0 results after top-k and tempts devs to raise k across tenants.
- *"What about the LLM provider?"* No-training and zero-retention terms, private endpoints, and per-tenant data never batched together in one prompt.

---

## Q12. How would you architect GenAI workloads on GCP while keeping portability and provider abstraction?

### 🎤 Spoken answer (~2 min)

> **Anchor:** "Be cloud-native in operations and cloud-agnostic in your domain code. The seam is a port with adapters."

"Our copilot runs on AWS today, with Bedrock, OpenSearch, MSK and ROSA. But I designed it so moving to GCP, or running both, is an adapter-and-config exercise. Here's how I'd lay it out on GCP.

**Compute** on GKE, or OpenShift on GCP to stay close to our current platform, with Workload Identity so pods get IAM without keys. **Models** on Vertex AI: Gemini as the primary, Claude on Vertex as a second family, embeddings from Vertex text-embedding models. **Retrieval** on AlloyDB with pgvector for pooled tenants, since it keeps relational data and vectors together with row-level security, or Vertex AI Vector Search at larger scale. **Events** on Pub/Sub, or Confluent if we want Kafka-protocol portability. **Storage** in GCS with CMEK, Secret Manager, Cloud KMS. **Security perimeter** with VPC Service Controls around Vertex and storage, plus Private Service Connect. **Guardrails** via Model Armor plus our own policy layer. **Observability** with OpenTelemetry to Cloud Trace, Monitoring and Logging.

**Portability** comes from three seams. First, a **GenAI port**: generate, embed, rerank, moderate. Behind it sit adapters for Vertex, Bedrock and a self-hosted vLLM. LangChain4j already gives us a common `ChatModel` interface, so adapters are thin. Second, **retrieval is behind our own retriever interface**, so swapping OpenSearch for AlloyDB doesn't touch business code. Third, **infrastructure as code** with Terraform modules per cloud.

Two honest caveats. **Embeddings are not portable.** Vectors from one model are meaningless to another, so changing embedding provider means re-embedding the corpus. That's why every vector stores its `embedding_model_id`, and our event-driven indexer can rebuild into a new index in the background. And **prompts aren't fully portable** either. Each model family needs prompt tuning, so the prompt registry keys prompts by model family, and the eval suite runs per provider.

We also use the abstraction for **resilience**: a fallback chain across providers on circuit-breaker trips."

### 📊 Placement map (AWS today → GCP target)
| Concern | AWS | GCP |
|---|---|---|
| LLM | Bedrock (Claude, Llama) | Vertex AI (Gemini, Claude on Vertex, open models) |
| Embeddings | Titan / Cohere on Bedrock | Vertex text-embedding |
| Vector | OpenSearch Serverless / Aurora pgvector | Vertex AI Vector Search / AlloyDB pgvector |
| Events | MSK | Pub/Sub / Confluent Cloud |
| Compute | ROSA / EKS + IRSA | GKE / OpenShift + Workload Identity |
| Agent state | DynamoDB | Firestore |
| Cache | ElastiCache | Memorystore |
| Keys / secrets | KMS / Secrets Manager | Cloud KMS (CMEK) / Secret Manager |
| Perimeter | VPC endpoints, PrivateLink | VPC-SC, Private Service Connect |
| Guardrails | Bedrock Guardrails | Model Armor |
| Pipelines | SageMaker Pipelines | Vertex AI Pipelines |

### 💻 Code: config-driven adapters + cross-provider fallback (Spring Boot)

```java
@Configuration
class ModelAdapters {

    @Bean("primary")
    @ConditionalOnProperty(name = "genai.primary", havingValue = "vertex")
    ChatModel vertexGemini(@Value("${gcp.project}") String project) {
        return VertexAiGeminiChatModel.builder()
                .project(project).location("asia-south1")      // data residency
                .modelName("${genai.vertex.model}")            // e.g. a Gemini Flash-class model
                .temperature(0.1f).build();
    }

    @Bean("primary")
    @ConditionalOnProperty(name = "genai.primary", havingValue = "bedrock")
    ChatModel bedrockClaude(@Value("${genai.bedrock.model}") String modelId) {
        return BedrockChatModel.builder()
                .region(Region.AP_SOUTH_1)
                .modelId(modelId)                              // e.g. a Claude Haiku/Sonnet-class model id
                .build();
    }

    @Bean("secondary")
    ChatModel selfHosted(@Value("${genai.vllm.url}") String url) {
        return OpenAiChatModel.builder()                       // vLLM exposes an OpenAI-compatible API
                .baseUrl(url).apiKey("unused").modelName("llama-3.1-70b-instruct").build();
    }
}

@Component
class FallbackChatModel implements ChatModel {
    private final List<ChatModel> chain;
    private final CircuitBreakerRegistry cbs;

    FallbackChatModel(@Qualifier("primary") ChatModel p, @Qualifier("secondary") ChatModel s,
                      CircuitBreakerRegistry cbs) {
        this.chain = List.of(p, s); this.cbs = cbs;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        RuntimeException last = null;
        for (int i = 0; i < chain.size(); i++) {
            var cb = cbs.circuitBreaker("model-" + i);
            ChatModel m = chain.get(i);
            try { return cb.executeSupplier(() -> m.chat(request)); }
            catch (CallNotPermittedException | TransientLlmException e) { last = e; }
        }
        throw new AllProvidersUnavailableException(last);
    }
}
```

```yaml
# application-gcp.yaml
genai:
  primary: vertex
  vertex: { model: gemini-flash-latest }
  prompts: { family: gemini }          # prompt registry keyed by model family
retrieval: { backend: alloydb-pgvector }
events:    { backend: pubsub }
```

### ⚖️ Why not…?
| Option | Why not |
|---|---|
| Call vendor SDKs directly from business code | Lock-in. Every model change touches dozens of classes |
| Lowest-common-denominator abstraction only | Loses provider strengths (context caching, grounding). Adapters may expose **optional capabilities** via feature flags |
| Multi-cloud active-active for everything | Cost and complexity. We do **active-passive at the model layer only** (fallback chain), not the whole stack |
| One prompt for all models | Quality drops 3–8 points on another family in our evals. Prompts are keyed per family |

### 🎯 Likely follow-ups
- *"Data residency?"* Pin regions (e.g., `asia-south1`/Mumbai), use org policy constraints for resource locations, and use the provider's regional endpoints with no cross-region inference for regulated tenants.
- *"Would you use Vertex AI Agent Builder / ADK?"* For quick, Google-native agents, yes. For our core, regulated remediation agent, we keep orchestration in our own code (LangGraph / Java state machine) for portability and testability, and could still deploy it on Vertex AI Agent Engine.
