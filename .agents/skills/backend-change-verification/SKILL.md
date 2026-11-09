---
name: backend-change-verification
description: Use before delivery or merge readiness to reconcile Kairos issue acceptance, tests, CI and security or retrieval impacts.
---
# Backend change verification

1. Follow AGENTS.md and docs/ai/issue-workflow.md; do not replace their required branch, preflight, commit or documentation rules.
2. Build a requirement -> implementation -> test -> result matrix for the exact proposed diff.
3. Inspect user-isolation, input validation, schema compatibility, transaction boundaries, partial ingestion, failed async enrichment and recovery.
4. Run focused tests then bash scripts/ai/validate.sh with Docker available where required. For ranking changes use retrieval-evaluation and its versioned baseline.
5. Check what actually ran. Distinguish complete 60-query retrieval results from the 12-query PR quality gate and from old published measurements.
6. Inspect the final diff for unrelated changes, secrets, weakened thresholds, accidental generated files and regression risks.
7. Check CI for the exact head SHA. Report pass/fail/skip/unverified per criterion; a merged branch is not a deployed system.
8. Document according to docs/ai/documentation-policy.md; do not automatically merge.
