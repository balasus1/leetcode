# Level 2 — Intermediate RAG Interview Questions & Spoken Answers

> **Target Role:** Senior AI Engineer / AI Architect  
> **Spoken Target:** ~1.5 minutes per answer (220–250 words at 150 wpm)  
> **Delivery Style:** First person, conversational, confident, zero bullet points inside spoken answers.

---

## I1 — How do you choose chunk size?

### Spoken answer (~210 words, ~84 sec)

I choose chunk size by conducting empirical evaluations driven by the structural density of the source documents, the expected granularity of user queries, and the context window of our embedding model. Rather than picking an arbitrary number like 500, I analyze whether our end-users ask needle-in-a-haystack factual questions or broad conceptual synthesis questions. In our enterprise technical support platform deployed on FastAPI and Neon PostgreSQL, users frequently ask about exact configuration flags and error codes; testing across our domain benchmark showed that 256-token chunks with 30-token overlap yielded an 89% retrieval recall, whereas larger 1,024-token chunks diluted the specific error keys with surrounding narrative and caused recall to drop to 71%. However, when building an executive legal summarizer where the questions require synthesizing multi-paragraph contract clauses, I invert that decision and expand chunks to 800 or 1,024 tokens so the model grasps the entire contractual obligation in one coherent passage. The trade-off is precision versus semantic completeness; smaller chunks deliver razor-sharp vector matching but risk losing macro-context, while larger chunks preserve rich context at the expense of higher prompt token costs and vector dilution. In production, I always benchmark chunk sizes between 256 and 1,024 tokens on a golden evaluation dataset using Ragas context recall before locking down ingestion parameters.

#### Key Terms
- **Chunk size tuning**
- **Retrieval recall**
- **Context dilution**
- **Query granularity**
- **Ragas context recall**

#### Likely Follow-ups
- **Q: How does chunk size interact with your re-ranking layer?**  
  *A: Smaller chunks allow retrieving a wider diversity of top-50 candidates, which a cross-encoder re-ranker can then accurately score, maximizing precision without ballooning the prompt context.*
- **Q: What role does tokenization mismatch play between embedder and chunker?**  
  *A: If your chunker splits text by raw characters or words instead of using the exact tokenizer of your embedding model, you risk exceeding the embedder's hard token limit and truncating critical text.*

#### Red Flags
- ⚠️ *Stating that 512 tokens is always the universally optimal chunk size without evaluating document structure or question types.*

---

## I2 — Recursive character splitting vs semantic chunking?

### Spoken answer (~198 words, ~79 sec)

Recursive character splitting divides text using a hierarchical list of natural separators like double newlines, single newlines, and sentence punctuation, whereas semantic chunking dynamically determines split boundaries by measuring cosine distance drops between consecutive sentence embeddings. Recursive character splitting operates entirely as a deterministic rule-based heuristic; it attempts to keep paragraphs together first, falling back to sentences and words only when necessary to stay within the character budget. In contrast, semantic chunking passes every individual sentence through an embedding model, calculates the semantic similarity between sequential sentences, and inserts a chunk boundary whenever the similarity drops below a predetermined percentile threshold. In my production documentation ingestion pipelines, I default to recursive character splitting with a 600-character target and 80-character overlap because it processes thousands of pages in seconds at zero computational inference cost. While semantic chunking creates more conceptually coherent units by never severing a topic mid-thought, the trade-off is an enormous ingestion penalty: embedding every individual sentence increases embedding API costs and ingestion wall-clock time by roughly four to five times. In production, I reserve semantic chunking exclusively for high-value, unstructured narrative documents like medical transcripts where grammatical paragraph breaks do not align with semantic topic transitions.

#### Key Terms
- **Recursive character splitting**
- **Semantic chunking**
- **Sentence embedding distance**
- **Separator hierarchy**
- **Ingestion compute cost**

#### Likely Follow-ups
- **Q: How do you calibrate the similarity threshold in semantic chunking?**  
  *A: We compute a moving window of cosine differences across sentence embeddings and split at standard deviation inflection points, such as the 95th percentile of semantic divergence.*
