package com.kairos.context_engine.infrastructure.persistence.repository.relation.source;

import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.port.repository.SourceRepository;
import com.kairos.context_engine.infrastructure.persistence.entity.relation.SourceEntity;
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
    public List<Source> findAll(int k) {
        var pageable = PageRequest.of(0, k);

        var entities = jpaSourceRepository.findAll(pageable);

        return entities.stream()
                .map(SourceEntity::toDomain)
                .toList();
    }

}
