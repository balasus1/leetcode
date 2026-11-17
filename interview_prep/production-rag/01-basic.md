# Level 1 — Basic RAG Interview Questions & Spoken Answers

> **Target Role:** Senior AI Engineer / AI Architect  
> **Spoken Target:** ~1–1.2 minutes per answer (180–200 words at 150 wpm)  
> **Delivery Style:** First person, conversational, confident, zero bullet points inside spoken answers.

---

## B1 — What is RAG and why use it instead of only prompting an LLM?

### Spoken answer (~182 words, ~73 sec)

Retrieval-Augmented Generation, or RAG, is an architecture where an external retrieval step fetches authoritative, private domain documents into an LLM's prompt context before text generation begins. Instead of relying on the static, cutoff weights of a foundation model, RAG converts the incoming user query into a semantic representation, queries an external vector or hybrid store to retrieve the top-k most relevant chunks, and injects them into the prompt with explicit instructions to ground the response exclusively on that retrieved context. In my production enterprise policy assistant built on FastAPI and Neon pgvector, our internal human resources leave policies change quarterly; by simply updating the vector database, the LLM immediately provides up-to-date answers with verifiable citations without needing retraining or fine-tuning. The trade-off is added system latency, higher operational complexity, and dependency on retrieval precision; if a task requires purely creative writing or broad reasoning over general world knowledge that never changes, standard zero-shot prompting or fine-tuning is significantly cheaper and faster. In production, RAG's true value is reducing ungrounded hallucinations while providing strict corporate governance and auditability through verifiable inline source citations.

#### Key Terms
- **Grounding**
- **Hallucination mitigation**
- **Retrieval-Augmented Generation**
- **Source citations**
- **Parametric memory**

#### Likely Follow-ups
- **Q: When would you choose fine-tuning over RAG?**  
  *A: Fine-tuning is ideal for teaching an LLM style, tone, structured output syntax, or specialized terminology. However, fine-tuning is ineffective for injecting dynamic factual knowledge, where RAG remains the enterprise standard.*
- **Q: How does RAG provide verifiable citations?**  
  *A: During chunk ingestion, we attach document IDs, titles, and page numbers to chunk metadata. When generating the response, the prompt instructs the LLM to tag statements with chunk citation keys that map back to source URLs.*

#### Red Flags
- ⚠️ *Claiming that fine-tuning an LLM is a valid substitute for RAG to keep internal company documentation up to date.*

---

## B2 — Walk through the two pipelines of a RAG system.

### Spoken answer (~186 words, ~74 sec)

Every production RAG system is split into two distinct, decoupled pipelines: the asynchronous offline indexing pipeline and the synchronous online query pipeline. In the offline indexing pipeline, raw documents such as multi-page PDFs, markdown files, and web pages are extracted by document loaders, split into overlapping chunks, passed through an embedding model to generate dense vector embeddings, and stored alongside structured metadata inside a vector database. In the online query pipeline, an incoming user request hits our FastAPI gateway, where the query string is embedded using the exact same embedding model, matched against vector indices using approximate nearest neighbors, merged into a system prompt, and streamed back through the LLM. In my production setups, I decouple these pipelines entirely using background workers and Kafka message queues for document ingestion, ensuring heavy OCR or PDF parsing spikes never degrade user-facing API response times. The trade-off of this architectural separation is eventual consistency; if the asynchronous indexing pipeline lags behind document creation, end-users experience temporary retrieval staleness until the vector database catches up. In production, isolating ingestion from querying is essential to maintain sub-500 millisecond API latency budgets.

#### Key Terms
- **Offline indexing pipeline**
- **Online query pipeline**
- **Approximate Nearest Neighbor (ANN)**
- **Decoupled asynchronous ingestion**
- **Context augmentation**

#### Likely Follow-ups
- **Q: What happens if you use different embedding models for indexing and querying?**  
  *A: The vector spaces will be completely misaligned, meaning cosine similarity calculations will produce meaningless random distances and retrieval will fail completely.*
