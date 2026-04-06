package com.kairos.domain.event;

import com.kairos.domain.model.Source;

public interface SourceEventPublisher {

    void send(CreatedSourceEvent event);
}
