package com.kairos.infrastructure.persistence.repository;

import com.kairos.infrastructure.persistence.entity.SourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSourceRepository extends JpaRepository<SourceEntity, UUID> {
}
