package com.kairos.module.context_engine.domain.model.history;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.model.retrieval.source.RetrievalSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerSnapshotTest {

    @Test
    void capturesSelfContainedRetrievalDataWithoutEmbeddingsOrSourceDocuments() {
        UUID sourceId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Chunk chunk = Chunk.create(chunkId, new Source(sourceId, "Title", "Full source document", UUID.randomUUID()),
                "Selected passage", 0, true, new float[]{0.1f, 0.2f});
        RankedChunk rankedChunk = new RankedChunk(chunk, 1, 0.89, RetrievalSource.HYBRID);
        KnowledgeTriple triple = KnowledgeTriple.create("Spring", "USES", "Postgres", Passage.fromChunkId(chunkId), 0.7);

        AnswerSnapshot snapshot = AnswerSnapshot.from(
                new AnswerSnapshot.RetrievalParameters(10, 30, 10, 20, 0.45, 0.85),
                List.of(GraphSeed.passage(chunkId, 0.91), GraphSeed.concept("Spring", 0.8)),
                List.of(rankedChunk), Map.of(chunkId, 0.91), List.of(triple));

        assertThat(snapshot.rankedPassages()).singleElement().satisfies(passage -> {
            assertThat(passage.chunkId()).isEqualTo(chunkId);
            assertThat(passage.sourceId()).isEqualTo(sourceId);
            assertThat(passage.content()).isEqualTo("Selected passage");
            assertThat(passage.denseScore()).isEqualTo(0.91);
            assertThat(passage.graphScore()).isEqualTo(0.89);
            assertThat(passage.retrievalSource()).isEqualTo("HYBRID");
        });
        assertThat(snapshot.seeds()).extracting(AnswerSnapshot.GraphSeedSnapshot::type).containsExactly("PASSAGE", "CONCEPT");
        assertThat(snapshot.activatedTriples()).singleElement()
                .extracting(AnswerSnapshot.ActivatedTripleSnapshot::chunkId,
                        AnswerSnapshot.ActivatedTripleSnapshot::structuralWeight)
                .containsExactly(chunkId, 0.7);
    }
}
