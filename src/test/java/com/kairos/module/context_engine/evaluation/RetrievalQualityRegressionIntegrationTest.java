package com.kairos.module.context_engine.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.module.context_engine.application.query.SearchSourceQuery;
import com.kairos.module.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.content.TripleExtracted;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.PassageCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.candidate.TripleCandidate;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.module.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.infrastructure.config.RetrievalProperties;
import com.kairos.module.context_engine.infrastructure.embedding.onnx.OnnxEmbeddingProvider;
import com.kairos.module.context_engine.infrastructure.embedding.onnx.config.EmbeddingConfig;
import com.kairos.module.context_engine.infrastructure.embedding.onnx.factory.OrtTensorFactory;
import com.kairos.module.context_engine.infrastructure.graph.adapter.HippoRagKnowledgeGraphSearchAdapter;
import com.kairos.module.context_engine.infrastructure.graph.adapter.KnowledgeGraphStoreAdapter;
import com.kairos.module.context_engine.infrastructure.graph.executor.KnowledgeGraphGdsExecutor;
import com.kairos.module.context_engine.infrastructure.graph.executor.KnowledgeGraphMutationExecutor;
import com.kairos.module.context_engine.infrastructure.relational.repository.chunk.SpringChunkRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.history.SpringHistoryRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.source.SpringSourceRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.repository.triple.SpringTripleRepositoryAdapter;
import com.kairos.module.context_engine.infrastructure.relational.semantic.SemanticSearchAdapter;
import com.kairos.share.security.context.RequestContext;
import com.kairos.share.security.context.RequestContextProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast pull-request regression gate for retrieval quality.
 *
 * <p>The complete 60-query benchmark is intentionally excluded from normal Maven verification and
 * runs through a dedicated manual/scheduled workflow. This gate executes a stable 12-query subset
 * with real ONNX embeddings, PostgreSQL/pgvector and Neo4j GDS/PPR. It gates ranking quality only;
 * latency remains observational because shared CI runner timings are not stable enough for a hard
 * merge threshold.</p>
 */
