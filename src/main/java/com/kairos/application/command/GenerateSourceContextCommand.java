package com.kairos.application.command;

import java.util.UUID;

public record GenerateSourceContextCommand(
        UUID sourceId,
        String content
) {
}
