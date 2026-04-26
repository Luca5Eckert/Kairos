package com.kairos.context_engine.domain.event;

import java.util.UUID;

public record CreatedSourceEvent(
        UUID sourceId
) {
    public static CreatedSourceEvent of(UUID id) {
        return new CreatedSourceEvent(id);
    }
}
