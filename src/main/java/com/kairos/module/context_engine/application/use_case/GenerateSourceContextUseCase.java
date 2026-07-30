package com.kairos.module.context_engine.application.use_case;

import com.kairos.module.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.module.context_engine.domain.model.content.TripleExtracted;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.Triple;
import com.kairos.module.context_engine.domain.port.extraction.TripleExtractor;
import com.kairos.module.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphStore;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.module.context_engine.domain.port.repository.TripleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenerateSourceContextUseCase {

    private final TripleExtractor tripleExtractor;
    private final KnowledgeGraphStore knowledgeGraphStore;

    private final EmbeddingProvider embeddingProvider;

    private final ChunkRepository chunkRepository;
    private final SourceRepository sourceRepository;
    private final TripleRepository tripleRepository;

    /**
     * Generates context for a given source by chunking the content, extracting triples, and storing them in the knowledge graph.
     * @param command the command containing the source ID and content to process
     */
    public void execute(GenerateSourceContextCommand command) {
        Source source = sourceRepository.findById(command.sourceId())
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + command.sourceId()));

        UUID userId = source.getAuthorId();
        if (userId == null) {
            throw new IllegalStateException("Source author is required for context generation: " + source.getId());
        }

        List<Chunk> chunks = chunkRepository.findAllNotProcessedBySourceId(source.getId());
        chunks.forEach(chunk -> {
            chunk.markAsProcessing();
            chunkRepository.save(chunk);
        });

        processClaimedChunks(source, chunks);
    }

    public void executeClaimed(UUID sourceId, List<UUID> chunkIds) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + sourceId));

        List<Chunk> chunks = chunkRepository.findAllByIds(chunkIds).stream()
                .filter(chunk -> chunk.getSource().getId().equals(sourceId))
                .filter(chunk -> chunk.getProcessingStatus() ==
                        com.kairos.module.context_engine.domain.model.content.ChunkProcessingStatus.PROCESSING)
                .toList();

        processClaimedChunks(source, chunks);
    }

    private void processClaimedChunks(Source source, List<Chunk> chunks) {
        UUID userId = source.getAuthorId();
        if (userId == null) {
            throw new IllegalStateException("Source author is required for context generation: " + source.getId());
        }

        for (Chunk chunk : chunks) {
            try {
                processChunk(chunk, userId);
                chunk.markAsProcessed();
                chunkRepository.save(chunk);
            } catch (RuntimeException exception) {
                chunk.markAsFailed();
                chunkRepository.save(chunk);
                log.error("Failed to generate source context for sourceId={} chunkId={}",
                        source.getId(), chunk.getId(), exception);
            }
        }
    }

    private void processChunk(Chunk chunk, UUID userId) {
        float[] embedding = embeddingProvider.embed(chunk.getContent());
        chunk.addEmbedding(embedding);
        chunkRepository.save(chunk);

        Passage passage = Passage.fromChunkId(chunk.getId());
        knowledgeGraphStore.savePassages(List.of(passage), userId);

        List<Triple> triples = tripleExtractor.extract(chunk.getContent());
        List<TripleExtracted> extractedTriples = triples.stream()
                .map(triple -> createEmbeddingTriple(triple, chunk))
                .toList();
        List<KnowledgeTriple> knowledgeTriples = triples.stream()
                .map(triple -> KnowledgeTriple.create(triple, passage))
                .toList();

        tripleRepository.saveAll(extractedTriples);
        knowledgeGraphStore.saveAllForChunk(chunk.getId(), userId, knowledgeTriples);
    }

    /**
     * Creates a TripleExtracted object from a given Triple and its associated Chunk;
     * @param triple the triple extracted from the chunk content, containing the subject, predicate, and object of the knowledge statement
     * @param chunk the chunk from which the triple was extracted, providing the context for the knowledge statement
     * @return a TripleExtracted object that encapsulates the original triple.
     */
    private TripleExtracted createEmbeddingTriple(Triple triple, Chunk chunk) {
        var tripleExtracted = TripleExtracted.create(
                triple.subject(),
                triple.predicate(),
                triple.object(),
                chunk
        );
        float[] embedding = embeddingProvider.embed(tripleExtracted.getKey());

        tripleExtracted.addEmbedding(embedding);

        return tripleExtracted;
    }

}
