package com.kairos.module.context_engine.presentation.mapper;

import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.presentation.dto.response.ContextResponse;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {

    public ContextResponse toContextResponse(SearchResult searchResult) {
        return ContextResponse.of(
                searchResult.knowledgeTriples(),
                searchResult.chunks()
        );
    }


}
