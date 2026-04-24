package com.kairos.context_engine.infrastructure.persistence.repository.relation.chunk;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.port.ChunkRepository;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.ChunkEntity;
import org.springframework.stereotype.Repository;

@Repository
public class SpringChunkRepositoryAdapter implements ChunkRepository {

    private final JpaChunkRepository chunkRepository;

    public SpringChunkRepositoryAdapter(JpaChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Override
    public Chunk save(Chunk chunk) {
        var entity = ChunkEntity.create(chunk);

        var savedEntity = chunkRepository.save(entity);

        return savedEntity.toDomain();
    }
}