- **Q: Can recursive splitting produce malformed Markdown or HTML tables?**  
  *A: Yes, naive splitters can break table rows; for markdown documents, we use MarkdownHeaderTextSplitter before recursive splitting to isolate tables as indivisible blocks.*

#### Red Flags
- ⚠️ *Believing semantic chunking is always superior without realizing its 5x embedding cost and severe ingestion latency overhead.*

---

## I3 — How do embedding dimensions affect quality, storage and latency?

### Spoken answer (~194 words, ~78 sec)

Embedding dimensions represent the degree of latent semantic expressiveness: higher dimensions capture finer linguistic nuances, but linearly inflate vector database memory requirements and increase nearest-neighbor search latency. For instance, moving from a 384-dimensional vector like MiniLM to a 1,536-dimensional vector like OpenAI's `text-embedding-3-small` or 3,072 dimensions in `text-embedding-3-large` quadruples the memory required for every single chunk stored in your database. Mathematically, storing 10 million vectors at 1,536 dimensions using 4-byte single-precision floats requires approximately 61.4 gigabytes of raw vector storage, plus an additional 20 to 30 percent memory overhead to maintain the in-memory HNSW navigation graph. In our production system on Supabase and Neon PostgreSQL, shifting from uncompressed 1,536 dimensions to 768 dimensions using Matryoshka representation learning cut our RAM footprint by exactly 50% and dropped P99 query latency from 85 milliseconds down to 38 milliseconds, while preserving over 98% of downstream retrieval accuracy. The trade-off is informational capacity; truncating dimensions or selecting tiny models risks losing subtle distinctions between closely related concepts in complex domains like medicine or law. In production, your embedding dimension directly dictates your cloud infrastructure cost because HNSW graph traversal demands that index vectors reside entirely in active RAM.

#### Key Terms
- **Embedding dimensions**
- **Matryoshka representation learning**
- **HNSW graph memory footprint**
- **P99 search latency**
- **Curse of dimensionality**

#### Likely Follow-ups
- **Q: What is Matryoshka Representation Learning (MRL)?**  
  *A: MRL trains embedding models such that earlier vector dimensions capture the most significant semantic information, allowing developers to truncate embeddings to 512 or 768 dimensions without retraining.*
- **Q: How does scalar quantization (SQ8) impact memory compared to dimension reduction?**  
  *A: Scalar quantization compresses 32-bit floats into 8-bit integers, slashing memory usage by 75% with less than a 1% drop in recall, without altering the vector's dimensional structure.*

#### Red Flags
- ⚠️ *Assuming that higher dimensions are always strictly better without accounting for the quadrupling of vector database RAM costs.*

---

## I4 — How do you pick an embedding model?

### Spoken answer (~191 words, ~76 sec)

I pick an embedding model by combining public benchmarks from the Massive Text Embedding Benchmark, or MTEB leaderboard, with an internal empirical evaluation conducted directly on our domain-specific corporate corpus. MTEB provides a solid baseline for general retrieval capability, but it frequently fails to reflect real-world performance on proprietary enterprise acronyms, financial tabular structures, or multi-lingual edge cases. In our architecture stack, I evaluate candidates across five concrete dimensions: retrieval recall on our golden dataset, inference latency per batch, context window capacity, hosting flexibility, and per-million-token cost. For example, when architecting an internal compliance assistant, we benchmarked OpenAI's `text-embedding-3-small` against open-source `BAAI/bge-large-en-v1.5` hosted locally on an AWS GPU; while both scored within 2% on NDCG@10, the OpenAI API simplified operational overhead, supported 8,191 input tokens, and allowed us to leverage Matryoshka truncation down to 768 dimensions. However, if data sovereignty or strict on-premise governance prohibits sending documents across public networks, I immediately choose self-hosted open-source models like BGE or E5 on Triton Inference Server. In production, picking an embedding model is an architectural commitment: once you index millions of chunks, migrating to a new model requires a complete, zero-downtime corpus backfill.

#### Key Terms
- **MTEB leaderboard**
- **Domain-specific evaluation**
- **NDCG@10**
- **Matryoshka truncation**
- **Data sovereignty**

