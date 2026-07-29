package com.kairos.module.context_engine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kairos.retrieval")
public record RetrievalProperties(
        int semanticAnchorLimit,
        int graphPassageLimit,
        int tripleCandidateLimit,
        int recognitionSeedLimit,
        double seedMinScore,
        double seedRelativeThreshold
) {
    public RetrievalProperties {
        semanticAnchorLimit = positiveOrDefault(semanticAnchorLimit, 10);
        graphPassageLimit = positiveOrDefault(graphPassageLimit, 20);
        tripleCandidateLimit = positiveOrDefault(tripleCandidateLimit, 30);
        recognitionSeedLimit = positiveOrDefault(recognitionSeedLimit, 10);
        seedMinScore = positiveOrDefault(seedMinScore, 0.45d);
        seedRelativeThreshold = positiveOrDefault(seedRelativeThreshold, 0.85d);
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private static double positiveOrDefault(double value, double defaultValue) {
        return Double.isFinite(value) && value > 0 ? value : defaultValue;
    }
}
