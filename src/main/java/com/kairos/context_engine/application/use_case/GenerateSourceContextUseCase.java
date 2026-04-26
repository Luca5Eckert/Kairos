package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.domain.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.KnowledgeTriple;
import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.domain.semantic.ChunkerExtractor;
import com.kairos.context_engine.domain.graph.TripleExtractor;
import com.kairos.context_engine.domain.port.ChunkRepository;
import com.kairos.context_engine.domain.graph.KnowledgeGraphStore;
import com.kairos.context_engine.domain.port.SourceRepository;
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

        List<Chunk> chunks = chunkRepository.findAllBySourceId(source.getId());

        embedChunks(chunks);

        generateContext(source, chunks);

    }

    /**
     * Embeds the content of each chunk using the embedding provider and saves the updated chunks back to the repository.
     * @param chunks the list of chunks to embed and save
     */
    private void embedChunks(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            float[] embedding = embeddingProvider.embed(chunk.getContent());
            chunk.addEmbedding(embedding);
            chunkRepository.save(chunk);
        }
    }

    /**
     * Generates context for a specific chunk of text by creating a Chunk entity, extracting triples, and saving them to the knowledge graph.
     * @param source the source document the chunk belongs to
     * @param text the text content of the chunk
     * @param index the index of the chunk within the source document
     */
    private void generateContext(Source source, String text, int index) {
        List<Triple> triples = tripleExtractor.extract(text);
        List<KnowledgeTriple> knowledgeTriples = triples.stream()
                .map(triple -> KnowledgeTriple.create(triple, chunk.getId()))
                .toList();

        knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);
    }

}