#### Likely Follow-ups
- **Q: Why is context window length important when picking an embedder?**  
  *A: Short 512-token embedders truncate longer chunk contexts, whereas modern 8k-token embedders allow ingesting larger hierarchical documents without losing surrounding context.*
- **Q: What is asymmetric versus symmetric embedding?**  
  *A: Asymmetric models use different prompt prefixes or encoders for short queries versus long documents to account for their distinct structural characteristics during similarity calculation.*

#### Red Flags
- ⚠️ *Choosing an embedding model solely based on the overall MTEB rank without testing on domain-specific enterprise data.*

---

## I5 — Your RAG system gives wrong answers. How do you debug it?

### Spoken answer (~230 words, ~92 sec)

When a production RAG system produces incorrect answers, I systematically decouple and isolate the pipeline into two halves: the retrieval tier and the generation tier, using distributed execution traces in LangSmith. Recently in our customer support pipeline on FastAPI and Neon pgvector, users reported that the assistant gave completely erroneous answers regarding software warranty return windows. I opened LangSmith, filtered for the failing run ID, and inspected the exact inputs and outputs of each individual node in the chain. Step one was inspecting the `Retriever` trace: I checked the retrieved chunk texts, their similarity scores, and their source metadata; in this incident, the retrieved top-3 chunks were discussing hardware replacement rather than software licensing, with cosine similarities hovering below 0.62. This immediately proved it was a retrieval failure rather than an LLM generation defect; the LLM was simply trying to answer from irrelevant context. Digging into the indexing step, I discovered our recursive chunker had sliced the software warranty table into two fragments, splitting the clause from its parent header. I fixed the issue by switching to a layout-aware Markdown splitter that preserved table blocks, re-indexed the warranty manual, and verified in LangSmith that the new trace retrieved the exact clause with a 0.86 similarity score, restoring correct answers immediately. In production, never tweak system prompts until LangSmith traces confirm that retrieval successfully placed the correct factual context inside top-k.

#### Key Terms
- **LangSmith tracing**
- **Decoupled debugging**
- **Retrieval vs generation failure**
- **Score inspection**
- **Layout-aware chunking**

#### Likely Follow-ups
- **Q: What specific metrics in LangSmith alert you to systematic retrieval failure?**  
  *A: A sustained drop in average cosine similarity scores below our baseline threshold alongside a spike in LLM fallback refusal phrases.*
- **Q: How do you prevent debugging regressions in production?**  
  *A: We export the failing LangSmith run into an annotated evaluation dataset and execute automated regression assertions on every pull request.*

#### Red Flags
- ⚠️ *Immediately modifying the LLM prompt or temperature without first checking the retrieved chunks in your observability traces.*

---

## I6 — How do you tell a retrieval failure from a generation failure?

### Spoken answer (~241 words, ~96 sec)

You distinguish a retrieval failure from a generation failure by asking one simple binary question verified through execution traces: did the retrieved top-k context chunks actually contain the factual answer to the user's query? If the correct factual answer is completely absent from the retrieved chunks, you have a retrieval failure, which stems from query-document vocabulary mismatch, sub-optimal chunk size, low top-k limits, or missing metadata filters. Conversely, if the retrieved chunks clearly contain the exact factual answer but the LLM still generates an incorrect, incomplete, or hallucinated output, you have a generation failure. In a concrete incident traced through LangSmith on our enterprise HR portal, an employee asked: 'Can I carry over more than five accrued vacation days into the next calendar year?' Inspecting the LangSmith trace revealed that chunk number two explicitly stated: 'Employees may carry over a maximum of five vacation days unless granted executive exemption.' Because the factual truth was sitting directly inside the retrieved context, this was unequivocally a generation failure; the model suffered from the 'lost-in-the-middle' effect because three long, irrelevant chunks were positioned ahead of chunk two, causing the LLM to overlook the conditional clause. I resolved it on the generation side by placing chunk two at the top using a cross-encoder re-ranker and adjusting the system prompt to explicitly cite exceptions. In production, diagnosing this distinction prevents wasting engineering cycles on prompt engineering when your vector retrieval pipeline is fundamentally missing the data.

