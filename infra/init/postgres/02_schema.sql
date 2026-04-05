CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    embedding vector(384) NOT NULL
);

CREATE INDEX ON sources USING ivfflat (embedding vector_cosine_ops);