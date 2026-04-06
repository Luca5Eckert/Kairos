package com.kairos.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptRepository {

    void save(Concept concept);

    Optional<Concept> findById(UUID id);

    List<Concept> findAll(int k);

    void saveAll(List concepts);
}
