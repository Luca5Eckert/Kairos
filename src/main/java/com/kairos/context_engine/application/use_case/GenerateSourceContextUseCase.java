package com.kairos.context_engine.application.use_case;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.domain.model.content.TripleExtracted;
import com.kairos.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.model.Triple;
import com.kairos.context_engine.domain.port.extraction.TripleExtractor;
import com.kairos.context_engine.domain.port.repository.ChunkRepository;
import com.kairos.context_engine.domain.port.graph.KnowledgeGraphStore;
import com.kairos.context_engine.domain.port.repository.SourceRepository;
import com.kairos.context_engine.domain.port.repository.TripleRepository;
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
    private final TripleRepository tripleRepository;

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

    /**
     * Generates a knowledge graph context for the given source and its associated chunks.
     * @param source the source for which the knowledge graph context is being generated
     * @param chunks the list of chunks associated with the source, from which to extract triples and of the knowledge graph context
     */
    private void generateKnowledgeGraph(Source source, List<Chunk> chunks) {
        knowledgeGraphStore.createContext(chunks); // TODO: this is a bad abstraction. First we to create a series of passages nodes and this will be the foundation of context.
        createContextForKnowledgeGraph(source,chunks); // TODO: this is a causal bad abstraction. This parte will create the relationships and structure of the foundation create before.
    }


    /**
     * Extracts triples from the content of each chunk and saves them to the knowledge graph store, associating them with the corresponding chunk ID.
     * @param source the source for which the knowledge graph context is being generated
     * @param chunks the list of chunks from which to extract triples and of the knowledge graph context
     */
    private void createContextForKnowledgeGraph(Source source, List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            List<Triple> triples = tripleExtractor.extract(chunk.getContent());
            Passage passage = Passage.fromChunkId(chunk.getId());

            List<TripleExtracted> extractedTriples = triples.stream()
                    .map(triple -> this.createEmbeddingTriple(triple, chunk))
                    .toList();

            List<KnowledgeTriple> knowledgeTriples = triples.stream()
                    .map(triple -> KnowledgeTriple.create(triple, passage))
                    .toList();

            tripleRepository.saveAll(extractedTriples);
            knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);

            chunk.markAsProcessed();
            chunkRepository.save(chunk);
        }
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
