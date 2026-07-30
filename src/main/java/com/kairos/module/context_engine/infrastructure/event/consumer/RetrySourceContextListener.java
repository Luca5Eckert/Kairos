package com.kairos.module.context_engine.infrastructure.event.consumer;

import com.kairos.module.context_engine.application.use_case.GenerateSourceContextUseCase;
import com.kairos.module.context_engine.domain.event.RetrySourceContextEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetrySourceContextListener {

    private final GenerateSourceContextUseCase generateSourceContextUseCase;

    @Async
    @TransactionalEventListener(RetrySourceContextEvent.class)
    public void handle(RetrySourceContextEvent event) {
        log.info("Retrying failed source context for sourceId={} chunks={}",
                event.sourceId(), event.chunkIds());
        generateSourceContextUseCase.executeClaimed(event.sourceId(), event.chunkIds());
    }
}
