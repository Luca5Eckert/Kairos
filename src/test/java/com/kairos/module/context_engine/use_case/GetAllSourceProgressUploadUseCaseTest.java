package com.kairos.module.context_engine.use_case;

import com.kairos.module.context_engine.application.use_case.GetAllSourceProgressUploadUseCase;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.domain.port.repository.SourceRepository;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAllSourceProgressUploadUseCaseTest {

    @Mock private SourceRepository sourceRepository;
    @Mock private RequestContextProvider contextProvider;

    @InjectMocks
    private GetAllSourceProgressUploadUseCase useCase;

    @Test
    void execute_retrievesProgressForTheCurrentUser() {
        UUID authorId = UUID.randomUUID();
        List<SourceProgressUpload> expected = List.of(
                new SourceProgressUpload(new Source("RAG notes", "content"), 3, 2)
        );
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(authorId, "lucas@example.com", List.of()));
        when(sourceRepository.findAllSourcesProgressByAuthorId(authorId)).thenReturn(expected);

        List<SourceProgressUpload> result = useCase.execute();

        assertThat(result).isSameAs(expected);
        verify(contextProvider).getRequestContext();
        verify(sourceRepository).findAllSourcesProgressByAuthorId(authorId);
    }

    @Test
    void execute_returnsEmptyListWhenCurrentUserHasNoSources() {
        UUID authorId = UUID.randomUUID();
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(authorId, "lucas@example.com", List.of()));
        when(sourceRepository.findAllSourcesProgressByAuthorId(authorId)).thenReturn(List.of());

        List<SourceProgressUpload> result = useCase.execute();

        assertThat(result).isEmpty();
        verify(sourceRepository).findAllSourcesProgressByAuthorId(authorId);
    }
}
