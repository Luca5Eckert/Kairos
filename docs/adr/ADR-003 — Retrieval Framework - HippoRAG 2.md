
- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

Kairos is a personal knowledge engine that accumulates content over time and must retrieve relevant knowledge in response to free-form user queries. The retrieval problem here is fundamentally different from standard document search:

- The user's query may be loosely worded or partial — the system must complete the pattern from the knowledge base
- The answer to a query often requires integrating information from multiple sources that are conceptually connected but not lexically similar
- Retrieval quality must improve passively as more content is ingested — the system should get smarter over time without re-indexing

Standard vector RAG (embed query → nearest neighbor search) fails here because it retrieves chunks based on proximity to the query vector alone. It has no mechanism for multi-hop reasoning — connecting a query about "weight updates" to a source about "backpropagation" via an intermediate concept "gradient descent" — unless all three happen to be semantically close to the query embedding, which they often are not.

---

## Decision

Kairos adopts **HippoRAG 2** (NeurIPS 2024 / arXiv 2502.14802) as its core retrieval framework.

HippoRAG 2 is a neurobiologically inspired retrieval framework that orchestrates dense embeddings, an open knowledge graph, and Personalized PageRank to mimic the pattern separation and completion functions of human long-term memory. It outperforms standard RAG by up to 20% on multi-hop QA benchmarks while being 10–30x cheaper and 6–13x faster than iterative retrieval methods.

### Biological Analogy

|Human Memory Component|HippoRAG 2 Equivalent|Kairos Implementation|
|---|---|---|
|Neocortex (knowledge processing)|LLM (OpenIE extraction, query entity extraction)|Gemini Flash|
|Parahippocampal Region (pattern linking)|Retrieval encoder (synonym detection, semantic anchor lookup)|all-MiniLM-L6-v2 via ONNX|
|Hippocampus (indexing and recall)|Open Knowledge Graph + Personalized PageRank|Neo4j|

### Knowledge Graph Structure

The graph in Kairos contains two types of nodes and four types of edges:

**Nodes:**

- `PhraseNode` — a concept or entity extracted from ingested content (e.g., `backpropagation`, `gradient descent`)
- `PassageNode` — a text chunk from an ingested source

**Edges:**

- `TRIPLE` — directed, typed relationship between two phrase nodes extracted via OpenIE (e.g., `backpropagation -[USES]-> chain rule`)
- `SYNONYMY` — undirected edge between two phrase nodes whose embeddings have cosine similarity ≥ 0.8. Connects lexical variants (`backprop` ↔ `backpropagation`) and semantically equivalent expressions without requiring exact string matching
- `CONTEXT` — undirected edge linking a passage node to every phrase node it contains
- `COOCCURRENCE` — undirected edge between two phrase nodes that appear in the same passage but have no explicit triple relation

### Offline Indexing (Ingestion)

For each ingested source, the following steps run during ingestion:

```
[text chunk]
      │
      ├──→ LLM OpenIE extraction
      │         → (subject, predicate, object) triples
      │         → PhraseNodes created or merged (MERGE in Neo4j)
      │         → TRIPLE edges created
      │         → PassageNode created, linked via CONTEXT edges
      │
      └──→ Embedding of each new PhraseNode
                → float[384] stored in pgvector (node vector index)
                → Synonymy detection: cosine similarity against existing node vectors
                → SYNONYMY edges created for pairs above threshold τ = 0.8
```

Node deduplication is handled by `MERGE` in Cypher — a concept mentioned in 50 sources produces one node with 50 connections, not 50 separate nodes.

### Online Retrieval (Query Time)

Retrieval runs in two parallel stages:

**Stage 1 — Anchor Node Identification**

The query is processed to identify seed nodes in the knowledge graph:

```
[user query: "how does backpropagation update weights?"]
      │
      ├──→ embed(query) → pgvector cosine search over node vectors
      │         → top-k semantically similar PhraseNodes
      │         → these become the anchor nodes (seeds for PPR)
      │
      └──→ embed(query) → pgvector cosine search over passage vectors
                → top-k semantically similar PassageNodes
                → these also become seeds for PPR
```

Both phrase nodes and passage nodes serve as PPR seeds. This is the key improvement in HippoRAG 2 over the original — passage-level seeding gives PPR richer starting points, especially for queries that don't map cleanly to extracted entity names.

**Stage 2 — Personalized PageRank**

With anchor nodes identified, PPR propagates relevance across the graph:

