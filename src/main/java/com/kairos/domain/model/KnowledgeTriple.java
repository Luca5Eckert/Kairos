package com.kairos.domain.model;

import java.util.UUID;

public record KnowledgeTriple(
        Concept subject,
        String predicate,
        Concept object,
        UUID sourceId
) {
}
