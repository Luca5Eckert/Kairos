package com.kairos.context_engine.application.command;

public record UploadSourceCommand(
        String title,
        String content
) {
    public static UploadSourceCommand of(String title, String content) {
        return new UploadSourceCommand(title, content);
    }
}
