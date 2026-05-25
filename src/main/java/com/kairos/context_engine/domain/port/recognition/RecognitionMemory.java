package com.kairos.context_engine.domain.port.recognition;

import com.kairos.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;

import java.util.List;

public interface RecognitionMemory {

    List<GraphSeed> recognize(String searchTerm, List<TripleCandidate> candidates, int maxSeeds);
}
