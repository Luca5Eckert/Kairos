package com.kairos.domain.port;

import com.kairos.domain.model.Source;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository {

    void save(Source source);

    Optional<Source> findById(UUID id);

    List<Source> findAll(int k);

}
