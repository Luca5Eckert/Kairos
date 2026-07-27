package com.kairos.context_engine.infrastructure.event.consumer;

import com.kairos.module.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.module.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.module.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.module.context_engine.infrastructure.event.consumer.CreatedSourceListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.*;

class CreatedSourceListenerTest {

    private GenerateSourceContextUseCase useCase;
    private CreatedSourceListener listener;

    @BeforeEach
    void setUp() {
        useCase = mock(GenerateSourceContextUseCase.class);
        listener = new CreatedSourceListener(useCase);
    }

    @Test
    void shouldHandleCreatedSourceEvent() {
        UUID sourceId = UUID.randomUUID();
        CreatedSourceEvent event = new CreatedSourceEvent(sourceId);

        listener.handleCreatedSourceEvent(event);

        ArgumentCaptor<GenerateSourceContextCommand> captor =
                ArgumentCaptor.forClass(GenerateSourceContextCommand.class);

        verify(useCase, times(1)).execute(captor.capture());

        GenerateSourceContextCommand command = captor.getValue();

        assertThat(command.sourceId()).isEqualTo(sourceId);
    }

    @Test
    void shouldPropagateUseCaseFailures() {
        UUID sourceId = UUID.randomUUID();
        CreatedSourceEvent event = new CreatedSourceEvent(sourceId);
        doThrow(new RuntimeException("Context generation failed"))
                .when(useCase).execute(any(GenerateSourceContextCommand.class));

        assertThatThrownBy(() -> listener.handleCreatedSourceEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Context generation failed");

        ArgumentCaptor<GenerateSourceContextCommand> captor =
                ArgumentCaptor.forClass(GenerateSourceContextCommand.class);
        verify(useCase).execute(captor.capture());
        assertThat(captor.getValue().sourceId()).isEqualTo(sourceId);
    }
}
