package com.kairos.context_engine.infrastructure.semantic;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.semantic.SemanticSearchPort;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.ChunkEntity;
import com.kairos.context_engine.infrastructure.persistence.repository.relation.chunk.JpaChunkRepository;
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
public class SemanticSearchAdapter implements SemanticSearchPort {

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
                .map(entity -> Chunk.create(
                        entity.getId(),
                        entity.getSource().toDomain(),
                        entity.getContent(),
                        entity.getIndex(),
                        entity.getEmbedding()
                ))
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
                .map(entity -> Chunk.create(
                        entity.getId(),
                        entity.getSource().toDomain(),
                        entity.getContent(),
                        entity.getIndex(),
                        entity.getEmbedding()
                ))
                .toList();
    }

}
