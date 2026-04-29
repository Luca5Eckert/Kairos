package com.kairos.context_engine.infrastructure.ai.gemini;

import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.infrastructure.ai.gemini.dto.GeminiResponse;
import com.kairos.context_engine.infrastructure.ai.gemini.exception.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GeminiResponseParser {

    private static final Pattern COMPLETE_TRIPLE_PATTERN = Pattern.compile(
            "\\{\\s*\"subject\"\\s*:\\s*\"(?<subject>(?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*\"predicate\"\\s*:\\s*\"(?<predicate>(?:\\\\.|[^\"\\\\])*)\"\\s*,\\s*\"object\"\\s*:\\s*\"(?<object>(?:\\\\.|[^\"\\\\])*)\"\\s*\\}",
            Pattern.DOTALL
    );

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
            JsonNode root = tryReadTree(raw);

            if (root != null && root.isArray()) {
                return parseArray(root);
            }

            if (root != null && root.isObject() && root.has("triples") && root.get("triples").isArray()) {
                return parseArray(root.get("triples"));
            }

            List<Triple> salvagedTriples = salvageTriples(raw);
            if (!salvagedTriples.isEmpty()) {
                log.warn("Gemini response was not valid JSON, but {} complete triples were recovered.", salvagedTriples.size());
                return salvagedTriples;
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
            double weight = Double.parseDouble(Objects.requireNonNull(text(item, "weight")));

            if (subject != null && predicate != null && object != null) {
                triples.add(new Triple(subject, predicate, object, weight));
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

    private JsonNode tryReadTree(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            String extracted = extractLikelyJson(raw);
            if (extracted == null || extracted.equals(raw)) {
                return null;
            }

            try {
                return objectMapper.readTree(extracted);
            } catch (Exception nestedException) {
                return null;
            }
        }
    }

    private String extractLikelyJson(String raw) {
        int objectStart = raw.indexOf('{');
        int arrayStart = raw.indexOf('[');

        int start;
        if (objectStart == -1) {
            start = arrayStart;
        } else if (arrayStart == -1) {
            start = objectStart;
        } else {
            start = Math.min(objectStart, arrayStart);
        }

        if (start < 0) {
            return null;
        }

        int objectEnd = raw.lastIndexOf('}');
        int arrayEnd = raw.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);

        if (end <= start) {
            return raw.substring(start);
        }

        return raw.substring(start, end + 1);
    }

    private List<Triple> salvageTriples(String raw) {
        Matcher matcher = COMPLETE_TRIPLE_PATTERN.matcher(raw);
        List<Triple> triples = new ArrayList<>();

        while (matcher.find()) {
            String subject = unescapeJson(matcher.group("subject"));
            String predicate = unescapeJson(matcher.group("predicate"));
            String object = unescapeJson(matcher.group("object"));
            unescapeJson(matcher.group("weight"));
            double weight = Double.parseDouble(unescapeJson(matcher.group("weight")));

            if (!subject.isBlank() && !predicate.isBlank() && !object.isBlank()) {
                triples.add(new Triple(subject, predicate, object, weight));
            }
        }

        return triples;
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r");
    }

}
