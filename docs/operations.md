# Security, Privacy, and Operational Limitations

This document describes the behavior implemented by Kairos today. It is not a security audit, an operational runbook, or a production-readiness claim.

## Maturity and support boundary

Kairos is an experimental retrieval backend. It has automated Maven, coverage, container, static-analysis, dependency-review, and vulnerability-scanning workflows, but it has not undergone an independent security audit.

The repository currently has no published software license, `CONTRIBUTING.md`, or vulnerability-disclosure policy. Do not assume redistribution rights, contribution rules, or a security-response SLA until those policies are published.

Spring AI is currently pinned to `2.0.0-M6`, a milestone release. Upgrades can introduce compatibility changes and should be tested before deployment.

## Data locality and Gemini

ONNX embeddings are generated locally by the JVM. Gemini remains part of the data path:

| Operation | Data sent to Gemini |
| --- | --- |
| Triple extraction during ingestion | The complete content of each source chunk, embedded in the extraction prompt |
| Recognition memory during search | The user query plus the dense candidate triples: key, subject, predicate, object, and similarity score |

The recognition-memory request does not send full chunk text by design. Triple extraction does. Treat both document chunks and candidate triples as potentially sensitive data and review the configured provider's terms, retention, and regional controls before use.

The code isolates extraction and recognition behind domain ports, so an alternative provider can be implemented. Kairos does not currently ship, configure, or document a local LLM provider.

Application logs intentionally identify processing by source ID; they are not a documented content-redaction boundary. Treat logs and external-provider telemetry as part of the deployment threat model.

## Security controls and gaps

Implemented controls include JWT-protected source routes, authenticated request context, user-scoped retrieval queries, `.env` exclusion from Git, and CI security checks.

The following controls are **not** implemented at the application boundary:

- request-content size limits beyond non-blank validation;
- API rate limiting or per-user quotas;
- upload-abuse protection;
- a documented secrets-file integration; configuration is supplied through environment variables, with `.env` used only for local Compose convenience;
- a public vulnerability-disclosure process.

SMTP and Gemini credentials are required by the supplied Compose workflow. Use distinct, non-default credentials and a long random `AUTH_SESSION_SECRET` outside local development.

## PostgreSQL and Neo4j consistency

PostgreSQL is the durable source of truth. Neo4j is a derived graph projection. Their writes do not participate in a distributed transaction.

For each unprocessed chunk, Kairos writes the embedding and extracted triples to PostgreSQL, merges graph data into Neo4j, then marks the chunk as processed in PostgreSQL. A failure can therefore leave an intermediate state, such as durable relational data without a completed graph projection. There is no reconciliation job or supported external rebuild procedure yet.

Neo4j graph mutation uses `MERGE` for a passage keyed by `chunkId` and for a triple relationship keyed by predicate, chunk ID, and user ID. Repeating those graph writes does not create another relationship with the same key. This is graph-write idempotency, not a guarantee of end-to-end recovery across both databases.

The internal enrichment use case selects chunks whose `processed` flag is false. It runs automatically after a new source is committed through an in-process asynchronous event. There is currently no `POST /sources/{id}/reprocess` endpoint, scheduler, CLI command, or administrative job to invoke it after a failed run.

## Multi-user graph isolation

Each `Passage` node has a `user_id`. `CONTAINS` and `TRIPLE` relationships also carry `user_id`; `PhraseNode` nodes are shared by normalized name.

For graph search, Kairos creates a GDS projection filtered by the authenticated user's passages and relationships. Personalized PageRank and the final PostgreSQL hydration both use that user scope. This prevents traversal through another user's relationships even when two users share a concept name. The design should still be treated as application-enforced tenant isolation, not as separate Neo4j databases per tenant.

## Recovery and operator limitations

The model permits Neo4j to be reconstructed from durable PostgreSQL chunks and triples, but Kairos does not expose a supported command, endpoint, or job to perform that reconstruction. Similarly, source progress is observable through `GET /sources/progress`, but failed enrichment cannot be retried through the public API.

For now, investigate failed enrichment through application logs and database state. Do not present restart, re-upload, or direct use-case invocation as a supported recovery procedure.
