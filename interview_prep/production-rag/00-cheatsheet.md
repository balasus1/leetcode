# Production RAG Quick Reference Cheatsheet & Architecture Guide

> **Source:** Based on the *Production RAG with LangChain & Vector Databases* curriculum (freeCodeCamp / Paulo Dichone) and enterprise production patterns.  
> **Target Audience:** Senior AI Engineer / Enterprise AI Solution Architect  
> **Core Stack:** FastAPI, Python, Neon PostgreSQL (`pgvector`), Upstash Redis, LangChain / LangGraph, LangSmith, and Gemini API.

---

## 1. End-to-End Architectural Tool Stack

| Architectural Layer | Core Course Tool | Production Responsibility | Industry Alternatives to Mention |
| :--- | :--- | :--- | :--- |
| **Orchestration** | **LangChain (LCEL)** | Loaders, text splitters, retrievers, prompt formatting, and async streaming chains (`\|`). | LlamaIndex, Haystack, raw SDK native async clients |
| **Agent / Control Flow** | **LangGraph** | Stateful cyclic graph: query routing, document grading, self-correcting rewrite loops, and human-in-the-loop checkpoints. | CrewAI, AutoGen, custom state machines |
| **Document Ingestion** | **LangChain Loaders** | Parsing unstructured PDFs, Markdown, HTML, and Word documents while extracting page/source metadata. | Unstructured.io, LlamaParse, Docling |
| **Chunking & Splitting** | **Recursive Character Splitter** | Slicing text along paragraph/sentence hierarchies (~500–800 tokens, 10–20% overlap). | Semantic chunking, Late chunking, AST code splitters |
| **Dense Embeddings** | **OpenAI / Gemini Embeddings** | Mapping text to high-dimensional semantic vectors (768 to 1,536 dimensions). | Cohere Embed v3, BAAI/BGE-M3, Jina-v3 |
| **Vector Store (Dev)** | **Chroma DB** | Local embedded in-process database for rapid sandboxing and automated CI unit tests. | FAISS, LanceDB, DuckDB |
| **Vector Store (Prod)** | **Supabase / Neon (pgvector)** | Production relational ACID storage + metadata joins + Row-Level Security (RLS) + HNSW vector index. | Qdrant, Pinecone, Weaviate, Milvus |
| **Keyword Retrieval** | **BM25 / Postgres FTS** | Exact sparse lexical retrieval for alphanumeric SKUs, error codes, legal IDs, and exact names. | Elasticsearch, OpenSearch |
| **Rank Fusion** | **Reciprocal Rank Fusion (RRF)** | Merging keyword and vector candidate lists by reciprocal rank: `Score = sum(1 / (60 + rank))`. | Linear score combination ($\alpha$), Cross-encoders |
| **Re-Ranking** | **Cross-Encoder / Cohere Rerank** | Scoring joint (query + document) token attention across top-30 candidates down to top-5. | BGE-Reranker-large, ColBERT late interaction |
| **Foundation Model (LLM)**| **Gemini 1.5 Pro / Flash** | Grounded generative synthesis, contextual reasoning, and structured citation formatting. | Claude 3.5 Sonnet, GPT-4o, Llama 3 via vLLM / Ollama |
| **API Transport Tier** | **FastAPI** | Asynchronous non-blocking endpoints, Pydantic V2 validation, and Server-Sent Events (SSE) streaming. | Spring Boot 3 WebFlux, Express.js |
| **Observability & Tracing**| **LangSmith** | Distributed tracing, component latency/token breakdown, similarity distributions, and eval datasets. | Langfuse, Arize Phoenix, OpenTelemetry |
| **Security & Guardrails** | **NeMo Guardrails / Presidio** | PII redaction, direct/indirect prompt injection defense, token rate limits, and least-privilege tools. | Guardrails AI, Lakera, Llama-Guard |
| **Advanced Retrieval** | **Contextual / ColPali / GraphRAG**| Document preambles (Contextual), multi-vector page images (ColPali), knowledge graph communities (GraphRAG). | Neo4j, Microsoft GraphRAG, ColQwen |

---

## 2. Design Decisions & Trade-Offs Matrix

