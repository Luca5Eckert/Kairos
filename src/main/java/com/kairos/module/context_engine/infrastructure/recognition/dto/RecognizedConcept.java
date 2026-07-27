package com.kairos.module.context_engine.infrastructure.recognition.dto;

public record RecognizedConcept(
        String tripleKey,
        String concept,
        double confidence
) {
}
