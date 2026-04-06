package com.kairos.application.use_case;

import com.kairos.application.command.UploadSourceCommand;
import com.kairos.domain.extractor.info.Triple;
import com.kairos.domain.extractor.port.TripleExtractor;
import com.kairos.domain.model.Concept;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.ConceptRepository;
import com.kairos.domain.port.EmbeddingProvider;
import com.kairos.domain.port.SourceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use case responsible for handling the process of uploading a source, which includes generating embeddings and extracting concepts.
 * @version 1.0
 * @since 2024-06-01
 */
@Component
public class UploadSourceUseCase {

    private final SourceRepository sourceRepository;
    private final ConceptRepository conceptRepository;

    private final EmbeddingProvider embeddingProvider;
    private final TripleExtractor tripleExtractor;

    public UploadSourceUseCase(SourceRepository sourceRepository, ConceptRepository conceptRepository, EmbeddingProvider embeddingProvider, TripleExtractor tripleExtractor) {
        this.sourceRepository = sourceRepository;
        this.conceptRepository = conceptRepository;
        this.embeddingProvider = embeddingProvider;
        this.tripleExtractor = tripleExtractor;
    }

    /**
     * Realized the process of uploading a source, which includes:
     * 1. Generating an embedding for the source content using the EmbeddingProvider.
     * 2. Extracting concepts from the source content using the TripleExtractor.
     * @param command The command object containing information for uploading the source.
     */
    @Transactional
    public void execute(UploadSourceCommand command){
        var embedding = embeddingProvider.embed(command.content());

        var source = new Source(
                command.title(),
                command.content(),
                embedding
        );

        var triples = tripleExtractor.extract(command.content());

        var concepts = generateConceptsFromTriples(triples);

        sourceRepository.save(source);
        conceptRepository.saveAll(concepts);
    }

    /**
     * Converts extracted triples into Concept entities.
     * Verify if a concept with the same name already exists in the repository to avoid duplicates.
     * @param triples List of triples extracted from the source content.
     * @return List of Concept entities generated from the triples.
     */
    private List<Concept> generateConceptsFromTriples(List<Triple> triples) {

    }

}
