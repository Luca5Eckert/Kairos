package com.kairos.infrastructure.graph;

import com.kairos.domain.graph.KnowledgeGraphSearch;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.infrastructure.persistence.repository.graph.Neo4jPassageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Neo4j adapter for orchestrating graph-augmented retrieval via SDN Repositories.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeGraphSearchAdapter implements KnowledgeGraphSearch {

    private final Neo4jPassageNodeRepository passageNodeRepository;

    @Override
    public List<KnowledgeTriple> expandKnowledge(List<Chunk> semanticAnchors) {
        if (semanticAnchors == null || semanticAnchors.isEmpty()) {
            return List.of();
        }

        List<String> anchorIds = semanticAnchors.stream()
                .map(chunk -> chunk.getId().toString())
                .toList();

        var results = passageNodeRepository.expandKnowledgeFromAnchors(anchorIds);

        return results.stream()
                .map(result -> KnowledgeTriple.create(
                        result.subject(),
                        result.predicate(),
                        result.object(),
                        UUID.fromString(result.chunkId())
                ))
                .toList();
    }
}