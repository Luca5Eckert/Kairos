package com.kairos.share.infrastructure.persistence.config;

import com.kairos.module.context_engine.infrastructure.graph.repository.Neo4jPassageNodeRepository;
import com.kairos.module.context_engine.infrastructure.graph.repository.Neo4jPhraseNodeRepository;
import com.kairos.module.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.module.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.module.context_engine.infrastructure.relational.entity.TripleEntity;
import com.kairos.module.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.source.JpaSourceRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.triple.JpaTripleRepository;
import com.kairos.module.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.module.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackageClasses = {
                UserEntityJpaRepository.class,
                JpaChunkRepository.class,
                JpaSourceRepository.class,
                JpaTripleRepository.class
        }
)
@EntityScan(
        basePackageClasses = {
                UserEntity.class,
                ChunkEntity.class,
                SourceEntity.class,
                TripleEntity.class
        }
)
@EnableNeo4jRepositories(
        basePackageClasses = {
                Neo4jPhraseNodeRepository.class,
                Neo4jPassageNodeRepository.class
        }
)
public class PersistenceConfig {
}