- **Q: How do you handle real-time document updates in the indexing pipeline?**  
  *A: We use change data capture triggers on our primary relational database to enqueue incremental chunk upserts and deletes rather than executing full corpus re-indexing.*

#### Red Flags
- ⚠️ *Confusing the offline indexing pipeline with the runtime query pipeline or running heavy PDF parsing synchronously inside the user API thread.*

---

## B3 — What is an embedding?

### Spoken answer (~180 words, ~72 sec)

An embedding is a dense, high-dimensional vector of floating-point numbers that captures the semantic meaning and contextual relationships of a piece of text. Unlike traditional keyword representations like TF-IDF or one-hot encoding which are high-dimensional and sparse, modern embedding models project text into a continuous latent geometric space where semantically similar phrases are located physically close to each other. For example, in my customer support search system, the queries 'cancel my annual subscription' and 'stop recurring membership payments' share almost zero identical vocabulary, yet their cosine similarity in OpenAI's 1536-dimensional space is near 0.88, allowing our system to retrieve the exact same cancellation procedure. The primary trade-off is the loss of exact keyword precision; embeddings can sometimes blur fine distinctions between specific alphanumeric error codes or product part numbers, which is why pure dense embeddings must often be paired with sparse BM25 retrieval. In production, picking your embedding model determines your baseline semantic recall ceiling, while embedding dimensionality directly drives memory consumption and vector indexing speed; every doubling of vector dimensions demands proportional RAM overhead across production HNSW graph caches.

#### Key Terms
- **Dense vector**
- **Semantic space**
- **Latent geometric representation**
- **Cosine similarity**
- **Lexical mismatch**

#### Likely Follow-ups
- **Q: Can you compare small 384-dimensional embeddings with large 1536-dimensional embeddings?**  
  *A: Smaller embeddings like BGE-small use substantially less RAM and deliver faster search latency, whereas 1536-dimensional models capture richer semantic subtleties at the cost of higher storage and slower indexing.*
- **Q: What causes out-of-vocabulary degradation in embeddings?**  
  *A: Modern subword tokenizers handle almost all vocabulary, but domain-specific jargon or alphanumeric SKUs can still produce degraded semantic representations if not represented in the training distribution.*

#### Red Flags
- ⚠️ *Describing embeddings as simple hash codes or claiming they understand keywords rather than geometric vector proximity.*

---

## B4 — What does a vector database do that a normal DB does not?

### Spoken answer (~173 words, ~69 sec)

A vector database specializes in indexing, storing, and performing Approximate Nearest Neighbor search across multi-dimensional embedding vectors at sub-second latencies, which standard relational databases cannot natively accomplish at scale. While traditional databases use B-Trees or Hash indexes to execute exact matches or range filters in logarithmic time, high-dimensional vector spaces suffer from the curse of dimensionality, requiring specialized graph-based or cluster-based indexes like HNSW or IVFFlat to approximate nearest neighbors without scanning every row. In my enterprise setups, using pgvector with HNSW on PostgreSQL allows me to execute sub-50 millisecond vector similarity lookups while simultaneously applying relational SQL filters on user permissions and tenant IDs in a single query execution plan. The trade-off is recall versus query speed; vector databases deliberately sacrifice 100% exact retrieval recall to return results in milliseconds rather than minutes. In production, choosing an index like HNSW requires balancing significant memory overhead against strict query throughput requirements, as high-recall vector search demands that graph indexes reside entirely in RAM to prevent disk swapping bottlenecks during high-frequency concurrent traffic spikes.

#### Key Terms
- **Approximate Nearest Neighbor (ANN)**
- **HNSW index**
- **Curse of dimensionality**
- **Metadata pre-filtering**
- **Vector similarity search**

#### Likely Follow-ups
- **Q: Why is exact KNN search impractical in production databases?**  
  *A: Exact KNN requires calculating distances against every single vector in the table, resulting in $O(N)$ linear scans that collapse latency under millions of documents.*
