# Kairos Configuration

This file is the full configuration reference for local development and tuning.

## Environment Variables

Docker Compose requires database, Neo4j, Gemini, auth, and mail settings.

```env
POSTGRES_DB=kairos
POSTGRES_USER=kairos
POSTGRES_PASSWORD=change-me
POSTGRES_PORT=5432

NEO4J_USER=neo4j
NEO4J_PASSWORD=change-me
NEO4J_HTTP_PORT=7474
NEO4J_BOLT_PORT=7687

GEMINI_API_KEY=your-gemini-api-key
KAIROS_LLM_MODEL=gemini-2.5-flash
KAIROS_LLM_TEMPERATURE=0.0
KAIROS_LLM_MAX_OUTPUT_TOKENS=4096

AUTH_SESSION_SECRET=change-me-to-a-long-random-secret
AUTH_SESSION_ISSUER=kairos
AUTH_ACCESS_TOKEN_TTL=2h

MAIL_HOST=smtp-relay.example.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-user
MAIL_PASSWORD=your-smtp-password
MAIL_FROM=no-reply@kairos.local
KAIROS_MAIL_HEALTH_ENABLED=false

KAIROS_ADMIN_BOOTSTRAP_ENABLED=true
KAIROS_ADMIN_NAME=Kairos Admin
KAIROS_ADMIN_USERNAME=admin
KAIROS_ADMIN_EMAIL=admin@kairos.local
KAIROS_ADMIN_PASSWORD=Admin123!
```

The `docker` profile creates a confirmed local admin user when
`KAIROS_ADMIN_BOOTSTRAP_ENABLED=true`. Override the default password in shared
or long-lived environments.

## Spring AI

Kairos uses Spring AI with the Google GenAI chat model. LLM calls are limited to triple extraction and recognition-memory seed selection.

```yaml
spring:
  ai:
    model:
      chat: google-genai
    google:
      genai:
        api-key: ${GEMINI_API_KEY}
        chat:
          options:
            model: ${KAIROS_LLM_MODEL:gemini-2.5-flash}
            temperature: ${KAIROS_LLM_TEMPERATURE:0.0}
            max-output-tokens: ${KAIROS_LLM_MAX_OUTPUT_TOKENS:4096}
```

## Retrieval Tuning

```yaml
kairos:
  retrieval:
    semantic-anchor-limit: ${KAIROS_SEMANTIC_ANCHOR_LIMIT:10}
    graph-passage-limit: ${KAIROS_GRAPH_PASSAGE_LIMIT:20}
    triple-candidate-limit: ${KAIROS_TRIPLE_CANDIDATE_LIMIT:30}
    recognition-seed-limit: ${KAIROS_RECOGNITION_SEED_LIMIT:10}
    seed-min-score: ${KAIROS_SEED_MIN_SCORE:0.45}
    seed-relative-threshold: ${KAIROS_SEED_RELATIVE_THRESHOLD:0.85}
  graph:
    orphan-cleanup-interval-ms: ${KAIROS_GRAPH_ORPHAN_CLEANUP_INTERVAL_MS:600000}
```

| Property | Default | Purpose |
| --- | --- | --- |
| `KAIROS_SEMANTIC_ANCHOR_LIMIT` | `10` | Passage candidates retrieved from pgvector |
| `KAIROS_TRIPLE_CANDIDATE_LIMIT` | `30` | Triple candidates retrieved before recognition memory |
| `KAIROS_RECOGNITION_SEED_LIMIT` | `10` | Maximum concept seeds accepted from recognition memory |
| `KAIROS_GRAPH_PASSAGE_LIMIT` | `20` | Maximum graph-ranked passages returned |
| `KAIROS_SEED_MIN_SCORE` | `0.45` | Minimum score required for a seed |
| `KAIROS_SEED_RELATIVE_THRESHOLD` | `0.85` | Seed must be within this fraction of the best score |

## Graph Search

```yaml
hipporag:
  ppr:
    max-iterations: 20
    damping-factor: 0.85
    score-threshold: 0.001
```

| Property | Default | Purpose |
| --- | --- | --- |
| `hipporag.ppr.max-iterations` | `20` | PageRank iteration cap |
| `hipporag.ppr.damping-factor` | `0.85` | PageRank damping factor |
| `hipporag.ppr.score-threshold` | `0.001` | Minimum activated node score |

## ChatClient Beans

Spring AI is used through dedicated `ChatClient` beans:

- `tripleExtractionChatClient` extracts factual triples.
- `recognitionMemoryChatClient` selects relevant graph seed concepts from retrieved triples.

Both calls use typed structured output and are hidden behind domain ports.
