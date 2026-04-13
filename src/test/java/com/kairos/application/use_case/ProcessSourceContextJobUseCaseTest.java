package com.kairos.application.use_case;

import com.kairos.domain.graph.KnowledgeGraphStore;
import com.kairos.domain.graph.TripleExtractor;
import com.kairos.domain.model.Chunk;
import com.kairos.domain.model.Source;
import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.model.SourceContextJobStatus;
import com.kairos.domain.model.SourceStatus;
import com.kairos.domain.model.Triple;
import com.kairos.domain.port.ChunkRepository;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.domain.port.SourceRepository;
import com.kairos.infrastructure.context.config.SourceContextJobProperties;
import com.kairos.infrastructure.gemini.GeminiFailureClassifier;
import com.kairos.infrastructure.gemini.exception.GeminiIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessSourceContextJobUseCaseTest {

    @Mock private TripleExtractor tripleExtractor;
    @Mock private KnowledgeGraphStore knowledgeGraphStore;
    @Mock private ChunkRepository chunkRepository;
    @Mock private SourceRepository sourceRepository;
    @Mock private SourceContextJobRepository sourceContextJobRepository;
    @Mock private GeminiFailureClassifier geminiFailureClassifier;

    private ProcessSourceContextJobUseCase useCase;
    private Clock clock;
    private Source source;
    private UUID sourceId;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-13T10:15:30Z"), ZoneOffset.UTC);
        useCase = new ProcessSourceContextJobUseCase(
                tripleExtractor,
                knowledgeGraphStore,
                chunkRepository,
                sourceRepository,
                sourceContextJobRepository,
                new SourceContextJobProperties(5, 10, Duration.ofSeconds(30), 2.0, Duration.ofMinutes(30)),
                geminiFailureClassifier,
                clock
        );

        sourceId = UUID.randomUUID();
        source = new Source(sourceId, "Source", "content", SourceStatus.PROCESSING);
        when(sourceRepository.findById(sourceId)).thenReturn(Optional.of(source));
        lenient().when(chunkRepository.save(any(Chunk.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sourceContextJobRepository.save(any(SourceContextJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("execute â€” marks job completed and source completed when all chunks succeed")
    void execute_allChunksSucceed_marksCompleted() {
        Chunk chunk = Chunk.create(UUID.randomUUID(), source, "alpha", 0, new float[]{0.1f});
        SourceContextJob job = SourceContextJob.create(sourceId, 5, Instant.now(clock));

        when(chunkRepository.findBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(tripleExtractor.extract("alpha")).thenReturn(List.of(new Triple("a", "USES", "b")));

        useCase.execute(job);

        verify(knowledgeGraphStore).saveAllForChunk(eq(chunk.getId()), anyList());
        verify(chunkRepository).save(org.mockito.ArgumentMatchers.argThat(saved -> saved.isTriplesExtracted() && saved.getId().equals(chunk.getId())));
        assertThat(source.getStatus()).isEqualTo(SourceStatus.COMPLETED);
        assertThat(job.getStatus()).isEqualTo(SourceContextJobStatus.COMPLETED);
    }

    @Test
    @DisplayName("execute â€” schedules retry when Gemini downtime leaves pending chunks")
    void execute_retryableFailure_schedulesRetry() {
        Chunk chunk = Chunk.create(UUID.randomUUID(), source, "alpha", 0, new float[]{0.1f});
        SourceContextJob job = SourceContextJob.create(sourceId, 5, Instant.now(clock));

        when(chunkRepository.findBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(tripleExtractor.extract("alpha")).thenThrow(new GeminiIntegrationException("temporary outage"));
        when(geminiFailureClassifier.isRetryable(any())).thenReturn(true);

        useCase.execute(job);

        assertThat(source.getStatus()).isEqualTo(SourceStatus.PROCESSING);
        assertThat(job.getStatus()).isEqualTo(SourceContextJobStatus.RETRY_SCHEDULED);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getNextAttemptAt()).isAfter(Instant.now(clock));
    }

    @Test
    @DisplayName("execute â€” marks partial failure when some chunks succeed and retries are exhausted")
    void execute_partialSuccessWithExhaustedRetries_marksPartialFailure() {
        Chunk successChunk = Chunk.create(UUID.randomUUID(), source, "alpha", 0, new float[]{0.1f});
        Chunk failingChunk = Chunk.create(UUID.randomUUID(), source, "beta", 1, new float[]{0.2f});
        SourceContextJob job = SourceContextJob.create(sourceId, 1, Instant.now(clock));
        job.scheduleRetry("previous retry", Instant.now(clock), Instant.now(clock));

        when(chunkRepository.findBySourceId(sourceId)).thenReturn(List.of(successChunk, failingChunk, successChunk.markTriplesExtracted()));
        when(tripleExtractor.extract("alpha")).thenReturn(List.of(new Triple("a", "USES", "b")));
        when(tripleExtractor.extract("beta")).thenThrow(new GeminiIntegrationException("still down"));
        when(geminiFailureClassifier.isRetryable(any())).thenReturn(true);

        useCase.execute(job);

        assertThat(source.getStatus()).isEqualTo(SourceStatus.PARTIAL_FAILURE);
        assertThat(job.getStatus()).isEqualTo(SourceContextJobStatus.FAILED);
    }

    @Test
    @DisplayName("execute â€” marks source failed when all chunks fail with non-retryable error")
    void execute_nonRetryableFailure_marksFailed() {
        Chunk chunk = Chunk.create(UUID.randomUUID(), source, "alpha", 0, new float[]{0.1f});
        SourceContextJob job = SourceContextJob.create(sourceId, 5, Instant.now(clock));

        when(chunkRepository.findBySourceId(sourceId)).thenReturn(List.of(chunk));
        when(tripleExtractor.extract("alpha")).thenThrow(new IllegalArgumentException("bad prompt"));
        when(geminiFailureClassifier.isRetryable(any())).thenReturn(false);

        useCase.execute(job);

        assertThat(source.getStatus()).isEqualTo(SourceStatus.FAILED);
        assertThat(job.getStatus()).isEqualTo(SourceContextJobStatus.FAILED);
    }

    @Test
    @DisplayName("execute â€” completes immediately when no pending chunks remain")
    void execute_noPendingChunks_completesImmediately() {
        Chunk completedChunk = Chunk.create(UUID.randomUUID(), source, "alpha", 0, new float[]{0.1f}).markTriplesExtracted();
        SourceContextJob job = SourceContextJob.create(sourceId, 5, Instant.now(clock));

        when(chunkRepository.findBySourceId(sourceId)).thenReturn(List.of(completedChunk));

        useCase.execute(job);

        verifyNoInteractions(tripleExtractor, knowledgeGraphStore);
        assertThat(source.getStatus()).isEqualTo(SourceStatus.COMPLETED);
        assertThat(job.getStatus()).isEqualTo(SourceContextJobStatus.COMPLETED);
    }
}