- **Q: What is pre-filtering versus post-filtering in vector databases?**  
  *A: Pre-filtering restricts the search space using metadata attributes before traversing the vector graph, whereas post-filtering prunes results after retrieval, often resulting in fewer returned items than the requested top-k.*

#### Red Flags
- ⚠️ *Saying that relational databases can do vector search efficiently with normal B-Tree indexes or that vector databases always guarantee 100% exact nearest neighbors.*

---

## B5 — Cosine similarity vs dot product vs Euclidean distance?

### Spoken answer (~172 words, ~69 sec)

Cosine similarity measures the angular difference between two vector directions regardless of magnitude, dot product measures both direction and magnitude, and Euclidean distance measures the straight-line geometric distance between vector coordinates. Mathematically, when embeddings are normalized to unit length, cosine similarity and dot product become identical because the denominator norm is exactly one, and Euclidean distance becomes inversely proportional to cosine similarity. In my production RAG pipelines using OpenAI's `text-embedding-3-small` or Cohere embeddings, vectors are output pre-normalized; therefore, I configure pgvector to use inner product (`<#>`), which executes significantly faster on modern CPU SIMD registers than calculating square roots. The key trade-off depends on whether vector magnitude carries semantic significance; for text embeddings where magnitude usually reflects token count rather than relevance, cosine similarity or normalized dot product is universally preferred over raw Euclidean distance. In production, you must strictly align your vector database distance metric with the exact metric used during embedding model training, as using the wrong distance operator silently degrades retrieval accuracy and wastes compute cycles re-normalizing unnormalized vectors.

#### Key Terms
- **Cosine similarity**
- **Normalized dot product**
- **Euclidean distance ($L_2$)**
- **Unit normalization**
- **SIMD vector instructions**

#### Likely Follow-ups
- **Q: Why does dot product execute faster than cosine similarity in databases?**  
  *A: Cosine similarity requires calculating vector magnitudes and divisions for each comparison, whereas dot product only requires multiply-accumulate operations without square roots.*
- **Q: When would you ever use Euclidean distance instead of cosine similarity?**  
  *A: Euclidean distance is appropriate in clustering tasks, image feature spaces, or spatial coordinate domains where the absolute vector magnitude represents a physical or quantitative attribute.*

#### Red Flags
- ⚠️ *Thinking cosine similarity and dot product are unrelated concepts or failing to know that on unit-normalized vectors they produce the exact same ranking.*

---

## B6 — Why do we chunk documents?

### Spoken answer (~167 words, ~67 sec)

We chunk documents because embedding models and LLMs have finite token context limits, and smaller, focused text segments drastically improve retrieval precision by isolating distinct semantic thoughts. If you embed an entire 50-page legal contract or enterprise manual as a single vector, the specific clauses get averaged out into a noisy, generic embedding that matches poorly against targeted user questions. In our enterprise RAG implementation, splitting technical manuals into 512-token chunks ensured that when an engineer asked about a specific valve failure code, the retriever returned the exact two-paragraph diagnostic procedure rather than twenty pages of irrelevant schematic preamble. The trade-off is context fragmentation; if a chunk is made too small, say 50 tokens, it loses its surrounding sentence context and becomes incomprehensible to the LLM during generation. In production, optimal chunk size is a direct function of query granularity and the natural semantic structure of your source documents, which is why chunk boundaries must always be validated using retrieval recall benchmarks on domain-specific evaluation golden sets.

#### Key Terms
- **Chunking**
- **Context dilution**
- **Semantic precision**
- **Embedding context window**
- **Granularity matching**

#### Likely Follow-ups
- **Q: What is the consequence of chunking text too small?**  
  *A: Chunks lose necessary grammatical and contextual background, causing the embedding to lose meaning and the generator to lack sufficient context to formulate a coherent answer.*
- **Q: How do you preserve section titles across isolated chunks?**  
  *A: We inject document hierarchy metadata, such as prepending the chapter and section header to every individual text chunk before embedding.*

#### Red Flags
- ⚠️ *Claiming that you don't need chunking because modern LLMs have 1-million-token context windows.*

---

## B7 — What is chunk overlap and why use it?

