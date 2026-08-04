# Kairos agent entry point

Before working on an issue, read [`docs/ai/issue-workflow.md`](docs/ai/issue-workflow.md). Follow its sequence and keep the change scoped to the issue.

## Branch and commit discipline

Issue work must never be performed directly on `main` or `master`. Before editing, inspect the checkout and create or switch to a dedicated branch named `feature/<issue-number>-<short-slug>` (for example, `feature/94-history-api`). Create the branch before running the issue preflight so the evidence records the correct branch.

Commits are required for issue work. Make small, atomic commits after each coherent increment. Group implementation and its focused tests when they form one behavior; keep documentation, CI, and tooling changes in separate commits when they are independently reviewable. Stage explicit paths, never unrelated user files, and follow the repository's existing conventional commit format with the issue reference when appropriate. Do not leave issue changes uncommitted at delivery. Push only when the user authorizes publishing.

After implementation, read [`docs/ai/documentation-policy.md`](docs/ai/documentation-policy.md) before deciding whether versioned documentation, an ADR, or an Obsidian note is needed.

Use `scripts/ai/preflight.sh` to capture the issue, checkout state, commit conventions, and baseline. Use `scripts/ai/validate.sh` as the source of truth for the complete Maven validation. Keep `.ai-runs/` local, preserve unrelated working-tree changes, and add or update focused tests with implementation changes.

The final report must include the issue, branch, files changed, commit hashes, tests and validation commands with results, documentation decision, and known risks. Explicitly report any unrelated user-owned files left uncommitted.
