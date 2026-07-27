package com.kairos.module.context_engine.domain.port.repository;

import com.kairos.module.context_engine.domain.model.content.TripleExtracted;

import java.util.List;

public interface TripleRepository {
    void saveAll(List<TripleExtracted> extractedTriples);
}