### Spoken answer (~170 words, ~68 sec)

Chunk overlap is the intentional repetition of a specified percentage of tokens between consecutive text chunks to prevent critical semantic context from being split across chunk boundaries. When text splitters slice a document at arbitrary character or token limits, essential thoughts, complex sentences, or coreference nouns frequently get severed in half, rendering both resulting chunks semantically incomplete. In my production document processing pipelines, I enforce a 512-token chunk size with a 10 to 15 percent overlap, approximately 50 to 80 tokens; this guarantees that if a complex conditional rule begins at token 500, the complete statement is preserved in full inside the subsequent chunk. The trade-off is redundant storage and slightly higher embedding API costs; an ongoing 15 percent overlap increases total vector count, database storage, and indexing overhead by roughly 15 percent across your vector tier. In production, chunk overlap is essential insurance against boundary-split retrieval failures, ensuring that critical semantic units remain intact for both embedding distance calculation and prompt comprehension without creating severe context dilution during retrieval.

#### Key Terms
- **Chunk overlap**
- **Boundary splitting**
- **Coreference preservation**
- **Sliding window**
- **Storage overhead**

#### Likely Follow-ups
- **Q: What is the standard recommended overlap percentage in production?**  
  *A: Between 10% and 20% is the industry standard; exceeding 25% creates excessive duplication and wastes LLM prompt budget without delivering measurable recall improvements.*
- **Q: How does recursive character splitting handle overlaps differently than fixed-token splitting?**  
  *A: Recursive character splitting attempts to shift boundaries backward to the nearest paragraph break or sentence punctuation before applying the sliding window overlap.*

#### Red Flags
- ⚠️ *Setting overlap to zero in production and wondering why sentences severed mid-thought fail retrieval.*

---

## B8 — What is top-k?

### Spoken answer (~165 words, ~66 sec)

Top-k is the hyperparameter that dictates the maximum number of most similar document chunks the vector database retrieves and returns for a given query vector. During the vector similarity search, the database computes approximate distance scores across the indexed corpus and returns the $k$ chunks with the highest similarity or lowest distance to the prompt. In our FastAPI knowledge base, setting top-k to 5 allows us to provide sufficient context for standard multi-step user questions while keeping our prompt token consumption under 2,000 tokens per call. The trade-off is recall versus noise; setting $k$ too low risks omitting the actual answer, while setting $k$ too high introduces irrelevant context, inflates latency, increases API token costs, and can trigger the 'lost-in-the-middle' attention degradation in LLMs. In production, tuning top-k requires finding the empirical sweet spot between retrieval recall and generator distraction, which is why many advanced systems retrieve a larger top-k like 50 and use a cross-encoder to re-rank down to 5 before final prompt construction.

#### Key Terms
- **Top-k retrieval**
- **Recall vs precision trade-off**
- **Token budget**
- **Lost-in-the-middle phenomenon**
- **Context noise**

#### Likely Follow-ups
- **Q: What is the difference between top-k in vector retrieval and top-k in LLM sampling?**  
  *A: Top-k in retrieval defines how many document chunks are fetched from the database, whereas top-k in LLM sampling restricts next-token generation probabilities to the $k$ most likely vocabulary tokens.*
- **Q: How do you dynamically adjust top-k based on query complexity?**  
  *A: We implement a routing layer that assigns $k=3$ for factual lookup questions but expands to $k=10$ paired with a re-ranker for complex comparative queries.*

#### Red Flags
- ⚠️ *Confusing vector retrieval top-k with LLM token sampling top-k, or blindly setting top-k to 20 without considering prompt token cost.*

---

## B9 — What role do document loaders play?

### Spoken answer (~170 words, ~68 sec)

