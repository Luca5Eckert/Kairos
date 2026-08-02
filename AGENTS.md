# Kairos agent entry point

Before working on an issue, read [`docs/ai/issue-workflow.md`](docs/ai/issue-workflow.md). Follow its sequence and keep the change scoped to the issue.

After implementation, read [`docs/ai/documentation-policy.md`](docs/ai/documentation-policy.md) before deciding whether versioned documentation, an ADR, or an Obsidian note is needed.

Use `scripts/ai/preflight.sh` to capture the issue, checkout state, commit conventions, and baseline. Use `scripts/ai/validate.sh` as the source of truth for the complete Maven validation. Keep `.ai-runs/` local, preserve unrelated working-tree changes, and add or update focused tests with implementation changes.

Commits should be small, atomic, and follow the repository's existing conventional format. The final report must include the issue, files changed, tests and validation commands with results, documentation decision, known risks, and commit references.
