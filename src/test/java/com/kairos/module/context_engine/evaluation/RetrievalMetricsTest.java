package com.kairos.module.context_engine.evaluation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalMetricsTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID D = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    void scoresRecallAndReciprocalRankAgainstExplicitJudgments() {
        Map<UUID, Integer> judgments = new LinkedHashMap<>();
        judgments.put(B, 2);
        judgments.put(D, 1);

        var scores = RetrievalMetrics.score(List.of(A, B, C, D), judgments);

        assertThat(scores.recallAt5()).isEqualTo(1.0d);
        assertThat(scores.recallAt10()).isEqualTo(1.0d);
        assertThat(scores.mrrAt10()).isEqualTo(0.5d);
    }

    @Test
    void givesPerfectNdcgOnlyWhenGradedResultsAreIdeallyOrdered() {
        Map<UUID, Integer> judgments = new LinkedHashMap<>();
        judgments.put(B, 2);
        judgments.put(D, 1);

        double ideal = RetrievalMetrics.ndcgAt(List.of(B, D, A, C), judgments, 10);
        double reversed = RetrievalMetrics.ndcgAt(List.of(D, B, A, C), judgments, 10);

        assertThat(ideal).isEqualTo(1.0d);
        assertThat(reversed).isBetween(0.0d, 1.0d);
    }

    @Test
    void returnsZeroWhenNoRelevantDocumentExists() {
        var scores = RetrievalMetrics.score(List.of(A, B), Map.of());

        assertThat(scores.recallAt5()).isZero();
        assertThat(scores.recallAt10()).isZero();
        assertThat(scores.mrrAt10()).isZero();
        assertThat(scores.ndcgAt10()).isZero();
    }
}
