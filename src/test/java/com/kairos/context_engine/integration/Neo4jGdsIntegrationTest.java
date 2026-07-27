package com.kairos.context_engine.integration;

import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.infrastructure.graph.adapter.HippoRagKnowledgeGraphSearchAdapter;
import com.kairos.module.context_engine.infrastructure.graph.adapter.KnowledgeGraphStoreAdapter;
import com.kairos.module.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor;
import com.kairos.module.context_engine.infrastructure.graph.executor.KnowledgeGraphMutationExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class Neo4jGdsIntegrationTest {

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5.26")
            .withAdminPassword("test-password")
            .withEnv("NEO4J_PLUGINS", "[\"graph-data-science\"]")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "gds.*");

    private Driver driver;

    @AfterEach
    void cleanGraph() {
        if (driver != null) {
            try (Session session = driver.session()) {
                session.run("MATCH (n) DETACH DELETE n").consume();
            }
            driver.close();
            driver = null;
        }
    }

    @Test
    void expandsOnlyTheRequestingUsersPassagesAndTriplesAndDropsTheProjection() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();
        KnowledgeGraphStoreAdapter store = new KnowledgeGraphStoreAdapter(new KnowledgeGraphMutationExecutor(driver()));
        KnowledgeGraphGdsExecutor executor = new KnowledgeGraphGdsExecutor(driver());
        HippoRagKnowledgeGraphSearchAdapter search = new HippoRagKnowledgeGraphSearchAdapter(executor, 10, 0.85, 0.001);

        store.saveAllForChunk(chunkA, userA, List.of(KnowledgeTriple.create(
                "Spring", "USES", "Postgres", Passage.fromChunkId(chunkA), 1.0
        )));
        store.saveAllForChunk(chunkB, userB, List.of(KnowledgeTriple.create(
                "Spring", "USES", "Secret", Passage.fromChunkId(chunkB), 1.0
        )));

        var result = search.expandKnowledge(GraphSearchRequest.from(
                userA, List.of(GraphSeed.concept("Spring", 1.0)), 10
        ));

        assertThat(result.scoredPassages())
                .extracting(scored -> scored.chunkId())
                .containsExactly(chunkA);
        assertThat(result.activatedTriples())
                .singleElement()
                .satisfies(triple -> {
                    assertThat(triple.subject().name()).isEqualTo("Spring");
                    assertThat(triple.object().name()).isEqualTo("Postgres");
                    assertThat(triple.passage().chunkId()).isEqualTo(chunkA);
                });
        assertThat(executor.dropOrphanProjections()).isEmpty();
    }

    private Driver driver() {
        if (driver == null) {
            driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        }
        return driver;
    }
}
