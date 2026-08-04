# Issue workflow for AI agents

This document is the detailed workflow routed by `AGENTS.md`. It is intentionally separate from the entry point so agents load the long instructions only when they start issue work.

## Sequence

```text
Read the issue
    |
Create or switch to a feature branch
    |
Preflight and baseline
    |
Investigation
    |
Plan
    |
Tests
    |
Incremental implementation
    |
Atomic commits
    |
Complete validation
    |
Self-review
    |
Delivery
```

## 1. Read the issue

Confirm the repository, issue number, acceptance criteria, constraints, and out-of-scope items. Record the issue URL and a short interpretation in the local run directory. Do not infer additional product scope from nearby issues.

## 2. Branch setup

Resolve the current checkout and working tree before editing. Issue work must not be performed directly on `main` or `master`.

Create or switch to a dedicated branch before preflight:

```bash
git switch -c feature/<issue-number>-<short-slug>
```

Use an existing issue branch when one is already present. If the working tree contains changes, preserve them and identify which files are unrelated; never stage or commit those files. If the existing changes belong to the issue, the new branch may carry them so they can be committed atomically.

## 3. Preflight and baseline

Start a run from the repository root:

```bash
bash scripts/ai/preflight.sh --repository Luca5Eckert/Kairos --issue 123
```

The script stores temporary evidence in `.ai-runs/`. It uses the GitHub CLI when available and falls back to the public GitHub API. A clean tree is recommended; use `--require-clean` when a clean checkout is a prerequisite.

Inspect the current branch, working tree, recent commits, build configuration, CI, and relevant tests before editing. Treat existing uncommitted files as user-owned unless the issue explicitly includes them. The preflight records this context and runs the current validation baseline unless `--skip-baseline` is supplied.

The baseline is evidence, not permission to delete or reset local changes. If it fails, capture the failure and distinguish a pre-existing failure from a regression.

## 4. Investigation

Trace the affected behavior through the smallest relevant set of modules, configuration, migrations, tests, and documentation. Prefer repository code, Git history, and existing CI configuration over assumptions. Identify integration boundaries and compatibility constraints before choosing an implementation.

## 5. Plan

Write a short, concrete plan before modifying code. It should identify:

- the files or components to change;
- the behavior and acceptance criterion each change addresses;
- the focused test strategy;
- validation commands and likely risks;
- the intended atomic commit boundaries.

Keep the plan in the task conversation or in the local `.ai-runs/` evidence; do not add a planning document to the repository unless the issue requires one.

## 6. Tests

Add or adjust a focused test before or alongside the implementation when behavior changes. Follow the existing JUnit and Mockito/Testcontainers conventions. For documentation and workflow changes, use `scripts/ai/test.sh` as the structural smoke test and run the affected scripts with `bash -n` where appropriate.

Tests must verify observable behavior, not merely implementation details. Keep integration tests explicit about their Docker requirement and do not make local development depend on unavailable services unless the existing project convention already does so.

## 7. Incremental implementation

Make the smallest coherent change that satisfies one part of the plan. After each meaningful increment, run the narrowest relevant test and inspect the diff. Avoid unrelated formatting, dependency upgrades, generated files, and drive-by refactors.

## 8. Atomic commits (required)

Commit issue work throughout implementation; do not wait until the end for one large commit. Each commit must be independently understandable and limited to one intent:

- implementation plus its focused tests when they form one behavior;
- documentation updates in a separate commit when independently reviewable;
- CI or tooling changes in their own commit.

Use explicit staging paths and verify `git diff --cached` before every commit. Never stage unrelated user-owned files or use broad staging when the tree contains unrelated changes. Follow the repository's existing conventional format, include `#<issue-number>` when appropriate, and use imperative summaries. Run the focused test for the commit before creating it.

Before delivery, confirm that all issue files are committed, the branch is correct, and only explicitly identified unrelated files remain uncommitted. Pushing the branch requires user authorization.

## 9. Complete validation

Run the canonical validation from the repository root:

```bash
bash scripts/ai/validate.sh
```

This prepares the pinned model and runs `./mvnw --batch-mode --no-transfer-progress clean verify`, including tests, packaging, and JaCoCo enforcement. The script also fails when Testcontainers reports Docker-backed tests as skipped, so a successful complete validation means those integration tests actually ran. The `validation.yml` workflow reuses this script. Container smoke tests and vulnerability scanning remain in the CI container job because they require Docker and GitHub Actions services.

Run focused tests during development, then run the complete validation before delivery. Report skipped tests and environmental limitations explicitly.

## 10. Self-review

Review the final diff and status for scope, security, error handling, backward compatibility, tests, documentation, and accidental secrets or generated artifacts. Confirm `.ai-runs/` and local Vault configuration are ignored. Re-run the structural smoke test if workflow files changed.

## 11. Delivery

The final report must be independently verifiable and contain:

1. issue URL and concise outcome;
2. branch name;
3. implementation and test files changed;
4. focused and complete validation commands with results;
5. documentation and Obsidian decision;
6. known risks, skips, or pre-existing failures;
7. commit hashes, with one-line intent for each commit;
8. unrelated user-owned files left uncommitted, if any.

## Context boundaries

Load this workflow at issue start. Load `documentation-policy.md` only after implementation and validation planning. Keep run logs, issue snapshots, and baselines in `.ai-runs/`; never commit personal paths, tokens, or private Vault content.
