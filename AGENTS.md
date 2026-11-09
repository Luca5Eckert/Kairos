# Kairos agent entry point

Before working on an issue, read [`docs/ai/issue-workflow.md`](docs/ai/issue-workflow.md). Follow its sequence and keep the change scoped to the issue.

## Branch and commit discipline

Issue work must never be performed directly on `main` or `master`. Before editing, inspect the checkout and create or switch to a dedicated branch named `feature/<issue-number>-<short-slug>` (for example, `feature/94-history-api`). Create the branch before running the issue preflight so the evidence records the correct branch.

Commits are required for issue work. Make small, atomic commits after each coherent increment. Group implementation and its focused tests when they form one behavior; keep documentation, CI, and tooling changes in separate commits when they are independently reviewable. Stage explicit paths, never unrelated user files, and follow the repository's existing conventional commit format with the issue reference when appropriate. Do not leave issue changes uncommitted at delivery. Push only when the user authorizes publishing.

After implementation, read [`docs/ai/documentation-policy.md`](docs/ai/documentation-policy.md) before deciding whether versioned documentation, an ADR, or an Obsidian note is needed.

Use `scripts/ai/preflight.sh` to capture the issue, checkout state, commit conventions, and baseline. Use `scripts/ai/validate.sh` as the source of truth for the complete Maven validation. Keep `.ai-runs/` local, preserve unrelated working-tree changes, and add or update focused tests with implementation changes.

The final report must include the issue, branch, files changed, commit hashes, tests and validation commands with results, documentation decision, and known risks. Explicitly report any unrelated user-owned files left uncommitted.

## Backend Agent Skills (opt-in; existing workflow remains authoritative)

Use only the matching skill for the task:
- Spring/Java: .agents/skills/spring-boot-engineering/SKILL.md
- JPA, graph/vector persistence and migrations: .agents/skills/jpa-persistence/SKILL.md (apply JPA guidance only to JPA portions)
- JUnit, Mockito and PostgreSQL/Testcontainers: .agents/skills/backend-testing/SKILL.md
- Security and data access: .agents/skills/backend-security/SKILL.md
- OTel and benchmarks: .agents/skills/backend-observability/SKILL.md
- Module constraints: .agents/skills/architecture-fitness/SKILL.md
- Vector + graph ranking evaluation: .agents/skills/hybrid-retrieval-evaluation/SKILL.md
- Reproduction/root-cause debugging: .agents/skills/systematic-debugging/SKILL.md
- Final diff review: .agents/skills/backend-code-review/SKILL.md
- CI and workflow risks: .agents/skills/ci-hardening/SKILL.md
- JVM diagnostics: .agents/skills/jvm-performance/SKILL.md
- Before reporting completion: .agents/skills/backend-delivery-verification/SKILL.md

Precedence: the existing docs/ai/issue-workflow.md, branch/commit rules, scripts/ai/preflight.sh, scripts/ai/validate.sh and documentation-policy.md remain mandatory. Skills supplement—not replace—the canonical validation. Do not imply the opt-in 60-query benchmark ran unless it was explicitly executed. Never report production behavior based solely on CI.
