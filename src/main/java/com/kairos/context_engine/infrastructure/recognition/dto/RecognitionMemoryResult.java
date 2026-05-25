package com.kairos.context_engine.infrastructure.recognition.dto;

import java.util.List;

public record RecognitionMemoryResult(
        List<RecognizedConcept> concepts
) {
}
