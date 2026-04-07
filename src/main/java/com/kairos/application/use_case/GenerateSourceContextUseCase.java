package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.extractor.ChunkerExtractor;
import com.kairos.domain.extractor.TripleExtractor;
import com.kairos.domain.model.*;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.KnowledgeGraphStore;
import com.kairos.domain.port.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    public void execute(GenerateSourceContextCommand command) {
        Source source = sourceRepository.findById(command.sourceId())
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + command.sourceId()));

        List<String> chunks = chunkerExtractor.extract(command.content(), 200, 50);

        for (int i = 0; i < chunks.size(); i++) {
            generateContext(source, chunks.get(i), i);
        }
    }

    private void generateContext(Source source, String text, int index) {
        Chunk chunk = Chunk.create(source, text, index, embeddingProvider.embed(text));
        chunkRepository.save(chunk);

        List<Triple> triples = tripleExtractor.extract(text);
        List<KnowledgeTriple> knowledgeTriples = triples.stream()
                .map(triple -> KnowledgeTriple.create(triple, chunk.getId()))
                .toList();

        knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);
    }

}