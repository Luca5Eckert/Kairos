package com.kairos.infrastructure.event;

import com.kairos.domain.event.CreatedSourceEvent;
import com.kairos.domain.event.SourceEventPublisher;
import org.springframework.context.ApplicationEventPublisher;

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
