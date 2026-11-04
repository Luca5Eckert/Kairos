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


## Measured accomplishment analysis — issue #126

Use `scripts/metrics/analyze_outcomes.py` **after actual benchmark execution** to
calculate paired per-query nDCG@10 differences, wins/ties/losses, descriptive
scenario-cluster bootstrap intervals and same-run vector/graph p95.
If a matching-SHA `embedding-performance.json` exists, include throughput
and p95 for each ONNX input size. In its absence, mark ONNX `MISSING`.

```sh
python3 -m unittest discover -s tests -p 'test_analyze_outcomes.py' -v
python3 scripts/metrics/analyze_outcomes.py --sha "$(git rev-parse HEAD)"
```

Outputs are `target/metrics/achievement-outcomes.{json,md}`. The dedicated
`retrieval-evaluation.yml` reruns the 60-query real PostgreSQL/pgvector +
Neo4j GDS suite and the isolated ONNX measurement only on a scoped metrics
harness PR, scheduled evaluation or manual invocation.

The dataset contains **10 multi-hop scenarios with three query paraphrases
each**, so bootstrap resampling uses **scenario clusters**, not 30
independent queries. The resulting interval describes this synthetic fixture:
it is not an out-of-sample population confidence claim.

Original dated reference: [GitHub Actions 41245447107](https://github.com/Luca5Eckert/Kairos/actions/runs/41245447107),
`1595d0d5954a38171822f95b3cdb4eb56b3ca53c`:
- Graph retrieval improves **29/30 multi-hop queries**, ties one,
  worsens none; the mean *paired absolute* nDCG@10 gain is **0.3096**
  (cluster bootstrap 95% interval approximately **0.2462–0.3616**).
  This is distinct from **+47.6% relative lift**.
- No single-hop queries improved (30 ties). Same-run p95 vector
  **21.87 ms** vs graph **511.23 ms**; not a latency improvement.
- Local warm ONNX tokenizer+inference measured **128.86 embeddings/s**
  for approximately 32-word synthetic inputs; **49.00** for 128 words,
  **47.10** for 256 words. This is *not* documents/s or end-to-end
  ingestion throughput.

Higher-value future work would benchmark actual ingest end-to-end
(chunks/documents/s, memory and cold-start) or use a held-out query
corpus to validate quality generalization. Never invent those figures.
