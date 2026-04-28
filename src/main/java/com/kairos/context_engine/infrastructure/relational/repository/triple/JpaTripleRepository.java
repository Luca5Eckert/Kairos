package com.kairos.context_engine.infrastructure.relational.repository.triple;

import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTripleRepository extends JpaRepository<TripleEntity, String> {
}
