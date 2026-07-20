package com.kairos.context_engine.integration;

import com.kairos.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.context_engine.domain.model.knowledge.Passage;
import com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchRequest;
import com.kairos.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.context_engine.infrastructure.graph.adapter.HippoRagKnowledgeGraphSearchAdapter;
import com.kairos.context_engine.infrastructure.graph.adapter.KnowledgeGraphStoreAdapter;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor;
import com.kairos.context_engine.infrastructure.graph.executor.KnowledgeGraphMutationExecutor;
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
import static org.assertj.core.api.Assertions.assertThatCode;

@Testcontainers(disabledWithoutDocker = true)
class Neo4jGraphIntegrationTest {

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5.26")
            .withAdminPassword("test-password");

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
    void writesPassagesAndTriplesWithUserScopedRelationships() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();
        KnowledgeGraphStoreAdapter store = new KnowledgeGraphStoreAdapter(new KnowledgeGraphMutationExecutor(driver()));

        store.savePassages(List.of(Passage.fromChunkId(chunkA)), userA);
        store.saveAllForChunk(chunkA, userA, List.of(KnowledgeTriple.create("Spring", "USES", "Postgres", Passage.fromChunkId(chunkA), 0.8)));
        store.saveAllForChunk(chunkB, userB, List.of(KnowledgeTriple.create("Secret", "USES", "Other", Passage.fromChunkId(chunkB), 0.9)));

        try (Session session = driver().session()) {
            long visibleContains = session.run("""
                    MATCH (:Passage {user_id: $userId})-[r:CONTAINS]->()
                    WHERE r.user_id = $userId
                    RETURN count(r) AS count
                    """, org.neo4j.driver.Values.parameters("userId", userA.toString()))
                    .single().get("count").asLong();
            long visibleTriples = session.run("""
                    MATCH (:PhraseNode)-[r:TRIPLE]->(:PhraseNode)
                    WHERE r.user_id = $userId
                    RETURN count(r) AS count
                    """, org.neo4j.driver.Values.parameters("userId", userA.toString()))
                    .single().get("count").asLong();

            assertThat(visibleContains).isEqualTo(2);
            assertThat(visibleTriples).isEqualTo(1);
        }
    }

    @Test
    void returnsEmptyExpansionAndCleansUpPredictablyWhenGdsIsUnavailable() {
        UUID userId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        KnowledgeGraphGdsExecutor executor = new KnowledgeGraphGdsExecutor(driver());
        HippoRagKnowledgeGraphSearchAdapter search = new HippoRagKnowledgeGraphSearchAdapter(executor, 5, 0.85, 0.001);

        assertThat(search.expandKnowledge(GraphSearchRequest.from(userId, List.of(GraphSeed.passage(chunkId, 1.0)), 10)))
                .isEqualTo(com.kairos.context_engine.domain.model.retrieval.graph.GraphSearchResult.empty());
        assertThatCode(search::cleanupOrphanProjections).doesNotThrowAnyException();
    }

    private Driver driver() {
        if (driver == null) {
            driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J.getAdminPassword()));
        }
        return driver;
    }
}
