package com.kairos.application.use_case;

import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPendingSourceContextJobsUseCaseTest {

    @Mock private SourceContextJobRepository sourceContextJobRepository;
    @Mock private ProcessSourceContextJobUseCase processSourceContextJobUseCase;

    private ProcessPendingSourceContextJobsUseCase useCase;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-13T10:15:30Z"), ZoneOffset.UTC);
        useCase = new ProcessPendingSourceContextJobsUseCase(
                sourceContextJobRepository,
                processSourceContextJobUseCase,
                new SourceContextJobProperties(5, 10, Duration.ofSeconds(30), 2.0, Duration.ofMinutes(30)),
                clock
        );
    }

    @Test
    void execute_claimsDueJobsAndDispatchesProcessing() {
        SourceContextJob job = SourceContextJob.create(UUID.randomUUID(), 5, Instant.now(clock));
        when(sourceContextJobRepository.claimDueJobs(Instant.now(clock), 10)).thenReturn(List.of(job));

        useCase.execute();

        verify(processSourceContextJobUseCase).execute(job);
    }
}
