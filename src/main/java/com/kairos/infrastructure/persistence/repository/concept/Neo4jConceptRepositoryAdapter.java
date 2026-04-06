package com.kairos.infrastructure.persistence.repository.concept;

import com.kairos.domain.model.Concept;
import com.kairos.domain.port.ConceptRepository;
import com.kairos.infrastructure.persistence.entity.ConceptNode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class Neo4jConceptRepositoryAdapter implements ConceptRepository {

    private final Neo4jConceptRepository neo4jConceptRepository;

    public Neo4jConceptRepositoryAdapter(Neo4jConceptRepository neo4jConceptRepository) {
        this.neo4jConceptRepository = neo4jConceptRepository;
    }

    @Override
    public void save(Concept concept) {
        ConceptNode entity = ConceptNode.of(concept);
        neo4jConceptRepository.save(entity);
    }

    @Override
    public Optional<Concept> findById(UUID id) {
        Optional<ConceptNode> entity = neo4jConceptRepository.findById(id);
        return entity.map(ConceptNode::toDomain);
    }

    @Override
    public List<Concept> findAll(int k) {
        PageRequest pageable = PageRequest.of(0, k);

        var entities = neo4jConceptRepository.findAll(pageable);

        return entities.stream()
                .map(ConceptNode::toDomain)
                .collect(Collectors.toList());
    }
}