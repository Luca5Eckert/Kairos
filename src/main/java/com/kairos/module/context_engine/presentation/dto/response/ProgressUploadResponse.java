package com.kairos.module.context_engine.presentation.dto.response;

import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;

import java.util.List;

public record ProgressUploadResponse(
        List<SourceProgressUploadResponse> sourceProgressUploads
) {
    public static ProgressUploadResponse of(List<SourceProgressUpload> uploads) {
        return new ProgressUploadResponse(
                uploads.stream()
                        .map(SourceProgressUploadResponse::of)
                        .toList()
        );
    }

    record SourceProgressUploadResponse(
            String sourceId,
            String sourceTitle,
            String status,
            int totalChunks,
            int pendingChunks,
            int processingChunks,
            int completedChunks,
            int failedChunks
    ) {

        public static SourceProgressUploadResponse of(SourceProgressUpload upload) {
            return new SourceProgressUploadResponse(
                    upload.source().getId().toString(),
                    upload.source().getTitle(),
                    upload.status().name(),
                    upload.totalChunks(),
                    upload.pendingChunks(),
                    upload.processingChunks(),
                    upload.completedChunks(),
                    upload.failedChunks()
            );
        }

    }

}
