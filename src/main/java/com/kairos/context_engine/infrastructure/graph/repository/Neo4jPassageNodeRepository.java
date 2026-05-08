package com.kairos.context_engine.infrastructure.graph.repository;

import com.kairos.context_engine.infrastructure.graph.entity.PassageNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Neo4jPassageNodeRepository extends Neo4jRepository<PassageNode, UUID> {
}