#### Key Terms
- **Retrieval failure vs generation failure**
- **Ground truth in top-k**
- **Lost-in-the-middle effect**
- **LangSmith trace inspection**
- **Cross-encoder re-ranking**

#### Likely Follow-ups
- **Q: What automated metrics evaluate retrieval and generation independently?**  
  *A: We use Ragas: Context Recall and Context Precision evaluate retrieval quality, while Faithfulness and Answer Relevance evaluate generation quality.*
- **Q: How do you fix a generation failure when the context is present but too verbose?**  
  *A: We apply LLM-based contextual compression or extractive summarization to distill the retrieved chunks down to the essential sentences before passing them to the generator.*

#### Red Flags
- ⚠️ *Assuming that any wrong answer is a hallucination caused by the LLM without verifying whether the retriever delivered the relevant information.*

---

## I7 — What is hybrid search and when does it help?

### Spoken answer (~213 words, ~85 sec)

Hybrid search is an information retrieval technique that executes both sparse lexical keyword matching, typically using BM25, and dense semantic vector search in parallel, merging their independent result sets into a single unified ranking. Dense vector search excels at capturing conceptual likeness, synonyms, and generalized topic queries, but it consistently stumbles on exact lexical identifiers such as specific product part numbers, software error codes, alphanumeric customer IDs, and acronyms. Conversely, BM25 excels at exact keyword frequency and inverted index lookups, but completely fails when a user asks a conceptual question using synonyms that do not match the document vocabulary. In our production e-commerce RAG platform, when a customer searched for 'replacement O-ring gasket for pump model PX-900', pure vector search returned general water pump installation guides because 'PX-900' had no distinct semantic vector representation; meanwhile, BM25 instantly matched the exact product catalog page. By deploying hybrid search in Supabase with pgvector and PostgreSQL Full-Text Search, we captured both the exact hardware specification and related conceptual maintenance procedures in one query. The trade-off is architectural complexity and dual indexing overhead; you must maintain both an inverted text index and an HNSW vector index in your database. In production, hybrid search is essential whenever your enterprise corpus contains alphanumeric identifiers, legal citations, or domain-specific nomenclature.

#### Key Terms
- **Hybrid search**
- **BM25 sparse retrieval**
- **Dense semantic vector search**
- **Lexical vs semantic mismatch**
- **Alphanumeric entity matching**

#### Likely Follow-ups
- **Q: How does PostgreSQL handle hybrid search natively?**  
  *A: PostgreSQL combines `tsvector` and `tsquery` for BM25-like lexical scoring with `pgvector` HNSW index operators, querying both in a single SQL statement.*
- **Q: What happens if BM25 and vector search return completely disjoint result sets?**  
  *A: A rank fusion algorithm like Reciprocal Rank Fusion (RRF) normalizes their ordinal ranks, ensuring items appearing high in either list are fairly represented in the final candidate pool.*

#### Red Flags
- ⚠️ *Claiming that dense vector search renders keyword search obsolete in production enterprise environments.*

---

## I8 — How do you fuse keyword and vector results?

### Spoken answer (~205 words, ~82 sec)

You fuse keyword and vector search results using either score-based linear combination or rank-based fusion, with Reciprocal Rank Fusion, or RRF, being the industry gold standard for production reliability. Linear score combination normalizes BM25 scores and vector cosine similarities onto a shared zero-to-one scale using min-max or sigmoid transformations, then calculates a weighted sum: $lpha 	imes 	ext{vector} + (1 - lpha) 	imes 	ext{BM25}$. While linear combination allows tuning the $lpha$ parameter, in practice score normalization is brittle because BM25 scores are unbounded and heavily skewed by document length and term frequency variations across diverse queries. In my production systems on PostgreSQL pgvector, I use Reciprocal Rank Fusion; RRF ignores raw, incompatible score magnitudes entirely and evaluates only the ordinal rank of each document across the two lists using the formula: $1 / (60 + 	ext{rank})$. Documents appearing near the top of either the keyword list or the vector list receive high reciprocal scores, while items appearing in both lists are boosted to the very top. The trade-off is that RRF cannot be dynamically weighted as easily as linear combination, but it completely eliminates the operational risk of score calibration drift. In production, RRF is the most robust, zero-tuning fusion algorithm for combining disparate retrieval engines.