Document loaders are the foundational ingestion components responsible for reading raw files across diverse source formats and converting them into standardized, structured document objects containing clean text and associated metadata. Raw enterprise data lives across varied formats like unstructured PDFs, DOCX files, HTML pages, and Notion exports; document loaders parse these file structures, extract layout elements, filter out noise, and construct unified objects with unified schema attributes. In my production pipelines, we use specialized loaders like PyPDFDirectoryLoader and custom Markdown parsers to extract text while systematically capturing critical metadata such as source URLs, page numbers, author, and timestamp. The trade-off is parsing accuracy versus pipeline speed; simple text extractors are fast but fail on multi-column tables, whereas complex OCR or vision-based loaders handle scanned documents accurately but introduce significant compute latency and monetary cost. In production, garbage in means garbage out: your RAG system's generation quality is fundamentally bounded by your document loader's parsing fidelity, making robust preprocessing the most crucial phase of ingestion to avoid corrupted text streams downstream.

#### Key Terms
- **Document loader**
- **Unstructured data ingestion**
- **Metadata extraction**
- **Format standardization**
- **Layout parsing**

#### Likely Follow-ups
- **Q: Why do basic open-source PDF loaders struggle with complex financial reports?**  
  *A: Basic loaders extract characters in raw stream order, which scrambles multi-column tables and breaks the semantic structure of headers and footers.*
- **Q: How do you handle password-protected or encrypted PDFs during ingestion?**  
  *A: We decouple ingestion with a validation step that flags encrypted documents and routes them to an authenticated decryption service or notifies the upload owner.*

#### Red Flags
- ⚠️ *Assuming document loaders are trivial string readers, ignoring metadata extraction or table parsing failures.*

---

## B10 — Why is metadata important in a vector store?

### Spoken answer (~167 words, ~67 sec)

Metadata is critical in a vector store because it provides the structured contextual attributes—such as tenant ID, access control lists, publication date, and source references—necessary to perform hybrid filtering and ensure enterprise security. While vector embeddings capture purely semantic likeness, real-world business queries almost always require hard relational constraints, such as restricting retrieval to documents belonging only to the authenticated user's organization or published within the last ninety days. In my multi-tenant RAG systems on Supabase and PostgreSQL pgvector, every chunk is indexed with a `tenant_id` and `user_role` metadata column; our queries apply relational SQL WHERE clauses to enforce row-level security before calculating vector distances. The trade-off is index storage overhead and query planning complexity, as combining high-dimensional graph traversal with selective scalar filters requires carefully tuned partial or compound indexes to avoid slow table scans. In production, robust metadata is the only mechanism that prevents cross-tenant data leaks, enables selective cache invalidation, and powers verifiable inline source citations that build end-user trust in critical enterprise applications.

#### Key Terms
- **Metadata filtering**
- **Row-Level Security (RLS)**
- **Pre-filtering**
- **Audit citations**
- **Multi-tenancy isolation**

#### Likely Follow-ups
- **Q: What is the danger of relying on client-side metadata filtering instead of server-side?**  
  *A: Client-side filtering allows unauthenticated vector chunks to leave the database, introducing critical data leakage vulnerabilities and wasting network bandwidth.*
- **Q: How does metadata aid in document deletion and cache invalidation?**  
  *A: When a document is updated, we query the vector store by `document_id` metadata to delete or replace all corresponding chunk vectors in a single transaction.*

#### Red Flags
- ⚠️ *Viewing vector stores as isolated vector arrays and forgetting that production applications mandate strict metadata for authorization and tenant isolation.*

---

## B11 — What is LangChain and what is LCEL?

### Spoken answer (~161 words, ~64 sec)

LangChain is an open-source orchestration framework that provides standardized abstractions for integrating LLMs with external tools, vector stores, and memory systems, while LCEL, or LangChain Expression Language, is its declarative composition syntax for building production runtimes. LCEL uses a Unix-style pipe syntax (`|`) to link prompts, retrievers, models, and output parsers into a unified chain that natively supports async execution, streaming, batch processing, and fallback handling out of the box. In my backend services, defining a RAG pipeline using LCEL allows me to stream token chunks directly to FastAPI SSE endpoints using `.astream()` with zero custom thread management or boilerplates. The trade-off is framework abstraction overhead and rapid API churn; over-relying on deeply nested LangChain abstractions can make low-level debugging challenging compared to writing direct API calls against provider endpoints. In production, LCEL provides immediate architectural velocity for prototyping and standardizing async streaming pipelines, though mission-critical enterprise state machines often require transitioning to LangGraph for cyclic control flow and durable checkpoints.