| Decision Domain | Default Production Choice | Architectural Trade-Off | When to Switch / Alternative |
| :--- | :--- | :--- | :--- |
| **Vector Store** | **pgvector on Supabase / Neon** | One unified database for relational data, metadata, auth, and vectors; requires RAM tuning for HNSW. | Switch to **Qdrant** or **Pinecone** if scaling past 50M–100M vectors with high write throughput. |
| **Dev vs. Prod Store** | **Chroma locally, pgvector in prod** | Instant local iteration without cloud dependencies vs. enterprise durability and ACID guarantees. | Switch to containerized pgvector (Testcontainers) when testing complex Row-Level Security policies. |
| **Chunking Strategy** | **Recursive Character (512 tokens, 15% overlap)** | Simple, fast, deterministic; can occasionally sever thoughts mid-sentence. | Switch to **Contextual Retrieval** or **Late Chunking** if evaluations show cross-chunk context loss. |
| **Embedding Dims** | **Mid-size (768 to 1,536 dims)** | High semantic expressiveness vs. linear memory footprint ($V \times D \times 4$ bytes). | Use **Matryoshka Truncation** (to 512/768) or **Scalar Quantization (SQ8)** to cut RAM by 50–75%. |
| **Retrieval Engine** | **Hybrid Search (BM25 + Dense) with RRF** | Requires dual indexing (inverted index + HNSW); dramatically improves exact alphanumeric recall. | Use **Dense-only** for purely conceptual narrative text without part numbers, codes, or proper names. |
| **Precision Layer** | **Two-Stage: Top-30 retrieve $\to$ Re-rank top-5** | Adds ~35–80ms latency and small compute cost; delivers 20–30% boost in answer precision. | Skip re-ranking only on ultra-strict sub-200ms latency SLAs where top-3 vector hits are sufficient. |
| **Control Flow** | **LangGraph Agent (Grading & Rewriting)** | Adds LLM hops and execution latency; eliminates bad answers via self-correcting retry loops. | Use a **Simple LCEL Chain** for trivial internal FAQ lookups where failure impact is negligible. |
| **Long Context vs RAG**| **RAG for enterprise dynamic corpora** | Retrieval misses vs. massive prompt token billing, 15s+ latency, and "lost-in-the-middle" degradation. | Use **Pure Long Context** for static, offline analysis of a single complete book or codebase. |
| **pgvector Index** | **HNSW (Hierarchical Navigable Small World)** | Consumes ~1.5GB RAM per 1M vectors and takes longer to build; delivers sub-10ms queries at 98%+ recall. | Use **IVFFlat** only for massive bulk backfills or on severe memory-constrained test databases. |
| **Observability** | **LangSmith distributed tracing** | Managed SaaS dependency with deep LangChain hooks; visualizes prompts, chunks, and scores. | Switch to **Langfuse** or **OpenTelemetry** for air-gapped on-premise or strict HIPAA environments. |
| **Multi-Tenancy** | **Server-side JWT validation + Postgres RLS** | Requires careful schema design; cryptographically prevents cross-tenant data leakage. | Provision **Dedicated Database Schemas** for Tier-1 enterprise clients demanding physical data isolation. |
| **Caching Tier** | **Redis Semantic Cache + Provider Prompt Cache** | Risk of serving stale data if docs change; slashes per-query costs by 30% and drops latency to <25ms. | Disable or set short TTLs for rapidly fluctuating data like live stock quotes or active tickets. |
| **Graph-Based RAG** | **GraphRAG (Entity-Relation Extraction)** | Heavy offline LLM extraction cost and complex updates; enables multi-hop and global corpus synthesis. | Use standard **Vector RAG** for localized questions that live within 1–2 specific document passages. |
| **Multimodal Documents**| **ColPali (Vision-Language Page Embeddings)** | Multi-vector storage footprint; completely bypasses fragile OCR pipelines for tables and diagrams. | Use **Standard Text Loaders (Docling)** for plain, single-column text PDFs with no visual layout. |

---

## 3. Master 60-Question Cheatsheet (Question ID $\to$ 3 Core Key Terms)

