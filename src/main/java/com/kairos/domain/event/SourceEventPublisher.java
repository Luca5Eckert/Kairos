package com.kairos.domain.event;

public interface SourceEventPublisher {

    void send(CreatedSourceEvent event);
}
