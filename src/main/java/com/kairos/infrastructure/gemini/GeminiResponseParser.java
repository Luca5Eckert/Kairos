package com.kairos.infrastructure.gemini;

import com.kairos.domain.model.Triple;
import com.kairos.infrastructure.gemini.dto.GeminiResponse;
import com.kairos.infrastructure.gemini.exception.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    public GeminiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Triple> parseResponse(GeminiResponse response) {
        if (response == null || response.text() == null || Objects.requireNonNull(response.text()).isBlank()) {
            return List.of();
        }

        try {
            String raw = sanitize(Objects.requireNonNull(response.text()));
            JsonNode root = objectMapper.readTree(raw);

            if (root.isArray()) {
                return parseArray(root);
            }

            if (root.isObject() && root.has("triples") && root.get("triples").isArray()) {
                return parseArray(root.get("triples"));
            }

            log.warn("Gemini response does not contain a valid triples JSON array. Response: {}", raw);
            return List.of();

        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", response.text(), e);
            return List.of();
        }
    }

    private List<Triple> parseArray(JsonNode arrayNode) throws JsonProcessingException {
        List<Triple> triples = new ArrayList<>();

        for (JsonNode item : arrayNode) {
            if (!item.isObject()) {
                continue;
            }

            String subject = text(item, "subject");
            String predicate = text(item, "predicate");
            String object = text(item, "object");

            if (subject != null && predicate != null && object != null) {
                triples.add(new Triple(subject, predicate, object));
            }
        }

        return triples;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asString() : null;
    }

    private String sanitize(String text) {
        String trimmed = text.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        return trimmed;
    }

}