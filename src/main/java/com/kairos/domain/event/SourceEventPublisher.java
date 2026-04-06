package com.kairos.domain.event;

public interface SourceEventPublisher {

    /**
     * Publish a CreatedSourceEvent to notify subscribers that a new source has been created and is ready for processing.
     * @param event The CreatedSourceEvent containing the source ID and content to be processed.
     */
    void send(CreatedSourceEvent event);
}
