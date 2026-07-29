# ADR-005 — ONNX Embedding Pipeline - all-MiniLM-L6-v2 on the JVM

- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

Kairos requires dense vector embeddings in three distinct contexts:

1. **Chunk embedding** — each text chunk is embedded during ingestion and stored in pgvector for semantic retrieval
2. **Concept node embedding** — each extracted phrase node is embedded during ingestion and stored in pgvector for semantic anchor lookup during HippoRAG 2 retrieval
3. **Query embedding** — the user's query is embedded at query time for both dense chunk retrieval and anchor node identification

All three contexts use the same embedding model to ensure vectors exist in the same semantic space — a requirement for cosine similarity comparisons between queries, chunks, and concept nodes.

Kairos has an explicit architectural principle: no Python sidecars. The embedding pipeline must run entirely on the JVM.

---

## Decision

Kairos uses **`all-MiniLM-L6-v2`** exported in ONNX format, executed on the JVM via **ONNX Runtime**, with tokenization provided by **DJL HuggingFace Tokenizers**.

### Why all-MiniLM-L6-v2

- Trained by Microsoft on over 1 billion sentence pairs — robust semantic representation across domains
- Produces **384-dimensional** vectors — sufficient expressiveness for semantic search, compact enough for low latency and storage efficiency
- Official ONNX export available at `sentence-transformers/all-MiniLM-L6-v2` on Hugging Face
- Maximum sequence length of 256 tokens aligns with the chunking parameters (see ADR-005)
- Runs on CPU at ~20-50ms per embedding — acceptable for async ingestion and sub-100ms query latency

### Required Files

Stored in `src/main/resources/model/`:

```
sentence-transformers/all-MiniLM-L6-v2/tree/main/onnx/

├── model.onnx        ← model weights and computational graph (~23MB)
└── tokenizer.json    ← WordPiece vocabulary and tokenization rules
```

No network connection is required at runtime. Both files are static artifacts versioned with the application.

### Dependencies

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.17.0</version>
</dependency>

<dependency>
    <groupId>ai.djl.huggingface</groupId>
    <artifactId>tokenizers</artifactId>
    <version>0.25.0</version>
</dependency>
```

### Embedding Pipeline

Five sequential steps per text input:

```
[input text]
      │
      ▼
① Tokenization  (DJL HuggingFaceTokenizer)
  Reads tokenizer.json, applies WordPiece
  Output: inputIds, attentionMask, tokenTypeIds
  Example:
    "backpropagation uses chain rule"
    → [101, 2067, 4904, 6182, 2038, 4677, 2484, 102]
      │
      ▼
② Tensor Assembly
  Shape: [1, sequenceLength] per tensor  (batchSize = 1)
      │
      ▼
③ ONNX Inference  (OrtSession.run)
  Output shape: [1, sequenceLength, 384]
  One 384-dim vector per token
      │
      ▼
④ Mean Pooling
  Average of token vectors where attentionMask = 1
  Padding tokens excluded — including them pulls the vector toward zero
  Output: float[384]
      │
      ▼
⑤ L2 Normalization
  Divides each dimension by the vector's Euclidean norm
  Ensures unit magnitude — required for cosine distance correctness
  Without this, longer sequences produce higher-norm vectors and are
  systematically favored in <=> searches regardless of semantic relevance
  Output: float[384], ||v|| = 1
      │
      ▼
[float[384] → pgvector]
```

### Singleton Initialization

`OrtSession` creation takes ~500ms and must happen once at application startup:

```java
@Bean
public OrtSession onnxSession(OrtEnvironment env,
        @Value("classpath:model/model.onnx") Resource model) throws Exception {
    return env.createSession(model.getFile().getAbsolutePath());
}

@Bean
public HuggingFaceTokenizer huggingFaceTokenizer(
        @Value("classpath:model/tokenizer.json") Resource tokenizer) throws Exception {
    return HuggingFaceTokenizer.newInstance(tokenizer.getFile().toPath());
}
```

Both beans are Spring singletons — initialized once, shared across all requests and virtual threads.

### Port Architecture

```
EmbeddingPort
      │
      └──→ OnnxEmbeddingAdapter     (production — local model.onnx)
      └──→ MockEmbeddingAdapter     (tests — normalized random vectors)
```

```java
public interface EmbeddingPort {
    float[] embed(String text);
}
```

---

## Consequences

**Positive**

- Zero network dependency at runtime — works fully offline
- No Python sidecar — the system is honestly a JVM application
- The same model used for chunks, concept nodes, and queries guarantees a unified semantic space
- DJL Tokenizers handles WordPiece correctly without manual implementation
- `OrtSession` singleton eliminates per-request initialization overhead

**Negative**

- `model.onnx` adds ~23MB to the artifact
- DJL Tokenizers uses JNI — introduces a native library dependency
- Swapping the model requires recompilation and redeployment
- CPU-only by default — for high ingestion volumes, ONNX Runtime supports CUDA providers but requires additional configuration

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|Python sidecar with sentence-transformers|Violates the no-sidecar architectural principle|
|External embedding API (OpenAI, Cohere)|Runtime network dependency; per-token cost; data leaves local environment|
|Spring AI TransformersEmbeddingModel|Couples unnecessarily to Spring AI ecosystem; same ONNX mechanism underneath|
|Manual WordPiece tokenizer|~500 lines of fragile Java; DJL solves it with one dependency|
|Larger model (all-mpnet-base-v2, 768 dims)|Better quality but 2x slower, 4x more storage per vector; unnecessary for MVP|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Implementação modelo de embedding]]
- [[Embedding]]
