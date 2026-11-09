---
name: security-authorization-review
description: Use for authenticated API, sessions, JWT authorization and tenant isolation across relational, vector and graph retrieval paths.
---
# Security and authorization review

1. Map actor identity, trust boundary, authentication method and requested resource ownership.
2. Verify issuer/signature/expiration and session behavior against the actual code; do not invent token guarantees from external examples.
3. Apply user scope to source documents, chunks, triple/vector candidates, Neo4j graph traversal and hydrated results. A scoped entry query alone may not protect downstream joins.
4. Test cross-user reads, unauthorized modification, invalid credentials, expired tokens and mixed-tenant graph results with negative fixtures.
5. Review asynchronous worker context propagation and ensure failures cannot mix source ownership.
6. Avoid logging secrets, token content, raw private documents or provider credentials.
7. Explain residual threats separately from passing tests.
