-- Kairos PostgreSQL initialization
-- Enables pgvector extension for semantic similarity search

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;

-- Confirm extensions
DO $$
BEGIN
  RAISE NOTICE 'pgvector version: %',
    (SELECT extversion FROM pg_extension WHERE extname = 'vector');
END $$;