```
seed nodes: {backpropagation (score: 0.92), weights (score: 0.87), ...}
      │
      ▼
Personalized PageRank on Neo4j graph
      │
      Personalization vector v:
        - Phrase seeds: score = average retrieval score of their triple-generating nodes
        - Passage seeds: score = weighted embedding similarity to query
        - All other nodes: score = 0
      │
      ▼
PPR converges (power iteration, 20 iterations)
      │
      ▼
All passage nodes receive a PageRank score
      │
      ▼
Top-k passages ranked by PageRank score → Candidate Set A
```

PPR naturally performs multi-hop reasoning in a single pass — relevance flows from the query's anchor nodes through the graph, activating passages that are conceptually connected even if not directly similar to the query.

**Stage 3 — Dense Parallel Retrieval**

In parallel with PPR, a standard dense retrieval runs on pgvector:

```
embed(query) → pgvector <=> search over chunk vectors → Candidate Set B
```

This ensures that semantically similar chunks that may not be well-represented in the graph are still surfaced.

**Stage 4 — LLM Recognition Filter (optional)**

The top-k triples activated by PPR are passed to Gemini Flash with a lightweight prompt asking it to filter out irrelevant triples. This improves precision at the cost of one LLM call per query. Can be disabled for latency-sensitive use cases.

**Stage 5 — RRF Fusion**

Candidate Sets A (PPR-ranked passages) and B (dense retrieval) are merged using Reciprocal Rank Fusion:

```
RRF(d) = Σ  1 / (k + rank(d))     where k = 60
```

The final ranked list is the input to summary generation.

### Node Specificity

HippoRAG 2 applies a node specificity weight to PPR seeds — analogous to IDF in classical IR. Concepts that appear in many sources get lower specificity weight as seeds, preventing ubiquitous concepts (e.g., "model", "data") from dominating the activation and drowning out more specific, informative nodes.

```
specificity(node) = 1 / log(1 + document_frequency(node))
```

### Full Retrieval Flow

```
[query]
   │
   ├──→ embed(query)
   │       ├──→ pgvector node search → anchor PhraseNodes
   │       └──→ pgvector chunk search → anchor PassageNodes + Candidate Set B
   │
   ├──→ Personalized PageRank (Neo4j)
   │       seeds: anchor nodes weighted by retrieval score + node specificity
   │       output: Candidate Set A (PPR-ranked passages)
   │
   ├──→ [optional] LLM Recognition Filter on top triples
   │
   └──→ RRF(Candidate Set A, Candidate Set B)
               │
           top-k chunks
               │
           AI Summary (Gemini Flash)
```

---

## Performance Characteristics

Based on HippoRAG 2 published benchmarks:

|Metric|Standard RAG|HippoRAG 2|
|---|---|---|
|Multi-hop QA F1 (MuSiQue)|44.8%|51.9%|
|Multi-hop Recall@5 (2Wiki)|76.5%|90.4%|
|Factual QA F1|Comparable|Comparable|
|Indexing cost vs GraphRAG|—|~12x fewer LLM tokens|
|Retrieval latency|Fast|Fast (single-step, no iterative LLM calls)|

---

## Consequences

**Positive**

- Single-step multi-hop retrieval — no iterative LLM calls per query
- Relevance propagates through the graph, surfacing connections the user never explicitly made
- Synonymy edges solve lexical variation without manual normalization
- Passage node seeding (HippoRAG 2 improvement) makes retrieval robust to queries that don't map cleanly to extracted concepts
- Node specificity prevents common terms from dominating retrieval
- The knowledge graph is the Graph View visualization — retrieval infrastructure and UI share the same data model

**Negative**

- PPR adds computational overhead per query (~10-50ms for a moderately sized graph)
- Synonymy edge computation during indexing requires pairwise similarity checks — must use approximate nearest neighbor (ANN) index to remain tractable as graph grows
- LLM recognition filter adds latency if enabled — should be optional
- Cold-start problem: retrieval quality scales with graph richness; a sparse graph produces less useful PPR propagation

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|Standard vector RAG|No multi-hop capability; retrieval quality does not improve with graph growth|
|GraphRAG (Microsoft)|Superior sense-making but 12x more LLM tokens at indexing; no passage-level retrieval granularity|
|RAPTOR|Hierarchical summarization without explicit graph — cannot power the Graph View|
|HippoRAG v1|HippoRAG 2 strictly dominates on all benchmark categories while maintaining efficiency|
|LightRAG|Strong performance but less transparency in graph structure; harder to expose graph for visualization|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Sistema de busca]]
