package com.kairos.module.context_engine.infrastructure.relational.repository.chunk;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.infrastructure.relational.entity.ChunkEntity;
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

    @Override
    public List<Chunk> findAllNotProcessedBySourceId(UUID sourceId) {
        return findAllBySourceIdAndStatus(sourceId, ChunkProcessingStatus.PENDING);
    }

    @Override
    public List<Chunk> findAllBySourceIdAndStatus(UUID sourceId, ChunkProcessingStatus status) {
        return chunkRepository.findAllBySource_IdAndProcessingStatus(sourceId, status).stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }

    @Override
    public List<Chunk> findAllByIds(List<UUID> ids) {
        return chunkRepository.findAllById(ids).stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }

}
