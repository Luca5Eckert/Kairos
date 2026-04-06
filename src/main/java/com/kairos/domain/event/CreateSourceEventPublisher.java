package com.kairos.domain.event;

import com.kairos.domain.model.Source;

public interface CreateSourceEventPublisher {

    void send(Source source);
}
