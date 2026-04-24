package com.kairos.context_engine.presentation.mapper;

import com.kairos.context_engine.domain.model.SearchResult;
import com.kairos.context_engine.presentation.dto.response.ContextResponse;
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
