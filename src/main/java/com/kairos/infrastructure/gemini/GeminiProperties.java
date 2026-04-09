package com.kairos.infrastructure.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        Double temperature,
        Integer maxOutputTokens,
        Integer timeoutSeconds
) {}