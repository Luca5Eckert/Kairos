package com.kairos.context_engine.infrastructure.relational.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.knowledge.Concept;
import com.kairos.context_engine.domain.model.retrieval.candidate.ConceptCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.context_engine.domain.model.retrieval.source.RetrievalSource;
import com.kairos.context_engine.domain.port.semantic.SemanticSearch;
import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import com.kairos.context_engine.infrastructure.relational.repository.triple.JpaTripleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PostgreSQL and pgvector adapter for dense semantic retrieval.
 * Provides implementation for vector similarity searches and payload hydration.
 */
@Component
@RequiredArgsConstructor
public class SemanticSearchAdapter implements SemanticSearch {

    private final JpaChunkRepository jpaChunkRepository;
    private final JpaTripleRepository jpaTripleRepository;

    /**
     * Performs a nearest-neighbor vector search over text chunks to identify semantic anchors.
     * Utilizes the pgvector cosine distance operator ({@code <=>}) for optimal alignment with
     * the all-MiniLM-L6-v2 embedding space.
     *
     * @param queryVector The embedding vector of the user's search query.
     * @param k           The maximum number of anchor chunks to retrieve.
     * @return A list of the top-k most semantically similar {@link Chunk}s.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PassageCandidate> findPassageCandidate(float[] queryVector, int k) {
        return jpaChunkRepository.findCandidates(queryVector, k)
                .stream()
                .map(candidate -> new PassageCandidate(
                        candidate.getChunkId(),
                        candidate.getDenseScore()
                ))
                .toList();
    }

    /**
     * Hydrates chunk domain models by their unique identifiers.
     * @param chunkIds A list of UUIDs representing the chunks to be fetched.
     * @return A list of {@link Chunk} objects corresponding to the provided IDs.
     * The order of the returned list is not guaranteed to match the input list.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Chunk> findChunks(List<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }

        List<ChunkEntity> chunks = jpaChunkRepository.findAllById(chunkIds);
        return chunks.stream()
                .map(ChunkEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankedChunk> hydrateAndRankChunks(List<ScoredPassage> scoredPassages) {
        if (scoredPassages == null || scoredPassages.isEmpty()) {
            return List.of();
        }

        List<UUID> chunkIds = scoredPassages.stream()
                .map(ScoredPassage::chunkId)
                .distinct()
                .toList();

        Map<UUID, Chunk> chunksById = findChunks(chunkIds).stream()
                .collect(Collectors.toMap(Chunk::getId, Function.identity(), (existing, duplicate) -> existing));

        AtomicInteger rank = new AtomicInteger(1);
        return scoredPassages.stream()
                .map(scoredPassage -> toRankedChunk(scoredPassage, chunksById.get(scoredPassage.chunkId()), rank))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConceptCandidate> findConceptCandidate(float[] queryVector, int semanticAnchorLimit) {
        var conceptCandidate = jpaTripleRepository.findCandidates(queryVector, semanticAnchorLimit);

        return conceptCandidate.stream()
                .map(candidate -> new ConceptCandidate(
                        new Concept(candidate.getName()),
                        candidate.getSimilarity()
                ))
                .toList();
    }

    private RankedChunk toRankedChunk(ScoredPassage scoredPassage, Chunk chunk, AtomicInteger rank) {
        if (chunk == null) {
            return null;
        }

        return new RankedChunk(
                chunk,
                rank.getAndIncrement(),
                scoredPassage.graphScore(),
                RetrievalSource.GRAPH
        );
    }



}
