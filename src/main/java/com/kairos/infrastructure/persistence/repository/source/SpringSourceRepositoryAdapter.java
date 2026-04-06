package com.kairos.infrastructure.persistence.repository.source;

import com.kairos.domain.model.Source;
import com.kairos.domain.port.SourceRepository;
import com.kairos.infrastructure.persistence.entity.SourceEntity;
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
