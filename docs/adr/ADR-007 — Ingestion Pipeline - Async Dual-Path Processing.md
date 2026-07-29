# ADR-007 — Ingestion Pipeline - Async Dual-Path Processing

- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

When a source is ingested, it must populate two independent stores — pgvector (semantic index) and Neo4j (knowledge graph) — through a multi-step pipeline. The pipeline has the following characteristics that must inform its design:

**Step heterogeneity** — some steps are CPU-bound and fast (~20-50ms per chunk), others are I/O-bound and depend on external APIs (~500ms-1s per chunk for Gemini Flash).

**Path independence** — the pgvector path (embed → store chunk vector) and the Neo4j path (OpenIE extraction → store triples + passage nodes + synonymy edges) are fully independent. Neither path needs to wait for the other.

**Failure isolation** — a failure in one path must not corrupt or block the other. If Gemini fails for a chunk, the embedding already computed must not be discarded. If the ONNX model fails, the Neo4j path should still complete.

**User experience** — sources can be large (multi-page documents, long articles). The user must not be blocked waiting for full ingestion to complete.

---

## Decision

The ingestion pipeline is **asynchronous** and runs the semantic and graph paths **in parallel per chunk**, using Java 21 Virtual Threads.

### Pipeline Architecture

```
POST /sources  →  202 Accepted  {id, status: PENDING}
                        │
                [Virtual Thread]
                        │
                        ▼
              ① ChunkerPort.chunk(text)
              Produces: List<String> chunks
                        │
              For each chunk (sequential — respects Gemini rate limit):
                        │
              ┌─────────┴──────────────────────────────┐
              │                                        │
              ▼                                        ▼
     ── SEMANTIC PATH ──                    ── GRAPH PATH ──
     ② EmbeddingPort.embed(chunk)          ④ OpenIEPort.extract(chunk)
        ONNX Runtime (~30ms)                  Gemini Flash (~700ms)
              │                                        │
              ▼                                        ▼
     ③ pgvector: INSERT chunk vector       ⑤ Neo4j: MERGE triples,
        + PassageNode chunk_id reference       PhraseNodes, PassageNode,
                                               CONTEXT + TRIPLE edges
                                               │
                                               ▼
                                          ⑥ Synonymy detection:
                                             embed new PhraseNodes →
                                             pgvector ANN lookup →
                                             MERGE SYNONYMY edges
              │                                        │
              └──────────────┬─────────────────────────┘
                             │
                    Chunk status updated
                             │
              [After all chunks complete]
                             │
                    Source status → COMPLETED
                    (or PARTIAL_FAILURE / FAILED)
```

### Async Response Contract

```
POST /sources
Body: { "title": "...", "content": "..." }

→ 202 Accepted
  { "id": "uuid", "status": "PENDING" }

GET /sources/{id}
→ 200 OK
  {
    "id": "uuid",
    "title": "...",
    "status": "COMPLETED",        ← PENDING | PROCESSING | COMPLETED | PARTIAL_FAILURE | FAILED
    "chunk_count": 12,
    "chunks_processed": 12,
    "concepts_extracted": 47
  }
```

### Parallelism Model

Within each chunk, the semantic path and graph path run in parallel via `CompletableFuture`:

```java
var semanticFuture = CompletableFuture.runAsync(() -> {
    var vector = embeddingPort.embed(chunk);
    vectorRepository.saveChunk(chunkId, vector, content);
}, virtualThreadExecutor);

var graphFuture = CompletableFuture.runAsync(() -> {
    var triples = openIEPort.extract(chunk);
    graphRepository.persistTriples(chunkId, triples);
    synonymyService.detectAndLink(triples.concepts());
}, virtualThreadExecutor);

CompletableFuture.allOf(semanticFuture, graphFuture).join();
```

Chunks are processed **sequentially** (not in parallel) to respect Gemini Flash's free tier rate limit of 1,500 requests/day and avoid overwhelming Neo4j with concurrent writes.

### Failure Handling

|Failure Scenario|Behavior|Source Status Impact|
|---|---|---|
|ONNX fails on one chunk|Log error, chunk skipped in pgvector, continue|`PARTIAL_FAILURE`|
|Gemini fails on one chunk|Retry with exponential backoff (1s, 2s, 4s), then skip|`PARTIAL_FAILURE`|
|Gemini returns malformed JSON|Retry once with stricter prompt, then skip|`PARTIAL_FAILURE`|
|Gemini rate limit (429)|Wait for backoff window, then retry|No impact if recovered|
|pgvector unavailable|Fail entire ingestion — semantic index is critical path|`FAILED`|
|Neo4j unavailable|Fail graph path only — semantic path continues|`PARTIAL_FAILURE`|
|ONNX session uninitialized|Fail entire ingestion — application startup failure|`FAILED`|

### Source Status Semantics

|Status|Meaning|
|---|---|
|`PENDING`|Ingestion queued, not yet started|
|`PROCESSING`|Currently being chunked and processed|
|`COMPLETED`|All chunks processed successfully in both paths|
|`PARTIAL_FAILURE`|Embeddings saved, but some chunks missing graph data (or vice versa)|
|`FAILED`|Critical failure — source is not usable for retrieval|

### Reprocessing

Sources with `PARTIAL_FAILURE` status expose a reprocessing endpoint:

```
POST /sources/{id}/reprocess
```

Reprocesses only the failed chunks. For chunks where the semantic path succeeded but the graph path failed, only the graph path is re-executed — embeddings already stored are not regenerated.

### Configuration

```yaml
kairos:
  ingestion:
    chunk-size: 200
    overlap-size: 40
    min-chunk-size: 20
    openie:
      retry-max-attempts: 3
      retry-initial-backoff-ms: 1000
      retry-backoff-multiplier: 2
```

---

## Consequences

**Positive**

- Immediate `202 Accepted` response — user is never blocked by long ingestion
- Semantic and graph paths are decoupled — failure in one does not corrupt the other
- Virtual Threads provide lightweight concurrency for I/O-bound Gemini calls without thread pool tuning
- Partial failures are recoverable without re-running the full pipeline
- Sequential chunk processing naturally rate-limits Gemini calls

**Negative**

- `PENDING`/`PROCESSING` status requires client polling — WebSocket or SSE push notification is a post-MVP improvement
- Async error handling is more complex — stack traces are disconnected from the originating HTTP request
- Sequential chunk processing means a 100-chunk source takes at minimum 100 × ~700ms ≈ 70s for the graph path — fast for the semantic path, but the graph lags
- Synonymy detection per chunk adds Neo4j + pgvector round-trips — may become a bottleneck for large batches (addressed post-MVP by batching)

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|Synchronous pipeline|Unacceptable UX for large sources — HTTP timeout|
|Message queue (Kafka, RabbitMQ)|Operational overhead unjustified for MVP; Virtual Threads solve the async problem|
|Parallel chunk processing|Risks Gemini rate limiting and Neo4j write contention|
|All-or-nothing failure (no partial status)|Wastes already-computed embeddings on graph failures; forces full re-ingestion|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Sistema de busca]]
