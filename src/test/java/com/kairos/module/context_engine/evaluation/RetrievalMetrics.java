package com.kairos.module.context_engine.evaluation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    static Scores score(List<UUID> retrieved, Map<UUID, Integer> judgments) {
        return new Scores(
                recallAt(retrieved, judgments, 5),
                recallAt(retrieved, judgments, 10),
                reciprocalRankAt(retrieved, judgments, 10),
                ndcgAt(retrieved, judgments, 10)
        );
    }

    static double recallAt(List<UUID> retrieved, Map<UUID, Integer> judgments, int k) {
        long relevant = judgments.values().stream().filter(grade -> grade != null && grade > 0).count();
        if (relevant == 0) {
            return 0.0d;
        }

        long hits = retrieved.stream()
                .limit(k)
                .distinct()
                .filter(id -> judgments.getOrDefault(id, 0) > 0)
                .count();

        return (double) hits / relevant;
    }

    static double reciprocalRankAt(List<UUID> retrieved, Map<UUID, Integer> judgments, int k) {
        for (int index = 0; index < Math.min(k, retrieved.size()); index++) {
            if (judgments.getOrDefault(retrieved.get(index), 0) > 0) {
                return 1.0d / (index + 1);
            }
        }
        return 0.0d;
    }

    static double ndcgAt(List<UUID> retrieved, Map<UUID, Integer> judgments, int k) {
        double dcg = 0.0d;
        for (int index = 0; index < Math.min(k, retrieved.size()); index++) {
            int grade = judgments.getOrDefault(retrieved.get(index), 0);
            dcg += gain(grade) / log2(index + 2.0d);
        }

        List<Integer> idealGrades = judgments.values().stream()
                .filter(grade -> grade != null && grade > 0)
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .toList();

        double idealDcg = 0.0d;
        for (int index = 0; index < idealGrades.size(); index++) {
            idealDcg += gain(idealGrades.get(index)) / log2(index + 2.0d);
        }

        return idealDcg == 0.0d ? 0.0d : dcg / idealDcg;
    }

    private static double gain(int relevance) {
        return Math.pow(2.0d, relevance) - 1.0d;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0d);
    }

    record Scores(
            double recallAt5,
            double recallAt10,
            double mrrAt10,
            double ndcgAt10
    ) {
    }
}
