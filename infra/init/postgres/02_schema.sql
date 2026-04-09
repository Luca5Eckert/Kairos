CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT NOT NULL
);

CREATE INDEX ON sources USING hnsw (embedding vector_cosine_ops);
