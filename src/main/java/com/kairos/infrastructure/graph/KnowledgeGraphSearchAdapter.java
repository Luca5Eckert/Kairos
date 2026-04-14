package com.kairos.infrastructure.graph;

import com.kairos.domain.graph.KnowledgeGraphSearch;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Neo4j adapter for HippoRAG 2 graph-augmented retrieval.
 *
 * <p>Implements {@link KnowledgeGraphSearch} by orchestrating three GDS lifecycle
 * operations against {@link KnowledgeGraphGdsExecutor}:
 * <ol>
 *   <li>{@code projectPhraseGraph} — creates a named in-memory GDS projection.</li>
 *   <li>{@code runPPRExpansion} — executes Personalized PageRank seeded from the
 *       anchor passages and returns the top-scored knowledge triples.</li>
 *   <li>{@code dropProjectedGraph} — releases the in-memory projection.</li>
 * </ol>
 *
 * <p>Steps 1 and 3 are separated so that the drop can be placed in a
 * {@code finally} block, guaranteeing cleanup even when PPR throws. GDS projections
 * are not transaction-scoped: a Neo4j rollback will not remove a projection that
 * was already created.
 *
 * <p>A {@link #cleanupOrphanProjections()} job handles projections that survived
 * abrupt JVM termination or pod eviction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGraphSearchAdapter implements KnowledgeGraphSearch {

    private static final int    MAX_ITERATIONS = 20;
    private static final double DAMPING_FACTOR = 0.85;
    private static final int    PASSAGE_LIMIT  = 10;

    private static final String GRAPH_NAME_PREFIX = "hipporag-";

    private final KnowledgeGraphGdsExecutor gdsExecutor;

    /**
     * Expands knowledge from a set of semantic anchor chunks using PPR on the
     * Neo4j knowledge graph.
     *
     * <p>Returns an empty list immediately when {@code semanticAnchors} is
     * {@code null} or empty, without creating any GDS projection.
     *
     * <p>A unique graph name is generated per invocation to prevent name collisions
     * under concurrent requests. The projection is always dropped in the
     * {@code finally} block regardless of whether PPR succeeded or failed.
     *
     * <p>Rows returned by the repository with a {@code null} {@code chunkId} are
     * filtered out with a warning — they represent passages that were scored by PPR
     * but whose identifier could not be resolved, and cannot be mapped to a
     * {@link KnowledgeTriple}.
     *
     * @param semanticAnchors chunks selected as PPR seed sources; must not be {@code null}
     * @return knowledge triples ranked by passage score; never {@code null}
     */
    @Override
    public List<KnowledgeTriple> expandKnowledge(List<Chunk> semanticAnchors) {
        if (semanticAnchors == null || semanticAnchors.isEmpty()) {
            return List.of();
        }

        List<String> anchorIds = semanticAnchors.stream()
                .map(chunk -> chunk.getId().toString())
                .toList();

        log.info("Expanding knowledge graph | anchors={} ids={}", anchorIds.size(), anchorIds);

        String graphName = GRAPH_NAME_PREFIX + UUID.randomUUID();

        try {
            gdsExecutor.projectPhraseGraph(graphName);

            var results = gdsExecutor.runPPRExpansion(
                    graphName,
                    anchorIds,
                    MAX_ITERATIONS,
                    DAMPING_FACTOR,
                    PASSAGE_LIMIT
            );

            log.info("Graph expansion results | total={}", results.size());

            return results.stream()
                    .peek(result -> log.debug(
                            "Graph triple | subject='{}' predicate='{}' object='{}' chunkId='{}'",
                            result.subject(), result.predicate(), result.object(), result.chunkId()
                    ))
                    .filter(result -> {
                        if (result.chunkId() == null) {
                            log.warn("Skipping triple — null chunkId | subject='{}' predicate='{}' object='{}'",
                                    result.subject(), result.predicate(), result.object());
                            return false;
                        }
                        return true;
                    })
                    .map(result -> KnowledgeTriple.create(
                            result.subject(),
                            result.predicate(),
                            result.object(),
                            UUID.fromString(result.chunkId())
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Graph expansion failed | graphName='{}' anchors={}", graphName, anchorIds, e);
            throw e;

        } finally {
            dropSafely(graphName);
        }
    }

    /**
     * Drops all GDS projections prefixed with {@code "hipporag-"} that were left
     * behind by abrupt JVM termination, pod eviction, or any other failure that
     * prevented the {@code finally} block inside {@link #expandKnowledge} from running.
     *
     * <p>The interval is configurable via
     * {@code kairos.graph.orphan-cleanup-interval-ms} (default: 10 minutes).
     * Tune this value based on the expected call rate and GDS heap budget.
     */
    @Scheduled(fixedRateString = "${kairos.graph.orphan-cleanup-interval-ms:600000}")
    public void cleanupOrphanProjections() {
        try {
            List<String> removed = gdsExecutor.dropOrphanProjections();
            if (!removed.isEmpty()) {
                log.warn("Orphan GDS cleanup: removed {} projection(s): {}", removed.size(), removed);
            }
        } catch (Exception e) {
            log.error("Orphan GDS cleanup job failed.", e);
        }
    }

    /**
     * Drops the named GDS projection, swallowing any exception so it never masks
     * an error already propagating from the {@code try} block.
     *
     * @param graphName name of the projection to drop
     */
    private void dropSafely(String graphName) {
        try {
            gdsExecutor.dropProjectedGraph(graphName);
            log.debug("GDS projection '{}' dropped successfully.", graphName);
        } catch (Exception e) {
            log.warn("Failed to drop GDS projection '{}'. " +
                    "It will be collected by the next cleanupOrphanProjections() run.", graphName, e);
        }
    }
}
