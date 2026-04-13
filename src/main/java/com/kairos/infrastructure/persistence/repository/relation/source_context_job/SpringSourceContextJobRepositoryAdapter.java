package com.kairos.infrastructure.persistence.repository.relation.source_context_job;

import com.kairos.domain.model.SourceContextJob;
import com.kairos.domain.model.SourceContextJobStatus;
import com.kairos.domain.port.SourceContextJobRepository;
import com.kairos.infrastructure.persistence.entity.relation.SourceContextJobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SpringSourceContextJobRepositoryAdapter implements SourceContextJobRepository {

    private final JpaSourceContextJobRepository repository;

    @Override
    public SourceContextJob save(SourceContextJob job) {
        return repository.save(SourceContextJobEntity.of(job)).toDomain();
    }

    @Override
    public Optional<SourceContextJob> findBySourceId(UUID sourceId) {
        return repository.findBySourceId(sourceId).map(SourceContextJobEntity::toDomain);
    }

    @Override
    @Transactional
    public List<SourceContextJob> claimDueJobs(Instant now, int limit) {
        List<SourceContextJobEntity> jobs = repository.findDueJobs(
                List.of(SourceContextJobStatus.PENDING.name(), SourceContextJobStatus.RETRY_SCHEDULED.name()),
                now,
                PageRequest.of(0, limit)
        );

        jobs.forEach(job -> {
            job.setStatus(SourceContextJobStatus.PROCESSING.name());
            job.setUpdatedAt(now);
        });

        repository.saveAll(jobs);

        return jobs.stream()
                .map(SourceContextJobEntity::toDomain)
                .toList();
    }
}