#### Key Terms
- **Reciprocal Rank Fusion (RRF)**
- **Linear score combination**
- **Score normalization**
- **Ordinal ranking**
- **Score calibration drift**

#### Likely Follow-ups
- **Q: Why is the constant 60 used in the standard RRF formula?**  
  *A: The constant $k=60$ was empirically validated in Information Retrieval literature to prevent low-ranking outlier documents from disproportionately skewing the aggregated score.*
- **Q: Can you implement RRF directly inside PostgreSQL?**  
  *A: Yes, by executing BM25 and vector similarity searches in Common Table Expressions (CTEs), assigning `ROW_NUMBER()`, and joining on document ID with `SUM(1.0 / (60 + rank))`.*

#### Red Flags
- ⚠️ *Directly adding raw BM25 scores to cosine similarity scores without normalization or understanding that their mathematical scales are incompatible.*

---

## I9 — What is token budgeting?

### Spoken answer (~189 words, ~76 sec)

Token budgeting is the disciplined architectural practice of partitioning an LLM's finite context window into strict, pre-allocated quotas across all runtime components: system instructions, chat history, retrieved context, user prompt, and reserved output generation space. Without proactive token budgeting, expanding conversation histories or high top-k retrieval payloads will unexpectedly breach the model's maximum context limit, causing API crashes, high financial costs, or silent context truncation. In our FastAPI production assistant powered by models with an 8,192-token operating envelope, I establish a rigid budget: 500 tokens for system prompt and guardrails, 1,500 tokens for sliding-window conversation history, 4,000 tokens for top-k retrieved context chunks, 500 tokens for the user query, and 1,500 tokens strictly reserved for generated response completion. We enforce this budget programmatically using `tiktoken` before dispatching the payload; if the retrieved context exceeds 4,000 tokens, our compressor dynamically truncates lower-ranked chunks or triggers extractive summarization. The trade-off is potential context loss; aggressive token budgeting can discard peripheral document context, but it guarantees predictable per-query operational costs and prevents catastrophic latency spikes. In production, token budgeting is your primary defense against runaway inference billing and unpredictable Time-to-First-Token latency degradation.

#### Key Terms
- **Token budgeting**
- **Context window partitioning**
- **Sliding window memory**
- **tiktoken counting**
- **Inference latency predictability**

#### Likely Follow-ups
- **Q: What happens if you don't reserve sufficient tokens for the generation budget?**  
  *A: The LLM will abruptly terminate mid-sentence when it hits the maximum completion limit (`finish_reason: length`), returning an incomplete and broken response to the user.*
- **Q: How do long-context models change token budgeting?**  
  *A: While 1M-token windows eliminate hard overflow crashes, token budgeting remains vital because processing 500k prompt tokens increases query latency to over 10 seconds and inflates per-query cost exponentially.*

#### Red Flags
- ⚠️ *Assuming that token limits don't need active enforcement because the latest models have massive context windows.*

---

## I10 — What do you do when retrieved context exceeds the budget?

### Spoken answer (~190 words, ~76 sec)

When retrieved context exceeds the token budget, I apply a tiered reduction strategy: two-stage re-ranking and hard truncation, contextual compression, or dynamic extractive summarization. The first and most effective production technique is two-stage re-ranking; we retrieve a generous candidate set of 30 chunks, pass them through a lightweight cross-encoder model like `bge-reranker-large`, and greedily accumulate chunks in descending score order until the cumulative token count hits the 3,500-token ceiling, discarding all lower-ranked noise. If individual chunks are long, we apply contextual compression using LangChain's `LLMChainExtractor` or smaller SLMs like Phi-3 to strip away irrelevant sentences while preserving only the sentences directly pertinent to the user query. In our enterprise financial analytics tool, when quarterly earnings reports exceeded our context budget, applying cross-encoder pruning followed by selective sentence compression reduced raw context volume by 58% while actually improving answer accuracy. The trade-off is processing latency; running contextual compression adds another LLM inference hop, introducing roughly 200 to 400 milliseconds of latency to the query path. In production, I prefer cross-encoder threshold truncation over secondary LLM summarization because it runs in under 40 milliseconds on CPU and never introduces secondary hallucination risks.

