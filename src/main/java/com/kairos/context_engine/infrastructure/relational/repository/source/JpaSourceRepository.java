package com.kairos.context_engine.infrastructure.relational.repository.source;

import com.kairos.context_engine.infrastructure.relational.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {


}