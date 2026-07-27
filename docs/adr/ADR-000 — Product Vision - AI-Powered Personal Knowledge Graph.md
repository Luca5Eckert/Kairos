- **Status:** Accepted
- **Date:** 2026-04-05
- **Context:** Kairos — Personal Knowledge Engine

---

## Context

Existing personal knowledge management tools like Obsidian require users to manually create connections between notes. This demands continuous discipline — users must identify relationships, create links, and maintain structure themselves. Most connections are never made because the cognitive overhead is too high, and the user's understanding of their own knowledge structure is inherently limited.

The deeper problem: the value of a knowledge base is not in storing information, but in surfacing the relationships between pieces of information that the user didn't consciously notice. A system that only stores what you explicitly organized returns exactly what you put in — nothing more.

---

## Decision

Kairos is a **personal knowledge graph engine** where the user ingests content freely and the AI automatically builds, maintains, and exposes the conceptual structure behind that content.

### Core Metaphor

Kairos is Obsidian where the graph builds itself.

The user's only job is to feed the system. Kairos's job is to understand that content, extract its concepts and relationships, persist them in a growing knowledge graph, and make that structure explorable and queryable in ways that surface knowledge the user didn't know they had.

### Primary Views

**Source View** A list of all ingested sources. Each source shows its extracted concepts and links to related sources via shared concept nodes. This is the entry point for browsing raw content.

**Graph View** A visual, interactive graph of all concepts extracted from the entire knowledge base. Nodes are concepts. Edges are typed relationships (USES, EXTENDS, CONTRADICTS, etc.). Node size reflects centrality — concepts referenced across many sources appear larger, giving an immediate visual sense of what is most central in the user's knowledge.

Clicking a concept node:

- Shows all sources that reference that concept
- Shows the concept's direct neighbors in the graph
- Optionally generates an AI summary of what the knowledge base says about that concept, grounded exclusively in ingested sources

**Semantic Search** The user types a free-form natural language query. The system returns:

- Ranked relevant source chunks from the knowledge base
- The concepts most activated by the query, with links into the graph
- A concise AI-generated summary synthesizing what the knowledge base knows about the query — grounded only in ingested content, never in external world knowledge

### What Kairos Is Not

- Not a chat interface — there is no conversational loop
- Not a search engine over raw text — retrieval is grounded in the knowledge graph
- Not a note editor — the user writes elsewhere and ingests into Kairos
- Not an LLM wrapper — the LLM is used for extraction and summarization, not as the primary interface

### Why This Is Different

The differentiation is not in the storage or the search — it is in the **automatic construction of semantic structure** over time. Every new source ingested makes the graph richer. Concepts gain more connections. The system's ability to surface relevant knowledge improves passively as the knowledge base grows. The user never has to think about structure.

---

## Consequences

**Positive**

- Zero friction ingestion — no tagging, linking, or categorization required
- The graph grows richer automatically with every ingested source
- Semantic search is grounded in the user's actual accumulated knowledge
- The graph view surfaces connections the user never consciously made
- The system gets better over time without any extra user effort

**Negative**

- The entire product depends on the quality of concept extraction — poor extraction produces a misleading or noisy graph
- Summarization quality depends on retrieval quality — bad retrieval produces summaries that misrepresent the knowledge base
- A sparse knowledge base (few sources ingested) produces a sparse, low-value graph — the product has a cold-start problem that requires user commitment to overcome

## Related
- [[Kairos]]
- [[Definição de projeto]]

## Related
- [[Kairos]]
- [[Definição de projeto]]
