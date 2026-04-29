package com.kairos.context_engine.domain.model.knowledge;

import com.kairos.context_engine.domain.model.Triple;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeTripleTest {

    @Test
    void createFromTriple_shouldKeepStructuralFieldsAndPassage() {
        Passage passage = Passage.fromChunkId(UUID.randomUUID());
        Triple extractedTriple = new Triple("Subject", "RELATES_TO", "Object", 0.42);

        KnowledgeTriple knowledgeTriple = KnowledgeTriple.create(extractedTriple, passage);

        assertThat(knowledgeTriple.subject()).isEqualTo(new Concept("Subject"));
        assertThat(knowledgeTriple.predicate()).isEqualTo("RELATES_TO");
        assertThat(knowledgeTriple.object()).isEqualTo(new Concept("Object"));
        assertThat(knowledgeTriple.passage()).isEqualTo(passage);
        assertThat(knowledgeTriple.weight()).isEqualTo(0.42);
    }

    @Test
    void createFromFields_shouldNotRequireGraphMetricsOnConcepts() {
        Passage passage = Passage.fromChunkId(UUID.randomUUID());

        KnowledgeTriple knowledgeTriple = KnowledgeTriple.create(
                "Concept A",
                "CONNECTS_TO",
                "Concept B",
                passage,
                1.0
        );

        assertThat(knowledgeTriple.subject().name()).isEqualTo("Concept A");
        assertThat(knowledgeTriple.object().name()).isEqualTo("Concept B");
        assertThat(knowledgeTriple.weight()).isEqualTo(1.0);
    }
}
