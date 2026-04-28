package com.kairos.context_engine.domain.port.repository;

import com.kairos.context_engine.domain.model.content.Source;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceRepository {

    /**
     * Save a source to the repository.
     * @param source The source to be saved.
     */
    void save(Source source);

    /**
     * Find a source by its unique identifier.
     * @param id The unique identifier of the source to be retrieved.
     * @return An Optional containing the source if found, or empty if not found.
     */
    Optional<Source> findById(UUID id);

    /**
     * Retrieve a list of sources from the repository, limited to a specified number.
     * @param k The maximum number of sources to retrieve.
     * @return A list of sources retrieved from the repository, limited to the specified number.
     */
    List<Source> findAll(int k);

}
