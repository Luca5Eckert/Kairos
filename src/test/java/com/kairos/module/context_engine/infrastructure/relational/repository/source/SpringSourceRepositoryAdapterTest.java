package com.kairos.module.context_engine.infrastructure.relational.repository.source;

import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.infrastructure.relational.projection.SourceProgressProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringSourceRepositoryAdapterTest {

    @Mock private JpaSourceRepository jpaSourceRepository;
    @Mock private SourceProgressProjection firstProjection;
    @Mock private SourceProgressProjection secondProjection;

    @Test
    void findAllSourcesProgressByAuthorId_mapsEveryProjectionFieldInOrder() {
        UUID authorId = UUID.randomUUID();
        when(firstProjection.getTitle()).thenReturn("First source");
        when(firstProjection.getContent()).thenReturn("first content");
        when(firstProjection.getTotalChunks()).thenReturn(5);
        when(firstProjection.getProcessedChunks()).thenReturn(3);
        when(secondProjection.getTitle()).thenReturn("Second source");
        when(secondProjection.getContent()).thenReturn("second content");
        when(secondProjection.getTotalChunks()).thenReturn(2);
        when(secondProjection.getProcessedChunks()).thenReturn(2);
        when(jpaSourceRepository.findAllSourcesProgressByAuthorId(authorId))
                .thenReturn(List.of(firstProjection, secondProjection));

        var adapter = new SpringSourceRepositoryAdapter(jpaSourceRepository);
        List<SourceProgressUpload> result = adapter.findAllSourcesProgressByAuthorId(authorId);

        assertThat(result)
                .extracting(upload -> upload.source().getTitle(), upload -> upload.source().getContent(), SourceProgressUpload::totalChunks, SourceProgressUpload::processedChunks)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("First source", "first content", 5, 3),
                        org.assertj.core.groups.Tuple.tuple("Second source", "second content", 2, 2)
                );
        assertThat(result).extracting(upload -> upload.source().getId()).doesNotHaveDuplicates();
        verify(jpaSourceRepository).findAllSourcesProgressByAuthorId(authorId);
    }

    @Test
    void findAllSourcesProgressByAuthorId_returnsEmptyListWhenJpaHasNoMatches() {
        UUID authorId = UUID.randomUUID();
        when(jpaSourceRepository.findAllSourcesProgressByAuthorId(authorId)).thenReturn(List.of());

        var adapter = new SpringSourceRepositoryAdapter(jpaSourceRepository);

        assertThat(adapter.findAllSourcesProgressByAuthorId(authorId)).isEmpty();
        verify(jpaSourceRepository).findAllSourcesProgressByAuthorId(authorId);
    }
}
