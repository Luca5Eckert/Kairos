package com.kairos.domain.port;

import com.kairos.domain.model.Source;

public interface CreateSourceEventPublisher {

    void send(Source source);
}
