# ADR-008 — Graph View and Summary Generation

- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

The Graph View is Kairos's primary differentiating feature — the visual representation of the user's accumulated knowledge as an interactive concept graph. It must expose the knowledge graph stored in Neo4j in a way that is both visually meaningful and actionable.

Two specific behaviors require architectural decisions:

1. **Graph visualization** — how the concept graph is rendered, what data drives it, and how it scales as the knowledge base grows
2. **Concept summary** — when the user clicks a concept node and requests a summary, how the system generates a grounded synthesis of what the knowledge base knows about that concept

Both features must be grounded exclusively in the user's ingested knowledge — not in the LLM's pretrained world knowledge.

---

## Decision

### Graph Visualization

The Graph View is rendered client-side using **D3.js force-directed graph** layout, fed by a dedicated Neo4j traversal API endpoint.

**Data served by the API:**

```
GET /graph

Response:
{
  "nodes": [
    {
      "id": "backpropagation",
      "label": "backpropagation",
      "degree": 23,           ← number of direct connections
      "source_count": 7,      ← number of sources that mention this concept
      "centrality": 0.87      ← normalized degree centrality [0, 1]
    }
  ],
  "edges": [
    {
      "source": "backpropagation",
      "target": "chain rule",
      "type": "USES",
      "weight": 4             ← number of sources that contain this triple
    }
  ]
}
```

**Visual encoding:**

- Node size → `centrality` — larger nodes are more central in the knowledge base
- Node color → cluster membership (future: community detection via Neo4j Graph Data Science)
- Edge thickness → `weight` — thicker edges represent relationships confirmed by more sources
- Edge label → `type` — the typed predicate (USES, EXTENDS, COMPUTES, etc.)

**Scalability:** For large knowledge bases (1,000+ nodes), the full graph is too dense to render meaningfully. The API supports:

```
GET /graph?min_degree=3          ← only nodes with 3+ connections
GET /graph?concept=backpropagation&depth=2   ← ego graph, 2 hops
```

The concept ego graph (centered on a clicked node, N hops deep) is the primary interaction mode for the Graph View.

### Concept Click — Related Sources

When the user clicks a concept node, the system returns all sources associated with that concept, ordered by relevance:

```
GET /concepts/{name}/sources

Pipeline:
1. Neo4j: find all PassageNodes connected to this PhraseNode via CONTEXT edges
2. pgvector: embed concept name → cosine search over chunk vectors
3. RRF fusion of both result sets
4. Return top-k sources with chunk previews
```

This uses the same HippoRAG 2 retrieval infrastructure as Semantic Search — the concept name is treated as a query.

### Concept Summary Generation

When the user requests a summary for a concept, the system:

1. Runs the concept click retrieval (above) to get the top-k most relevant chunks
2. Passes those chunks to Gemini Flash with a strict summarization prompt

**Summarization prompt:**

```
You are a knowledge synthesis engine. The user has accumulated knowledge about
"{concept}" across multiple sources. Based exclusively on the excerpts below,
write a concise summary of what the user knows about this concept.

Rules:
- Use ONLY information present in the excerpts below
- Do NOT add external knowledge or context not present in the excerpts
- If the excerpts are insufficient to summarize the concept, say so explicitly
- Keep the summary under 200 words
- Write in clear, direct prose — no bullet points

Excerpts from the user's knowledge base:
{top_k_chunks}
```

The "use only what is in the excerpts" constraint is critical — it ensures the summary reflects the user's actual knowledge, not the LLM's world knowledge. A user who has only read one article about backpropagation should receive a summary of that article, not a comprehensive explanation of backpropagation from the LLM's training data.

### Semantic Search Summary Generation

The same summarization approach applies to the Semantic Search screen, where the query replaces the concept name:

```
You are a knowledge synthesis engine. Based exclusively on the excerpts below
from the user's knowledge base, write a concise answer to the following question:

Question: "{query}"

Rules:
- Use ONLY information present in the excerpts below
- Do NOT add external knowledge
- If the knowledge base does not contain sufficient information to answer,
  say explicitly: "Your knowledge base does not contain enough information about this."
- Keep the response under 200 words

Excerpts:
{top_k_chunks}
```

### Centrality Computation

Node centrality is computed periodically (not per-request) via a Neo4j background job that calculates **degree centrality** — the number of direct connections normalized by the maximum degree in the graph:

```cypher
MATCH (n:PhraseNode)
WITH n, size((n)--()) AS degree
WITH max(degree) AS maxDegree, collect({node: n, degree: degree}) AS nodes
UNWIND nodes AS item
SET item.node.centrality = toFloat(item.degree) / maxDegree
SET item.node.degree = item.degree
```

This runs after each ingestion batch completes, not after each chunk, to avoid excessive write amplification.

Post-MVP: replace degree centrality with **PageRank centrality** via Neo4j Graph Data Science library for a more nuanced importance signal.

---

## Consequences

**Positive**

- D3.js force-directed layout naturally groups related concepts visually without requiring manual positioning
- API-driven graph data allows the frontend to be fully stateless — the graph is always a live read from Neo4j
- Ego graph mode makes the visualization tractable for large knowledge bases
- Summary grounding constraint ensures the product never misleads the user with LLM hallucinations
- Centrality computed in batch — no per-request overhead for graph rendering

**Negative**

- Large knowledge bases (5,000+ nodes) may require server-side graph sampling to keep the full graph view performant — the D3.js renderer has practical node limits (~2,000 nodes at interactive frame rates)
- Summary quality degrades gracefully but visibly when the knowledge base has few sources about a concept — the prompt handles this explicitly
- Centrality recomputation after every ingestion adds a Neo4j write burst — must be debounced for rapid consecutive ingestions

---

## Alternatives Considered

|Option|Reason Rejected|
|---|---|
|Server-side graph rendering (e.g., Graphviz)|Static image; not interactive; cannot support node click events|
|Cytoscape.js|Capable but heavier than D3.js; D3 is already in the planned stack|
|Summary without retrieval grounding|LLM generates from world knowledge — the summary would not reflect the user's actual knowledge base|
|Streaming summary generation|Adds frontend complexity for marginal UX gain at MVP stage|
|Pre-computed summaries per concept|Summaries go stale as more sources are ingested; real-time generation is more accurate|

## Related
- [[Kairos]]
- [[Definição de projeto]]
- [[Sistema de busca]]
- [[LLM]]