@SpringBootTest(
        classes = RetrievalQualityRegressionIntegrationTest.IntegrationApplication.class,
        properties = "spring.jpa.hibernate.ddl-auto=validate"
)
@Import({
        EmbeddingConfig.class,
        OrtTensorFactory.class,
        OnnxEmbeddingProvider.class,
        SemanticSearchAdapter.class,
        SpringSourceRepositoryAdapter.class,
        SpringChunkRepositoryAdapter.class,
        SpringTripleRepositoryAdapter.class,
        SpringHistoryRepositoryAdapter.class
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class RetrievalQualityRegressionIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000114");
    private static final int TOP_K = 10;
    private static final Set<String> REGRESSION_SCENARIOS = Set.of("atlas", "orion", "cedar", "lumen");
    private static final double MIN_GRAPH_MULTI_HOP_NDCG_AT_10 = 0.90d;
    private static final double MIN_GRAPH_OVERALL_NDCG_AT_10 = 0.95d;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("kairos")
            .withUsername("kairos")
            .withPassword("kairos");

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5.26")
            .withAdminPassword("testcontainers-password")
            .withEnv("NEO4J_PLUGINS", "[\"graph-data-science\"]")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "gds.*");

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Autowired
    private SemanticSearchAdapter semanticSearch;

    @Autowired
    private SpringSourceRepositoryAdapter sourceRepository;

    @Autowired
    private SpringChunkRepositoryAdapter chunkRepository;

    @Autowired
    private SpringTripleRepositoryAdapter tripleRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Driver driver;
    private KnowledgeGraphSearch graphSearch;
    private Map<String, String> seedByQuery;
    private List<QueryCase> queries;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpDataset() throws IOException {
        JsonNode dataset = objectMapper.readTree(
                new ClassPathResource("evaluation/retrieval-v1.json").getInputStream()
        );
        seedByQuery = new HashMap<>();
        queries = new ArrayList<>();

        driver = GraphDatabase.driver(
                NEO4J.getBoltUrl(),
                AuthTokens.basic("neo4j", NEO4J.getAdminPassword())
        );

        var graphStore = new KnowledgeGraphStoreAdapter(new KnowledgeGraphMutationExecutor(driver));
        graphSearch = new HippoRagKnowledgeGraphSearchAdapter(
                new KnowledgeGraphGdsExecutor(driver),
                20,
                0.85d,
                0.001d
        );

        Source source = new Source(
                UUID.randomUUID(),
                "Kairos retrieval CI regression gate",
                "Stable subset of retrieval-v1 for pull-request quality regression checks.",
                USER_ID
        );
        sourceRepository.save(source);

        int chunkIndex = 0;
        for (JsonNode scenario : dataset.path("scenarios")) {
            String scenarioId = scenario.path("id").asText();
            if (!REGRESSION_SCENARIOS.contains(scenarioId)) {
                continue;
            }

            Map<String, UUID> scenarioPassages = new LinkedHashMap<>();
            for (JsonNode passage : scenario.path("passages")) {
                UUID chunkId = persistPassage(source, passage, chunkIndex++, graphStore);
                scenarioPassages.put(passage.path("key").asText(), chunkId);
            }

            Map<UUID, Integer> judgments = new LinkedHashMap<>();
            for (JsonNode judgment : scenario.path("judgments")) {
                String passageKey = judgment.path("passageKey").asText();
                UUID passageId = scenarioPassages.get(passageKey);
                if (passageId == null) {
                    throw new IllegalStateException(
                            "Regression judgment references unknown passage: " + scenarioId + "/" + passageKey
                    );
                }
                judgments.merge(passageId, judgment.path("grade").asInt(), Math::max);
            }

            QueryType type = QueryType.valueOf(scenario.path("type").asText());
            String seedConcept = scenario.path("seedConcept").asText();
            for (JsonNode query : scenario.path("queries")) {
                String text = query.path("text").asText();
                seedByQuery.put(text, seedConcept);
                queries.add(new QueryCase(query.path("id").asText(), type, text, Map.copyOf(judgments)));
            }
        }

        for (JsonNode distractor : dataset.path("distractors")) {
            persistPassage(source, distractor, chunkIndex++, graphStore);
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void graphRetrievalMustPreserveRankingQualityOnTheStableCiSubset() {
        assertThat(queries).hasSize(12);
        assertThat(queries.stream().filter(query -> query.type() == QueryType.SINGLE_HOP)).hasSize(6);
        assertThat(queries.stream().filter(query -> query.type() == QueryType.MULTI_HOP)).hasSize(6);

        List<QueryEvaluation> evaluations = queries.stream()
                .map(query -> new QueryEvaluation(
                        query,
                        RetrievalMetrics.score(runVector(query), query.judgments()),
                        RetrievalMetrics.score(runGraph(query), query.judgments())
                ))
                .toList();

        AggregateComparison overall = aggregate(evaluations);
        AggregateComparison multiHop = aggregate(evaluations.stream()
                .filter(evaluation -> evaluation.query().type() == QueryType.MULTI_HOP)
                .toList());

        assertThat(multiHop.graph().ndcgAt10())
                .as("graph multi-hop nDCG@10 must not regress below vector-only on the CI subset")
                .isGreaterThanOrEqualTo(multiHop.vector().ndcgAt10());
        assertThat(multiHop.graph().ndcgAt10())
                .as("graph multi-hop nDCG@10 must stay above the stable regression floor")
                .isGreaterThanOrEqualTo(MIN_GRAPH_MULTI_HOP_NDCG_AT_10);
        assertThat(overall.graph().ndcgAt10())
                .as("overall graph nDCG@10 must stay above the stable regression floor")
                .isGreaterThanOrEqualTo(MIN_GRAPH_OVERALL_NDCG_AT_10);

        driver.close();
        driver = null;
    }

    private UUID persistPassage(
            Source source,
            JsonNode fixture,
            int index,
            KnowledgeGraphStoreAdapter graphStore
    ) {
        String content = fixture.path("content").asText();
        Chunk chunk = Chunk.create(
                UUID.randomUUID(),
                source,
                content,
                index,
                true,
                embeddingProvider.embed(content)
        );
        chunkRepository.save(chunk);

        List<TripleExtracted> relationalTriples = new ArrayList<>();
        List<KnowledgeTriple> graphTriples = new ArrayList<>();

        for (JsonNode triple : fixture.path("triples")) {
            String subject = triple.path("subject").asText();
            String predicate = triple.path("predicate").asText();
            String object = triple.path("object").asText();

            TripleExtracted extracted = TripleExtracted.create(subject, predicate, object, chunk);
            extracted.addEmbedding(embeddingProvider.embed(
                    subject + " " + readablePredicate(predicate) + " " + object
            ));
            relationalTriples.add(extracted);
            graphTriples.add(KnowledgeTriple.create(
                    subject,
                    predicate,
                    object,
                    Passage.fromChunkId(chunk.getId()),
                    1.0d
            ));
        }

        tripleRepository.saveAll(relationalTriples);
        graphStore.saveAllForChunk(chunk.getId(), USER_ID, graphTriples);
        return chunk.getId();
    }

    private String readablePredicate(String predicate) {
        return predicate.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private List<UUID> runVector(QueryCase query) {
        float[] queryVector = embeddingProvider.embed(query.text());
        return semanticSearch.findPassageCandidate(queryVector, USER_ID, TOP_K).stream()
                .map(PassageCandidate::chunkId)
                .toList();
    }

    private List<UUID> runGraph(QueryCase query) {
        RecognitionMemory recognition = (searchTerm, candidates, maxSeeds) ->
                deterministicRecognition(searchTerm, candidates, maxSeeds);
        RequestContextProvider requestContextProvider = () ->
                new RequestContext(USER_ID, "retrieval-quality-gate", List.of());

        SearchSourceUseCase useCase = new SearchSourceUseCase(
                embeddingProvider,
                graphSearch,
                semanticSearch,
                recognition,
                requestContextProvider,
                historyRepository,
                new RetrievalProperties(10, TOP_K, 30, 10, 0.45d, 0.85d)
        );

        SearchResult result = useCase.execute(SearchSourceQuery.of(query.text()));
        return result.chunks().stream()
                .map(RankedChunk::chunk)
                .map(Chunk::getId)
                .toList();
    }

    private List<GraphSeed> deterministicRecognition(
            String searchTerm,
            List<TripleCandidate> candidates,
            int maxSeeds
    ) {
        String expectedConcept = seedByQuery.get(searchTerm);
        if (expectedConcept == null || maxSeeds <= 0) {
            return List.of();
        }

        boolean candidateVisible = candidates.stream().anyMatch(candidate ->
                expectedConcept.equalsIgnoreCase(candidate.subject())
                        || expectedConcept.equalsIgnoreCase(candidate.object())
        );

        return candidateVisible
                ? List.of(GraphSeed.concept(expectedConcept, 1.0d))
                : List.of();
    }

    private AggregateComparison aggregate(List<QueryEvaluation> evaluations) {
        return new AggregateComparison(
                average(evaluations.stream().map(QueryEvaluation::vectorScores).toList()),
                average(evaluations.stream().map(QueryEvaluation::graphScores).toList())
        );
    }

    private RetrievalMetrics.Scores average(List<RetrievalMetrics.Scores> scores) {
        if (scores.isEmpty()) {
            return new RetrievalMetrics.Scores(0, 0, 0, 0);
        }
        return new RetrievalMetrics.Scores(
                scores.stream().mapToDouble(RetrievalMetrics.Scores::recallAt5).average().orElse(0),
                scores.stream().mapToDouble(RetrievalMetrics.Scores::recallAt10).average().orElse(0),
                scores.stream().mapToDouble(RetrievalMetrics.Scores::mrrAt10).average().orElse(0),
                scores.stream().mapToDouble(RetrievalMetrics.Scores::ndcgAt10).average().orElse(0)
        );
    }

    private enum QueryType {
        SINGLE_HOP,
        MULTI_HOP
    }

    private record QueryCase(String id, QueryType type, String text, Map<UUID, Integer> judgments) {
    }

    private record QueryEvaluation(
            QueryCase query,
            RetrievalMetrics.Scores vectorScores,
            RetrievalMetrics.Scores graphScores
    ) {
    }

    private record AggregateComparison(
            RetrievalMetrics.Scores vector,
            RetrievalMetrics.Scores graph
    ) {
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
