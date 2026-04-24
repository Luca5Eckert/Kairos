package com.kairos.context_engine.application.command;

import java.util.UUID;

public record UploadSourceCommand(
        String title,
        String content,
        UUID authorId
) {
    public static UploadSourceCommand of(String title, String content, UUID authorId) {
        return new UploadSourceCommand(title, content, authorId);
    }
}
