---
name: hybrid-retrieval-evaluation
description: Design reproducible hybrid vector, graph and lexical retrieval experiments before changing ranking or ingestion.
---

# hybrid-retrieval-evaluation

## Trigger
Kairos retrieval pipeline, vector search, Neo4j graph traversals/PPR, RRF, reranking, ingestion or benchmark tuning.

## Procedure
1. Read docs/adr/ and actual retrieval code; identify the question/query set, data snapshot, graph/vector freshness and ground-truth relevance judgments.
2. Establish individual baselines (lexical, vector/HNSW, graph/PPR) plus hybrid and candidate changes. Hold datasets, model, ANN parameters and hardware fixed when comparing.
3. Choose appropriate metrics: Recall@k, Precision@k, MRR, nDCG@k, latency p50/p95, index size, memory and ingestion/update lag. Explain trade-offs and relevance judgment coverage.
4. Validate exact document/seed identity and joins, deduplication, missing vectors or triples, PPR convergence, RRF score comparability and cold-start paths.
5. Avoid leakage between evaluation set and tuning; report sample counts, uncertainty, query cohorts and regressions instead of one aggregate number.
6. Use the repository's normal quality gate by default. The 60-query benchmark is opt-in; run it explicitly when measuring retrieval quality.
7. Record commands, data version, configuration, commit SHA and measured result. Keep unrun or historical metrics clearly labeled.
8. Do not change ontology, embeddings model, schema or ranking weights without a hypothesis, compatibility assessment and focused tests.

## Output
Experiment hypothesis -> baseline -> comparable result -> quality/latency trade-off -> decision.

## Reference
Original Kairos specialization based on current repository architecture and retrieval-evaluation discipline.
