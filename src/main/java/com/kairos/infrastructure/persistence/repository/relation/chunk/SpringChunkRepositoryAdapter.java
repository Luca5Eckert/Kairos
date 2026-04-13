package com.kairos.infrastructure.persistence.repository.relation.chunk;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.infrastructure.persistence.entity.relation.ChunkEntity;
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
    public List<Chunk> findBySourceId(UUID sourceId) {
        return chunkRepository.findBySourceIdOrderByIndexAsc(sourceId).stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }
}
