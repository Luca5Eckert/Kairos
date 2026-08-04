package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.history.Answer;
import java.time.Instant;
import java.util.UUID;

public record AnswerHistorySummaryResponse(
        UUID answerId,
        int schemaVersion,
        Instant createdAt,
        String retrievalVersion,
        AnswerHistoryResponse.RetrievalParametersResponse parameters,
        int seedsCount,
        int passagesCount,
        int triplesCount
) {
    public static AnswerHistorySummaryResponse of(Answer answer) {
        var snapshot = answer.snapshot();
        return new AnswerHistorySummaryResponse(answer.id(), answer.schemaVersion(), answer.createdAt(),
                snapshot.retrievalVersion(), AnswerHistoryResponse.RetrievalParametersResponse.of(snapshot.parameters()), snapshot.seeds().size(),
                snapshot.rankedPassages().size(), snapshot.activatedTriples().size());
    }
}
