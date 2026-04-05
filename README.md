# Kairos

*What changes when memory has semantic structure?*

You read. You take notes. Three weeks later you have an intuition that something you learned is relevant — but you can't remember where it was, how it was phrased, or how it connects to what you've learned since.

Kairos is a personal knowledge engine built around that problem. Not a note-taking app. Not a search tool. A dual-store memory system that finds what you **understand**, not what you **wrote**.

---

## How it works

Every piece of knowledge you ingest goes through two parallel paths:

**Semantic path** — content is chunked with overlap, embedded via a local ONNX model, and stored in pgvector. Queries return results by meaning, not keyword.

**Relational path** — concepts are extracted from the content and stored as nodes in a graph. Connections between concepts form automatically as your knowledge grows.

A query triggers both paths in parallel. Results are merged and ranked via Reciprocal Rank Fusion — giving you chunks that are semantically close *and* conceptually connected to your question.

```
[input]  →  chunker  →  embedding pipeline (ONNX)  →  pgvector
                    →  concept extractor            →  Neo4j

[query]  →  embed query  →  vector search   ─┐
                         →  graph traversal ─┴→  fusion  →  result
```

---

## Stack

| Layer | Technology |
|---|---|
| Core | Java 21 · Virtual Threads · WebFlux |
| Embedding | ONNX Runtime · all-MiniLM-L6-v2 |
| Vector store | PostgreSQL · pgvector |
| Graph store | Neo4j |
| Cache | Redis |
| Frontend | React 19 · D3.js |
| Infra | Docker Compose |

No ML framework wrappers. The embedding pipeline runs directly on the JVM via ONNX Runtime — the model is a dependency, not a black box.

---

## Design decisions

**Why two stores?**
Semantic similarity and explicit relationship are different things. pgvector answers *what is close to this?* Neo4j answers *what is connected to this?* Both matter. Neither replaces the other.

**Why ONNX on the JVM?**
Avoiding a Python sidecar keeps the architecture honest. If the embedding pipeline can't run on the JVM, it's not a JVM system — it's two systems pretending to be one.

**Why no LangChain?**
The goal is to understand what happens between input and vector, not to abstract it away.

---

## Status

> Active development. Not production-ready.

- [ ] Semantic chunker with overlap
- [ ] ONNX embedding pipeline
- [ ] pgvector ingestion and search
- [ ] Neo4j concept graph
- [ ] Fusion layer (RRF)
- [ ] Query API
- [ ] React interface with graph visualization

---

*Built by [Lucas Eckert](https://luca5eckert.github.io)*
