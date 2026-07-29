# ADR-002 — Dual-Store Architecture pgvector + Neo4j

- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

Kairos serves two fundamentally different retrieval needs from a single knowledge base:

**Graph View** requires traversing explicit conceptual relationships — which concepts are connected to this one, how they relate, which sources mention them. This is a graph problem. It requires a store that understands nodes, edges, and path traversal natively.

**Semantic Search** requires finding content that is close in meaning to a free-form query, even when different words are used. This is a vector similarity problem. It requires a store optimized for high-dimensional nearest-neighbor search.

No single storage system solves both problems with equal effectiveness. A vector store has no native concept of relationships or graph traversal. A graph store has no native concept of semantic similarity across arbitrary text.

The retrieval framework chosen for Kairos — HippoRAG 2 (see ADR-002) — explicitly requires both: a knowledge graph for Personalized PageRank traversal and a vector index for dense semantic retrieval. The architecture is designed around this dual requirement.

---

## Decision

Kairos maintains two parallel storage systems with clearly separated responsibilities:

|Store|Technology|Primary Role|
|---|---|---|
|Semantic Index|PostgreSQL + pgvector|Dense vector search over embedded chunks and concept nodes|
|Knowledge Graph|Neo4j|Concept graph storage, traversal, and Personalized PageRank|

Every ingested source populates both stores. They are written to in parallel during ingestion and queried in parallel during retrieval.

### pgvector — Semantic Index

Stores two types of embeddings:

**Chunk embeddings** — each text chunk from an ingested source is embedded via the ONNX pipeline and stored as a `vector(384)` row. Used for dense semantic retrieval during search.

**Concept node embeddings** — each concept node in the knowledge graph also has its name embedded and stored in pgvector. Used for semantic anchor lookup during HippoRAG 2 retrieval — finding which graph nodes are most relevant to a query via cosine similarity, not exact name match.

This dual use of pgvector is what makes semantic matching on the graph possible. Searching for "backprop" can semantically resolve to the "backpropagation" node without requiring exact string matching.

### Neo4j — Knowledge Graph

Stores the open knowledge graph constructed from ingested sources:

- **Phrase nodes** — extracted concepts and entities (e.g., `backpropagation`, `chain rule`)
- **Passage nodes** — each ingested chunk, linked to the phrase nodes it contains
- **Triple edges** — typed relationships between phrase nodes (e.g., `USES`, `COMPUTES`, `EXTENDS`)
- **Synonymy edges** — undirected edges between phrase nodes whose embeddings exceed cosine similarity threshold τ = 0.8, connecting semantically equivalent nodes that may use different surface forms
- **Context edges** — edges linking passage nodes to all phrase nodes they contain

This graph structure is what enables Personalized PageRank to propagate relevance across the entire knowledge base in a single retrieval step.

### Data Flow

```
[Source Ingested]
       │
       ├──→ Chunker
       │       ├──→ ONNX Embedding → pgvector (chunk vectors)
       │       └──→ LLM OpenIE Extraction → Neo4j (triples + passage nodes)
       │
       └──→ Concept Embedding → pgvector (node vectors) + Neo4j (synonymy edges)


[Semantic Search]
       └──→ HippoRAG 2 Retrieval (see ADR-002)
               ├──→ pgvector: anchor node lookup by cosine similarity
               ├──→ Neo4j: Personalized PageRank from anchor nodes
               └──→ pgvector: dense chunk retrieval in parallel
                       └──→ RRF fusion → ranked chunks → AI summary


[Graph View]
       └──→ Neo4j: concept graph traversal → D3.js visualization
               └──→ concept click → Neo4j passage nodes + pgvector chunk retrieval
```

---

## Consequences

**Positive**

- Each store is optimized for what it does best — no compromises
- Synonymy edges in Neo4j solve the lexical variation problem at the graph level
- Concept node embeddings in pgvector enable semantic anchor lookup — the graph becomes searchable by meaning, not just by name
- Both stores evolve independently without coupling
- HippoRAG 2's Personalized PageRank runs entirely on Neo4j — no cross-store computation during retrieval

**Negative**

- Two storage systems to operate, monitor, and back up
- Ingestion must handle partial failures gracefully — Neo4j failure must not block pgvector writes and vice versa
- Concept node embeddings must be kept in sync between Neo4j (graph structure) and pgvector (vector index) — a new node in Neo4j requires a corresponding vector insert in pgvector

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|pgvector only|Cannot power the Graph View or run Personalized PageRank|
|Neo4j only|No native dense vector search; cannot perform semantic anchor lookup|
|Neo4j with built-in vector index|Neo4j's vector index is less mature and performant than pgvector for high-volume similarity search|
|Single unified graph database (e.g., Weaviate)|Does not provide the graph traversal depth and PPR capability required by HippoRAG 2|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Sistema de busca]]
