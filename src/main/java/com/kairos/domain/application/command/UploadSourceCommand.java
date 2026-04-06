package com.kairos.domain.application.command;

import java.util.UUID;

public record UploadSourceCommand(
        String title,
        String content,
        UUID authorId
) {
}
