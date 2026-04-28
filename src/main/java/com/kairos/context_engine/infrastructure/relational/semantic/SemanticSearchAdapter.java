package com.kairos.context_engine.infrastructure.relational.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * PostgreSQL and pgvector adapter for dense semantic retrieval.
 * Provides implementation for vector similarity searches and payload hydration.
 */
@Component
@RequiredArgsConstructor
public class SemanticSearchAdapter implements SemanticSearch {

    private final JpaChunkRepository jpaChunkRepository;

    /**
     * Performs a nearest-neighbor vector search over text chunks to identify semantic anchors.
     * Utilizes the pgvector cosine distance operator ({@code <=>}) for optimal alignment with
     * the all-MiniLM-L6-v2 embedding space.
     *
     * @param queryVector The embedding vector of the user's search query.
     * @param k           The maximum number of anchor chunks to retrieve.
     * @return A list of the top-k most semantically similar {@link Chunk}s.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Chunk> findTopK(float[] queryVector, int k) {
        List<ChunkEntity> chunks = jpaChunkRepository.findTopKByEmbedding(queryVector, k);
        return chunks.stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }

    /**
     * Hydrates chunk domain models by their unique identifiers.
     * @param chunkIds A list of UUIDs representing the chunks to be fetched.
     * @return A list of {@link Chunk} objects corresponding to the provided IDs.
     * The order of the returned list is not guaranteed to match the input list.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Chunk> findChunks(List<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }

        List<ChunkEntity> chunks = jpaChunkRepository.findAllById(chunkIds);
        return chunks.stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }

}
