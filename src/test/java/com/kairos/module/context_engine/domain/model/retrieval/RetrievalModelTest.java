package com.kairos.module.context_engine.domain.model.retrieval;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchResult;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.module.context_engine.domain.model.retrieval.seed.ConceptSeedTarget;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.model.retrieval.seed.PassageSeedTarget;
import com.kairos.module.context_engine.domain.model.retrieval.seed.SeedType;
import com.kairos.module.context_engine.domain.model.retrieval.source.RetrievalSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalModelTest {

    @Test
    void graphSeed_shouldCreateTypedPassageAndConceptSeeds() {
        UUID chunkId = UUID.randomUUID();

        GraphSeed passageSeed = GraphSeed.passage(chunkId, 0.7);
        GraphSeed conceptSeed = GraphSeed.concept("  Spring  ", 0.8);

        assertThat(passageSeed.type()).isEqualTo(SeedType.PASSAGE);
        assertThat(((PassageSeedTarget) passageSeed.target()).chunkId()).isEqualTo(chunkId);
        assertThat(passageSeed.weight()).isEqualTo(0.7);
        assertThat(conceptSeed.type()).isEqualTo(SeedType.CONCEPT);
        assertThat(((ConceptSeedTarget) conceptSeed.target()).concept().name()).isEqualTo("Spring");
        assertThat(conceptSeed.weight()).isEqualTo(0.8);
    }

    @Test
    void graphSeed_shouldRejectInvalidFieldsAndMismatchedTypes() {
        UUID chunkId = UUID.randomUUID();

        assertThatThrownBy(() -> new GraphSeed(null, SeedType.PASSAGE, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph seed target cannot be null");
        assertThatThrownBy(() -> new GraphSeed(new PassageSeedTarget(chunkId), null, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph seed type cannot be null");
        assertThatThrownBy(() -> GraphSeed.passage(chunkId, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph seed weight must be a positive finite value");
        assertThatThrownBy(() -> GraphSeed.passage(chunkId, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph seed weight must be a positive finite value");
        assertThatThrownBy(() -> GraphSeed.passage(chunkId, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph seed weight must be a positive finite value");
        assertThatThrownBy(() -> new GraphSeed(new PassageSeedTarget(chunkId), SeedType.CONCEPT, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage seed target must use PASSAGE seed type");
        assertThatThrownBy(() -> new GraphSeed(ConceptSeedTarget.fromName("Spring"), SeedType.PASSAGE, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Concept seed target must use CONCEPT seed type");
    }

    @Test
    void seedTargets_shouldRejectNullValues() {
        assertThatThrownBy(() -> new PassageSeedTarget(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage seed chunkId cannot be null");
        assertThatThrownBy(() -> new ConceptSeedTarget(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Concept seed target cannot be null");
    }

    @Test
    void graphSearchRequest_shouldDefensivelyCopySeeds() {
        UUID userId = UUID.randomUUID();
        List<GraphSeed> seeds = new ArrayList<>();
        seeds.add(GraphSeed.passage(UUID.randomUUID(), 0.7));

        GraphSearchRequest request = GraphSearchRequest.from(userId, seeds, 10);
        seeds.add(GraphSeed.concept("Spring", 0.8));

        assertThat(request.userId()).isEqualTo(userId);
        assertThat(request.seeds()).hasSize(1);
        assertThat(request.limit()).isEqualTo(10);
        assertThatThrownBy(() -> request.seeds().add(GraphSeed.concept("JPA", 0.9)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void graphSearchRequest_shouldRejectInvalidFields() {
        List<GraphSeed> seeds = List.of(GraphSeed.passage(UUID.randomUUID(), 0.7));

        assertThatThrownBy(() -> GraphSearchRequest.from(null, seeds, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph search userId cannot be null");
        assertThatThrownBy(() -> GraphSearchRequest.from(UUID.randomUUID(), null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph search seeds cannot be null");
        assertThatThrownBy(() -> GraphSearchRequest.from(UUID.randomUUID(), seeds, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph search limit must be positive");
    }

    @Test
    void graphSearchResult_shouldDefensivelyCopyLists() {
        UUID chunkId = UUID.randomUUID();
        List<ScoredPassage> scoredPassages = new ArrayList<>();
        scoredPassages.add(new ScoredPassage(chunkId, 0.9));
        List<KnowledgeTriple> activatedTriples = new ArrayList<>();
        activatedTriples.add(KnowledgeTriple.create("Spring", "USES", "JPA", Passage.fromChunkId(chunkId), 1.0));

        GraphSearchResult result = new GraphSearchResult(scoredPassages, activatedTriples);
        scoredPassages.clear();
        activatedTriples.clear();

        assertThat(result.scoredPassages()).hasSize(1);
        assertThat(result.activatedTriples()).hasSize(1);
        assertThatThrownBy(() -> result.scoredPassages().add(new ScoredPassage(UUID.randomUUID(), 0.5)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.activatedTriples().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void graphSearchResult_shouldRejectNullLists() {
        assertThatThrownBy(() -> new GraphSearchResult(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph search scored passages cannot be null");
        assertThatThrownBy(() -> new GraphSearchResult(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Graph search activated triples cannot be null");
    }

    @Test
    void rankingModels_shouldRejectInvalidFields() {
        Chunk chunk = new Chunk(UUID.randomUUID(), new Source("title", "content"), "content", 0, true, new float[]{0.1f});

        assertThatThrownBy(() -> new ScoredPassage(null, 0.8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scored passage chunkId cannot be null");
        assertThatThrownBy(() -> new ScoredPassage(UUID.randomUUID(), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scored passage graphScore must be finite");
        assertThatThrownBy(() -> new ScoredPassage(UUID.randomUUID(), Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Scored passage graphScore must be finite");
        assertThatThrownBy(() -> new RankedChunk(null, 1, 0.8, RetrievalSource.GRAPH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ranked chunk cannot be null");
        assertThatThrownBy(() -> new RankedChunk(chunk, 0, 0.8, RetrievalSource.GRAPH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ranked chunk rank must be positive");
        assertThatThrownBy(() -> new RankedChunk(chunk, 1, Double.NaN, RetrievalSource.GRAPH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ranked chunk score must be finite");
        assertThatThrownBy(() -> new RankedChunk(chunk, 1, Double.POSITIVE_INFINITY, RetrievalSource.GRAPH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ranked chunk score must be finite");
        assertThatThrownBy(() -> new RankedChunk(chunk, 1, 0.8, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ranked chunk source cannot be null");
    }
}