#### Key Terms
- **Contextual compression**
- **Cross-encoder pruning**
- **Token budget enforcement**
- **Extractive summarization**
- **Inference latency overhead**

#### Likely Follow-ups
- **Q: How does Maximal Marginal Relevance (MMR) help with context overflow?**  
  *A: MMR selects chunks that balance relevance with diversity, ensuring that budget tokens are not wasted on near-duplicate chunks saying the exact same thing.*
- **Q: What is the risk of using an LLM to summarize retrieved context before passing it to the final generator?**  
  *A: The intermediate summarization LLM can hallucinate or omit crucial numerical details, compounding errors before the final answer is synthesized.*

#### Red Flags
- ⚠️ *Simply slicing off chunks at arbitrary token boundaries mid-sentence without ranking or semantic awareness.*

---

## I11 — What is MMR?

### Spoken answer (~216 words, ~86 sec)

Maximal Marginal Relevance, or MMR, is a retrieval re-ranking algorithm designed to maximize the relevance of retrieved document chunks to the user's query while actively penalizing and minimizing semantic redundancy among the selected results. Standard vector search naively returns the top-k chunks with the absolute highest similarity scores, which often results in five nearly identical paragraphs extracted from different sections or versions of the same document, wasting the prompt's token budget. MMR solves this by iteratively selecting candidates using a weighted formula: it balances the query-document similarity against the maximum similarity between the candidate and already selected documents, controlled by a hyperparameter $\lambda$. When $\lambda = 1$, MMR behaves identically to standard similarity search; when $\lambda = 0$, it optimizes purely for diversity. In our enterprise HR knowledge base, setting $\lambda = 0.7$ ensured that when an employee asked about parental leave benefits, our system retrieved one chunk on maternity leave, one on paternity leave, and one on adoption policies, rather than three repetitive variations of the maternity preamble. The trade-off is a slight drop in pure topical relevance for individual chunks, and minor computational overhead from calculating pairwise similarity across the candidate pool. In production, MMR is essential when your source documents contain heavy internal duplication, such as changelogs, recurring reports, or multi-version policy handbooks.

#### Key Terms
- **Maximal Marginal Relevance (MMR)**
- **Diversity vs relevance trade-off**
- **Redundancy penalty**
- **Lambda weighting parameter**
- **Pairwise candidate similarity**

#### Likely Follow-ups
- **Q: How is MMR computed in LangChain or vector databases?**  
  *A: LangChain provides `max_marginal_relevance_search`, fetching a candidate pool of `fetch_k` (e.g., 20) and iteratively selecting `k` (e.g., 5) items using greedy diversity scoring.*
- **Q: Why would you choose MMR over a cross-encoder re-ranker?**  
  *A: MMR operates directly on pre-computed embedding vectors with simple cosine math in under 5 milliseconds, making it vastly faster and cheaper than cross-encoder inference.*

#### Red Flags
- ⚠️ *Believing that retrieving the top-5 highest similarity chunks always yields the best prompt context, ignoring information redundancy.*

---

## I12 — What is re-ranking and why is it two-stage?

### Spoken answer (~219 words, ~88 sec)

