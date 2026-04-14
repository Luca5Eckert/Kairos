package com.kairos.infrastructure.gemini.dto;

import java.util.List;
import java.util.Objects;

public record GeminiResponse(
        List<Candidate> candidates
) {
    public String text() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        var candidate = candidates.getFirst();
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content().parts().stream()
                .map(Part::text)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}
