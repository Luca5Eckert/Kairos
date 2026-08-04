package com.kairos.module.context_engine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kairos.history")
public record HistoryProperties(int defaultPageSize, int maxPageSize) {
    public HistoryProperties {
        defaultPageSize = positiveOrDefault(defaultPageSize, 20);
        maxPageSize = positiveOrDefault(maxPageSize, 100);
        if (defaultPageSize > maxPageSize) {
            defaultPageSize = maxPageSize;
        }
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}
