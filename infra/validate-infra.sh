#!/usr/bin/env bash
# validate-infra.sh — smoke-tests for Kairos dual storage
# Usage: ./infra/validate-infra.sh
set -euo pipefail

PG_HOST="${POSTGRES_HOST:-localhost}"
PG_PORT="${POSTGRES_PORT:-5432}"
PG_DB="${POSTGRES_DB:-kairos}"
PG_USER="${POSTGRES_USER:-kairos}"
PG_PASS="${POSTGRES_PASSWORD:-kairos_secret}"

NEO4J_HOST="${NEO4J_HOST:-localhost}"
NEO4J_HTTP_PORT="${NEO4J_HTTP_PORT:-7474}"
NEO4J_BOLT_PORT="${NEO4J_BOLT_PORT:-7687}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASS="${NEO4J_PASSWORD:-kairos_secret}"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[PASS]${NC} $*"; }
fail() { echo -e "${RED}[FAIL]${NC} $*"; exit 1; }

# ── Wait helpers ──────────────────────────────────────────────
wait_for() {
  local name=$1 host=$2 port=$3 retries=30
  echo "Waiting for $name at $host:$port..."
  for i in $(seq 1 $retries); do
    nc -z "$host" "$port" 2>/dev/null && return 0
    sleep 2
  done
  fail "$name did not become reachable after $((retries * 2))s"
}

# ── PostgreSQL checks ─────────────────────────────────────────
check_postgres() {
  wait_for "PostgreSQL" "$PG_HOST" "$PG_PORT"

  PGPASSWORD="$PG_PASS" psql \
    -h "$PG_HOST" -p "$PG_PORT" \
    -U "$PG_USER" -d "$PG_DB" \
    -c "SELECT 1;" -q --no-align --tuples-only > /dev/null \
    && ok "PostgreSQL connection" \
    || fail "PostgreSQL connection"

  VECTOR_EXT=$(PGPASSWORD="$PG_PASS" psql \
    -h "$PG_HOST" -p "$PG_PORT" \
    -U "$PG_USER" -d "$PG_DB" \
    -tAc "SELECT extname FROM pg_extension WHERE extname = 'vector';")

  [[ "$VECTOR_EXT" == "vector" ]] \
    && ok "pgvector extension enabled" \
    || fail "pgvector extension NOT found — check infra/init/postgres/01_extensions.sql"

  # Quick functional test: create a temp table with a vector column
  PGPASSWORD="$PG_PASS" psql \
    -h "$PG_HOST" -p "$PG_PORT" \
    -U "$PG_USER" -d "$PG_DB" -q <<'SQL'
CREATE TEMP TABLE _kairos_vec_test (embedding vector(3));
INSERT INTO _kairos_vec_test VALUES ('[1,2,3]');
SELECT embedding <-> '[1,2,3]' AS distance FROM _kairos_vec_test;
SQL
  ok "pgvector functional test (cosine distance query)"
}

# ── Neo4j checks ──────────────────────────────────────────────
check_neo4j() {
  wait_for "Neo4j HTTP" "$NEO4J_HOST" "$NEO4J_HTTP_PORT"
  wait_for "Neo4j Bolt" "$NEO4J_HOST" "$NEO4J_BOLT_PORT"

  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    "http://$NEO4J_HOST:$NEO4J_HTTP_PORT")
  [[ "$HTTP_STATUS" == "200" ]] \
    && ok "Neo4j HTTP reachable (status $HTTP_STATUS)" \
    || fail "Neo4j HTTP returned status $HTTP_STATUS"

  if command -v cypher-shell &>/dev/null; then
    echo "RETURN 'kairos' AS ping;" \
      | cypher-shell -a "bolt://$NEO4J_HOST:$NEO4J_BOLT_PORT" \
          -u "$NEO4J_USER" -p "$NEO4J_PASS" --format plain > /dev/null \
      && ok "Neo4j Bolt connection (cypher-shell)" \
      || fail "Neo4j Bolt connection"
  else
    # Fallback: HTTP transactional endpoint
    CYPHER_RESP=$(curl -s -o /dev/null -w "%{http_code}" \
      -u "$NEO4J_USER:$NEO4J_PASS" \
      -H "Content-Type: application/json" \
      -d '{"statements":[{"statement":"RETURN 1 AS n"}]}' \
      "http://$NEO4J_HOST:$NEO4J_HTTP_PORT/db/neo4j/tx/commit")
    [[ "$CYPHER_RESP" == "200" ]] \
      && ok "Neo4j Bolt reachable via HTTP transaction API" \
      || fail "Neo4j transaction API returned status $CYPHER_RESP"
  fi
}

# ── Run ───────────────────────────────────────────────────────
echo "═══════════════════════════════════════"
echo " Kairos infrastructure validation"
echo "═══════════════════════════════════════"
check_postgres
check_neo4j
echo "═══════════════════════════════════════"
echo -e "${GREEN}All checks passed.${NC}"