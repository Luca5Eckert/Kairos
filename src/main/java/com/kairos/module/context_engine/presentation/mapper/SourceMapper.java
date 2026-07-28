package com.kairos.module.context_engine.presentation.mapper;

import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.presentation.dto.response.ContextResponse;
import com.kairos.module.context_engine.presentation.dto.response.ProgressUploadResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SourceMapper {

    public ContextResponse toContextResponse(SearchResult searchResult) {
        return ContextResponse.of(
                searchResult.knowledgeTriples(),
                searchResult.chunks()
        );
    }

    public ProgressUploadResponse toProgressUploadResponse(List<SourceProgressUpload> sourceProgressUploads) {
        return ProgressUploadResponse.of(sourceProgressUploads);
    }


}
