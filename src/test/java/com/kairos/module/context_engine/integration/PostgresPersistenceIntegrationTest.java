package com.kairos.module.context_engine.integration;

import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.content.TripleExtracted;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.chunk.JpaChunkRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.chunk.SpringChunkRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.source.SpringSourceRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.triple.SpringTripleRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.history.SpringHistoryRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.semantic.SemanticSearchAdapter;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PostgresPersistenceIntegrationTest.IntegrationApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@Import({
        SemanticSearchAdapter.class,
        SpringSourceRepositoryAdapter.class,
        SpringChunkRepositoryAdapter.class,
        SpringTripleRepositoryAdapter.class,
        SpringHistoryRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
class PostgresPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("kairos")
            .withUsername("kairos")
            .withPassword("kairos");

    @Autowired
    private Flyway flyway;

    @Autowired
    private SpringSourceRepositoryAdapter sourceRepository;

    @Autowired
    private SpringChunkRepositoryAdapter chunkRepository;

    @Autowired
    private SpringTripleRepositoryAdapter tripleRepository;

    @Autowired
    private SemanticSearchAdapter semanticSearch;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private JpaChunkRepository jpaChunkRepository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @AfterEach
    void clearPersistenceContext() {
        entityManager.clear();
    }

    @Test
    void appliesInitialMigrationBeforeJpaValidation() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    }

    @Test
    void persistsVector384AndKeepsSemanticResultsScopedAndOrderedBySimilarity() {
        UUID authorA = UUID.randomUUID();
        UUID authorB = UUID.randomUUID();
        Source sourceA = source(authorA, "A");
        Source sourceB = source(authorB, "B");
        sourceRepository.save(sourceA);
        sourceRepository.save(sourceB);

        assertThat(sourceRepository.findByAuthorIdAndTitleAndContent(authorA, sourceA.getTitle(), sourceA.getContent()))
                .map(Source::getId)
                .contains(sourceA.getId());

        Chunk closest = chunk(sourceA, "closest", vector(1f, 0f));
        Chunk second = chunk(sourceA, "second", vector(0.8f, 0.6f));
        Chunk foreign = chunk(sourceB, "foreign", vector(1f, 0f));
        chunkRepository.save(closest);
        chunkRepository.save(second);
        chunkRepository.save(foreign);
        entityManager.flush();
        entityManager.clear();

        assertThat(jpaChunkRepository.findById(closest.getId()).orElseThrow().getEmbedding())
                .hasSize(384)
                .containsExactly(closest.getEmbedding());

        assertThat(semanticSearch.findPassageCandidate(vector(1f, 0f), authorA, 10))
                .extracting(candidate -> candidate.chunkId())
                .containsExactly(closest.getId(), second.getId());
    }

    @Test
    void hydratesOnlyRequestedUsersChunksAndPreservesGraphScoreOrder() {
        UUID authorA = UUID.randomUUID();
        UUID authorB = UUID.randomUUID();
        Source sourceA = source(authorA, "A");
        Source sourceB = source(authorB, "B");
        sourceRepository.save(sourceA);
        sourceRepository.save(sourceB);
        Chunk first = chunk(sourceA, "first", vector(1f, 0f));
        Chunk foreign = chunk(sourceB, "foreign", vector(1f, 0f));
        chunkRepository.save(first);
        chunkRepository.save(foreign);
        entityManager.flush();

        assertThat(semanticSearch.hydrateAndRankChunks(List.of(
                new ScoredPassage(foreign.getId(), 0.99),
                new ScoredPassage(first.getId(), 0.70)
        ), authorA))
                .singleElement()
                .satisfies(ranked -> {
                    assertThat(ranked.chunk().getId()).isEqualTo(first.getId());
                    assertThat(ranked.rank()).isEqualTo(1);
                    assertThat(ranked.score()).isEqualTo(0.70);
                });
    }

    @Test
    void persistsTriplesAndFiltersVectorSearchByAuthor() {
        UUID authorA = UUID.randomUUID();
        UUID authorB = UUID.randomUUID();
        Source sourceA = source(authorA, "A");
        Source sourceB = source(authorB, "B");
        sourceRepository.save(sourceA);
        sourceRepository.save(sourceB);
        Chunk chunkA = chunk(sourceA, "A chunk", vector(1f, 0f));
        Chunk chunkB = chunk(sourceB, "B chunk", vector(1f, 0f));
        chunkRepository.save(chunkA);
        chunkRepository.save(chunkB);
        tripleRepository.saveAll(List.of(triple("spring", "USES", "postgres", chunkA, vector(1f, 0f))));
        tripleRepository.saveAll(List.of(triple("secret", "USES", "other", chunkB, vector(1f, 0f))));
        entityManager.flush();
        entityManager.clear();

        assertThat(semanticSearch.findTripleCandidates(vector(1f, 0f), authorA, 10))
                .singleElement()
                .satisfies(candidate -> {
                    assertThat(candidate.subject()).isEqualTo("spring");
                    assertThat(candidate.chunkId()).isEqualTo(chunkA.getId());
                });
    }

    @Test
    void persistsVersionedAnswerSnapshotWithoutLoadingCurrentKnowledgeRecords() {
        UUID userId = UUID.randomUUID();
        Question question = Question.create(userId, "How does retrieval work?");
        AnswerSnapshot snapshot = new AnswerSnapshot("hipporag-2",
                new AnswerSnapshot.RetrievalParameters(10, 30, 10, 20, 0.45, 0.85), List.of(), List.of(), List.of());
        Answer first = Answer.create(question.id(), snapshot);
        Answer second = Answer.create(question.id(), snapshot);

        historyRepository.saveQuestion(question);
        historyRepository.saveAnswer(first);
        historyRepository.saveAnswer(second);
        entityManager.flush();
        entityManager.clear();

        assertThat(historyRepository.findQuestionByIdAndUserId(question.id(), userId)).contains(question);
        assertThat(historyRepository.findAnswersByQuestionIdAndUserId(question.id(), userId))
                .extracting(Answer::snapshot)
                .containsExactly(snapshot, snapshot);
        assertThat(historyRepository.findAnswersByQuestionIdAndUserId(question.id(), UUID.randomUUID())).isEmpty();
    }

    private Source source(UUID authorId, String suffix) {
        return new Source(UUID.randomUUID(), "source-" + suffix, "content-" + suffix, authorId);
    }

    private Chunk chunk(Source source, String content, float[] embedding) {
        return Chunk.create(UUID.randomUUID(), source, content, 0, true, embedding);
    }

    private TripleExtracted triple(String subject, String predicate, String object, Chunk chunk, float[] embedding) {
        TripleExtracted triple = TripleExtracted.create(subject, predicate, object, chunk);
        triple.addEmbedding(embedding);
        return triple;
    }

    private float[] vector(float first, float second) {
        float[] vector = new float[384];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.kairos.module.context_engine.infrastructure.relational.entity")
    @EnableJpaRepositories(basePackages = {
            "com.kairos.module.context_engine.infrastructure.relational.repository.chunk",
            "com.kairos.module.context_engine.infrastructure.relational.repository.source",
            "com.kairos.module.context_engine.infrastructure.relational.repository.triple",
            "com.kairos.module.context_engine.infrastructure.relational.repository.history"
    })
    static class IntegrationApplication {
    }
}
