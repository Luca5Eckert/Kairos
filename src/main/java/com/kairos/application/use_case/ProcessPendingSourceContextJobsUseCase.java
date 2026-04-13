package com.kairos.application.use_case;

import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ProcessPendingSourceContextJobsUseCase {

    private final SourceContextJobRepository sourceContextJobRepository;
    private final ProcessSourceContextJobUseCase processSourceContextJobUseCase;
    private final SourceContextJobProperties jobProperties;
    private final Clock clock;

    public void execute() {
        Instant now = Instant.now(clock);
        sourceContextJobRepository.claimDueJobs(now, jobProperties.batchSize())
                .forEach(processSourceContextJobUseCase::execute);
    }
}
