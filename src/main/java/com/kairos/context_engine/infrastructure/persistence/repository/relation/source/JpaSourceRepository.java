package com.kairos.context_engine.infrastructure.persistence.repository.relation.source;

import com.kairos.context_engine.infrastructure.persistence.entity.relation.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {



}