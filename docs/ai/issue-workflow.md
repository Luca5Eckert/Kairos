# Issue workflow for AI agents

This document is the detailed workflow routed by `AGENTS.md`. It is intentionally separate from the entry point so agents load the long instructions only when they start issue work.

## Sequence

```text
Read the issue
    ↓
Preflight and baseline
    ↓
Investigation
    ↓
Plan
    ↓
Tests
    ↓
Incremental implementation
    ↓
Atomic commits
    ↓
Complete validation
    ↓
Self-review
    ↓
Delivery
```

## 1. Read the issue

Confirm the repository, issue number, acceptance criteria, constraints, and out-of-scope items. Record the issue URL and a short interpretation in the local run directory. Do not infer additional product scope from nearby issues.

Start a run from the repository root:

```bash
bash scripts/ai/preflight.sh --repository Luca5Eckert/Kairos --issue 123
```

The script stores temporary evidence in `.ai-runs/`. It uses the GitHub CLI when available and falls back to the public GitHub API. A clean tree is recommended; use `--require-clean` when a clean checkout is a prerequisite.

## 2. Preflight and baseline

Inspect the current branch, working tree, recent commits, build configuration, CI, and relevant tests before editing. Treat existing uncommitted files as user-owned unless the issue explicitly includes them. The preflight records this context and runs the current validation baseline unless `--skip-baseline` is supplied.

The baseline is evidence, not permission to delete or reset local changes. If it fails, capture the failure and distinguish a pre-existing failure from a regression.

## 3. Investigation

Trace the affected behavior through the smallest relevant set of modules, configuration, migrations, tests, and documentation. Prefer repository code, Git history, and existing CI configuration over assumptions. Identify integration boundaries and compatibility constraints before choosing an implementation.

## 4. Plan

Write a short, concrete plan before modifying code. It should identify:

- the files or components to change;
- the behavior and acceptance criterion each change addresses;
- the focused test strategy;
- validation commands and likely risks.

Keep the plan in the task conversation or in the local `.ai-runs/` evidence; do not add a planning document to the repository unless the issue requires one.

## 5. Tests

Add or adjust a focused test before or alongside the implementation when behavior changes. Follow the existing JUnit and Mockito/Testcontainers conventions. For documentation and workflow changes, use `scripts/ai/test.sh` as the structural smoke test and run the affected scripts with `bash -n` where appropriate.

Tests must verify observable behavior, not merely implementation details. Keep integration tests explicit about their Docker requirement and do not make local development depend on unavailable services unless the existing project convention already does so.

## 6. Incremental implementation

Make the smallest coherent change that satisfies one part of the plan. After each meaningful increment, run the narrowest relevant test and inspect the diff. Avoid unrelated formatting, dependency upgrades, generated files, and drive-by refactors.

## 7. Atomic commits

When commits are requested or part of the delivery workflow, group changes by intent: implementation and tests together when they form one behavior, documentation separately when it has independent value, and CI or tooling changes by concern. Use the repository's existing conventional style, include the issue reference when appropriate, and do not stage unrelated user files.

## 8. Complete validation

Run the canonical validation from the repository root:

```bash
bash scripts/ai/validate.sh
```

This prepares the pinned model and runs `./mvnw --batch-mode --no-transfer-progress clean verify`, including tests, packaging, and JaCoCo enforcement. The `validation.yml` workflow reuses this script. Container smoke tests and vulnerability scanning remain in the CI container job because they require Docker and GitHub Actions services.

Run focused tests during development, then run the complete validation before delivery. Report skipped tests and environmental limitations explicitly.

## 9. Self-review

Review the final diff and status for scope, security, error handling, backward compatibility, tests, documentation, and accidental secrets or generated artifacts. Confirm `.ai-runs/` and local Vault configuration are ignored. Re-run the structural smoke test if workflow files changed.

## 10. Delivery

The final report must be independently verifiable and contain:

1. issue URL and concise outcome;
2. implementation and test files changed;
3. focused and complete validation commands with results;
4. documentation and Obsidian decision;
5. known risks, skips, or pre-existing failures;
6. commit hashes, if commits were created.

## Context boundaries

Load this workflow at issue start. Load `documentation-policy.md` only after implementation and validation planning. Keep run logs, issue snapshots, and baselines in `.ai-runs/`; never commit personal paths, tokens, or private Vault content.
