package com.kairos.module.context_engine.integration;

import com.kairos.module.context_engine.domain.model.history.Answer;
import com.kairos.module.context_engine.domain.model.history.AnswerSnapshot;
import com.kairos.module.context_engine.domain.model.history.HistoryPageRequest;
import com.kairos.module.context_engine.domain.model.history.Question;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.relational.entity.AnswerEntity;
import com.kairos.module.context_engine.infrastructure.relational.entity.QuestionEntity;
import com.kairos.module.context_engine.infrastructure.relational.repository.history.JpaAnswerRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.history.JpaQuestionRepository;
import com.kairos.module.context_engine.infrastructure.relational.repository.history.SpringHistoryRepositoryAdapter;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = HistoryPersistenceIntegrationTest.IntegrationApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(SpringHistoryRepositoryAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class HistoryPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("kairos")
            .withUsername("kairos")
            .withPassword("kairos");

    @Autowired private Flyway flyway;
    @Autowired private HistoryRepository historyRepository;
    @Autowired private EntityManager entityManager;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void pagesQuestionsAndAnswersWithUserScopeAndDeterministicDescendingOrder() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Instant older = Instant.parse("2026-01-01T00:00:00Z");
        Instant newer = Instant.parse("2026-01-02T00:00:00Z");
        Question first = new Question(UUID.randomUUID(), userId, "older", older);
        Question second = new Question(UUID.randomUUID(), userId, "newer", newer);
        Question foreign = new Question(UUID.randomUUID(), otherUserId, "foreign", newer);
        historyRepository.saveQuestion(first);
        historyRepository.saveQuestion(second);
        historyRepository.saveQuestion(foreign);

        AnswerSnapshot snapshot = new AnswerSnapshot("hipporag-2",
                new AnswerSnapshot.RetrievalParameters(10, 30, 10, 20, 0.45, 0.85), List.of(), List.of(), List.of());
        Answer olderAnswer = new Answer(UUID.randomUUID(), second.id(), 1, snapshot, older);
        Answer newerAnswer = new Answer(UUID.randomUUID(), second.id(), 1, snapshot, newer);
        historyRepository.saveAnswer(olderAnswer);
        historyRepository.saveAnswer(newerAnswer);
        entityManager.flush();
        entityManager.clear();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(historyRepository.findQuestionsByUserId(userId, new HistoryPageRequest(0, 20)).content())
                .extracting(question -> question.id())
                .containsExactly(second.id(), first.id());
        assertThat(historyRepository.findAnswersByQuestionIdAndUserId(second.id(), userId,
                new HistoryPageRequest(0, 1)))
                .satisfies(page -> {
                    assertThat(page.content()).extracting(Answer::id).containsExactly(newerAnswer.id());
                    assertThat(page.totalElements()).isEqualTo(2);
                });
        assertThat(historyRepository.findAnswersByQuestionIdAndUserId(second.id(), otherUserId,
                new HistoryPageRequest(0, 20)).content()).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {QuestionEntity.class, AnswerEntity.class})
    @EnableJpaRepositories(basePackageClasses = {JpaQuestionRepository.class, JpaAnswerRepository.class})
    static class IntegrationApplication {
    }
}
