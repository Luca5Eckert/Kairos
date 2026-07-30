package com.kairos.module.context_engine.infrastructure.event.publisher;

import com.kairos.module.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.module.context_engine.domain.event.RetrySourceContextEvent;
import com.kairos.module.context_engine.domain.port.event.SourceEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringSourceEventPublisher implements SourceEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringSourceEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void send(CreatedSourceEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void send(RetrySourceContextEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
