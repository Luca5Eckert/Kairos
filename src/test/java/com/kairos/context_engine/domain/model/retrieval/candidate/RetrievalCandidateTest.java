package com.kairos.context_engine.domain.model.retrieval.candidate;

import com.kairos.module.context_engine.domain.model.knowledge.Concept;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.ConceptCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalCandidateTest {

    @Test
    void tripleCandidate_shouldTrimTextFields() {
        UUID chunkId = UUID.randomUUID();

        TripleCandidate candidate = new TripleCandidate(
                "  key  ",
                "  subject  ",
                "  predicate  ",
                "  object  ",
                chunkId,
                0.75
        );

        assertThat(candidate.key()).isEqualTo("key");
        assertThat(candidate.subject()).isEqualTo("subject");
        assertThat(candidate.predicate()).isEqualTo("predicate");
        assertThat(candidate.object()).isEqualTo("object");
        assertThat(candidate.chunkId()).isEqualTo(chunkId);
        assertThat(candidate.similarityScore()).isEqualTo(0.75);
    }

    @Test
    void tripleCandidate_shouldRejectInvalidFields() {
        UUID chunkId = UUID.randomUUID();

        assertThatThrownBy(() -> new TripleCandidate(null, "s", "p", "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate key cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate(" ", "s", "p", "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate key cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", null, "p", "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate subject cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", " ", "p", "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate subject cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", null, "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate predicate cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", " ", "o", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate predicate cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", "p", null, chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate object cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", "p", " ", chunkId, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate object cannot be null or blank");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", "p", "o", null, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate chunkId cannot be null");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", "p", "o", chunkId, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate similarity score must be finite");
        assertThatThrownBy(() -> new TripleCandidate("k", "s", "p", "o", chunkId, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Triple candidate similarity score must be finite");
    }

    @Test
    void passageCandidate_shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new PassageCandidate(null, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage candidate chunkId cannot be null");
        assertThatThrownBy(() -> new PassageCandidate(UUID.randomUUID(), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage candidate denseScore must be finite");
        assertThatThrownBy(() -> new PassageCandidate(UUID.randomUUID(), Double.NEGATIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage candidate denseScore must be finite");
    }

    @Test
    void conceptCandidate_shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new ConceptCandidate(null, 0.7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Concept cannot be null");
        assertThatThrownBy(() -> new ConceptCandidate(Concept.create("Spring"), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SimilarityScore must be finite");
        assertThatThrownBy(() -> new ConceptCandidate(Concept.create("Spring"), Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("SimilarityScore must be finite");
    }
}
