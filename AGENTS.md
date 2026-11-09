# Kairos agent entry point

Before working on an issue, read [`docs/ai/issue-workflow.md`](docs/ai/issue-workflow.md). Follow its sequence and keep the change scoped to the issue.

## Branch and commit discipline

Issue work must never be performed directly on `main` or `master`. Before editing, inspect the checkout and create or switch to a dedicated branch named `feature/<issue-number>-<short-slug>` (for example, `feature/94-history-api`). Create the branch before running the issue preflight so the evidence records the correct branch.

Commits are required for issue work. Make small, atomic commits after each coherent increment. Group implementation and its focused tests when they form one behavior; keep documentation, CI, and tooling changes in separate commits when they are independently reviewable. Stage explicit paths, never unrelated user files, and follow the repository's existing conventional commit format with the issue reference when appropriate. Do not leave issue changes uncommitted at delivery. Push only when the user authorizes publishing.

After implementation, read [`docs/ai/documentation-policy.md`](docs/ai/documentation-policy.md) before deciding whether versioned documentation, an ADR, or an Obsidian note is needed.

Use `scripts/ai/preflight.sh` to capture the issue, checkout state, commit conventions, and baseline. Use `scripts/ai/validate.sh` as the source of truth for the complete Maven validation. Keep `.ai-runs/` local, preserve unrelated working-tree changes, and add or update focused tests with implementation changes.

The final report must include the issue, branch, files changed, commit hashes, tests and validation commands with results, documentation decision, and known risks. Explicitly report any unrelated user-owned files left uncommitted.

## Backend agent skills

Load only the relevant skill for the task, in addition to (never instead of) the issue workflow above:

- Java/Spring, persistence and boundaries: .agents/skills/spring-backend-engineering/SKILL.md
- Unit, integration and contract testing: .agents/skills/backend-testing/SKILL.md
- Failed checks or behavior regressions: .agents/skills/systematic-debugging/SKILL.md
- JWT, user ownership or cross-tenant access: .agents/skills/security-authorization-review/SKILL.md
- Vector/graph ranking and experiments: .agents/skills/retrieval-evaluation/SKILL.md
- Architecture rules and dual-store ownership: .agents/skills/architecture-fitness-review/SKILL.md
- Before PR delivery / merge readiness: .agents/skills/backend-change-verification/SKILL.md

These skills add task-specific checks; they do not override existing scripts/ai validation, the issue contract, CI quality gates, documentation policy or security boundaries. Never report a skipped Docker-backed test or old benchmark as current validation.

