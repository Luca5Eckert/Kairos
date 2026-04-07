package com.kairos.infrastructure.persistence.repository.relation.chunk;

import com.kairos.domain.model.Chunk;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.infrastructure.persistence.entity.relation.ChunkEntity;
import org.springframework.stereotype.Repository;

@Repository
public class SpringChunkRepositoryAdapter implements ChunkRepository {

    private final JpaChunkRepository chuckRepository;

    public SpringChunkRepositoryAdapter(JpaChunkRepository chuckRepository) {
        this.chuckRepository = chuckRepository;
    }

    @Override
    public void save(Chunk chunk) {
        var entity = ChunkEntity.create(chunk);

        chuckRepository.save(entity);
    }
}
