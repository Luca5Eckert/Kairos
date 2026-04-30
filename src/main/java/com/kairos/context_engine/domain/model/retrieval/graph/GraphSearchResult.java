package com.kairos.context_engine.domain.model.retrieval.graph;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;

import java.util.List;

public record GraphSearchResult(
        List<ScoredPassage> scoredPassages,
        List<KnowledgeTriple> activatedTriples
) {
    public GraphSearchResult {
        if (scoredPassages == null) {
            throw new IllegalArgumentException("Graph search scored passages cannot be null");
        }
        if (activatedTriples == null) {
            throw new IllegalArgumentException("Graph search activated triples cannot be null");
        }

        scoredPassages = List.copyOf(scoredPassages);
        activatedTriples = List.copyOf(activatedTriples);
    }

    public static GraphSearchResult empty() {
        return new GraphSearchResult(List.of(), List.of());
    }
}