#### Key Terms
- **LangChain**
- **LCEL (LangChain Expression Language)**
- **Declarative composition**
- **Streaming runtimes**
- **Async primitives**

#### Likely Follow-ups
- **Q: What is the primary advantage of LCEL over legacy LangChain chains like `RetrievalQA`?**  
  *A: LCEL provides first-class support for streaming, parallel execution branches, automated tracing, and unified async methods, which legacy classes lacked.*
- **Q: When would you choose raw SDK calls over LangChain?**  
  *A: When building ultra-low-latency, single-hop microservices where dependency footprint and framework abstraction overhead must be minimized to the absolute bare minimum.*

#### Red Flags
- ⚠️ *Thinking LCEL is just syntactic sugar without understanding its underlying async execution and streaming runtime capabilities.*

---

## B12 — What is Chroma, and when is it the right choice?

### Spoken answer (~157 words, ~63 sec)

Chroma is a lightweight, open-source embedded vector database designed for fast local development, unit testing, and rapid prototyping without external infrastructure dependencies. It runs in-process inside your Python application using SQLite or DuckDB for persistence and hnswlib for vector indexing, allowing developers to spin up a fully functioning vector store with just two lines of code. I frequently use Chroma in our CI test suites and developer sandbox environments to run deterministic RAG integration tests without needing to connect to a remote cloud database cluster or manage network secrets. However, the clear trade-off is scalability and enterprise concurrency; Chroma is not designed for horizontally scaled, high-throughput multi-tenant production workloads that require strict row-level security, ACID guarantees, and distributed failovers across multiple availability zones. In production, Chroma is fantastic for local rapid iteration and integration tests, but I transition to PostgreSQL pgvector or dedicated managed clusters like Qdrant when deploying to live customer environments with strict uptime SLAs.

#### Key Terms
- **Chroma DB**
- **Embedded vector store**
- **Local prototyping**
- **In-process execution**
- **Production transition**

#### Likely Follow-ups
- **Q: What are the primary bottlenecks of using Chroma in a multi-pod Kubernetes deployment?**  
  *A: Chroma's embedded mode cannot handle distributed concurrent writes across multiple pods, risking SQLite database lock contention and state divergence.*
- **Q: How easy is it to migrate from Chroma to pgvector?**  
  *A: Because LangChain and LlamaIndex provide standardized `VectorStore` abstractions, migrating usually requires only swapping the connection string and initialization class.*

#### Red Flags
- ⚠️ *Recommending embedded Chroma for an enterprise multi-tenant system with millions of concurrent users and compliance requirements.*

---

## B13 — What does a similarity score tell you?

### Spoken answer (~162 words, ~65 sec)

A similarity score quantifies the relative semantic closeness between a query vector and a retrieved document vector within that model's latent embedding space. Depending on the distance metric used, this score might be a cosine similarity between -1 and 1, a normalized inner product between 0 and 1, or an unbounded L2 distance where smaller values indicate closer proximity. In my production RAG pipelines, I establish empirical similarity thresholds; for example, if the top retrieved chunk has a cosine similarity below 0.65, our system flags the retrieval as low-confidence and triggers a fallback prompt instructing the model to gracefully state that it cannot find the answer. The critical trade-off to recognize is that similarity scores are entirely model-dependent and relative rather than absolute probabilities of factual correctness. In production, never treat a high similarity score as a guarantee of factual truth; it only guarantees topical relevance, which is why post-retrieval validation and cross-encoder re-ranking are essential to filter out superficially matching distractors.

#### Key Terms
- **Similarity score**
- **Confidence threshold**
- **Distance metrics**
- **Fallback trigger**
- **Topical relevance vs truth**

