package com.kairos.infrastructure.gemini;

import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.infrastructure.gemini.GeminiResponseParser;
import com.kairos.context_engine.infrastructure.gemini.dto.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiResponseParserTest {

    private GeminiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new GeminiResponseParser(new ObjectMapper());
    }

    // --- Null / blank guard ---

    @Test
    void parseResponse_returnsEmpty_whenResponseIsNull() {
        assertThat(parser.parseResponse(null)).isEmpty();
    }

    @Test
    void parseResponse_returnsEmpty_whenTextIsNull() {
        GeminiResponse response = responseWithText(null);
        assertThat(parser.parseResponse(response)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void parseResponse_returnsEmpty_whenTextIsBlank(String text) {
        assertThat(parser.parseResponse(responseWithText(text))).isEmpty();
    }

    // --- Wrapped format: { "triples": [...] } ---

    @Test
    void parseResponse_parsesTriples_fromWrappedFormat() {
        String json = """
                {
                  "triples": [
                    { "subject": "spring boot", "predicate": "USES", "object": "embedded tomcat" }
                  ]
                }
                """;

        List<Triple> result = parser.parseResponse(responseWithText(json));

        assertThat(result).containsExactly(new Triple("spring boot", "USES", "embedded tomcat"));
    }

    @Test
    void parseResponse_parsesMultipleTriples_fromWrappedFormat() {
        String json = """
                {
                  "triples": [
                    { "subject": "spring boot", "predicate": "USES", "object": "embedded tomcat" },
                    { "subject": "tomcat", "predicate": "IMPLEMENTS", "object": "servlet container" },
                    { "subject": "spring boot", "predicate": "PROVIDES", "object": "auto-configuration" }
                  ]
                }
                """;

        List<Triple> result = parser.parseResponse(responseWithText(json));

        assertThat(result).hasSize(3);
    }

    @Test
    void parseResponse_returnsEmpty_whenWrappedTriplesArrayIsEmpty() {
        String json = "{ \"triples\": [] }";
        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    // --- Raw array format: [...] ---

    @Test
    void parseResponse_parsesTriples_fromRawArrayFormat() {
        String json = """
                [
                  { "subject": "neo4j", "predicate": "STORES", "object": "graph data" }
                ]
                """;

        List<Triple> result = parser.parseResponse(responseWithText(json));

        assertThat(result).containsExactly(new Triple("neo4j", "STORES", "graph data"));
    }

    @Test
    void parseResponse_returnsEmpty_whenRawArrayIsEmpty() {
        assertThat(parser.parseResponse(responseWithText("[]"))).isEmpty();
    }

    // --- Sanitization: markdown fences ---

    @Test
    void parseResponse_parsesCorrectly_whenTextWrappedInJsonFence() {
        String fenced = "```json\n{ \"triples\": [{ \"subject\": \"a\", \"predicate\": \"B\", \"object\": \"c\" }] }\n```";

        List<Triple> result = parser.parseResponse(responseWithText(fenced));

        assertThat(result).containsExactly(new Triple("a", "B", "c"));
    }

    @Test
    void parseResponse_parsesCorrectly_whenTextWrappedInGenericFence() {
        String fenced = "```\n{ \"triples\": [{ \"subject\": \"a\", \"predicate\": \"B\", \"object\": \"c\" }] }\n```";

        List<Triple> result = parser.parseResponse(responseWithText(fenced));

        assertThat(result).containsExactly(new Triple("a", "B", "c"));
    }

    @Test
    void parseResponse_parsesCorrectly_whenTextHasLeadingAndTrailingWhitespace() {
        String padded = "   { \"triples\": [{ \"subject\": \"a\", \"predicate\": \"B\", \"object\": \"c\" }] }   ";

        List<Triple> result = parser.parseResponse(responseWithText(padded));

        assertThat(result).containsExactly(new Triple("a", "B", "c"));
    }

    // --- Partial / malformed triples ---

    @Test
    void parseResponse_skipsTriple_whenSubjectIsMissing() {
        String json = """
                { "triples": [{ "predicate": "USES", "object": "tomcat" }] }
                """;

        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_skipsTriple_whenPredicateIsMissing() {
        String json = """
                { "triples": [{ "subject": "spring boot", "object": "tomcat" }] }
                """;

        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_skipsTriple_whenObjectIsMissing() {
        String json = """
                { "triples": [{ "subject": "spring boot", "predicate": "USES" }] }
                """;

        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_skipsTriple_whenAnyFieldIsJsonNull() {
        String json = """
                { "triples": [{ "subject": null, "predicate": "USES", "object": "tomcat" }] }
                """;

        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_skipsInvalidTriples_butKeepsValidOnes() {
        String json = """
                {
                  "triples": [
                    { "subject": "spring boot", "predicate": "USES", "object": "embedded tomcat" },
                    { "predicate": "USES", "object": "tomcat" },
                    { "subject": "tomcat", "predicate": "IMPLEMENTS", "object": "servlet container" }
                  ]
                }
                """;

        List<Triple> result = parser.parseResponse(responseWithText(json));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Triple::subject)
                .containsExactly("spring boot", "tomcat");
    }

    @Test
    void parseResponse_skipsNonObjectItems_inArray() {
        String json = """
                { "triples": ["not an object", 42, null] }
                """;

        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    // --- Unrecognized JSON shape ---

    @Test
    void parseResponse_returnsEmpty_whenJsonIsObjectWithoutTriplesKey() {
        String json = "{ \"data\": [] }";
        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_returnsEmpty_whenJsonIsMalformed() {
        assertThat(parser.parseResponse(responseWithText("not json at all"))).isEmpty();
    }

    @Test
    void parseResponse_returnsEmpty_whenTriplesValueIsNotAnArray() {
        String json = "{ \"triples\": \"oops\" }";
        assertThat(parser.parseResponse(responseWithText(json))).isEmpty();
    }

    @Test
    void parseResponse_salvagesCompleteTriples_whenJsonIsTruncated() {
        String truncated = """
                {
                  "triples": [
                    { "subject": "modern language models", "predicate": "UTILIZE", "object": "neural networks" },
                    { "subject": "neural networks", "predicate": "ENCODE", "object": "texts" },
                    { "subject"
                """;

        List<Triple> result = parser.parseResponse(responseWithText(truncated));

        assertThat(result).containsExactly(
                new Triple("modern language models", "UTILIZE", "neural networks"),
                new Triple("neural networks", "ENCODE", "texts")
        );
    }

    @Test
    void parseResponse_extractsJsonPayload_whenResponseContainsPreambleText() {
        String text = """
                Here is the JSON:
                {
                  "triples": [
                    { "subject": "spring boot", "predicate": "USES", "object": "embedded tomcat" }
                  ]
                }
                """;

        List<Triple> result = parser.parseResponse(responseWithText(text));

        assertThat(result).containsExactly(new Triple("spring boot", "USES", "embedded tomcat"));
    }

    // --- Fixture ---

    private static GeminiResponse responseWithText(String text) {
        if (text == null) {
            return new GeminiResponse(List.of());
        }
        return new GeminiResponse(
                List.of(new GeminiResponse.Candidate(
                        new GeminiResponse.Content(
                                List.of(new GeminiResponse.Part(text)))))
        );
    }
}
