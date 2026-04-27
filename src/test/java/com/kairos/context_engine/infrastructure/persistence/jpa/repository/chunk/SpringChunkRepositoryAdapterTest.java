package com.kairos.context_engine.infrastructure.persistence.jpa.repository.chunk;

import com.kairos.context_engine.domain.model.Chunk;
import com.kairos.context_engine.domain.model.Source;
import com.kairos.context_engine.infrastructure.persistence.jpa.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.persistence.jpa.entity.SourceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringChunkRepositoryAdapterTest {

    @Mock
    private JpaChunkRepository jpaChunkRepository;

    private SpringChunkRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpringChunkRepositoryAdapter(jpaChunkRepository);
    }

    @Test
    @DisplayName("findAllBySourceId - delegates to the Spring Data source id query")
    void findAllBySourceId_delegatesToJpaRepository() {
        UUID sourceId = UUID.randomUUID();
        when(jpaChunkRepository.findAllBySource_Id(sourceId)).thenReturn(List.of());

        adapter.findAllBySourceId(sourceId);

        verify(jpaChunkRepository).findAllBySource_Id(sourceId);
    }

    @Test
    @DisplayName("findAllBySourceId - maps entities back to domain chunks")
    void findAllBySourceId_mapsEntitiesToDomainChunks() {
        UUID sourceId = UUID.randomUUID();
        SourceEntity source = new SourceEntity(sourceId, "Title", "Source content");
        ChunkEntity entity = new ChunkEntity(
                UUID.randomUUID(),
                source,
                "chunk content",
                3,
                true,
                new float[]{0.1f, 0.2f}
        );

        when(jpaChunkRepository.findAllBySource_Id(sourceId)).thenReturn(List.of(entity));

        List<Chunk> result = adapter.findAllBySourceId(sourceId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSource().getId()).isEqualTo(sourceId);
        assertThat(result.getFirst().getContent()).isEqualTo("chunk content");
        assertThat(result.getFirst().getIndex()).isEqualTo(3);
        assertThat(result.getFirst().isProcessed()).isTrue();
        assertThat(result.getFirst().getEmbedding()).containsExactly(0.1f, 0.2f);
    }

    @Test
    @DisplayName("save - persists a domain chunk and returns the saved entity as domain")
    void save_mapsDomainToEntityAndBack() {
        Source source = new Source(UUID.randomUUID(), "Title", "Source content");
        Chunk chunk = Chunk.create(UUID.randomUUID(), source, "chunk content", 0, false, new float[]{0.3f});
        ChunkEntity savedEntity = new ChunkEntity(
                chunk.getId(),
                SourceEntity.of(source),
                chunk.getContent(),
                chunk.getIndex(),
                chunk.isProcessed(),
                chunk.getEmbedding()
        );

        when(jpaChunkRepository.save(any(ChunkEntity.class))).thenReturn(savedEntity);

        Chunk result = adapter.save(chunk);

        assertThat(result.getId()).isEqualTo(chunk.getId());
        assertThat(result.getSource().getId()).isEqualTo(source.getId());
        assertThat(result.getContent()).isEqualTo("chunk content");
        assertThat(result.getEmbedding()).containsExactly(0.3f);
    }
}