### Level 1 — Basic Concepts (B1 to B15)
- **B1 (What is RAG):** Grounding • Hallucination mitigation • Parametric vs non-parametric memory
- **B2 (Two Pipelines):** Offline indexing • Online querying • Approximate Nearest Neighbors (ANN)
- **B3 (Embedding):** Dense vector • Continuous latent space • Cosine similarity
- **B4 (Vector DB vs Normal DB):** HNSW graph indexing • Curse of dimensionality • Metadata pre-filtering
- **B5 (Cosine vs Dot vs L2):** Unit normalization • Angular difference • SIMD inner product
- **B6 (Why Chunk):** Context dilution • Embedding token limits • Granularity matching
- **B7 (Chunk Overlap):** Boundary splitting • Coreference preservation • 10–20% sliding window
- **B8 (Top-k):** Retrieval candidate count • Recall vs noise trade-off • Lost-in-the-middle
- **B9 (Document Loaders):** Unstructured parsing • Metadata preservation • Layout awareness
- **B10 (Metadata Importance):** Row-Level Security (RLS) • Pre-filtering • Verifiable citations
- **B11 (LangChain & LCEL):** Declarative composition (`\|`) • Streaming runtimes • Async primitives
- **B12 (Chroma DB):** In-process embedded store • Local prototyping • Zero-infrastructure testing
- **B13 (Similarity Score):** Relative closeness • Confidence thresholds • Topical relevance vs truth
- **B14 (Hallucinations):** Ungrounded generation • Negative constraint prompts • Ragas Faithfulness
- **B15 (Prompt Template):** Structural delimiters (XML) • Instruction hierarchy • Source attribution

### Level 2 — Intermediate Building & Debugging (I1 to I15)
- **I1 (Chunk Size Tuning):** Empirical evaluation • Query granularity • 256–1024 token benchmarks
- **I2 (Recursive vs Semantic):** Separator hierarchy • Sentence embedding distance • 5x compute trade-off
- **I3 (Embedding Dimensions):** Matryoshka truncation • HNSW RAM footprint • Latency vs nuance
- **I4 (Picking Embedders):** MTEB leaderboard • Domain evaluation sets • Context window capacity
- **I5 (Debugging Bad Answers):** LangSmith trace inspection • Decoupled retrieval analysis • Chunk boundary fixes
- **I6 (Retrieval vs Generation Failure):** Ground truth in top-k • Prompt distraction • Cross-encoder re-ranking
- **I7 (Hybrid Search):** BM25 sparse matching • Dense vector semantics • Alphanumeric entity recall
- **I8 (Rank Fusion):** Reciprocal Rank Fusion (RRF) • Linear score combination ($\alpha$) • Scale calibration
- **I9 (Token Budgeting):** Context window partitioning • Sliding window memory • Predictable latency
- **I10 (Context Exceeds Budget):** Two-stage pruning • Contextual compression • Sentence extraction
- **I11 (Maximal Marginal Relevance):** Diversity vs relevance • Lambda ($\lambda$) weighting • Redundancy penalty
- **I12 (Two-Stage Re-Ranking):** Bi-encoder speed • Cross-encoder joint attention • NDCG@5 boost
- **I13 (Query Rewriting / HyDE):** Hypothetical embeddings • Multi-query expansion • Semantic bridging
- **I14 (Updates & Deletes):** Deterministic document IDs • SHA-256 content hashes • Atomic upserts
- **I15 (LangSmith Observability):** Distributed execution traces • Token/latency attribution • CI eval datasets

### Level 3 — Advanced Production Architecture (A1 to A15)
- **A1 (Three Pillars of Visibility):** Structured JSON logs • P99 latency/token metrics • LangSmith distributed traces
- **A2 (RAG Evaluation):** Ragas framework • Context Recall/Precision • Faithfulness & Answer Relevance
- **A3 (Chroma to Supabase/pgvector):** Unified relational joins • PostgreSQL RLS • Enterprise ACID durability
- **A4 (IVFFlat vs HNSW):** Multi-layer skip graphs • Voronoi clusters • RAM vs query speed trade-off
- **A5 (Real Costs of Vector Search):** $V \times D \times 4$ bytes formula • HNSW RAM overhead • LLM context token dominance
- **A6 (Scaling to Millions):** Declarative partitioning • Scalar Quantization (SQ8) • Asynchronous Kafka ingestion
- **A7 (Where to Cache):** Query vector cache • Redis semantic response cache • Provider prompt caching
- **A8 (Secure Multi-Tenancy):** Server-side JWT validation • PostgreSQL RLS policies • Zero client-filter trust
- **A9 (Prompt Injection Defense):** Direct vs indirect injection • Structural XML delimiters • Least-privilege tools
- **A10 (RAG Security Checklist):** Microsoft Presidio PII redaction • Token bucket rate limiting • Tamper-proof audit logs
- **A11 (LangGraph vs Chains):** Cyclic state machines • Conditional edge routing • Durable PostgreSQL checkpointing
- **A12 (FastAPI Design):** Asynchronous non-blocking I/O • Pydantic V2 • Server-Sent Events (SSE) streaming
- **A13 (Latency Reduction):** Concurrent `asyncio.gather` • Quantized ONNX re-rankers • Time-to-First-Token (TTFT)
- **A14 (LLM Cost Control):** Dynamic SLM routing • Redis semantic caching • Context pruning & prompt caching
- **A15 (CI/CD Testing):** Golden dataset regression gates • Testcontainers with pgvector • Automated Ragas assertions

