package com.kairos.context_engine.infrastructure.event.publisher;

import com.kairos.context_engine.domain.event.CreatedSourceEvent;
import com.kairos.context_engine.domain.event.SourceEventPublisher;
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
}