#### Likely Follow-ups
- **Q: Why can't you compare similarity scores across different embedding models?**  
  *A: Each embedding model maps semantic space differently, meaning a 0.75 in OpenAI's embedding model has an entirely different distribution and meaning than a 0.75 in BGE-large.*
- **Q: What is score normalization in hybrid search?**  
  *A: Because BM25 yields unbounded positive scores while cosine similarity is bounded between -1 and 1, min-max or sigmoid normalization is required before combining them.*

#### Red Flags
- ⚠️ *Assuming that a high similarity score proves the retrieved chunk contains the correct answer to the question.*

---

## B14 — What is a hallucination in RAG, and how does retrieval reduce it?

### Spoken answer (~173 words, ~69 sec)

A hallucination in RAG occurs when an LLM generates an answer that is factually incorrect, ungrounded, or completely contradicted by the retrieved context provided in its prompt. Retrieval mitigates hallucinations by constraining the generative model's sampling space; instead of forcing the model to extrapolate facts from its opaque parametric memory, we provide the verified source text directly in the prompt and enforce strict grounding instructions. In our enterprise legal compliance system, we prepend a strict system prompt stating: 'Answer solely using the provided context; if the context does not contain the answer, reply that you do not have sufficient information.' The trade-off is the risk of false negatives; if your retrieval pipeline fails to fetch the relevant chunk due to bad chunking or vector search misses, the LLM will correctly refuse to answer even though the document exists in the database. In production, retrieval shifts the engineering problem from stochastic LLM behavior to deterministic information retrieval quality, allowing us to measure faithfulness objectively using automated evaluation frameworks like Ragas in continuous integration pipelines.

#### Key Terms
- **Hallucination**
- **Grounding**
- **Parametric vs non-parametric memory**
- **Negative constraint prompt**
- **Faithfulness**

#### Likely Follow-ups
- **Q: What is the difference between intrinsic and extrinsic hallucinations in RAG?**  
  *A: Intrinsic hallucinations directly contradict the retrieved context, whereas extrinsic hallucinations introduce outside claims that cannot be verified from the provided context.*
- **Q: What automated metric evaluates hallucination in production?**  
  *A: We use Ragas Faithfulness, which measures the ratio of generated claims that can be directly inferred from the retrieved context.*

#### Red Flags
- ⚠️ *Claiming that RAG completely eliminates hallucinations 100% of the time without needing prompt guardrails or evaluation metrics.*

---

## B15 — What does a basic RAG prompt template contain?

### Spoken answer (~159 words, ~64 sec)

A production RAG prompt template contains four core architectural components: a system role definition, strict grounding constraints, the retrieved context payload with source identifiers, and the user's specific query. The prompt structure explicitly isolates retrieved documents from user input using clear delimiters like XML tags or triple backticks, which prevents the LLM from confusing document text with operational system instructions. In my production FastAPI applications, the template is structured as: system instructions defining tone and rules, followed by `<context>` tags encapsulating chunk text alongside chunk IDs and page numbers, ending with the user question and an instruction to cite sources using chunk bracket keys. The trade-off is prompt token overhead versus generation fidelity; including verbose instructions and multiple chunk headers consumes valuable context window space and increases per-query inference costs. In production, clean delimiter tagging is your primary prompt-level defense against both instruction confusion and indirect prompt injection attacks, ensuring predictable JSON or Markdown outputs across diverse foundation models.

#### Key Terms
- **Prompt template**
- **Context delimiters**
- **Grounding constraints**
- **Source attribution**
- **Instruction hierarchy**

#### Likely Follow-ups
- **Q: Why should you use XML or Markdown delimiters around retrieved context?**  
  *A: Delimiters clearly separate untrusted document data from system instructions, minimizing prompt injection risks and helping the attention mechanism focus on the payload.*
- **Q: How do you format citations inside the prompt template?**  
  *A: We instruct the model: 'Cite every factual assertion with the corresponding document ID in brackets, such as [Doc-1], referencing the tags provided in context.'*

#### Red Flags
- ⚠️ *Concatenating retrieved chunks directly into the prompt string without clear boundaries, delimiters, or grounding constraints.*

---

