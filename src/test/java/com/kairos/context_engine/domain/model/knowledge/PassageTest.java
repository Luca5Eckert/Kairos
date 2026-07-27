package com.kairos.context_engine.domain.model.knowledge;

import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassageTest {

    @Test
    void fromChunkId_shouldKeepStableChunkReference() {
        UUID chunkId = UUID.randomUUID();

        Passage passage = Passage.fromChunkId(chunkId);

        assertThat(passage.chunkId()).isEqualTo(chunkId);
    }

    @Test
    void fromChunkId_shouldRejectNullChunkId() {
        assertThatThrownBy(() -> Passage.fromChunkId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Passage chunkId cannot be null");
    }
}
