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
        generateKnowledgeGraph(source, chunks);
    }

    private void generateKnowledgeGraph(Source source, List<Chunk> chunks) {
        knowledgeGraphStore.createContext(chunks);
        createContextForKnowledgeGraph(source,chunks);
    }


    private void createContextForKnowledgeGraph(Source source, List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            List<Triple> triples = tripleExtractor.extract(chunk.getContent());
            List<KnowledgeTriple> knowledgeTriples = triples.stream()
                    .map(triple -> KnowledgeTriple.create(triple, chunk.getId()))
                    .toList();

            knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);
        }
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


}