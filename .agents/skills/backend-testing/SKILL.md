---
name: backend-testing
description: Use when implementing API, ingestion, persistence, user isolation, graph projection or retrieval behavior requiring reliable tests.
---
# Backend testing

1. Derive tests from issue acceptance criteria, invariant and counterexample.
2. Prefer focused JUnit/Mockito tests for pure behavior, Spring web/security tests for HTTP boundaries, and Testcontainers for PostgreSQL/pgvector, Neo4j or integration assertions.
3. Verify migrations, transaction rollback, user isolation, incomplete enrichment, retry/resume and derived-graph disagreement where changed behavior touches them.
4. Avoid passing suites that silently skipped Docker-backed tests; preserve scripts/ai/validate.sh as the canonical full Maven validation.
5. Keep retrieval quality evaluation separate from unit-test assertions. The normal validation gate is not evidence of the complete 60-query benchmark.
6. Check deterministic fixtures, expected rank ordering, sample size and source revisions before comparing metrics.
7. Run focused tests then bash scripts/ai/validate.sh. Record actual failures, skips, environment and head SHA; never weaken JaCoCo or quality gates to pass.
