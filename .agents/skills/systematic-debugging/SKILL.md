---
name: systematic-debugging
description: Use for failed CI, integration defects, ingestion errors, retrieval ranking regressions and JVM or database performance anomalies.
---
# Systematic debugging

1. Capture the exact failing command, expected vs actual behavior, commit, environment, logs and dataset.
2. Reproduce the smallest scenario and locate the boundary at fault (API, relational persistence, enrichment, vector rank, graph rank, or data hydration).
3. Write competing falsifiable hypotheses. Test the least intrusive explanation before changing design.
4. Inspect database state and projection lag separately; do not attribute graph/index inconsistency to ranking without proof.
5. Add a regression fixture or experiment that fails before the fix and passes after it.
6. Run focused checks and canonical validation; report the root cause, evidence and unresolved uncertainty.
7. Never claim the failure is fixed if the relevant Docker-backed test was skipped or the benchmark was not rerun.
