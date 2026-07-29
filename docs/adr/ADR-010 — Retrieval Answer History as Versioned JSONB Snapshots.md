# ADR-010 — Retrieval Answer History as Versioned JSONB Snapshots

- **Status:** Accepted
- **Date:** 2026-07-29
- **Context:** Kairos V1 retrieval history

---

## Context

Kairos V1 needs to preserve retrieval executions for auditability, explainability, and future quality-analysis workflows. A persisted result must retain the passages, scores, graph seeds, activated triples, and execution parameters that explain the outcome at the time it was produced.

These values are execution data. They are not the current relational state of `Chunk`, `Source`, `Triple`, or the Neo4j graph. Reconstructing them later through strong foreign-key relationships would require extra queries and could produce a different result after reindexing, deletion, or graph evolution.

The existing `SearchResult` is a runtime domain object. It contains full domain objects such as `Chunk` and must not be serialized as history because that can include embeddings, full source content, and implementation details.

## Decision

History remains inside the `context_engine` module, under the history domain.

```text
User 1 ── N Question 1 ── N Answer
```

`Question` represents a user query. `Answer` represents one complete retrieval execution for that question. Re-executing or regenerating a question creates a new answer instead of overwriting historical data.

An answer persists the following relational fields:

- `id: UUID`
- `questionId: UUID`
- `schemaVersion: int`
- `snapshot: AnswerSnapshot` as PostgreSQL `JSONB`
- `createdAt: Instant`

There is no separate `RetrievalTrace` entity or table.

`AnswerSnapshot` is a typed persistence model, similar in shape to `SearchResult` but independent of runtime domain objects. It contains:

- retrieval version and parameters;
- graph seeds and their weights;
- selected ranked passages with correlation IDs, selected content, rank, final score, dense score, graph score, and source;
- activated triples with correlation data, structural relation weight, and activation score when available.

The snapshot is immutable after creation. Its embedded IDs are correlation values, not mandatory FKs to the current chunks, sources, or triples. An answer is readable as a complete historical result without querying PostgreSQL content tables or Neo4j.

Snapshots must exclude query and passage embeddings and full source documents.

`schemaVersion` is mandatory so readers can handle future changes to the JSON structure.

## Consequences

### Positive

- One answer read returns the complete historical retrieval result without joins or graph lookups.
- History remains interpretable even if knowledge is reprocessed or graph state evolves.
- The shape of retrieval evidence can evolve without creating a normalized table for every intermediate algorithm stage.
- V1 writes stay simple; JSON indexes are deferred until a real analytical query requires them.
- The model keeps `User` as an identity boundary and keeps retrieval history in `context_engine`.

### Negative

- The database cannot enforce referential integrity for IDs inside the snapshot.
- Frequent analytical filtering over JSON fields may require expression indexes or a separate analytical projection later.
- The application must validate the typed snapshot and maintain compatibility by `schemaVersion`.
- Selected passage content is duplicated intentionally to make the historical answer self-contained.

## Alternatives Considered

| Alternative | Reason rejected |
|---|---|
| Separate `RetrievalTrace` entity | The trace is the answer's result, not an independent aggregate for V1. |
| Normalized passage, seed, and triple trace tables | Adds joins and rigid relationships for data that is an execution snapshot. |
| Serialize `SearchResult` directly | Risks serializing `Chunk`, embeddings, source content, and runtime implementation details. |
| Keep only IDs and rebuild the answer on read | Reprocessing or deletion can make historical results non-reproducible and requires additional lookups. |

## Related

- [[05 - Histórico de perguntas, respostas e rastros]]
- [[ADR-009 — HippoRAG 2 Retrieval Domain Model]]
- [[06 - Modelo de domínio de retrieval para HippoRAG 2]]
- [[Reforço de aprendizado por uso]]
