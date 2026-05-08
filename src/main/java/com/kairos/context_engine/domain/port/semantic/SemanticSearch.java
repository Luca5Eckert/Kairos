package com.kairos.context_engine.domain.port.semantic;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.retrieval.candidate.ConceptCandidate;
import com.kairos.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.context_engine.domain.model.retrieval.ranking.ScoredPassage;

import java.util.List;
import java.util.UUID;

public interface SemanticSearch {

    List<PassageCandidate> findPassageCandidate(float[] queryVector, int k);

    List<Chunk> findChunks(List<UUID> triples);

    List<RankedChunk> hydrateAndRankChunks(List<ScoredPassage> scoredPassages);

    List<ConceptCandidate> findConceptCandidate(float[] queryVector, int semanticAnchorLimit);
}
