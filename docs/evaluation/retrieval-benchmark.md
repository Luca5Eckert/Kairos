# Retrieval evaluation

This document defines the reproducible retrieval evaluation introduced by issue #113 and how it is enforced continuously in CI/CD.

## Question being measured

The evaluation tests whether Kairos graph-augmented retrieval improves passage ranking over a vector-only baseline, especially when the relevant evidence is split across connected passages.

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

The evaluation therefore measures the deterministic retrieval core. It does **not** measure live Gemini recognition quality or latency.

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

The complete benchmark records regressions as results rather than requiring the graph path to win. The pull-request quality gate is intentionally different: it protects a small, stable subset against known ranking regressions.

## Continuous evaluation strategy

Kairos separates **merge-time regression protection** from **full benchmark measurement**.

### Pull-request quality gate

Normal `bash scripts/ai/validate.sh` / Maven `verify` executes `RetrievalQualityRegressionIntegrationTest`.

The gate uses 12 stable queries from dataset v1:

- 6 single-hop queries;
- 6 multi-hop queries;
- the same distractor corpus;
- production ONNX embeddings;
- real PostgreSQL/pgvector;
- real Neo4j GDS/PPR;
- deterministic recognition constrained to actual dense triple candidates.

It blocks a merge when:

- graph multi-hop nDCG@10 falls below vector-only nDCG@10 on the stable subset;
- graph multi-hop nDCG@10 falls below `0.90`;
- overall graph nDCG@10 falls below `0.95`.

Those thresholds are regression floors, not claims about production quality. If the evaluation dataset or ranking protocol changes materially, the floors must be reviewed as part of the same change rather than silently weakened.

**Latency is not a pull-request gate.** Shared GitHub-hosted runner timing varies enough that a strict p95/p99 threshold would create flaky builds. Performance remains visible in the complete benchmark and should be investigated through trend/regression analysis rather than an arbitrary absolute merge threshold.

### Full benchmark

The 60-query `RetrievalEvaluationIntegrationTest` is excluded from normal Maven verification and is re-enabled only by the `retrieval-evaluation` Maven profile.

Canonical command:

```bash
bash scripts/ai/evaluate-retrieval.sh
```

The dedicated GitHub Actions workflow `.github/workflows/retrieval-evaluation.yml` runs:

- manually through `workflow_dispatch`;
- every Monday at `09:00 UTC` through `schedule`.

The scheduled/manual run measures the full quality table plus p50/p95/p99 and stage-level latency. It uploads the generated artifacts for 30 days.

This split keeps pull requests deterministic and reasonably fast while preserving periodic end-to-end evidence from the complete benchmark.

## Reference execution — dataset v1.0.0

The first reference run used 60 queries, a 5-query warm-up and 2 measured repetitions per query.

| Segment | Mode | Recall@5 | Recall@10 | MRR@10 | nDCG@10 |
| --- | --- | ---: | ---: | ---: | ---: |
| All | Vector-only | 0.9917 | 1.0000 | 1.0000 | 0.8249 |
| All | Graph-augmented | 1.0000 | 1.0000 | 1.0000 | **0.9797** |
| Single-hop | Vector-only | 1.0000 | 1.0000 | 1.0000 | 1.0000 |
| Single-hop | Graph-augmented | 1.0000 | 1.0000 | 1.0000 | 1.0000 |
| Multi-hop | Vector-only | 0.9833 | 1.0000 | 1.0000 | 0.6498 |
| Multi-hop | Graph-augmented | 1.0000 | 1.0000 | 1.0000 | **0.9593** |

Observed ranking lift:

- overall nDCG@10: **+18.8%**;
- multi-hop nDCG@10: **+47.6%**;
- Recall@10: no lift because both modes saturated at `1.0000`.

The correct interpretation is therefore a **ranking-quality improvement**, not a Recall@10 improvement.

Reference query-path latency:

| Mode | p50 | p95 | p99 |
| --- | ---: | ---: | ---: |
| Vector-only | 11.12 ms | 19.03 ms | 24.32 ms |
| Graph-augmented | 221.16 ms | 418.36 ms | 568.27 ms |

Neo4j GDS/PPR accounted for `400.80 ms` p95 in the graph-stage breakdown, making projection/PageRank the primary performance optimization target observed by the reference run.

## Running locally

### Merge-time validation

```bash
bash scripts/ai/validate.sh
```

This executes the lightweight retrieval quality gate together with the normal unit/integration suite, packaging and coverage enforcement.

### Complete retrieval evaluation

```bash
bash scripts/ai/evaluate-retrieval.sh
```

Both paths require Docker. The scripts treat skipped Docker-backed evaluation as a failure.

The full evaluation writes:

```text
target/evaluation/retrieval/run-metadata.json
target/evaluation/retrieval/raw-results.json
target/evaluation/retrieval/report.json
target/evaluation/retrieval/summary.md
```

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

## Security gate note

The pull request that introduced this evaluation exposed a pre-existing container scan failure in `tomcat-embed-core 11.0.22`. The container workflow was not bypassed. Kairos overrides Spring Boot 4.0.7's managed Tomcat version to `11.0.25`, the Apache Tomcat release that contains the relevant security fixes, and keeps the Trivy critical-vulnerability gate enabled.

Dependency/security upgrades are independent of retrieval-quality thresholds: a green evaluation must never be used to suppress a failing security scan.

## Interpretation and guardrails

Numbers from this benchmark may be described as reproducible local/CI measurements for dataset v1. They must not be presented as production latency guarantees or as evidence about live Gemini behavior.

Do not claim:

- Recall@10 improvement from the v1 reference run;
- production SLA from GitHub-hosted runner latency;
- live Gemini quality or latency from the deterministic recognition boundary;
- online user/business impact from this offline evaluation.

If the dataset, model, retrieval configuration or benchmark boundary changes, results should be treated as a new benchmark version rather than silently compared against v1.
