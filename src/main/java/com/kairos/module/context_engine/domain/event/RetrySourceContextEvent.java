package com.kairos.module.context_engine.domain.event;

import java.util.List;
import java.util.UUID;

public record RetrySourceContextEvent(
        UUID sourceId,
        List<UUID> chunkIds
) {
    public RetrySourceContextEvent {
        chunkIds = List.copyOf(chunkIds);
    }
}
