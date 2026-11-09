---
name: retrieval-evaluation
description: Use when changing hybrid retrieval, graph propagation, pgvector candidate selection, PPR, RRF, ranking or embedding configuration.
---
# Retrieval evaluation

1. State the hypothesis and compare against dense-only and relevant current hybrid baselines; an additional graph stage must justify its complexity.
2. Pin dataset/query fixture, relevant model revision, HNSW/index configuration, graph projection, ranking weights and environment.
3. Inspect seed selection, candidate recall, graph expansion/PPR, fusion/RRF, user scoping and final chunk hydration separately before tuning.
4. Report nDCG@10, MRR or Recall@k when actually measured; distinguish multi-hop from overall quality and show p50/p95 latency and workload.
5. Avoid conflating ranking gains with recall gains, or old measurements with current experiment results. Name missing inputs explicitly.
6. For an affected change, run the normal quality gate then the full benchmark using bash scripts/ai/evaluate-retrieval.sh when its Docker/model prerequisites are available.
7. Validate expected artifacts under target/evaluation/retrieval/ and the 60-query harness; do not infer success from a prior run or a skipped suite.
8. Record baseline vs candidate, regression thresholds, statistical limitations, SHA and decisions; use a durable ADR only for a meaningful architectural change.
