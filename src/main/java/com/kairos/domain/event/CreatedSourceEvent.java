package com.kairos.domain.event;

import java.util.UUID;

public record CreatedSourceEvent(
        UUID sourceId,
        String content
) {
    public static CreatedSourceEvent of(UUID id, String content)
    {
        return new CreatedSourceEvent(id, content);
    }
}
