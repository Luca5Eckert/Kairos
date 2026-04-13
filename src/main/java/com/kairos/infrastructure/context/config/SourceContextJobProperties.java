package com.kairos.infrastructure.context.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "source-context-job")
public record SourceContextJobProperties(
        int maxAttempts,
        int batchSize,
        Duration initialRetryDelay,
        double retryMultiplier,
        Duration maxRetryDelay
) {
}
