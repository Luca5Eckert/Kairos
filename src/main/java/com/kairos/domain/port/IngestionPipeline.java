package com.kairos.domain.port;

import com.kairos.domain.model.IngestionResult;
import com.kairos.domain.model.Source;

public interface IngestionPipeline {
    IngestionResult run(Source source);
}