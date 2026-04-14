package com.kairos.presentation.mapper;

import com.kairos.domain.model.SearchResult;
import com.kairos.presentation.dto.response.ContextResponse;
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
