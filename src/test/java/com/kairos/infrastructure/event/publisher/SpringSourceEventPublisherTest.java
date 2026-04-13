package com.kairos.infrastructure.event.publisher;

import com.kairos.domain.event.CreatedSourceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.Mockito.*;

class SpringSourceEventPublisherTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private SpringSourceEventPublisher publisher;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        publisher = new SpringSourceEventPublisher(applicationEventPublisher);
    }

    @Test
    void shouldPublishCreatedSourceEvent() {
        CreatedSourceEvent event = new CreatedSourceEvent(UUID.randomUUID(), "content");

        publisher.send(event);

        verify(applicationEventPublisher, times(1)).publishEvent(event);
    }
}