package com.kairos.context_engine.infrastructure.event.consumer;

import com.kairos.context_engine.application.command.GenerateSourceContextCommand;
import com.kairos.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CreatedSourceListener {

    private final GenerateSourceContextUseCase generateSourceContextUseCase;

    public CreatedSourceListener(GenerateSourceContextUseCase generateSourceContextUseCase) {
        this.generateSourceContextUseCase = generateSourceContextUseCase;
    }

    @Async
    @TransactionalEventListener(CreatedSourceEvent.class)
    public void handleCreatedSourceEvent(CreatedSourceEvent event) {
        log.info("Received CreatedSourceEvent for sourceId: {}", event.sourceId());

        var command = GenerateSourceContextCommand.of(event.sourceId());
        generateSourceContextUseCase.execute(command);

        log.info("Finished processing CreatedSourceEvent for sourceId: {}", event.sourceId());
    }

}
