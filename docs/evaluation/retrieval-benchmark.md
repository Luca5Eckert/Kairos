# Retrieval evaluation

This document defines the reproducible benchmark introduced by issue #113.

## Question being measured

The benchmark tests whether Kairos graph-augmented retrieval improves passage retrieval over a vector-only baseline, especially when the relevant evidence is split across connected passages.

It is intentionally an **offline retrieval evaluation**, not an answer-generation benchmark and not a production SLA.

## Dataset

The versioned fixture lives at:

`src/test/resources/evaluation/retrieval-v1.json`

Dataset v1 contains:

- 60 natural-language queries;
- 30 single-hop queries;
- 30 multi-hop queries;
- explicit graded passage judgments;
- 30 semantically related distractor passages.

Multi-hop scenarios contain an anchor passage and a second passage connected through an intermediate concept. Single-hop scenarios contain a directly relevant passage. Judgments are stored with the dataset rather than inferred from the retrieved output.

## Compared modes

### Vector-only

The baseline uses:

1. the production `all-MiniLM-L6-v2` ONNX embedding provider;
2. the production PostgreSQL/pgvector semantic search;
3. top-10 passage candidates.

It does not use triple recall, recognition memory, Neo4j or PPR.

### Graph-augmented

The graph path executes the production `SearchSourceUseCase` with:

1. the production ONNX embedding provider;
2. real pgvector passage and triple retrieval;
3. deterministic recognition-memory seed selection;
4. real Neo4j 5.26 Graph Data Science Personalized PageRank;
5. production chunk hydration/ranking.

Recognition-memory selection is deterministic for the reference benchmark because the production implementation calls Gemini. A concept is selected only when it is present in the actual dense triple-candidate set. This removes external API variance while preserving the retrieval boundary that feeds graph expansion.

The benchmark therefore measures the deterministic retrieval core. It does **not** measure live Gemini recognition quality or latency.

## Metrics

Quality is macro-averaged across queries and reported globally and by query type:

- Recall@5;
- Recall@10;
- MRR@10;
- nDCG@10.

Latency is recorded as p50/p95/p99 for both query paths. The graph path also records stage timings for:

- embedding;
- dense passage search;
- dense triple search;
- deterministic recognition;
- Neo4j GDS/PPR;
- hydration.

No assertion requires the graph path to outperform the baseline. The benchmark records regressions as results rather than hiding them behind a passing threshold.

## Running

The canonical complete validation command is:

```bash
bash scripts/ai/validate.sh
```

The evaluation is a Docker-backed integration test and therefore requires Docker. The validation script treats skipped Docker-backed tests as a failure.

Reference artifacts are written to:

```text
target/evaluation/retrieval/run-metadata.json
target/evaluation/retrieval/raw-results.json
target/evaluation/retrieval/report.json
target/evaluation/retrieval/summary.md
```

The pull-request Validation workflow uploads this directory together with test and coverage reports.

## Reproducibility metadata

`run-metadata.json` records:

- issue and experiment id;
- commit SHA and Git ref when running in GitHub Actions;
- dataset version;
- timestamp;
- Java/OS/runtime information;
- available processors and JVM memory ceiling;
- warm-up count;
- repetitions;
- `k`;
- benchmark boundary.

## Interpretation

Numbers from this benchmark may be described as reproducible local/CI measurements for dataset v1. They must not be presented as production latency guarantees or as evidence about live Gemini behavior.

If the dataset, model, retrieval configuration or benchmark boundary changes, results should be treated as a new benchmark version rather than silently compared against v1.
