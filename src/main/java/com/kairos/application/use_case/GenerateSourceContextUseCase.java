package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.semantic.ChunkerExtractor;
import com.kairos.domain.graph.TripleExtractor;
import com.kairos.domain.model.*;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.graph.KnowledgeGraphStore;
import com.kairos.domain.port.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GenerateSourceContextUseCase {

    private final ChunkerExtractor chunkerExtractor;
    private final TripleExtractor tripleExtractor;
    private final EmbeddingProvider embeddingProvider;

    private final KnowledgeGraphStore knowledgeGraphStore;
    private final ChunkRepository chunkRepository;
    private final SourceRepository sourceRepository;

    /**
     * Generates context for a given source by chunking the content, extracting triples, and storing them in the knowledge graph.
     * @param command the command containing the source ID and content to process
     */
    @Transactional
    public void execute(GenerateSourceContextCommand command) {
        Source source = sourceRepository.findById(command.sourceId())
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + command.sourceId()));

        List<String> chunks = chunkerExtractor.extract(command.content(), 200, 50);

        for (int i = 0; i < chunks.size(); i++) {
            generateContext(source, chunks.get(i), i);
        }
    }

    /**
     * Generates context for a specific chunk of text by creating a Chunk entity, extracting triples, and saving them to the knowledge graph.
     * @param source the source document the chunk belongs to
     * @param text the text content of the chunk
     * @param index the index of the chunk within the source document
     */
    private void generateContext(Source source, String text, int index) {
        Chunk chunk = Chunk.create(source, text, index, embeddingProvider.embed(text));
        chunkRepository.save(chunk);

        if (chunk.getId() == null) {
            throw new IllegalStateException("Chunk ID is null after save");
        }

        List<Triple> triples = tripleExtractor.extract(text);
        List<KnowledgeTriple> knowledgeTriples = triples.stream()
                .map(triple -> KnowledgeTriple.create(triple, chunk.getId()))
                .toList();

        knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);
    }

}