Re-ranking is the process of re-evaluating and re-ordering a preliminary candidate pool of retrieved documents using a more sophisticated scoring model, and it is structured as two-stage retrieval to reconcile speed with precision. In stage one, bi-encoder embedding search queries an Approximate Nearest Neighbor index across millions of chunks in under 30 milliseconds; because bi-encoders embed the query and documents independently into vector space, they are computationally fast but cannot model cross-token interactions between query and text. In stage two, a cross-encoder model, such as Cohere Rerank or `bge-reranker-large`, takes the query and the top-50 candidates together as joint inputs, passing all tokens through all transformer attention layers simultaneously to capture deep semantic relevance, conditional logic, and negation. Running a cross-encoder over millions of documents in real time would require hours of compute and collapse latency; by feeding it only the top 30 to 50 candidates from stage one, it scores and prunes them down to the top 5 in roughly 40 milliseconds. In our production system on FastAPI, adding this two-stage pipeline boosted our NDCG@5 by 24% while keeping total retrieval latency under 80 milliseconds. The trade-off is the operational footprint of hosting a secondary re-ranker model or paying per-call API fees to Cohere. In production, two-stage re-ranking is the single highest-ROI architectural intervention for boosting answer precision.

#### Key Terms
- **Two-stage retrieval**
- **Bi-encoder vs cross-encoder**
- **Full cross-attention**
- **NDCG@5 improvement**
- **Computational efficiency trade-off**

#### Likely Follow-ups
- **Q: Why can't cross-encoders be pre-computed and stored in an index?**  
  *A: Because cross-encoders evaluate the joint token sequence of (query + document) together; the representation is query-dependent and cannot exist prior to runtime.*
- **Q: What is ColBERT and how does it fit between bi-encoders and cross-encoders?**  
  *A: ColBERT uses late interaction: it pre-computes token-level embeddings and performs fast MaxSim tensor operations at runtime, offering cross-encoder quality at near bi-encoder speeds.*

#### Red Flags
- ⚠️ *Thinking bi-encoders and cross-encoders use the same underlying inference architecture or suggesting that cross-encoders can index a database directly.*

---

## I13 — What is query rewriting / multi-query / HyDE?

### Spoken answer (~227 words, ~91 sec)

Query rewriting, Multi-Query expansion, and Hypothetical Document Embeddings, or HyDE, are query transformation strategies designed to bridge the semantic and vocabulary gap between how users phrase questions and how factual information is stored in documents. Users frequently submit poorly structured, vague, or shorthand queries like '401k match rule'; query rewriting uses a lightweight LLM to rewrite this into an expanded, formal query such as 'What is the corporate employer matching percentage and vesting schedule for 401k contributions?'. Multi-query expansion generates three to five alternative phrasings from different perspectives, retrieves documents for each in parallel, and merges unique results, dramatically improving retrieval recall. HyDE takes a radical approach: it instructs an LLM to hallucinate a plausible hypothetical answer to the query, embeds that hypothetical passage, and searches vector space using the hypothetical answer rather than the raw query; because an answer embedding lives closer to document embeddings than a question vector does, semantic proximity is significantly improved. In our enterprise legal RAG system, HyDE increased our zero-shot document recall on ambiguous statutory questions from 64% to 88%. The trade-off is latency and cost; running an LLM query transformation step before retrieval adds 300 to 700 milliseconds of latency and extra token expenses. In production, I gate HyDE behind a query router, invoking it only for complex, ambiguous questions while letting straightforward keyword queries pass directly to hybrid search.

#### Key Terms
- **Query transformation**
- **Multi-Query expansion**
- **HyDE (Hypothetical Document Embeddings)**
- **Semantic vector alignment**
- **Query routing**

#### Likely Follow-ups
- **Q: What is the primary failure mode of HyDE?**  
  *A: If the LLM generates a wildly incorrect hypothetical answer in a highly specialized technical domain, its embedding will steer vector search toward completely irrelevant clusters.*
- **Q: How do you de-duplicate results in multi-query retrieval?**  
  *A: We execute the queries asynchronously across threads, pool the retrieved chunk candidates, and apply Reciprocal Rank Fusion to deduplicate and surface the most universally matched documents.*

#### Red Flags
- ⚠️ *Applying HyDE unconditionally to every single user query without considering the 500ms latency penalty and hallucination risk.*

---

## I14 — How do you handle document updates and deletes?

### Spoken answer (~204 words, ~82 sec)

