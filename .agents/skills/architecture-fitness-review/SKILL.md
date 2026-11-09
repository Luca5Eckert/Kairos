---
name: architecture-fitness-review
description: Use when modifying domain/application/adapter boundaries, dual-store ownership or quality gates in Kairos.
---
# Architecture fitness review

1. Read docs/adr and the current package structure; identify the relevant invariant before introducing a rule.
2. Validate domain independence, ports/adapters direction, query/data ownership and separation of PostgreSQL canonical state from Neo4j derived projections.
3. Translate invariants into executable checks where feasible; prefer targeted architectural/unit/integration tests over prose-only restrictions.
4. Add a known violating negative example where practical and ensure the check fails for that example.
5. Avoid weakening existing guards or treating a static boundary rule as evidence of runtime tenant isolation or recovery.
6. Report exactly which packages, tests and scenarios were validated and which guarantees remain unproven.
