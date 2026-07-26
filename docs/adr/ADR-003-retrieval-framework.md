# ADR-003 - Graph-Augmented Retrieval Framework

- Status: Accepted
- Scope: Kairos V1 retrieval
- Decision owner: Lucas Eckert
- Last reviewed: 2026-07-26

## Context

Kairos needs to retrieve useful context from personal source material when the relevant information is not contained in a single semantically similar passage.

Dense vector search is a strong baseline for lexical and semantic similarity, but it treats passages mostly independently. Some questions depend on relationships distributed across extracted facts, concepts, and passages.

The first graph-seed implementation retrieved concepts directly by dense similarity. That approach produced valid candidates, but isolated concept matches lost the subject-predicate-object context in which the concepts appeared.

## Decision

Use a multi-stage retrieval pipeline inspired by HippoRAG 2:

1. generate the query embedding locally with ONNX Runtime;
2. retrieve user-scoped passage candidates from PostgreSQL/pgvector;
3. retrieve user-scoped triple candidates from PostgreSQL/pgvector;
4. constrain Recognition Memory to selecting subjects or objects already present in the retrieved triples;
5. combine passage and concept seeds;
6. apply absolute and relative score thresholds;
7. project only the authenticated user's Neo4j subgraph;
8. run Personalized PageRank through Neo4j Graph Data Science;
9. rank passage nodes by graph score;
10. rehydrate final chunks from PostgreSQL under the same ownership boundary;
11. return activated triples beside ranked chunks as relationship evidence.

Graph expansion remains separate from seed selection. The redesign changes which anchors enter the graph rather than rewriting the propagation algorithm.

## Store responsibilities

- PostgreSQL is the durable source of truth for source text, chunks, triples, users, and authentication data.
- pgvector performs passage and triple dense retrieval.
- Neo4j stores a derived structural graph of passages, concepts, and relationships.
- Neo4j GDS performs Personalized PageRank.
- ONNX Runtime produces local JVM embeddings.
- Gemini, behind Spring AI ports, performs triple extraction and constrained recognition decisions.

## Alternatives considered

### Vector-only retrieval

Advantages:

- simpler architecture;
- deterministic serving path;
- no graph projection or propagation cost.

Rejected as the only retrieval mode because it cannot explicitly traverse relationships between passages and extracted facts.

### Direct concept-candidate retrieval

Advantages:

- simpler than triple retrieval;
- concepts can be embedded and selected directly.

Replaced because isolated concept candidates lose the relational context provided by complete triples.

### LLM-generated graph seeds without candidate constraints

Advantages:

- flexible interpretation of the query;
- potentially broad conceptual recall.

Rejected because the model could invent or normalize concepts that do not exist in the stored graph. Recognition Memory is therefore constrained to values present in retrieved triples.

### Neo4j as the only source of truth

Advantages:

- one store for graph traversal and source relationships.

Rejected because PostgreSQL is better suited to durable source text, authentication data, relational constraints, and vector search in the current system. Neo4j remains a derived graph projection.

## Consequences

### Positive

- Retrieval can combine semantic similarity and relationship structure.
- User isolation is applied during dense recall, graph projection, propagation, and hydration.
- LLM authority is limited to extraction and candidate selection rather than final ranking.
- Activated triples make the graph contribution inspectable.
- Each infrastructure concern remains behind a domain port.

### Negative

- The query path spans PostgreSQL, pgvector, an LLM recognition step, and Neo4j GDS.
- Per-user graph projection adds latency and operational complexity.
- Asynchronous enrichment means newly uploaded sources may not be immediately searchable through the graph.
- A missing GDS installation currently results in an empty graph expansion rather than a dense-only fallback.
- Triple extraction quality affects downstream graph quality.

## Verification

The current implementation is protected by tests covering:

- passage and triple candidate retrieval;
- recognition-memory selection;
- seed thresholding;
- user-scoped graph projection;
- Personalized PageRank parameter mapping;
- activated-triple mapping;
- PostgreSQL chunk hydration;
- missing-GDS and empty-result behavior.

Latest local verification executed 244 tests with 0 failures and 0 errors. JaCoCo reports 86.77% line coverage and 74.54% branch coverage.

These metrics validate implementation behavior, not retrieval relevance.

## Evaluation gap

No labeled retrieval dataset currently proves that this decision improves Recall@K, MRR, NDCG, or answer quality over vector-only retrieval or direct concept candidates.

The next evidence milestone is a reproducible evaluation comparing:

1. vector-only passage retrieval;
2. passage plus direct concept seeds;
3. passage plus triple recall and Recognition Memory.

The evaluation should publish relevance metrics, p50/p95 latency, model calls, cost, and representative failure cases.

## Related implementation

- [PR #59 - replace direct concept candidates with triple-based recognition seeds](https://github.com/Luca5Eckert/Kairos/pull/59)
- [Kairos README](../../README.md)
