package com.kairos.infrastructure.persistence.repository.concept;

import com.kairos.domain.model.Concept;
import com.kairos.infrastructure.persistence.entity.ConceptNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface Neo4jConceptRepository extends Neo4jRepository<ConceptNode, UUID> {
}
