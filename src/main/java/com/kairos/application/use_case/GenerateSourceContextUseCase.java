package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.domain.embedding.EmbeddingProvider;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.domain.port.SourceRepository;
import com.kairos.domain.semantic.ChunkerExtractor;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import com.kairos.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GenerateSourceContextUseCase {

    private final ChunkerExtractor chunkerExtractor;
    private final EmbeddingProvider embeddingProvider;
    private final ChunkRepository chunkRepository;
    private final SourceContextJobRepository sourceContextJobRepository;
    private final SourceRepository sourceRepository;
    private final SourceContextJobProperties jobProperties;
    private final Clock clock;

    /**
     * Generates context for a given source by chunking the content, extracting triples, and storing them in the knowledge graph.
     * @param command the command containing the source ID and content to process
     */
    @Transactional
    public void execute(GenerateSourceContextCommand command) {
        Source source = sourceRepository.findById(command.sourceId())
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + command.sourceId()));

        source.markProcessing();
        sourceRepository.save(source);

        List<String> chunks = chunkerExtractor.extract(command.content(), 200, 50);

        if (chunks.isEmpty()) {
            source.markCompleted();
            sourceRepository.save(source);
            return;
        }

        for (int i = 0; i < chunks.size(); i++) {
            persistChunk(source, chunks.get(i), i);
        }

        enqueueJobIfNeeded(source);
    }

    private void persistChunk(Source source, String text, int index) {
        Chunk chunk = Chunk.create(source, text, index, embeddingProvider.embed(text));
        Chunk savedChunk = chunkRepository.save(chunk);

        if (savedChunk.getId() == null) {
            throw new IllegalStateException("Chunk ID is null after save");
        }
    }

    private void enqueueJobIfNeeded(Source source) {
        if (sourceContextJobRepository.findBySourceId(source.getId()).isPresent()) {
            return;
        }

        Instant now = Instant.now(clock);
        sourceContextJobRepository.save(SourceContextJob.create(source.getId(), jobProperties.maxAttempts(), now));
    }
}
