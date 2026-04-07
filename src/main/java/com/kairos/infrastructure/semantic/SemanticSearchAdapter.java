package com.kairos.infrastructure.semantic;

import com.kairos.domain.model.Source;
import com.kairos.domain.semantic.SemanticSearchPort;
import com.kairos.infrastructure.persistence.entity.relation.SourceEntity;
import com.kairos.infrastructure.persistence.repository.relation.source.JpaSourceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SemanticSearchAdapter implements SemanticSearchPort {

    private final JpaSourceRepository jpaSourceRepository;

    public SemanticSearchAdapter(JpaSourceRepository jpaSourceRepository) {
        this.jpaSourceRepository = jpaSourceRepository;
    }

    /**
     * Search for the most relevant sources based on the provided embedding.
     * @param embedding The embedding vector to search for.
     * @param k The number of top relevant sources to return.
     * @return A list of the most relevant sources based on the provided embedding.
     */
    @Override
    public List<Source> search(float[] embedding, int k) {
        var sources = jpaSourceRepository.searchByEmbedding(embedding, k);

        return sources.stream()
                .map(SourceEntity::toDomain)
                .toList();
    }

}
