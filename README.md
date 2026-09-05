# Kairos

[![CI](https://github.com/Luca5Eckert/Kairos/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Luca5Eckert/Kairos/actions/workflows/ci.yml)
[![Retrieval Evaluation](https://github.com/Luca5Eckert/Kairos/actions/workflows/retrieval-evaluation.yml/badge.svg?branch=main)](https://github.com/Luca5Eckert/Kairos/actions/workflows/retrieval-evaluation.yml)
[![Code quality](https://github.com/Luca5Eckert/Kairos/actions/workflows/qodana_code_quality.yml/badge.svg?branch=main)](https://github.com/Luca5Eckert/Kairos/actions/workflows/qodana_code_quality.yml)
[![Security](https://github.com/Luca5Eckert/Kairos/actions/workflows/security.yml/badge.svg?branch=main)](https://github.com/Luca5Eckert/Kairos/actions/workflows/security.yml)

<p>
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.7-green" alt="Spring Boot 4.0.7">
  <img src="https://img.shields.io/badge/Spring%20AI-2.0.0--M6-green" alt="Spring AI 2.0.0-M6">
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue" alt="PostgreSQL 16">
  <img src="https://img.shields.io/badge/Neo4j-5.26-blue" alt="Neo4j 5.26">
  <img src="https://img.shields.io/badge/pgvector-HNSW-blue" alt="pgvector HNSW">
  <img src="https://img.shields.io/badge/ONNX%20Runtime-1.20.0-black" alt="ONNX Runtime 1.20.0">
  <img src="https://img.shields.io/badge/Docker-Compose-blue" alt="Docker Compose">
</p>

**Standard RAG retrieves similar passages. Kairos is designed to retrieve connected knowledge.**

Kairos is a JVM-native personal knowledge graph engine for graph-augmented retrieval. It turns source material into durable chunks, extracts factual triples, creates local embeddings, and combines vector search with graph propagation to recover context that is related by meaning and structure.

The project is intentionally built as a retrieval backend rather than a chat interface. Its boundaries are explicit: PostgreSQL stores durable text, vectors, and retrieval history; Neo4j stores the graph projection; Gemini is used for extraction and recognition-memory decisions; the embedding model runs locally on the JVM.

> **Maturity:** experimental retrieval backend. Kairos has automated quality and security checks, but it has not undergone an independent security audit and does not claim production readiness. No software license or contribution policy has been published yet.

## Contents

- [What Kairos does](#what-kairos-does)
- [Current capabilities](#current-capabilities)
- [Why graph-augmented retrieval](#why-graph-augmented-retrieval)
- [Architecture](#architecture)
- [Data model](#data-model)
- [Ingestion flow](#ingestion-flow)
- [Retrieval and history flow](#retrieval-and-history-flow)
- [API](#api)
- [Quick start](#quick-start)
- [Security, privacy, and operations](docs/operations.md)
- [Testing and CI/CD](#testing-and-cicd)
- [Agent issue workflow](#agent-issue-workflow)
- [Troubleshooting](#troubleshooting)
- [Limitations and roadmap](#limitations-and-roadmap)
- [Technical references](#technical-references)

## What Kairos does

Personal knowledge tools often rely on manual structure: folders, tags, links, and aliases. Kairos takes a different approach:

> Feed the system sources. Let the graph build itself.

The current retrieval path is inspired by HippoRAG 2:

1. A source is stored and split into overlapping chunks.
2. Chunks are embedded locally with `all-MiniLM-L6-v2` through ONNX Runtime.
3. Gemini extracts subject-predicate-object triples from each chunk.
4. Passages, concepts, and relationships are projected into a user-scoped Neo4j graph.
5. A query retrieves dense passage and triple candidates from pgvector.
6. Recognition memory selects concept seeds from the triple candidates.
7. Neo4j Graph Data Science runs weighted Personalized PageRank over the user's graph.
8. Final chunks are hydrated from PostgreSQL and returned together with the activated triples that explain the result.

## Current capabilities

| Area | Implemented capability |
| --- | --- |
| Application design | Hexagonal architecture with domain ports, application use cases, and infrastructure adapters |
| Authentication | Registration, email confirmation, login, JWT sessions, roles, and authenticated request context |
| User isolation | Source, vector, graph, progress, and retrieval queries are scoped to the authenticated user |
| Ingestion | Transactional source upload, overlapping chunking, durable persistence, and asynchronous enrichment after commit |
| Resume behavior | Failed chunks are visible in source progress and can be retried explicitly without reprocessing completed chunks |
| Embeddings | Local ONNX Runtime inference with `all-MiniLM-L6-v2` and `vector(384)` storage |
| Knowledge extraction | Gemini structured output for factual triple extraction and recognition-memory seed selection |
| Semantic search | PostgreSQL and pgvector HNSW indexes for passage and triple candidate retrieval |
| Graph retrieval | Neo4j 5.26 with Graph Data Science and weighted Personalized PageRank |
| Retrieval evaluation | Versioned IR evaluation with a pull-request quality gate plus scheduled/manual full benchmark |
| Retrieval history | Immutable, versioned `AnswerSnapshot` documents stored as PostgreSQL `JSONB` |
| Progress | `GET /sources/progress` reports aggregate status and chunk counts by processing state for the current user |
| Delivery | Docker Compose for local infrastructure and GitHub Actions for build, retrieval quality, security, and container validation |

## Why graph-augmented retrieval

Vector search is strong at finding passages close to a query embedding. It is weaker when the answer depends on relationships spread across passages or on concepts that do not share the same wording.

| Vector-only RAG | Kairos |
| --- | --- |
| Retrieves chunks by embedding proximity | Retrieves passages, triples, and graph-expanded context |
| Treats chunks mostly independently | Connects chunks through concepts and extracted relationships |
| Relies on the LLM to infer most relationships at answer time | Surfaces relationship evidence before generation |
| Often depends on an external embedding service | Runs embeddings locally in the JVM |
| Rebuilds context from current data | Persists self-contained retrieval snapshots for history |

### Measured retrieval evaluation

Kairos includes a versioned, controlled offline evaluation instead of relying only on architectural claims. Dataset v1 contains 60 labeled natural-language queries: 30 single-hop and 30 multi-hop, with graded passage relevance and semantic distractors.

The first reference execution compared the same corpus and queries using vector-only retrieval and the graph-augmented path with production ONNX embeddings, real PostgreSQL/pgvector, and real Neo4j GDS Personalized PageRank. Recognition-memory seed selection was frozen deterministically to isolate the retrieval core from Gemini latency and variance.

| Segment | Vector nDCG@10 | Graph nDCG@10 | Relative lift |
| --- | ---: | ---: | ---: |
| All queries | 0.8249 | **0.9797** | **+18.8%** |
| Multi-hop | 0.6498 | **0.9593** | **+47.6%** |

`Recall@10` reached `1.0000` in both modes, so the result is specifically a **ranking-quality gain**, not a Recall@10 gain. In the reference run, 29 of 30 multi-hop queries improved nDCG, one was unchanged, and none regressed.

The gain has a measurable latency cost: vector-only p95 was `19.03 ms`, while graph-augmented p95 was `418.36 ms`. Neo4j GDS/PPR accounted for `400.80 ms` p95 in the graph-stage breakdown, making graph projection/PageRank the primary optimization target exposed by the benchmark.

These are reproducible CI/offline measurements, **not production SLAs**, and they do not include live Gemini recognition. The complete methodology, boundaries, commands, and continuous-evaluation policy are documented in [docs/evaluation/retrieval-benchmark.md](docs/evaluation/retrieval-benchmark.md).

## Architecture

Kairos separates business decisions from framework and storage details. The domain owns retrieval concepts and ports; application use cases orchestrate workflows; infrastructure adapters integrate PostgreSQL, Neo4j, ONNX Runtime, Spring AI, mail, and security.

```mermaid
flowchart TB
    client[Client]

    subgraph api[HTTP API]
        auth[AuthController]
        sources[SourceController]
    end

    subgraph application[Application use cases]
        upload[Upload source]
        enrich[Generate source context]
        search[Search source context]
        progress[Read source progress]
    end

    subgraph semantic[Semantic and AI adapters]
        chunker[Chunker]
        embedder[ONNX embeddings]
        extractor[Gemini triple extraction]
        memory[Gemini recognition memory]
        vectors[Semantic search]
    end

    subgraph graph[Graph adapters]
        store[Graph mutation]
        graphSearch[Graph expansion]
        ppr[Neo4j GDS PageRank]
    end

    postgres[(PostgreSQL + pgvector)]
    neo4j[(Neo4j + GDS)]
    history[(JSONB retrieval history)]

    client --> auth
    client --> sources
    auth --> application
    sources --> upload
    sources --> search
    sources --> progress
    upload --> chunker
    upload --> postgres
    upload --> enrich
    enrich --> embedder
    enrich --> extractor
    enrich --> postgres
    enrich --> store
    store --> neo4j
    search --> embedder
    search --> vectors
    search --> memory
    search --> graphSearch
    search --> history
    vectors --> postgres
    progress --> postgres
    graphSearch --> ppr
    ppr --> neo4j
```

The stores have deliberately different responsibilities:

- **PostgreSQL** is the source of truth for users, sources, chunks, vectors, triples, progress, questions, and answers.
- **pgvector** is the PostgreSQL extension used for HNSW-backed dense passage and triple recall; it does not own the source data.
- **Neo4j with Graph Data Science** contains a user-scoped, derived projection of passages, concepts, and relationships, then runs weighted Personalized PageRank for graph propagation.
- **Gemini** is limited to structured triple extraction and constrained recognition-memory seed selection; it is not the primary ranking engine.
- **Answer snapshots** are self-contained historical records. Reading one does not require loading the current chunks, triples, or Neo4j graph.

## Data model

The persistent model separates durable knowledge from its graph projection:

| Record | Purpose | Storage |
| --- | --- | --- |
| Source | User-owned submitted document | PostgreSQL |
| Chunk | Overlapping, processable source segment with embedding and processing state | PostgreSQL + pgvector |
| Knowledge triple | Subject-predicate-object fact extracted from a chunk | PostgreSQL; projected to Neo4j |
| Passage and concept nodes | Derived graph representation connecting chunks through concepts | Neo4j |
| Question and `AnswerSnapshot` | Immutable, versioned record of a retrieval execution and its returned context | PostgreSQL JSONB |

The model makes a Neo4j rebuild possible from durable PostgreSQL data, while a historical answer remains readable without depending on the current graph or chunks. A supported rebuild command, endpoint, or job is **not** implemented yet.

## Ingestion flow

`POST /sources` commits the source and its chunks before asynchronous enrichment begins. This keeps the upload path durable and makes failed enrichment resumable.

```mermaid
sequenceDiagram
    participant C as Client
    participant API as SourceController
    participant U as UploadSourceUseCase
    participant PG as PostgreSQL
    participant E as CreatedSourceEvent
    participant G as GenerateSourceContextUseCase
    participant ORT as ONNX Runtime
    participant LLM as Gemini
    participant N as Neo4j

    C->>API: POST /sources
    API->>U: upload title and content
    U->>U: Resolve authenticated user
    U->>PG: Save source and chunks
    U->>E: Publish after transaction commit
    API-->>C: 201 Created
    E->>G: Async enrichment
    G->>PG: Load unprocessed chunks
    G->>ORT: Embed each chunk
    G->>LLM: Extract factual triples
    G->>PG: Save embeddings and triples
    G->>N: Merge passages and relationships
    G->>PG: Mark chunk processed
```

If enrichment fails, the affected chunk is marked as `FAILED`, chunks already completed remain complete, and processing continues with the remaining claimed chunks. The authenticated owner can retry only failed chunks through `POST /sources/{sourceId}/retry`; automatic retries remain intentionally out of scope.

## Retrieval and history flow

`POST /sources/search` is user-scoped from the request context. Every execution persists the question and an immutable answer snapshot, while the public response remains focused on ranked chunks and activated triples. Pass an existing `questionId` to append a new immutable answer without overwriting earlier executions. The `/history/questions` and `/history/answers` endpoints expose those records with user-scoped pagination; detailed answers are served entirely from the stored JSONB snapshot.

### Retrieval defaults

These defaults are bound from `kairos.retrieval` and can be overridden through the corresponding `KAIROS_*` environment variables.

| Setting | Default | Role |
| --- | ---: | --- |
| `semantic-anchor-limit` | 10 | Dense passage candidates used as direct graph anchors |
| `graph-passage-limit` | 20 | Maximum passages returned after graph ranking |
| `triple-candidate-limit` | 30 | Dense triple candidates presented to recognition memory |
| `recognition-seed-limit` | 10 | Maximum concept seeds selected from triple candidates |
| `seed-min-score` | 0.45 | Minimum score required for a graph seed |
| `seed-relative-threshold` | 0.85 | Relative score threshold applied to seed selection |
| `history.default-page-size` | 20 | Default page size for history endpoints |
| `history.max-page-size` | 100 | Maximum accepted page size for history endpoints |

All Spring configuration options are documented in [docs/configuration.md](docs/configuration.md). History responses use `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`; list endpoints default to page `0`, size `20`, and reject sizes above the configurable maximum.

## Data locality and external processing

Embeddings are generated locally in the JVM. This does **not** mean all document data stays local:

- during ingestion, the complete content of each chunk is sent to Gemini for triple extraction;
- during search, Gemini receives the user query and the selected candidate triples (subject, predicate, object, and score) for recognition-memory seed selection;
- Kairos currently configures Gemini as its LLM provider. The extraction and recognition interfaces are domain ports, but no local LLM provider is supplied or documented.

Do not submit sensitive content unless its processing by the configured Gemini provider is acceptable for your environment. See [security, privacy, and operational limitations](docs/operations.md) before deployment.

## API

All `/sources/**`, `/history/**`, and `/users/**` endpoints require a valid JWT bearer token. Authentication endpoints are public.

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Public | Creates a pending user and sends an email confirmation code |
| `POST` | `/auth/confirm-email` | Public | Confirms a user and returns a JWT |
| `POST` | `/auth/login` | Public | Authenticates by username or email and returns a JWT |
| `GET` | `/users/me` | JWT | Returns the authenticated user public profile |
| `PATCH` | `/users/me` | JWT | Updates only the authenticated user name and username |
| `POST` | `/users/me/password` | JWT | Changes password after current-password confirmation |
| `POST` | `/sources` | JWT | Stores a source and starts asynchronous enrichment |
| `POST` | `/sources/search` | JWT | Searches the authenticated user's graph-augmented knowledge base; accepts optional `questionId` to append a new answer |
| `GET` | `/history/questions` | JWT | Lists the authenticated user's questions with pagination |
| `GET` | `/history/questions/{questionId}` | JWT | Returns question metadata and execution counts |
| `GET` | `/history/questions/{questionId}/answers` | JWT | Lists persisted answer summaries with pagination |
| `GET` | `/history/answers/{answerId}` | JWT | Returns a complete immutable answer snapshot |
| `GET` | `/sources/progress` | JWT | Lists source chunk progress for the authenticated user |
| `POST` | `/sources/{sourceId}/retry` | JWT | Asynchronously retries only failed chunks owned by the authenticated user |

The versioned API contract is available in [docs/openapi.yaml](docs/openapi.yaml). Runtime Swagger UI is not configured. The complete request collection, validation cases, authentication flow, and local environment are available in [docs/postman/Kairos.postman_collection.json](docs/postman/Kairos.postman_collection.json) and [docs/postman/Kairos.local.postman_environment.json](docs/postman/Kairos.local.postman_environment.json).

Password changes do not revoke JWTs already issued: they remain valid until their normal expiration. The API never returns password hashes, confirmation codes, or other security internals.

### Try the API locally

After starting Docker Compose, the host-facing API is available at `http://127.0.0.1:8081` when using the supplied `.env.example`. The application listens on port `8080` inside its container.

The Docker profile creates a local admin when `KAIROS_ADMIN_BOOTSTRAP_ENABLED=true`. Change the default credentials before using the stack outside local development.

```bash
curl -X POST http://127.0.0.1:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"Admin123!"}'
```

Export the returned token as `TOKEN`, then upload a source:

```bash
curl -X POST http://127.0.0.1:8081/sources \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"RAG and knowledge graphs","content":"Knowledge graphs connect entities and relationships across passages."}'
```

Check asynchronous progress:

```bash
curl http://127.0.0.1:8081/sources/progress \
  -H "Authorization: Bearer $TOKEN"
```

Search the authenticated knowledge base after enrichment has processed the chunks. To re-execute an existing question, include its `questionId` in the same body: `{"termQuery":"How do knowledge graphs improve retrieval?","questionId":"<question-id>"}`.

```bash
curl -X POST http://127.0.0.1:8081/sources/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"termQuery":"How do knowledge graphs improve retrieval?"}'
```

The endpoint returns graph evidence separately from ranked chunks. UUIDs and scores below are illustrative; field names match the public response contract. History can then be read with:

```bash
curl "http://127.0.0.1:8081/history/questions?page=0&size=20" -H "Authorization: Bearer $TOKEN"
```

```json
{
  "knowledgeGraph": [
    {
      "subject": "Knowledge graphs",
      "predicate": "connect",
      "object": "entities across passages",
      "chunkId": "c4f2397a-c1a0-4eaa-92b7-117ef8cba3b9"
    }
  ],
  "chunkContexts": [
    {
      "chunkId": "c4f2397a-c1a0-4eaa-92b7-117ef8cba3b9",
      "content": "Knowledge graphs connect entities and relationships across passages.",
      "rank": 1,
      "score": 0.7812,
      "source": "GRAPH"
    }
  ]
}
```

## Quick start

### Prerequisites

- Docker Desktop with Docker Compose;
- a Gemini API key for extraction and recognition memory;
- SMTP credentials for confirmation email delivery;
- Java 21 and Bash if running Maven locally.

### Run with Docker Compose

1. Create the environment file (choose the command for your shell):

   ```bash
   cp .env.example .env
   ```

   ```powershell
   Copy-Item .env.example .env
   ```

2. Fill `GEMINI_API_KEY`, mail credentials, and database passwords. Replace `AUTH_SESSION_SECRET` with a long random value and change `KAIROS_ADMIN_PASSWORD`.
3. Start the stack:

   ```bash
   docker compose up --build --detach
   ```

4. Wait for the health checks and verify the application:

   ```bash
   curl http://127.0.0.1:8081/actuator/health
   ```

The host port comes from `APP_PORT` in `.env`; the provided example uses `8081`. The application always listens on `8080` inside the container. PostgreSQL, Neo4j HTTP, and Neo4j Bolt are bound to localhost by default.

For a complete configuration reference and retrieval tuning options, see [docs/configuration.md](docs/configuration.md).

### Run Maven locally

The canonical validation command is:

```bash
bash scripts/ai/validate.sh
```

It prepares the pinned ONNX model and runs Maven `clean verify`, including the lightweight retrieval quality regression gate. Integration tests use Testcontainers and require an accessible Docker daemon.

Run the complete 60-query retrieval benchmark separately with:

```bash
bash scripts/ai/evaluate-retrieval.sh
```

## Testing and CI/CD

Kairos treats retrieval quality as a continuously evaluated property rather than a one-off benchmark. Merge-time regression protection is intentionally separated from the complete evaluation so pull requests stay deterministic while full quality/performance evidence remains reproducible.

### Pull-request retrieval quality gate

Every normal `Validation` run executes a stable 12-query regression subset through real production boundaries:

- 6 single-hop and 6 multi-hop queries;
- production `all-MiniLM-L6-v2` ONNX embeddings;
- PostgreSQL/pgvector dense retrieval;
- Neo4j GDS Personalized PageRank;
- deterministic recognition seeds constrained to actual triple candidates.

The merge-time gate fails if graph multi-hop nDCG@10 falls below vector-only on that subset, if graph multi-hop nDCG@10 falls below `0.90`, or if overall graph nDCG@10 falls below `0.95`.

Latency is deliberately **not** a hard pull-request threshold. p95/p99 on shared GitHub-hosted runners is noisy; using an absolute timing cutoff would make CI flaky and conflate infrastructure variance with retrieval regressions.

### Full retrieval evaluation

The complete 60-query evaluation is excluded from normal Maven verification and runs in the dedicated `Retrieval Evaluation` workflow:

- manually through `workflow_dispatch`;
- every Monday at `09:00 UTC`;
- with the full quality table plus p50/p95/p99 and graph-stage latency breakdown;
- with `run-metadata.json`, raw query results, structured report, and Markdown summary uploaded as workflow artifacts for 30 days.

Locally, the same workflow boundary is available through `bash scripts/ai/evaluate-retrieval.sh`. See [the retrieval evaluation specification](docs/evaluation/retrieval-benchmark.md) for the exact protocol and guardrails.

### CI topology

Validation, container CI, Qodana, and Security start independently for pull requests and pushes to `main` or `develop`. Full retrieval evaluation has its own manual/scheduled cadence.

```mermaid
flowchart LR
    pr[Pull request or push] --> validation[Maven verify]
    pr --> container[Container validation]
    pr --> quality[Qodana Community]
    pr --> security[CodeQL]
    pr --> dependency[Dependency Review on PRs]
    validation --> qualityGate[12-query retrieval quality gate]
    validation --> reports[Test and coverage reports]
    container --> sbom[CycloneDX SBOM]
    container --> trivy[Trivy critical vulnerability gate]

    schedule[Weekly or manual] --> evaluation[60-query Retrieval Evaluation]
    evaluation --> metrics[nDCG / Recall / MRR + latency percentiles]
    evaluation --> artifacts[Versioned evaluation artifacts]
```

### Validation and container workflows

The `Validation` workflow:

1. sets up Java 21 and Maven caching;
2. runs `bash scripts/ai/validate.sh`;
3. executes unit tests, integration tests, the 12-query retrieval quality gate, packaging, and JaCoCo enforcement;
4. uploads test, coverage, and application artifacts.

The `CI container` workflow validates Compose, builds the application image, starts PostgreSQL and Neo4j, performs the health check, generates a CycloneDX SBOM, and runs the Trivy critical-vulnerability gate.

The workflows are intentionally independent: Maven validation, container/runtime security, static analysis, and full retrieval evaluation report separate classes of failure.

### Quality and security gates

- **Retrieval quality**: graph multi-hop ranking is protected by deterministic nDCG@10 regression floors on the 12-query PR subset.
- **JaCoCo**: the build fails below 80% line coverage or 70% branch coverage.
- **Qodana Community**: publishes annotations and workflow artifacts; it runs without `QODANA_TOKEN` and is not connected to Qodana Cloud.
- **CodeQL**: performs Java/Kotlin security analysis and publishes findings in the repository Security tab.
- **Dependency Review**: runs on pull requests and blocks dependency changes with high severity or higher.
- **Trivy**: creates the container SBOM and blocks fixable critical vulnerabilities in OS and library packages.
- **Dependabot**: checks Maven, Docker, and GitHub Actions weekly.

The container gate remains strict. During issue #113 it surfaced critical vulnerabilities in the Spring Boot 4.0.7-managed Tomcat `11.0.22`; Kairos overrides the managed Tomcat line to the security-fixed `11.0.25` rather than suppressing the scan.

Validation retains test/coverage artifacts and the JAR for 14 days. Container SBOM and full retrieval-evaluation artifacts are retained for 30 days.

### Branch rules

For the protected `main` branch, require the successful checks exposed by the pull-request workflows, including Maven validation/retrieval quality, container validation, Qodana, CodeQL, and Dependency Review. The scheduled full retrieval benchmark is observational and should not be configured as a required pull-request check.

The exact check names should be selected from a completed run in the repository ruleset UI. Enable the `merge_group` trigger before using merge queue with required checks.

## Agent issue workflow

`AGENTS.md` is the short entry point for AI-assisted issue work. The detailed sequence, evidence requirements, and documentation decision rules are versioned in [`docs/ai/issue-workflow.md`](docs/ai/issue-workflow.md) and [`docs/ai/documentation-policy.md`](docs/ai/documentation-policy.md). Temporary issue snapshots and validation logs belong in `.ai-runs/`, which is ignored by Git.

## Troubleshooting

| Symptom | Likely cause | What to check |
| --- | --- | --- |
| Compose reports a missing variable | A required database, mail, or credential variable is empty | Fill `.env` using [docs/configuration.md](docs/configuration.md) |
| The health endpoint is unreachable | Services are still starting or the host port differs from `APP_PORT` | Run `docker compose ps`, inspect logs, and use the configured host port |
| `/sources` or `/sources/search` returns `401` | The route requires a valid JWT | Log in or confirm the user and send `Authorization: Bearer $TOKEN` |
| Upload returns `201` but search is empty | Enrichment runs asynchronously | Poll `/sources/progress` and wait for processed chunks |
| A source reports `FAILED` | A chunk failed during enrichment | Inspect application logs, then call `POST /sources/{sourceId}/retry` to retry only failed chunks. |
| Graph results are empty | Neo4j GDS is unavailable or graph enrichment has not completed | Check Neo4j logs and the application warning about unavailable GDS procedures |
| Existing local data fails schema validation | A volume was created with an older schema | For disposable development data, run `docker compose down --volumes` and recreate the stack |
| Gemini extraction fails | The API key or model configuration is missing or invalid | Check `GEMINI_API_KEY`, `KAIROS_LLM_MODEL`, and Spring AI settings |
| Logs mention `token_type_ids` are not declared | The current ONNX model does not require that input | This is expected and covered by embedding tests |

## Limitations and roadmap

The current V1 is experimental, not a complete end-user knowledge application or a security-audited production service. The main remaining gaps are:

- an automatic retry scheduler and Neo4j rebuild procedure;
- OpenAPI/Swagger publication;
- a dense-search fallback when Neo4j GDS is unavailable;
- request-size limits, rate limiting, and upload-abuse controls;
- a stable Spring AI release (the current `2.0.0-M6` dependency is a milestone release);
- a published software license, contribution guide, and vulnerability-disclosure policy;
- broader user-management features beyond authentication and source ownership.

Search the repository's [open GitHub Issues](https://github.com/Luca5Eckert/Kairos/issues) for the current roadmap. Completed retrieval work, including HippoRAG 2 search, continuous retrieval evaluation, and JSONB retrieval history, is documented as current capability rather than future work.

## Technical references

- [Retrieval evaluation and continuous quality policy](docs/evaluation/retrieval-benchmark.md)
- [Configuration reference](docs/configuration.md)
- [Security, privacy, and operations](docs/operations.md)
- [ADR index](docs/adr/)
- [Postman collection](docs/postman/Kairos.postman_collection.json)
- [HippoRAG 2: From RAG to Memory](https://arxiv.org/abs/2502.14802)
- [pgvector documentation](https://access.crunchydata.com/documentation/pgvector/latest/)
- [Neo4j Graph Data Science PageRank](https://neo4j.com/docs/graph-data-science/current/algorithms/page-rank/)
- [Spring AI structured output](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [ONNX Runtime for Java](https://onnxruntime.ai/docs/get-started/with-java.html)
- [all-MiniLM-L6-v2 model card](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2)

---

Built by [Lucas Eckert](https://luca5eckert.github.io)