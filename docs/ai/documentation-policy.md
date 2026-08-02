# Documentation policy for issue changes

This policy is consulted after implementation. The repository is the source of truth for behavior, decisions, operations, and anything needed by another contributor or agent.

## Versioned documentation

Update versioned documentation when a change affects an API, configuration, deployment or operational procedure, user-visible behavior, supported workflow, data model, security boundary, or contributor/agent process. Prefer the existing README, `docs/configuration.md`, `docs/operations.md`, and ADR directory before creating a new document.

Keep documentation close to the behavior it describes. Do not duplicate the complete issue workflow in module documents; link to `AGENTS.md` or `docs/ai/issue-workflow.md` instead.

## ADRs

Create or update an ADR only when the change records a durable architectural decision, a meaningful trade-off, or a constraint that future work must preserve. Do not create an ADR for a local bug fix, a mechanical refactor, a test-only change, or a routine dependency/configuration edit.

ADRs are versioned and must contain enough context, decision, consequences, and status to stand alone. Personal notes are not a substitute for an ADR.

## Obsidian Vault

An Obsidian note is optional and is appropriate for significant project context, unresolved investigation notes, delivery summaries, or cross-issue knowledge that is useful to the maintainers' private working process. It must not be the only place where technical behavior, security requirements, operational instructions, or architectural decisions are recorded.

Use `scripts/ai/document-change.sh` only when the change merits a note. Configure the destination locally with `KAIROS_OBSIDIAN_VAULT` or pass `--vault`; never commit an absolute personal path or Vault content. The script creates a starter note and does not decide its technical content.

```bash
KAIROS_OBSIDIAN_VAULT=/path/to/vault \
  bash scripts/ai/document-change.sh --issue 123 --title "Short change title"
```

The command refuses to overwrite an existing note. Review and complete the note manually, and keep secrets, tokens, personal data, and unverified claims out of it.

## Trivial changes

No note is required for formatting-only edits, generated files, typo fixes, isolated tests, mechanical renames, dependency lockfile updates without a decision, or changes already fully explained by nearby versioned documentation.

## Never Vault-only

The following must always be versioned in the repository when applicable:

- API contracts and externally observable behavior;
- required configuration and deployment steps;
- security, privacy, and data-retention rules;
- schema or migration meaning;
- architectural decisions and compatibility constraints;
- contributor, CI, and agent workflow requirements.
