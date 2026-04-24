package com.kairos.context_engine.application.command;

import java.util.UUID;

public record GenerateSourceContextCommand(
        UUID sourceId,
        String content
) {
    public static GenerateSourceContextCommand of(UUID sourceId, String content) {
        return new GenerateSourceContextCommand(sourceId, content);
    }
}
