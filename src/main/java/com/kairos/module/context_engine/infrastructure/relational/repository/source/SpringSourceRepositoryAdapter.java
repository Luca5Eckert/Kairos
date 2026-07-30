package com.kairos.module.context_engine.infrastructure.relational.repository.source;

import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.module.context_engine.infrastructure.relational.entity.SourceEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SpringSourceRepositoryAdapter implements SourceRepository {

    private final JpaSourceRepository jpaSourceRepository;

    public SpringSourceRepositoryAdapter(JpaSourceRepository jpaSourceRepository) {
        this.jpaSourceRepository = jpaSourceRepository;
    }

    @Override
    public void save(Source source) {
        var entity = SourceEntity.of(source);
        jpaSourceRepository.save(entity);
    }

    @Override
    public Optional<Source> findById(UUID id) {
        var entity = jpaSourceRepository.findById(id);
        return entity.map(SourceEntity::toDomain);
    }

    @Override
    public Optional<Source> findByIdAndAuthorIdForUpdate(UUID id, UUID authorId) {
        return jpaSourceRepository.findByIdAndAuthorId(id, authorId)
                .map(SourceEntity::toDomain);
    }

    @Override
    public Optional<Source> findByAuthorIdAndTitleAndContent(UUID authorId, String title, String content) {
        return jpaSourceRepository.findFirstByAuthorIdAndTitleAndContent(authorId, title, content)
                .map(SourceEntity::toDomain);
    }

    @Override
    public List<Source> findAll(int k) {
        var pageable = PageRequest.of(0, k);

        var entities = jpaSourceRepository.findAll(pageable);

        return entities.stream()
                .map(SourceEntity::toDomain)
                .toList();
    }

    @Override
    public List<SourceProgressUpload> findAllSourcesProgressByAuthorId(UUID authorId) {
        var sourceProgressEntities = jpaSourceRepository.findAllSourcesProgressByAuthorId(authorId);

        return sourceProgressEntities.stream()
                .map(projection -> new SourceProgressUpload(
                        new Source(projection.getId(), projection.getTitle(), projection.getContent(), projection.getAuthorId()),
                        projection.getTotalChunks(),
                        projection.getPendingChunks(),
                        projection.getProcessingChunks(),
                        projection.getCompletedChunks(),
                        projection.getFailedChunks()
                ))
                .toList();
    }
}
