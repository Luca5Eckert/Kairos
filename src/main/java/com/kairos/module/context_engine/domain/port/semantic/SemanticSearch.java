package com.kairos.module.context_engine.domain.port.semantic;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.ScoredPassage;

import java.util.List;
import java.util.UUID;

public interface SemanticSearch {

    List<PassageCandidate> findPassageCandidate(float[] queryVector, UUID userId, int k);

    List<Chunk> findChunks(List<UUID> chunkIds, UUID userId);

    List<RankedChunk> hydrateAndRankChunks(List<ScoredPassage> scoredPassages, UUID userId);

    List<TripleCandidate> findTripleCandidates(float[] queryVector, UUID userId, int limit);
}
