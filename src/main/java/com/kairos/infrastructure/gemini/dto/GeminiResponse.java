package com.kairos.infrastructure.gemini.dto;

import java.util.List;

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

        return candidate.content().parts().getFirst().text();
    }

    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}