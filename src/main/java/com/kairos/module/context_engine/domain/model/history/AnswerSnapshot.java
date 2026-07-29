package com.kairos.module.context_engine.domain.model.history;

import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.model.retrieval.seed.PassageSeedTarget;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

/** Immutable, self-contained persistence representation of a retrieval execution. */
public record AnswerSnapshot(
        String retrievalVersion,
        RetrievalParameters parameters,
        List<GraphSeedSnapshot> seeds,
        List<RankedPassageSnapshot> rankedPassages,
        List<ActivatedTripleSnapshot> activatedTriples
) {
    public static final int SCHEMA_VERSION = 1;

    public AnswerSnapshot {
        if (retrievalVersion == null || retrievalVersion.isBlank() || parameters == null
                || seeds == null || rankedPassages == null || activatedTriples == null) {
            throw new IllegalArgumentException("Answer snapshot fields are required");
        }
        seeds = List.copyOf(seeds);
        rankedPassages = List.copyOf(rankedPassages);
        activatedTriples = List.copyOf(activatedTriples);
    }

    public static AnswerSnapshot from(
            RetrievalParameters parameters,
            List<GraphSeed> seeds,
            List<RankedChunk> rankedChunks,
            Map<UUID, Double> denseScores,
            List<KnowledgeTriple> activatedTriples
    ) {
        return new AnswerSnapshot(
                "hipporag-2",
                parameters,
                IntStream.range(0, seeds.size()).mapToObj(index -> GraphSeedSnapshot.from(seeds.get(index), index)).toList(),
                rankedChunks.stream().map(chunk -> RankedPassageSnapshot.from(chunk, denseScores.get(chunk.chunk().getId()))).toList(),
                IntStream.range(0, activatedTriples.size())
                        .mapToObj(index -> ActivatedTripleSnapshot.from(activatedTriples.get(index), index)).toList()
        );
    }

    public record RetrievalParameters(int semanticAnchorLimit, int tripleCandidateLimit, int recognitionSeedLimit,
                                      int graphPassageLimit, double seedMinScore, double seedRelativeThreshold) { }

    public record GraphSeedSnapshot(String type, UUID chunkId, String concept, double weight, int order) {
        static GraphSeedSnapshot from(GraphSeed seed, int order) {
            UUID chunkId = seed.target() instanceof PassageSeedTarget target ? target.chunkId() : null;
            String concept = seed.target() instanceof ConceptSeedTarget target ? target.concept().name() : null;
            return new GraphSeedSnapshot(seed.type().name(), chunkId, concept, seed.weight(), order);
        }
    }

    public record RankedPassageSnapshot(UUID chunkId, UUID sourceId, String content, int rank, double finalScore,
                                        Double denseScore, double graphScore, String retrievalSource) {
        static RankedPassageSnapshot from(RankedChunk chunk, Double denseScore) {
            return new RankedPassageSnapshot(chunk.chunk().getId(), chunk.chunk().getSource().getId(),
                    chunk.chunk().getContent(), chunk.rank(), chunk.score(), denseScore, chunk.score(), chunk.source().name());
        }
    }

    public record ActivatedTripleSnapshot(String tripleKey, UUID chunkId, String subject, String predicate, String object,
                                          Double activationScore, double structuralWeight, int order) {
        static ActivatedTripleSnapshot from(KnowledgeTriple triple, int order) {
            return new ActivatedTripleSnapshot(null, triple.passage() == null ? null : triple.passage().chunkId(),
                    triple.subject().name(), triple.predicate(), triple.object().name(), null, triple.weight(), order);
        }
    }
}
