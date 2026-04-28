package com.kairos.context_engine.infrastructure.relational.repository.triple;

import com.kairos.context_engine.domain.model.content.TripleExtracted;
import com.kairos.context_engine.domain.port.repository.TripleRepository;
import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SpringTripleRepositoryAdapter implements TripleRepository {

    private final JpaTripleRepository jpaTripleRepository;

    public SpringTripleRepositoryAdapter(JpaTripleRepository jpaTripleRepository) {
        this.jpaTripleRepository = jpaTripleRepository;
    }

    @Override
    public void saveAll(List<TripleExtracted> extractedTriples) {
        List<TripleEntity> entities = extractedTriples.stream()
                .map(TripleEntity::of)
                .toList();

        jpaTripleRepository.saveAll(entities);
    }
}
