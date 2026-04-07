package com.kairos.application.use_case;

import com.kairos.application.query.SearchSourceQuery;
import com.kairos.domain.model.SearchResult;
import com.kairos.infrastructure.semantic.SemanticSearchAdapter;
import org.springframework.stereotype.Component;

@Component
public class SearchSourceUseCase {

    private final SemanticSearchAdapter semanticSearchAdapter;

    public SearchSourceUseCase(SemanticSearchAdapter semanticSearchAdapter) {
        this.semanticSearchAdapter = semanticSearchAdapter;
    }

    public SearchResult execute(SearchSourceQuery query){
        return new SearchResult(
                null, null
        );
    }
}
