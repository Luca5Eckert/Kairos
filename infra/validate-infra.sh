#!/usr/bin/env bash
# validate-infra.sh — smoke-tests for Kairos dual storage
# Compatible with Git Bash (Windows), macOS and Linux
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

# ── PostgreSQL checks (via docker exec — sem precisar de psql local) ──
check_postgres() {
  echo "Checking PostgreSQL..."

  docker exec kairos-postgres \
    env PGPASSWORD="$PG_PASS" \
    psql -h localhost -U "$PG_USER" -d "$PG_DB" \
    -tAc "SELECT 1;" > /dev/null 2>&1 \
    && ok "PostgreSQL connection" \
    || fail "PostgreSQL connection falhou — rode: docker logs kairos-postgres"

  VECTOR_EXT=$(docker exec kairos-postgres \
    env PGPASSWORD="$PG_PASS" \
    psql -h localhost -U "$PG_USER" -d "$PG_DB" \
    -tAc "SELECT extname FROM pg_extension WHERE extname = 'vector';")

  [[ "$VECTOR_EXT" == "vector" ]] \
    && ok "pgvector extension enabled" \
    || fail "pgvector extension NOT found"

  docker exec kairos-postgres \
    env PGPASSWORD="$PG_PASS" \
    psql -h localhost -U "$PG_USER" -d "$PG_DB" -q -c "
      CREATE TEMP TABLE _kairos_vec_test (embedding vector(3));
      INSERT INTO _kairos_vec_test VALUES ('[1,2,3]');
      SELECT embedding <-> '[1,2,3]' AS distance FROM _kairos_vec_test;
    " > /dev/null \
    && ok "pgvector functional test (distance query)" \
    || fail "pgvector functional test falhou"
}

# ── Neo4j checks (via curl HTTP — sem cypher-shell) ───────────
check_neo4j() {
  echo "Waiting for Neo4j HTTP at $NEO4J_HOST:$NEO4J_HTTP_PORT (pode levar ~60s)..."

  HTTP_STATUS="000"
  for i in $(seq 1 40); do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
      --connect-timeout 2 \
      "http://$NEO4J_HOST:$NEO4J_HTTP_PORT" 2>/dev/null || echo "000")
    [[ "$HTTP_STATUS" == "200" ]] && break
    printf "."
    sleep 3
  done
  echo ""

  [[ "$HTTP_STATUS" == "200" ]] \
    && ok "Neo4j HTTP reachable" \
    || fail "Neo4j HTTP nao respondeu apos 120s — rode: docker logs kairos-neo4j"

  CYPHER_RESP=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$NEO4J_USER:$NEO4J_PASS" \
    -H "Content-Type: application/json" \
    -d '{"statements":[{"statement":"RETURN 1 AS n"}]}' \
    "http://$NEO4J_HOST:$NEO4J_HTTP_PORT/db/neo4j/tx/commit")

  [[ "$CYPHER_RESP" == "200" ]] \
    && ok "Neo4j Cypher query succeeded" \
    || fail "Neo4j Cypher falhou — status $CYPHER_RESP (verifique credenciais no .env)"
}

# ── Run ───────────────────────────────────────────────────────
echo "======================================="
echo " Kairos infrastructure validation"
echo "======================================="
check_postgres
check_neo4j
echo "======================================="
echo -e "${GREEN}All checks passed.${NC}"