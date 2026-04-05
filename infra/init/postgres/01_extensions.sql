-- Kairos PostgreSQL initialization
-- Enables pgvector extension for semantic similarity search

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- optional: fuzzy text search
CREATE EXTENSION IF NOT EXISTS btree_gin; -- optional: composite GIN indexes

-- Confirm extensions
DO $$
BEGIN
  RAISE NOTICE 'pgvector version: %', (SELECT extversion FROM pg_extension WHERE extname = 'vector');
END $$;