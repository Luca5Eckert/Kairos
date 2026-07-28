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
            int totalChunks,
            int processedChunks
    ) {

        public static SourceProgressUploadResponse of(SourceProgressUpload upload) {
            return new SourceProgressUploadResponse(
                    upload.source().getId().toString(),
                    upload.source().getTitle(),
                    upload.totalChunks(),
                    upload.processedChunks()
            );
        }

    }

}
