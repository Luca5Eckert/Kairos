---
name: spring-backend-engineering
description: Use for Java 21/Spring Boot API, domain, JPA, ingestion or persistence work in Kairos while preserving hexagonal boundaries.
---
# Spring backend engineering

1. Read pom.xml, related ports/adapters, domain code, migrations and the current issue; follow docs/ai/issue-workflow.md.
2. Keep domain rules independent of HTTP, Spring infrastructure and storage implementation; preserve use-case boundaries and user scoping.
3. Trace request validation, use case, transactional write, async enrichment and observable API response before changing a flow.
4. For JPA/pgvector/Neo4j, check query shape, N+1, pagination, indexes, transaction scope, schema migration and consistency of derived graph projections.
5. Check retries and partial completion for asynchronous ingestion; do not silently reprocess completed chunks or conflate durable source state with derived graph state.
6. Preserve documented versions and established dependencies. Do not upgrade Spring Boot, Spring AI or Testcontainers to match an unrelated skill example.
7. Cover success, failure and cross-user negative paths; hand off to backend-testing and backend-change-verification.
