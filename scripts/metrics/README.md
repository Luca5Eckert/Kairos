# Evidence collection for Kairos backend metrics

This directory contains **opt-in** measurements, not hardcoded CV results.

Run on the intended checkout, record the exact SHA, and retain reports
together with the GitHub Actions execution, environment and source code.

M11 — Full retrieval evaluation (already implemented):

    GITHUB_SHA="$(git rev-parse HEAD)" bash scripts/ai/evaluate-retrieval.sh

Real PostgreSQL/pgvector, Neo4j GDS and production ONNX embeddings.
The same 60-query run measures nDCG@10, Recall@10 and p50/p95/p99 for
both retrieval modes. No live Gemini is included. Reference artifacts
are under target/evaluation/retrieval and are retained in CI for 30 days.

M12 — Opt-in isolated JVM benchmark (prepare the pinned model first):

    bash infra/scripts/prepare-model.sh
    ./mvnw -Dtest=EmbeddingPerformanceBenchmarkIT \
      -Dmetrics.commitSha="$(git rev-parse HEAD)" test

This measures tokenizer + ONNX inference + pooling + normalization on
synthetic input, after warm-up. Input size is approximate words, not
tokenizer tokens. No DB, HTTP request or external API is measured. Do
not call the result end-to-end ingestion throughput or cold-start latency.

M13 — Deterministic extraction payload validation:

    ./mvnw -Dtest=StructuredExtractionContractBenchmarkIT \
      -Dmetrics.commitSha="$(git rev-parse HEAD)" test

This uses a **test-local validator** with malformed and valid fixtures.
It does **not** test the live Gemini adapter, its retry policy, actual
factual correctness or token cost. A live provider experiment would
require separately authorized test credentials, budget and gold labels.

M16 — Terraform static checks only, without chargeable resources:

    bash scripts/metrics/validate-terraform.sh

Checks fmt, init with remote backend disabled, and validate. This is
not an AWS apply, deploy, readiness test or measured provisioning time.

Collect summaries for these *already executed* experiments:

    python3 scripts/metrics/collect_evidence.py --sha "$(git rev-parse HEAD)"

Results are in target/metrics/evidence.json. Missing outputs and SHA
mismatches remain explicitly unverified. Do not promote these records
to the resume without inspecting original artifacts and limitations.

M17 (Orca/AI): maintain a manually reviewed PR/commit audit with evidence
of human review, backend scope and tests. Do not infer causal speedup from
commit counts, raw number of PRs, or agent-generated text.
