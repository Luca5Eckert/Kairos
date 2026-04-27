package com.kairos.context_engine.infrastructure.persistence.repository.relation.chunk;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.ChunkEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

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

    @Override
    public List<Chunk> findAllBySourceId(UUID id) {
        return chunkRepository.findAllBySource_Id(id).stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }
}
