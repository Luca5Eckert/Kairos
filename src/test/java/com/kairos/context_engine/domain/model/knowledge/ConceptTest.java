package com.kairos.context_engine.domain.model.knowledge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConceptTest {

    @Test
    void create_shouldTrimConceptName() {
        Concept concept = Concept.create("  Backpropagation  ");

        assertThat(concept.name()).isEqualTo("Backpropagation");
    }

    @Test
    void create_shouldRejectBlankName() {
        assertThatThrownBy(() -> Concept.create("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Concept name cannot be null or blank");
    }
}
