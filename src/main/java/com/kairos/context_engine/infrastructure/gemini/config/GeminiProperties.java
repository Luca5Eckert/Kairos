package com.kairos.context_engine.infrastructure.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        Double temperature,
        Integer maxOutputTokens,
        Integer timeoutSeconds,
        Retry retry
) {
    public record Retry(
            int maxRetries,
            Duration delay,
            double multiplier,
            Duration maxDelay
    ) {}
}