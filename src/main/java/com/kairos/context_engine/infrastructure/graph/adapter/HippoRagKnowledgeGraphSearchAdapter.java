package com.kairos.context_engine.infrastructure.graph.adapter;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.context_engine.domain.model.retrieval.seed.PassageSeedTarget;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor.WeightedConceptSeed;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor.WeightedPassageSeed;
import com.kairos.context_engine.infrastructure.graph.repository.projection.GraphExpansionResult;
import com.kairos.context_engine.infrastructure.graph.repository.projection.PassageScoringResult;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HippoRagKnowledgeGraphSearchAdapter implements KnowledgeGraphSearch {

    private static final String GRAPH_NAME_PREFIX = "hipporag-";

    private final KnowledgeGraphGdsExecutor executor;
    private final int    maxIterations;
    private final double dampingFactor;
    private final double scoreThreshold;

    public HippoRagKnowledgeGraphSearchAdapter(
            KnowledgeGraphGdsExecutor executor,
            @Value("${hipporag.ppr.max-iterations:20}")    int    maxIterations,
            @Value("${hipporag.ppr.damping-factor:0.85}")  double dampingFactor,
            @Value("${hipporag.ppr.score-threshold:0.001}") double scoreThreshold
    ) {
        this.executor       = executor;
        this.maxIterations  = maxIterations;
        this.dampingFactor  = dampingFactor;
        this.scoreThreshold = scoreThreshold;
    }


    @Override
    public GraphSearchResult expandKnowledge(GraphSearchRequest request) {
        if (request == null || request.seeds().isEmpty()) {
            return GraphSearchResult.empty();
        }

        WeightedGraphSeeds weightedSeeds = toWeightedSeeds(request);

        String graphName = GRAPH_NAME_PREFIX + UUID.randomUUID();
        boolean projected = false;

        try {
            UUID userId = request.userId();
            executor.projectKnowledgeGraph(graphName, userId);
            projected = true;

            List<PassageScoringResult> passageScores = executor.runPPRPassageScores(
                    graphName, weightedSeeds.passages(), weightedSeeds.concepts(),
                    maxIterations, dampingFactor, scoreThreshold, request.limit(), userId);

            List<GraphExpansionResult> tripleRows = executor.runPPRActivatedTriples(
                    graphName, weightedSeeds.passages(), weightedSeeds.concepts(),
                    maxIterations, dampingFactor, scoreThreshold, userId);

            if (passageScores.isEmpty() && tripleRows.isEmpty()) {
                return GraphSearchResult.empty();
            }

            List<ScoredPassage> scoredPassages = toScoredPassages(passageScores);

            return new GraphSearchResult(
                    scoredPassages,
                    toActivatedTriples(tripleRows, scoredPassages)
            );

        } catch (ClientException e) {
            if (isMissingGdsProcedure(e)) {
                log.warn("Neo4j Graph Data Science procedures are unavailable. Returning empty graph expansion.");
                return GraphSearchResult.empty();
            }
            throw e;
        } finally {
            if (projected) {
                dropSafely(graphName);
            }
        }
    }

    @Scheduled(fixedRateString = "${kairos.graph.orphan-cleanup-interval-ms:600000}")
    public void cleanupOrphanProjections() {
        try {
            List<String> removed = executor.dropOrphanProjections();
            if (!removed.isEmpty()) {
                log.warn("Orphan GDS cleanup removed {} projection(s): {}", removed.size(), removed);
            }
        } catch (ClientException e) {
            if (isMissingGdsProcedure(e)) {
                log.warn("Neo4j Graph Data Science procedures are unavailable. Skipping orphan projection cleanup.");
                return;
            }
            log.error("Orphan GDS cleanup job failed.", e);
        } catch (Exception e) {
            log.error("Orphan GDS cleanup job failed.", e);
        }
    }

    /**
     * Cypher already grouped, sorted and limited — this is a pure 1-to-1 mapping.
     */
    private List<ScoredPassage> toScoredPassages(List<PassageScoringResult> scores) {
        return scores.stream()
                .filter(r -> r.chunkId() != null)
                .map(r -> new ScoredPassage(
                        UUID.fromString(r.chunkId()),
                        r.score()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Deduplicates graph seeds by target, keeping the strongest bias when the
     * same passage or concept appears more than once.
     */
    private WeightedGraphSeeds toWeightedSeeds(GraphSearchRequest request) {
        Map<String, Double> passageWeights = new LinkedHashMap<>();
        Map<String, Double> conceptWeights = new LinkedHashMap<>();

        for (var seed : request.seeds()) {
            switch (seed.target()) {
                case PassageSeedTarget t -> passageWeights.merge(t.chunkId().toString(), seed.weight(), Math::max);
                case ConceptSeedTarget t -> conceptWeights.merge(t.concept().name(), seed.weight(), Math::max);
            }
        }

        List<WeightedPassageSeed> passages = passageWeights.entrySet().stream()
                .map(entry -> new WeightedPassageSeed(entry.getKey(), entry.getValue()))
                .toList();
        List<WeightedConceptSeed> concepts = conceptWeights.entrySet().stream()
                .map(entry -> new WeightedConceptSeed(entry.getKey(), entry.getValue()))
                .toList();

        return new WeightedGraphSeeds(passages, concepts);
    }

    private List<KnowledgeTriple> toActivatedTriples(List<GraphExpansionResult> rows, List<ScoredPassage> scoredPassages) {
        record TripleKey(String subject, String predicate, String object, String chunkId) {}

        Set<String> allowedChunkIds = scoredPassages.stream()
                .map(scoredPassage -> scoredPassage.chunkId().toString())
                .collect(Collectors.toSet());

        Map<TripleKey, KnowledgeTriple> triplesByKey = rows.stream()
                .filter(r -> r.subject() != null
                        && r.predicate() != null
                        && r.object() != null
                        && r.chunkId() != null
                        && allowedChunkIds.contains(r.chunkId()))
                .collect(Collectors.toMap(
                        r -> new TripleKey(r.subject(), r.predicate(), r.object(), r.chunkId()),
                        r -> KnowledgeTriple.create(
                                r.subject(), r.predicate(), r.object(),
                                Passage.fromChunkId(UUID.fromString(r.chunkId())),
                                r.weight()
                        ),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ));

        return new ArrayList<>(triplesByKey.values());
    }

    private void dropSafely(String graphName) {
        try {
            executor.dropProjectedGraph(graphName);
        } catch (Exception e) {
            log.warn("Failed to drop GDS projection '{}'. It will be collected by orphan cleanup.", graphName, e);
        }
    }

    private boolean isMissingGdsProcedure(ClientException e) {
        String message = e.getMessage();
        return message != null
                && message.contains("There is no procedure with the name `gds.");
    }

    private record WeightedGraphSeeds(
            List<WeightedPassageSeed> passages,
            List<WeightedConceptSeed> concepts
    ) {}

}
