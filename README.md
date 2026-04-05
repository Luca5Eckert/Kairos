# Kairos

*What changes when memory has structure you didn't build?*

You read. You take notes. You highlight things that feel important. Three weeks later you have an intuition that something connects — but you can't remember where it was, how it was phrased, or what it actually links to.

The problem isn't that you forgot. The problem is that the structure was never built in the first place.

Kairos is a personal knowledge graph engine. You ingest content freely — notes, articles, highlights, ideas. Kairos reads it, understands it, and automatically constructs the conceptual structure behind it: a growing graph of what you know and how it connects. The graph builds itself. You just feed it.

---

## What it does

Every piece of content you ingest goes through two parallel pipelines:

**Semantic pipeline** — content is chunked with overlap, embedded via a local ONNX model, and stored in pgvector alongside embeddings of every extracted concept node. This powers meaning-based retrieval: queries return results by what they *mean*, not what they *say*.

**Graph pipeline** — an LLM performs Open Information Extraction on each chunk, producing typed knowledge graph triples. These triples are persisted in Neo4j as phrase nodes, passage nodes, and typed edges. Synonymy edges are automatically created between concept nodes whose embeddings exceed a cosine similarity threshold — connecting `backprop` to `backpropagation` without requiring manual normalization.

Retrieval is powered by **HippoRAG 2** (NeurIPS 2024) — a neurobiologically inspired framework that treats the knowledge graph as an artificial hippocampal index. Queries activate anchor nodes in the graph via semantic similarity, then run Personalized PageRank to propagate relevance across the entire knowledge base in a single step. The result is multi-hop retrieval that surfaces connections the user never consciously made.

```
[ingest]
    │
    ├──→ chunker (sliding window, 200 tokens, 40 overlap)
    │       ├──→ ONNX embedding  →  pgvector (chunk vectors + concept node vectors)
    │       └──→ LLM OpenIE     →  Neo4j (phrase nodes, passage nodes, typed triples,
    │                                      synonymy edges, context edges)
    │
[query]
    │
    ├──→ embed(query)  →  pgvector anchor lookup  →  seed nodes
    │                  →  pgvector dense retrieval →  candidate set B
    │
    └──→ Personalized PageRank (Neo4j, seeded by anchor nodes)
                                    │
                               candidate set A
                                    │
                            RRF fusion (A + B)
                                    │
                              top-k chunks  →  AI summary (Gemini Flash)
```

---

## Views

**Graph View** — An interactive D3.js force-directed visualization of the entire concept graph. Node size reflects degree centrality. Edge labels carry typed predicates. Clicking a concept node shows all sources that reference it and the concept's neighborhood in the graph. An AI-generated summary synthesizes what your knowledge base knows about that concept — grounded exclusively in your ingested content, never in the model's world knowledge.

**Source View** — A flat list of all ingested sources with their extracted concepts and links to related sources via shared graph nodes.

**Semantic Search** — A free-form natural language query interface. Returns ranked relevant chunks, the concepts most activated by the query, and a concise AI-generated synthesis grounded in your knowledge base.

---

## Stack

| Layer | Technology |
|---|---|
| Core | Java 21 · Virtual Threads · WebFlux |
| Embedding | ONNX Runtime · all-MiniLM-L6-v2 |
| Vector store | PostgreSQL · pgvector |
| Graph store | Neo4j |
| Concept extraction | Gemini Flash (free tier) |
| Frontend | React 19 · D3.js |
| Infra | Docker Compose |

No ML framework wrappers. The embedding pipeline runs directly on the JVM via ONNX Runtime — the model is a file, not a service.

---

## Design decisions

**Why HippoRAG 2 instead of standard RAG?**
Standard vector RAG retrieves by proximity to the query embedding alone. It has no mechanism for multi-hop reasoning — connecting a query about "weight updates" to a source about "backpropagation" via "gradient descent" unless all three happen to be semantically close to the query vector. HippoRAG 2 solves this by running Personalized PageRank on the knowledge graph seeded by anchor nodes, propagating relevance across the entire graph in a single retrieval step. It outperforms standard RAG by up to 20% on multi-hop benchmarks while remaining 10–30x cheaper than iterative approaches.

**Why two stores?**
pgvector and Neo4j solve different problems. pgvector answers *what is semantically close to this query?* Neo4j answers *what is conceptually connected to this concept across my entire knowledge base?* HippoRAG 2 requires both — concept node embeddings in pgvector for semantic anchor lookup, and the graph in Neo4j for PPR traversal. Neither store replaces the other.

**Why ONNX on the JVM?**
Avoiding a Python sidecar keeps the architecture honest. If the embedding pipeline can't run on the JVM, it's not a JVM system — it's two systems pretending to be one. ONNX Runtime with DJL HuggingFace Tokenizers runs `all-MiniLM-L6-v2` directly on the JVM with no external process, no network call, and no framework abstraction between the input text and the output vector.

**Why Gemini Flash for concept extraction?**
LLM-based Open Information Extraction produces significantly richer triples than end-to-end NLP models — HippoRAG's own ablation studies show 2x more triples than REBEL with better retrieval performance. Gemini Flash's free tier (1,500 requests/day) covers MVP-scale personal usage at zero cost. The extraction logic is isolated behind a port — migrating to a local Ollama model requires no architectural changes.

**Why no LangChain or Spring AI?**
The goal is to understand and control what happens between input and vector, between chunk and graph node, between query and ranked result. Framework abstractions hide exactly the parts that matter most here.

---

## Architecture

Kairos follows hexagonal architecture. Every external dependency — pgvector, Neo4j, Gemini Flash, the ONNX model — is accessed through a port interface. Adapters implement the ports. The application layer orchestrates use cases without knowing about infrastructure.

```
domain/
  model/          Source, Chunk, ConceptTriple, SearchResult
  port/           EmbeddingPort, OpenIEPort, VectorStorePort,
                  GraphStorePort, ChunkerPort, SummaryPort

application/
  service/        IngestionService, SearchService, GraphService

infrastructure/
  embedding/      OnnxEmbeddingAdapter
  openie/         GeminiOpenIEAdapter
  persistence/    SpringSourceRepositoryAdapter, JpaSourceRepository
  semantic/       SemanticSearchAdapter
  graph/          Neo4jGraphAdapter
```

---

## Status

> Active development. Not production-ready.

- [ ] Sliding window chunker with token-based overlap
- [ ] ONNX embedding pipeline (chunks + concept nodes)
- [ ] pgvector ingestion and semantic search
- [ ] LLM OpenIE concept extraction (Gemini Flash)
- [ ] Neo4j knowledge graph (phrase nodes, passage nodes, typed edges)
- [ ] Synonymy edge detection via embedding similarity
- [ ] HippoRAG 2 retrieval (Personalized PageRank + RRF fusion)
- [ ] AI-grounded summary generation
- [ ] Graph View with D3.js force-directed visualization
- [ ] Source View and Semantic Search UI

---

*Built by [Lucas Eckert](https://luca5eckert.github.io)*
