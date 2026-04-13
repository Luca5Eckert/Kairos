package com.kairos.application.use_case;

import com.kairos.application.command.GenerateSourceContextCommand;
import com.kairos.application.command.UploadSourceCommand;
import com.kairos.domain.model.Source;
import com.kairos.domain.port.SourceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadSourceUseCaseTest {

    @Mock private SourceRepository sourceRepository;
    @Mock private GenerateSourceContextUseCase generateSourceContextUseCase;

    @InjectMocks
    private UploadSourceUseCase useCase;

    @Test
    @DisplayName("execute â€” returns the id of the created source")
    void execute_validCommand_returnsSourceId() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        UUID result = useCase.execute(command);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("execute â€” saves source with correct title and content")
    void execute_validCommand_savesSourceWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        useCase.execute(command);

        var captor = ArgumentCaptor.forClass(Source.class);
        verify(sourceRepository).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Clean Code");
        assertThat(captor.getValue().getContent()).isEqualTo("some content");
    }

    @Test
    @DisplayName("execute â€” dispatches source context generation with matching sourceId and content")
    void execute_validCommand_dispatchesContextGenerationWithCorrectFields() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        UUID returnedId = useCase.execute(command);

        var captor = ArgumentCaptor.forClass(GenerateSourceContextCommand.class);
        verify(generateSourceContextUseCase).execute(captor.capture());

        GenerateSourceContextCommand dispatched = captor.getValue();
        assertThat(dispatched.sourceId()).isEqualTo(returnedId);
        assertThat(dispatched.content()).isEqualTo("some content");
    }

    @Test
    @DisplayName("execute â€” saves source before dispatching source context generation")
    void execute_validCommand_saveHappensBeforeDispatch() {
        var command = new UploadSourceCommand("Clean Code", "some content", UUID.randomUUID());

        var inOrder = inOrder(sourceRepository, generateSourceContextUseCase);

        useCase.execute(command);

        inOrder.verify(sourceRepository).save(any(Source.class));
        inOrder.verify(generateSourceContextUseCase).execute(any(GenerateSourceContextCommand.class));
    }
}