I handle document updates and deletes by establishing deterministic document identification schemes, content hashing, and transactional metadata cascading across our vector store. A naive implementation simply appends new chunks, leading to stale ghost chunks, contradictory answers, and index bloat over time. In our production architecture using PostgreSQL pgvector and Supabase, every parent document is assigned an immutable `document_id`, and each individual chunk is tagged with both this `document_id` and a SHA-256 hash of its text content. When a document modification event arrives via webhook or change data capture, the ingestion pipeline compares the incoming file hash against stored records; if modified, we execute an atomic SQL transaction that deletes all existing chunks where `document_id = :id` before inserting the newly generated chunk vectors and metadata. For full deletions, executing `DELETE FROM document_chunks WHERE document_id = :id` instantly prunes the vectors, and our nightly maintenance worker triggers a VACUUM and re-indexes the HNSW graph if deletion volume exceeds 10% of total table rows. The trade-off is temporary indexing lock contention during large bulk document replacements, which we mitigate by staging writes into a shadow table before swapping. In production, robust chunk lifecycle management is mandatory to prevent outdated corporate policies from contaminating live RAG responses.

#### Key Terms
- **Deterministic document IDs**
- **Content hashing (SHA-256)**
- **Atomic upserts**
- **HNSW vacuuming**
- **Change Data Capture (CDC)**

#### Likely Follow-ups
- **Q: What happens to HNSW indexes in vector databases when many rows are deleted?**  
  *A: In many vector databases, deleted vectors are merely soft-deleted or tombstoned, degrading HNSW search recall and wasting RAM until an explicit index rebuild or vacuum occurs.*
- **Q: How do you handle document versioning for audit compliance?**  
  *A: We add a `version_number` and `is_active` boolean to chunk metadata, allowing retrieval queries to filter for `is_active = true` while preserving historical snapshots for compliance reviews.*

#### Red Flags
- ⚠️ *Assuming vector stores automatically handle document updates without realizing that duplicate chunks will accumulate unless explicitly deleted.*

---

## I15 — Why add observability with LangSmith?

### Spoken answer (~214 words, ~86 sec)

You add observability with LangSmith because a production RAG system is a non-deterministic, multi-component distributed system where failures cannot be diagnosed through traditional application logs alone. Traditional APM tools like Datadog tell you that an HTTP request took two seconds, but they cannot show you the dynamic prompt payload, the specific chunks retrieved, their individual similarity scores, the model token usage, or intermediate agent routing decisions. LangSmith provides end-to-end trace visualization across every step of execution: document loader outputs, embedding model latency, vector database query filters, re-ranker scoring distributions, and the raw streaming LLM completion tokens. In our FastAPI production environment, integrating LangSmith via environment variables allowed us to trace a sudden spike in customer complaints directly to an unhandled null metadata field in our retrieval pre-filter, which had silently caused the retriever to return zero chunks and triggered generic fallback responses. Furthermore, LangSmith allows us to turn live production edge cases into curated evaluation datasets with one click, establishing automated regression tests against future model updates. The trade-off is minor telemetry network overhead and SaaS vendor dependency, though traces can be sampled or routed to open-source alternatives like Langfuse if data residency demands self-hosting. In production, operating a RAG application without LangSmith or equivalent tracing is essentially flying blind in a stochastic system.

#### Key Terms
- **Distributed LLM tracing**
- **LangSmith observability**
- **Component-level latency & token tracking**
- **Curated evaluation datasets**
- **Silent failure diagnosis**

#### Likely Follow-ups
- **Q: How do you sanitize sensitive PII data before sending traces to LangSmith?**  
  *A: We implement custom masking pre-processors in our LangChain callbacks to redact social security numbers, credit cards, and customer names before telemetry is dispatched.*
- **Q: Can you use LangSmith to run automated evaluation benchmarks?**  
  *A: Yes, LangSmith integrates directly with evaluators like Ragas or custom LLM-as-a-judge criteria, running automated scoring across test datasets in CI pipelines.*

#### Red Flags
- ⚠️ *Believing that standard console `print` statements or basic web server access logs are sufficient for monitoring and debugging production RAG pipelines.*

---

