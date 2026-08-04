package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnswerHistoryResponse(
        UUID answerId,
        UUID questionId,
        int schemaVersion,
        Instant createdAt,
        AnswerSnapshotResponse snapshot
) {
    public static AnswerHistoryResponse of(Answer answer) {
        return new AnswerHistoryResponse(answer.id(), answer.questionId(), answer.schemaVersion(), answer.createdAt(),
                AnswerSnapshotResponse.of(answer.snapshot()));
    }

    public record AnswerSnapshotResponse(
            String retrievalVersion,
            RetrievalParametersResponse parameters,
            List<GraphSeedResponse> seeds,
            List<RankedPassageResponse> rankedPassages,
            List<ActivatedTripleResponse> activatedTriples
    ) {
        static AnswerSnapshotResponse of(AnswerSnapshot snapshot) {
            return new AnswerSnapshotResponse(snapshot.retrievalVersion(), RetrievalParametersResponse.of(snapshot.parameters()),
                    snapshot.seeds().stream().map(GraphSeedResponse::of).toList(),
                    snapshot.rankedPassages().stream().map(RankedPassageResponse::of).toList(),
                    snapshot.activatedTriples().stream().map(ActivatedTripleResponse::of).toList());
        }
    }

    public record RetrievalParametersResponse(
            int semanticAnchorLimit,
            int tripleCandidateLimit,
            int recognitionSeedLimit,
            int graphPassageLimit,
            double seedMinScore,
            double seedRelativeThreshold
    ) {
        static RetrievalParametersResponse of(AnswerSnapshot.RetrievalParameters parameters) {
            return new RetrievalParametersResponse(parameters.semanticAnchorLimit(), parameters.tripleCandidateLimit(),
                    parameters.recognitionSeedLimit(), parameters.graphPassageLimit(), parameters.seedMinScore(),
                    parameters.seedRelativeThreshold());
        }
    }

    public record GraphSeedResponse(String type, UUID chunkId, String concept, double weight, int order) {
        static GraphSeedResponse of(AnswerSnapshot.GraphSeedSnapshot seed) {
            return new GraphSeedResponse(seed.type(), seed.chunkId(), seed.concept(), seed.weight(), seed.order());
        }
    }

    public record RankedPassageResponse(
            UUID chunkId,
            UUID sourceId,
            String content,
            int rank,
            double finalScore,
            Double denseScore,
            double graphScore,
            String retrievalSource
    ) {
        static RankedPassageResponse of(AnswerSnapshot.RankedPassageSnapshot passage) {
            return new RankedPassageResponse(passage.chunkId(), passage.sourceId(), passage.content(), passage.rank(),
                    passage.finalScore(), passage.denseScore(), passage.graphScore(), passage.retrievalSource());
        }
    }

    public record ActivatedTripleResponse(
            String tripleKey,
            UUID chunkId,
            String subject,
            String predicate,
            String object,
            Double activationScore,
            double structuralWeight,
            int order
    ) {
        static ActivatedTripleResponse of(AnswerSnapshot.ActivatedTripleSnapshot triple) {
            return new ActivatedTripleResponse(triple.tripleKey(), triple.chunkId(), triple.subject(), triple.predicate(),
                    triple.object(), triple.activationScore(), triple.structuralWeight(), triple.order());
        }
    }
}
