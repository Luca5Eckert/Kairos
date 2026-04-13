package com.kairos.infrastructure.persistence.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.kairos.infrastructure.persistence.repository.relation"
)
@EntityScan(
        basePackages = "com.kairos.infrastructure.persistence.entity.relation"
)
@EnableNeo4jRepositories(
        basePackages = "com.kairos.infrastructure.persistence.repository.graph"
)
public class PersistenceConfig {
}