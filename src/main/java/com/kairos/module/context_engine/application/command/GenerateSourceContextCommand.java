package com.kairos.module.context_engine.application.command;

import java.util.UUID;

public record GenerateSourceContextCommand(
        UUID sourceId
) {
    public static GenerateSourceContextCommand of(UUID sourceId) {
        return new GenerateSourceContextCommand(sourceId);
    }
}
