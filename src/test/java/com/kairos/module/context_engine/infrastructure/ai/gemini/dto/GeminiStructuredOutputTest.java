package com.kairos.module.context_engine.infrastructure.ai.gemini.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.module.context_engine.infrastructure.recognition.dto.RecognitionMemoryResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiStructuredOutputTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsValidExtractionAndEmptyResponse() throws Exception {
        TripleExtractionResult result = mapper.readValue("""
                {"triples":[{"subject":"spring ai","predicate":"USES","object":"gemini","weight":0.8}]}
                """, TripleExtractionResult.class);

        assertThat(result.triples()).containsExactly(new ExtractedTriple("spring ai", "USES", "gemini", 0.8));
        assertThat(mapper.readValue("{\"triples\":[]}", TripleExtractionResult.class).triples()).isEmpty();
    }

    @Test
    void rejectsMalformedStructuredResponses() {
        assertThatThrownBy(() -> mapper.readValue("{\"triples\":[", TripleExtractionResult.class))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    @Test
    void mapsRecognitionConceptAndEmptyResponse() throws Exception {
        RecognitionMemoryResult result = mapper.readValue("""
                {"concepts":[{"tripleKey":"a-USES-b","concept":"a","confidence":0.9}]}
                """, RecognitionMemoryResult.class);

        assertThat(result.concepts()).hasSize(1);
        assertThat(result.concepts().getFirst().tripleKey()).isEqualTo("a-USES-b");
        assertThat(mapper.readValue("{\"concepts\":[]}", RecognitionMemoryResult.class).concepts()).isEmpty();
    }
}
