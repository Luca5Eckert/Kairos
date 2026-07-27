- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

Before embedding or knowledge graph extraction, ingested source text must be split into chunks. This step sits at the very beginning of the ingestion pipeline and its parameters propagate through everything downstream:

- Chunk size determines the granularity of the semantic index — too large and embeddings become generic averages of many ideas; too small and chunks lack enough context to carry meaning
- Overlap between chunks determines whether ideas that span a chunk boundary are lost — without overlap, an idea that begins at the end of one chunk and concludes at the start of the next never appears complete in either
- Chunk size must respect the `all-MiniLM-L6-v2` hard limit of **256 tokens** — larger chunks are silently truncated by the model

In the HippoRAG 2 framework, chunks serve as both the unit of pgvector retrieval (chunk embeddings) and the passage nodes in the knowledge graph. The quality of both depends directly on chunking quality.

---

## Decision

Kairos uses a **Sliding Window with Overlap** chunking strategy, operating in token space.

### Parameters

|Parameter|Value|Rationale|
|---|---|---|
|`chunkSize`|200 tokens|Below the 256-token model limit with margin for special tokens `[CLS]` and `[SEP]`|
|`overlapSize`|40 tokens|~20% overlap — preserves context continuity across chunk boundaries|
|`minChunkSize`|20 tokens|Chunks below this threshold are discarded — insufficient for meaningful embedding or triple extraction|

### Sliding Window Mechanics

Given a tokenized sequence of N tokens:

```
Chunk 1:  tokens [0,   200)
Chunk 2:  tokens [160, 360)   ← 40-token overlap with chunk 1
Chunk 3:  tokens [320, 520)   ← 40-token overlap with chunk 2
...
```

The overlap window ensures that ideas crossing a chunk boundary are fully represented in at least one chunk on either side.

### Why Token-Based, Not Character-Based

Chunking is performed in token space because the model's limit is in tokens. A word like "backpropagation" tokenizes to 3–4 WordPiece tokens. Character or word-based chunking would produce chunks that, after tokenization, exceed the model limit and are silently truncated. The DJL tokenizer from the embedding pipeline (see ADR-004) is reused for token counting, ensuring consistency.

### Edge Cases

**Text shorter than `chunkSize`:** The entire text is returned as a single chunk with no splitting.

**Last chunk smaller than `minChunkSize`:** Discarded. A 15-token trailing fragment does not contain enough information for a useful embedding or meaningful triple extraction.

**Structured text (code, tables, lists):** Treated as plain text in MVP. Post-MVP improvement: detect structure and apply structure-aware splitting to avoid breaking code blocks or table rows mid-way.

### Impact on HippoRAG 2

Each chunk becomes both:

- A row in the pgvector chunk index (embedding for dense retrieval)
- A `PassageNode` in the Neo4j knowledge graph (linked to its extracted phrase nodes via `CONTEXT` edges)

The 40-token overlap means conceptually related chunks share phrase nodes, which densifies the graph around important ideas and improves PPR propagation quality.

### Port Architecture

```
ChunkerPort
      │
      └──→ SlidingWindowChunkerAdapter   (production)
      └──→ SingleChunkAdapter            (tests)
```

```java
public interface ChunkerPort {
    List<String> chunk(String text);
}
```

### Configuration

```yaml
kairos:
  ingestion:
    chunk-size: 200
    overlap-size: 40
    min-chunk-size: 20
```

---

## Consequences

**Positive**

- Token-based chunking respects the model's hard limit with no silent truncation
- 40-token overlap preserves idea continuity across boundaries
- Shared phrase nodes between overlapping chunks densify the knowledge graph around important concepts
- Parameters are externalized to `application.yml` — tunable without recompilation

**Negative**

- 20% overlap increases chunk count by ~20% — more embeddings, more OpenIE calls, more storage
- Does not respect natural semantic boundaries (paragraph, section) — an idea expressed across a paragraph boundary may still be split if it exceeds `chunkSize`
- `minChunkSize` discard may lose the last sentence of a source — acceptable tradeoff for MVP

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|Fixed-size without overlap|Abrupt cuts cause ideas to appear incomplete; degrades embedding and triple extraction quality|
|Paragraph-based chunking|Paragraphs vary wildly in length — some exceed the model limit, others are too short|
|Sentence-based chunking|A single sentence often lacks sufficient context for an expressive embedding|
|Character-based fixed size|Ignores token limit — produces silent truncation after model tokenization|
|Recursive splitting (LangChain style)|Additional complexity for marginal gain over sliding window at this stage|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Embedding]]
