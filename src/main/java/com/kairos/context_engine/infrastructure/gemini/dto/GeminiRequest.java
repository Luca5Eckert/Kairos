package com.kairos.context_engine.infrastructure.gemini.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kairos.context_engine.infrastructure.gemini.config.GeminiProperties;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {

    public static GeminiRequest of(String prompt, GeminiProperties properties) {
        return new GeminiRequest(
                List.of(new Content(
                        List.of(new Part(prompt))
                )),
                new GenerationConfig(
                        properties.temperature(),
                        properties.maxOutputTokens(),
                        "application/json"
                )
        );
    }

    public record Content(List<Part> parts) {}
    public record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(
            Double temperature,
            Integer maxOutputTokens,
            String responseMimeType
    ) {}
}
