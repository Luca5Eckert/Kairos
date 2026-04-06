package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.extractor.ChunkerExtractor;
import com.kairos.domain.extractor.TripleExtractor;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;
import com.kairos.domain.model.Triple;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.SourceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GenerateSourceContextUseCase {

    private final ChunkerExtractor chunkerExtractor;
    private final TripleExtractor tripleExtractor;
    private final EmbeddingProvider embeddingProvider;

    private final ChunkRepository chunkRepository;
    private final SourceRepository sourceRepository;

    public GenerateSourceContextUseCase(ChunkerExtractor chunkerExtractor, TripleExtractor tripleExtractor, EmbeddingProvider embeddingProvider, ChunkRepository chunkRepository, SourceRepository sourceRepository) {
        this.chunkerExtractor = chunkerExtractor;
        this.tripleExtractor = tripleExtractor;
        this.embeddingProvider = embeddingProvider;
        this.chunkRepository = chunkRepository;
        this.sourceRepository = sourceRepository;
    }

    public void execute(GenerateSourceContextCommand command) {
        var chunks = chunkerExtractor.extract(command.content(), 200, 46);

        var triples = chunks.stream()
                .map(tripleExtractor::extract)
                .toList();

        for (int i = 0; i < chunks.size(); i++) {
            generateContext(chunks.get(i), triples.get(i), command.sourceId(), i);
        }

    }

    private void generateContext(String text, List<Triple> triples, UUID sourceId, int index) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + sourceId));

        var chunk = Chunk.create(source, text, index, embeddingProvider.embed(text));

        chunkRepository.save(chunk);
    }


}
