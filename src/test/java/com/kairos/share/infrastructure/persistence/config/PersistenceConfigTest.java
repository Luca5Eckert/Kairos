package com.kairos.share.infrastructure.persistence.config;

import com.kairos.context_engine.infrastructure.graph.repository.Neo4jPassageNodeRepository;
import com.kairos.context_engine.infrastructure.graph.repository.Neo4jPhraseNodeRepository;
import com.kairos.context_engine.infrastructure.relational.entity.ChunkEntity;
import com.kairos.context_engine.infrastructure.relational.entity.SourceEntity;
import com.kairos.context_engine.infrastructure.relational.entity.TripleEntity;
import com.kairos.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import com.kairos.context_engine.infrastructure.relational.repository.source.JpaSourceRepository;
import com.kairos.context_engine.infrastructure.relational.repository.triple.JpaTripleRepository;
import com.kairos.user.infrastructure.persistence.entity.UserEntity;
import com.kairos.user.infrastructure.persistence.repository.UserEntityJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceConfigTest {

    @Test
    @DisplayName("persistence config is shared and scans all JPA and Neo4j stores explicitly")
    void persistenceConfig_scansJpaAndNeo4jStoresExplicitly() throws ClassNotFoundException {
        Class<?> configClass = Class.forName("com.kairos.share.infrastructure.persistence.config.PersistenceConfig");

        EnableJpaRepositories jpaRepositories = configClass.getAnnotation(EnableJpaRepositories.class);
        EntityScan entityScan = configClass.getAnnotation(EntityScan.class);
        EnableNeo4jRepositories neo4jRepositories = configClass.getAnnotation(EnableNeo4jRepositories.class);

        assertThat(jpaRepositories).isNotNull();
        assertThat(jpaRepositories.basePackageClasses())
                .containsExactlyInAnyOrder(
                        UserEntityJpaRepository.class,
                        JpaChunkRepository.class,
                        JpaSourceRepository.class,
                        JpaTripleRepository.class
                );

        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackageClasses())
                .containsExactlyInAnyOrder(
                        UserEntity.class,
                        ChunkEntity.class,
                        SourceEntity.class,
                        TripleEntity.class
                );

        assertThat(neo4jRepositories).isNotNull();
        assertThat(neo4jRepositories.basePackageClasses())
                .containsExactlyInAnyOrder(
                        Neo4jPhraseNodeRepository.class,
                        Neo4jPassageNodeRepository.class
                );
    }
}
