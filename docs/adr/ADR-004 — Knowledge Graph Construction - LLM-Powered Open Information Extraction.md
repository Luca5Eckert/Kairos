# ADR-004 — Knowledge Graph Construction - LLM-Powered Open Information Extraction

- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

The knowledge graph in Neo4j is the foundation of Kairos's Graph View and the index that powers HippoRAG 2 retrieval. Its quality determines the quality of everything above it — poor extraction produces a misleading graph, weak PPR propagation, and inaccurate summaries.

The graph must be constructed automatically from raw text without requiring any user input. This means extracting:

- **Concepts and entities** (phrase nodes) from each chunk
- **Typed relationships** between them (triple edges)
- **Passage-level associations** linking chunks to the concepts they contain (context edges)

Rule-based approaches (spaCy NER, dependency parsing) are too rigid for the varied domains and informal language of personal knowledge bases. End-to-end OpenIE models (REBEL) produce fewer triples than LLMs and miss conceptual associations. HippoRAG's own ablation studies confirm that LLM-based extraction produces 2x more triples than REBEL and significantly better retrieval performance.

---

## Decision

Kairos uses **Gemini Flash** as the OpenIE engine, called once per chunk during ingestion to extract open knowledge graph triples in JSON format.

### Why LLM-Based OpenIE

The HippoRAG paper's ablation studies (Table 5) show that LLM-based OpenIE significantly outperforms end-to-end models. The LLM's flexibility allows it to extract both explicit and implicit relationships, handle varied terminology, and produce triples with general concepts that narrow NLP models systematically miss.

Gemini 2.0 Flash is chosen for the MVP because of its free tier (1,500 requests/day), strong instruction-following for structured JSON output, and zero local hardware requirements. The extraction logic is isolated behind a port — swapping to a local Ollama model (Llama 3.x 8B) requires no architectural changes.

### Extraction Prompt

```
You are a knowledge graph construction engine. Given the text below, extract all
meaningful (subject, predicate, object) triples that represent relationships between
concepts, entities, or ideas.

Return ONLY valid JSON in this exact format, with no preamble or markdown:
{
  "triples": [
    { "subject": "concept_a", "predicate": "RELATION_TYPE", "object": "concept_b" }
  ]
}

Rules:
- All subjects, predicates, and objects must be in English
- Subjects and objects must be lowercase, normalized noun phrases
  (e.g. "gradient descent", not "Gradient Descent" or "the gradient descent algorithm")
- Predicates must be uppercase verb phrases (e.g. "USES", "EXTENDS", "COMPUTES", "CONTRADICTS")
- Extract both explicit and implicit relationships supported by the text
- Prefer specific, informative predicates over generic ones ("COMPUTES" over "IS RELATED TO")
- Do not extract trivial or circular relationships

Text:
{chunk}
```

### Node Deduplication

Before creating any node, Cypher's `MERGE` ensures deduplication across all ingested sources:

```cypher
MERGE (s:PhraseNode {name: $subject})
MERGE (o:PhraseNode {name: $object})
MERGE (s)-[:TRIPLE {type: $predicate, source_chunk: $chunk_id}]->(o)
```

A concept mentioned in 100 chunks produces one node with 100 connections. This accumulation of connections is what makes PPR retrieval increasingly powerful as the knowledge base grows.

### Passage Node Creation

After triple extraction, a passage node is created for each chunk and linked to every phrase node it contains:

```cypher
MERGE (p:PassageNode {chunk_id: $chunk_id, content: $content, source_id: $source_id})
WITH p
MATCH (phrase:PhraseNode) WHERE phrase.name IN $extracted_concepts
MERGE (p)-[:CONTEXT]->(phrase)
```

### Synonymy Edge Detection

After each batch of new phrase nodes is created, synonymy edges are computed by comparing the new nodes' embeddings against existing node vectors in pgvector:

```
for each new PhraseNode n:
    query pgvector for top-k most similar existing nodes
    for each result r where cosine_similarity(n, r) >= 0.8:
        MERGE (n)-[:SYNONYMY]-(r) in Neo4j
```

This connects `backprop` ↔ `backpropagation`, `ML` ↔ `machine learning`, and other lexical variants without requiring manual normalization. The threshold τ = 0.8 is the value used in the HippoRAG paper.

### Normalization

Before any node is created, the extracted text is normalized:

- Lowercase
- Strip leading/trailing whitespace
- Remove possessives ("gradient's" → "gradient")

Language is enforced as English via the prompt. This covers the most common deduplication failures. Near-duplicate detection (same concept, different surface form) is handled by synonymy edges — exact normalization handles obvious cases, embedding similarity handles the rest.

---

## Port Architecture

The extractor is isolated behind a port following the hexagonal architecture of the rest of Kairos:

```
OpenIEPort
      │
      └──→ GeminiOpenIEAdapter       (MVP — Gemini Flash, free tier)
      └──→ OllamaOpenIEAdapter       (future — local Llama 3.x 8B)
```

The port contract:

```java
public interface OpenIEPort {
    List<Triple> extract(String chunkText);
}

public record Triple(String subject, String predicate, String object) {}
```

---

## Consequences

**Positive**

- LLM flexibility extracts both explicit and implicit relationships across all domains
- Gemini Flash free tier supports MVP-scale usage with no cost
- Synonymy edges handle lexical variation without manual vocabulary maintenance
- Passage nodes in the graph make HippoRAG 2's passage-level PPR seeding possible
- Port isolation enables zero-cost migration to local inference when needed

**Negative**

- LLM extraction adds latency to ingestion (async pipeline mitigates this — see ADR-006)
- Occasional JSON malformation requires retry logic and graceful fallback
- LLM may hallucinate predicates not present in the text — prompt rules constrain this but do not eliminate it
- Synonymy edge computation scales as O(n) new nodes × ANN lookup — requires approximate nearest neighbor index in pgvector as graph grows

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|spaCy NER + dependency parsing|Too rigid for varied domains; misses implicit relationships|
|REBEL (end-to-end OpenIE model)|Produces 2x fewer triples than LLM per HippoRAG ablation studies; worse retrieval performance|
|Stanford OpenIE|Java-native but produces noisy, schema-free triples without semantic predicate typing|
|Manual tagging by user|Contradicts the core product premise — zero user overhead for structure|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[LLM]]
