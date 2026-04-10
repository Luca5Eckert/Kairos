
CREATE TABLE sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT NOT NULL
);

CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding VECTOR(384) NOT NULL,

    CONSTRAINT fk_chunks_source
        FOREIGN KEY (source_id)
        REFERENCES sources(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_chunks_source_index
        UNIQUE (source_id, chunk_index)
);

CREATE INDEX idx_chunks_source_id
    ON chunks(source_id);

CREATE INDEX idx_chunks_embedding_hnsw
    ON chunks
    USING hnsw (embedding vector_cosine_ops);