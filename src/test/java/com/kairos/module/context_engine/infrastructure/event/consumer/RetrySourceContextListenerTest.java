package com.kairos.module.context_engine.infrastructure.event.consumer;

import com.kairos.module.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.module.context_engine.domain.event.RetrySourceContextEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RetrySourceContextListenerTest {

    @Test
    void handle_processesExactlyTheClaimedChunks() {
        GenerateSourceContextUseCase useCase = mock(GenerateSourceContextUseCase.class);
        RetrySourceContextListener listener = new RetrySourceContextListener(useCase);
        UUID sourceId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        listener.handle(new RetrySourceContextEvent(sourceId, chunkIds));

        verify(useCase).executeClaimed(sourceId, chunkIds);
    }
}
