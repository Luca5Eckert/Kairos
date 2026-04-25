package com.kairos.context_engine.infrastructure.event.consumer;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.infrastructure.event.consumer.CreatedSourceListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

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
        String content = "test content";
        CreatedSourceEvent event = new CreatedSourceEvent(sourceId, content);

        listener.handleCreatedSourceEvent(event);

        ArgumentCaptor<GenerateSourceContextCommand> captor =
                ArgumentCaptor.forClass(GenerateSourceContextCommand.class);

        verify(useCase, times(1)).execute(captor.capture());

        GenerateSourceContextCommand command = captor.getValue();

        assert command.sourceId().equals(sourceId);
        assert command.content().equals(content);
    }
}