package com.kairos.context_engine.infrastructure.relational.repository.triple;

import com.kairos.context_engine.domain.model.content.Chunk;
import com.kairos.context_engine.domain.model.content.Source;
import com.kairos.context_engine.domain.model.content.TripleExtracted;
import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringTripleRepositoryAdapterTest {

    @Mock
    private JpaTripleRepository jpaTripleRepository;

    private SpringTripleRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpringTripleRepositoryAdapter(jpaTripleRepository);
    }

    @Test
    @DisplayName("saveAll - maps extracted triples to relational entities")
    void saveAll_mapsExtractedTriplesToRelationalEntities() {
        Source source = new Source(UUID.randomUUID(), "Title", "Source content");
        Chunk chunk = Chunk.create(UUID.randomUUID(), source, "chunk content", 0, true, new float[]{0.1f});
        TripleExtracted triple = TripleExtracted.create("spring", "USES", "jpa", chunk);
        triple.addEmbedding(new float[]{0.2f, 0.3f});

        adapter.saveAll(List.of(triple));

        ArgumentCaptor<List<TripleEntity>> captor = ArgumentCaptor.captor();
        verify(jpaTripleRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        TripleEntity entity = captor.getValue().getFirst();
        assertThat(entity.getKey()).isEqualTo("spring-USES-jpa");
        assertThat(entity.getSubject()).isEqualTo("spring");
        assertThat(entity.getPredicate()).isEqualTo("USES");
        assertThat(entity.getObject()).isEqualTo("jpa");
        assertThat(entity.getEmbedding()).containsExactly(0.2f, 0.3f);
        assertThat(entity.getChunk().getId()).isEqualTo(chunk.getId());
    }
}
