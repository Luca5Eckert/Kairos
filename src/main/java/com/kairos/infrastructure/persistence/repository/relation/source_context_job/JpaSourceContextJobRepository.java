package com.kairos.infrastructure.persistence.repository.relation.source_context_job;

import com.kairos.infrastructure.persistence.entity.relation.SourceContextJobEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSourceContextJobRepository extends JpaRepository<SourceContextJobEntity, UUID> {

    Optional<SourceContextJobEntity> findBySourceId(UUID sourceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job
            from SourceContextJobEntity job
            where job.status in :statuses
              and job.nextAttemptAt <= :now
            order by job.nextAttemptAt asc
            """)
    List<SourceContextJobEntity> findDueJobs(
            @Param("statuses") Collection<String> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );
}
