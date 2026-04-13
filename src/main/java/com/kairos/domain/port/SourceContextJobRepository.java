package com.kairos.domain.port;

import com.kairos.domain.model.SourceContextJob;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SourceContextJobRepository {

    SourceContextJob save(SourceContextJob job);

    Optional<SourceContextJob> findBySourceId(UUID sourceId);

    List<SourceContextJob> claimDueJobs(Instant now, int limit);
}
