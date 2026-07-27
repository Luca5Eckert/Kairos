---
type: adr
project:
  - kairos
tags:
  - kairos
  - adr
  - retrieval
  - hipporag
  - domain-model
status: accepted
created: 2026-04-27
updated: 2026-04-27
---

# ADR-009 — HippoRAG 2 Retrieval Domain Model

- **Status:** Accepted
- **Date:** 2026-04-27
- **Context:** Kairos — V1 retrieval architecture

---

## Context

Kairos adopts [[HippoRAG 2]] as its retrieval framework, as defined in [[ADR-003 — Retrieval Framework - HippoRAG 2]]. The current implementation, however, still models the MVP flow primarily around `Chunk` and `KnowledgeTriple`:

```text
query -> chunks -> graph -> triples -> chunks
```

This model is not expressive enough for the V1 retrieval pipeline. HippoRAG 2 needs to represent passage candidates, triple candidates, recognition memory, graph seeds, weighted propagation, scored passages, dense fallback, and activated triples.

The current domain loses important retrieval information too early, especially dense scores and graph scores. It also treats `KnowledgeTriple` as the main graph-search result, while the correct primary result should be a ranked set of passages that can later be hydrated from PostgreSQL.

---

## Decision

Kairos will introduce an explicit retrieval domain model before implementing full weighted Personalized PageRank.

The domain model is divided into three groups:

1. **Content model**
   - `Source`
   - `Chunk`
   - `PassageRef`

2. **Knowledge model**
   - `Concept`
   - `Triple` / future `ExtractedTriple`
   - `KnowledgeTriple`
   - future `Synonymy`
   - future graph metrics separated from concept identity

3. **Retrieval model**
   - `PassageCandidate`
   - `TripleCandidate`
   - `FilteredTriple`
   - `GraphSeed`
   - `SeedType`
   - `GraphSeedTarget`
   - `PassageSeedTarget`
   - `ConceptSeedTarget`
   - `GraphSearchRequest`
   - `ScoredPassage`
   - `GraphSearchResult`
   - `RankedChunk`
   - `RetrievalSource`
   - optional `RetrievalTrace`

`KnowledgeGraphSearch` should evolve from returning `List<KnowledgeTriple>` to returning `GraphSearchResult`.

`GraphSearchResult` should contain:

- `List<ScoredPassage>` as the primary ranking result;
- `List<KnowledgeTriple>` as activated explanatory knowledge.

`SearchResult` should evolve from returning raw `Chunk` objects to returning `RankedChunk` objects.

The domain must not expose Neo4j internal `nodeId` values. Graph seeds should target domain-level identifiers such as `PassageSeedTarget(chunkId)` and `ConceptSeedTarget(conceptKey)`. The Neo4j adapter is responsible for resolving those targets into internal graph nodes.

---

## Consequences

### Positive

- The domain can name the retrieval algorithm before the full algorithm is implemented.
- Dense and graph scores can be preserved through the pipeline.
- Graph search becomes passage-first instead of triple-first.
- PostgreSQL remains the textual source of truth.
- Neo4j remains a structural projection used for propagation.
- The final retrieval result becomes explicit about ranking source: graph, dense fallback, or both.

### Negative

- Existing DTOs and use-case tests will need to change.
- The first implementation PR mostly prepares the model and may not improve retrieval quality immediately.
- The current Neo4j adapter will need a temporary compatibility mapping until full weighted PPR is implemented.
- Changing `Concept` identity should be deferred because it affects Neo4j identity, Cypher queries, and future user scoping.

---

## Alternatives Considered

| Alternative | Reason rejected |
|---|---|
| Implement weighted PPR immediately | The current domain cannot represent seeds, scores, ranked passages, or fallback correctly. |
| Keep `KnowledgeTriple` as the main graph-search return type | Activated triples are explanatory knowledge; they are not the final retrieval ranking unit. |
| Change `Concept` identity in the same PR | Concept identity and user scoping require a separate migration and query strategy. |
| Store chunk text in Neo4j `PassageNode` | PostgreSQL is the textual source of truth; Neo4j should remain a structural projection. |

---

## Related

- [[Modelo de domínio de retrieval para HippoRAG 2]]
- [[ADR-002 — Dual-Store Architecture pgvector + Neo4j]]
- [[ADR-003 — Retrieval Framework - HippoRAG 2]]
- [[Arquitetura Hexagonal]]
- [[Arquitetura dual-store]]
- [[PostgreSQL]]
- [[pgvector]]
- [[Neo4j GDS]]
- [[Grafo de conhecimento]]
- [[PageRank Personalizado]]
