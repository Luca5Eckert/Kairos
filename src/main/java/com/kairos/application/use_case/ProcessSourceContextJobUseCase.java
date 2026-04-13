package com.kairos.application.use_case;

import com.kairos.domain.graph.KnowledgeGraphStore;
import com.kairos.domain.graph.TripleExtractor;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.KnowledgeTriple;
import com.kairos.domain.model.Source;
import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.domain.port.SourceRepository;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import com.kairos.infrastructure.gemini.GeminiFailureClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessSourceContextJobUseCase {

    private final TripleExtractor tripleExtractor;
    private final KnowledgeGraphStore knowledgeGraphStore;
    private final ChunkRepository chunkRepository;
    private final SourceRepository sourceRepository;
    private final SourceContextJobRepository sourceContextJobRepository;
    private final SourceContextJobProperties jobProperties;
    private final GeminiFailureClassifier geminiFailureClassifier;
    private final Clock clock;

    @Transactional
    public void execute(SourceContextJob job) {
        Source source = sourceRepository.findById(job.getSourceId())
                .orElseThrow(() -> new RuntimeException("Source not found for id: " + job.getSourceId()));

        List<Chunk> pendingChunks = chunkRepository.findBySourceId(job.getSourceId()).stream()
                .filter(chunk -> !chunk.isTriplesExtracted())
                .toList();

        if (pendingChunks.isEmpty()) {
            source.markCompleted();
            sourceRepository.save(source);
            job.markCompleted(Instant.now(clock));
            sourceContextJobRepository.save(job);
            return;
        }

        int successCount = 0;
        int retryableFailureCount = 0;
        int terminalFailureCount = 0;
        String lastError = null;

        for (Chunk chunk : pendingChunks) {
            try {
                List<KnowledgeTriple> knowledgeTriples = tripleExtractor.extract(chunk.getContent()).stream()
                        .map(triple -> KnowledgeTriple.create(triple, chunk.getId()))
                        .toList();

                knowledgeGraphStore.saveAllForChunk(chunk.getId(), knowledgeTriples);
                chunkRepository.save(chunk.markTriplesExtracted());
                successCount++;
            } catch (Exception exception) {
                lastError = exception.getMessage();

                if (geminiFailureClassifier.isRetryable(exception)) {
                    retryableFailureCount++;
                    log.warn("Retryable Gemini failure while processing source {} chunk {}: {}",
                            source.getId(), chunk.getId(), exception.getMessage());
                    continue;
                }

                terminalFailureCount++;
                log.error("Terminal Gemini failure while processing source {} chunk {}: {}",
                        source.getId(), chunk.getId(), exception.getMessage(), exception);
            }
        }

        finalizeJob(job, source, successCount, retryableFailureCount, terminalFailureCount, lastError);
    }

    private void finalizeJob(
            SourceContextJob job,
            Source source,
            int successCount,
            int retryableFailureCount,
            int terminalFailureCount,
            String lastError
    ) {
        Instant now = Instant.now(clock);

        if (retryableFailureCount == 0 && terminalFailureCount == 0) {
            source.markCompleted();
            sourceRepository.save(source);
            job.markCompleted(now);
            sourceContextJobRepository.save(job);
            return;
        }

        if (retryableFailureCount > 0 && terminalFailureCount == 0 && job.canRetry()) {
            Duration delay = computeRetryDelay(job.getAttemptCount());
            job.scheduleRetry(lastError, now.plus(delay), now);
            sourceRepository.save(source);
            sourceContextJobRepository.save(job);
            return;
        }

        if (successCount > 0 || hasExtractedChunks(source.getId())) {
            source.markPartialFailure();
        } else {
            source.markFailed();
        }

        sourceRepository.save(source);
        job.markFailed(lastError != null ? lastError : "Source context processing failed", now);
        sourceContextJobRepository.save(job);
    }

    private boolean hasExtractedChunks(java.util.UUID sourceId) {
        return chunkRepository.findBySourceId(sourceId).stream().anyMatch(Chunk::isTriplesExtracted);
    }

    private Duration computeRetryDelay(int attemptCount) {
        double multiplier = Math.pow(jobProperties.retryMultiplier(), attemptCount);
        long delayMillis = Math.round(jobProperties.initialRetryDelay().toMillis() * multiplier);
        long boundedMillis = Math.min(delayMillis, jobProperties.maxRetryDelay().toMillis());
        return Duration.ofMillis(boundedMillis);
    }
}
