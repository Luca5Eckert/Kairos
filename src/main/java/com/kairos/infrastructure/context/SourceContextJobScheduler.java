package com.kairos.infrastructure.context;

import com.kairos.application.use_case.ProcessPendingSourceContextJobsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceContextJobScheduler {

    private final ProcessPendingSourceContextJobsUseCase processPendingSourceContextJobsUseCase;

    @Scheduled(fixedDelayString = "${source-context-job.pollDelayMs:5000}")
    public void processPendingJobs() {
        log.debug("Polling pending source context jobs");
        processPendingSourceContextJobsUseCase.execute();
    }
}