### Level 4 — Architect & System Design (S1 to S15)
- **S1 (1M Context vs RAG):** Pre-fill latency penalty • Token unit economics • Dynamic access control
- **S2 (Contextual Retrieval):** Anthropic preambles • Document-level summarization • Prompt caching ingestion
- **S3 (Late Chunking):** Long-context bidirectional attention • Token-level mean pooling • Global context retention
- **S4 (Agentic Self-Correcting RAG):** LangGraph state graphs • Document relevance grading • Hallucination verification
- **S5 (When to Use GraphRAG):** Multi-hop entity traversal • Leiden community detection • Global corpus synthesis
- **S6 (Multimodal ColPali):** Vision-Language Models (PaliGemma) • Late-interaction MaxSim • Zero OCR pipeline
- **S7 (Design 10k Policy PDFs):** Docling layout parsing • PostgreSQL RLS • 768-dim HNSW with hybrid RRF
- **S8 (Customer Support <3s SLA):** Sub-25ms Redis semantic cache • Parallel search • High-throughput SLM streaming
- **S9 (Zero-Downtime Migration):** Dual-write ingestion • Historical shadow backfill • Atomic database view swap
- **S10 (Vector DB Selection Matrix):** pgvector consolidation • Qdrant Rust speed • Pinecone serverless ease
- **S11 (Tables, Code & Scanned Docs):** Markdown table reconstruction • Tree-sitter AST splitters • ColPali visual embeddings
- **S12 (Trust for Business Users):** Verifiable inline citations • Side-by-side PDF preview • Calibrated abstention fallback
- **S13 (Production Failure Post-Mortem):** HNSW memory paging • Stale chunk accumulation • PostgreSQL `shared_buffers` tuning
- **S14 (RAG Evolution Roadmap):** Naive $\to$ Advanced $\to$ Modular $\to$ Agentic/Multimodal • Enterprise CDC pipelines
- **S15 (Build vs Buy Decision):** Time-to-market vs control • Data sovereignty & compliance • Long-term unit economics

---

## 4. Master 60–90 Second Architect Answer Frame

When asked any system design question in an interview, deliver your spoken answer using this exact 8-stage sequence:

```text
[1. Requirements & Constraints] ──> Clarify scale, concurrency, latency SLA, and access control.
              │
[2. Ingestion Pipeline]          ──> Loaders (Docling/LlamaParse) ──> Chunking ──> Metadata Enrichment ──> Embedder.
              │
[3. Retrieval Tier]              ──> Semantic Cache (Redis) ──> Hybrid Search (BM25 + pgvector HNSW) ──> RRF Fusion.
              │
[4. Generation Tier]             ──> Two-Stage Re-ranking (Top-5) ──> XML Prompt Delimiters ──> SLM/LLM ──> SSE Stream.
              │
[5. Evaluation & Evals]          ──> Offline Golden Datasets in CI (Ragas Faithfulness & Recall) ──> Online sampling.
              │
[6. Security & Governance]       ──> Server-side JWT claims ──> PostgreSQL Row-Level Security ──> Presidio PII redaction.
              │
[7. Cost & Infrastructure Scale] ──> Storage sizing (V x D x 4 bytes) ──> Dynamic SLM routing ──> Redis caching.
              │
[8. Monitoring & Observability]  ──> LangSmith distributed tracing ──> P99 TTFT latency ──> Citation click telemetry.
```
