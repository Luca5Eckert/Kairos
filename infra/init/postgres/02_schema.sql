
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    username TEXT NOT NULL,
    email TEXT NOT NULL,
    password TEXT,
    role TEXT NOT NULL,
    email_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    confirmation_code_hash TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username_lower
    ON users (lower(username));

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower
    ON users (lower(email));

CREATE TABLE IF NOT EXISTS sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    author_id UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sources_author_id
    ON sources(author_id);

CREATE TABLE IF NOT EXISTS chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    embedding VECTOR(384),

    CONSTRAINT fk_chunks_source
        FOREIGN KEY (source_id)
        REFERENCES sources(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_chunks_source_index
        UNIQUE (source_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_chunks_source_id
    ON chunks(source_id);

CREATE INDEX IF NOT EXISTS idx_chunks_embedding_hnsw
    ON chunks
    USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS triples (
    key TEXT PRIMARY KEY,
    subject TEXT,
    predicate TEXT,
    object TEXT,
    embedding VECTOR(384) NOT NULL,
    chunk_id UUID,

    CONSTRAINT fk_triples_chunk
        FOREIGN KEY (chunk_id)
        REFERENCES chunks(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_triples_chunk_id
    ON triples(chunk_id);

CREATE INDEX IF NOT EXISTS idx_triples_embedding_hnsw
    ON triples
    USING hnsw (embedding vector_cosine_ops);
