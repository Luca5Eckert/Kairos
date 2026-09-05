package com.kairos.module.context_engine.evaluation;

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
import com.kairos.module.context_engine.domain.model.retrieval.ranking.ScoredPassage;
import com.kairos.module.context_engine.domain.model.retrieval.seed.GraphSeed;
import com.kairos.module.context_engine.domain.port.embedding.EmbeddingProvider;
import com.kairos.module.context_engine.domain.port.graph.KnowledgeGraphSearch;
import com.kairos.module.context_engine.domain.port.recognition.RecognitionMemory;
import com.kairos.module.context_engine.domain.port.repository.HistoryRepository;
import com.kairos.module.context_engine.domain.port.semantic.SemanticSearch;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = RetrievalEvaluationIntegrationTest.IntegrationApplication.class,
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
class RetrievalEvaluationIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000113");
    private static final int TOP_K = 10;
    private static final int REPETITIONS = 2;
    private static final int WARMUP_QUERIES = 5;
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "evaluation", "retrieval");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("kairos")
            .withUsername("kairos")
            .withPassword("kairos");

    @Container
    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5.26")
            .withAdminPassword("test-password")
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
    private DatasetFixture dataset;
    private Map<String, UUID> passageIds;
    private Map<String, String> seedByQuery;
    private KnowledgeGraphSearch graphSearch;

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUpDataset() throws IOException {
        dataset = objectMapper.readValue(
                new ClassPathResource("evaluation/retrieval-v1.json").getInputStream(),
                DatasetFixture.class
        );
        passageIds = new LinkedHashMap<>();
        seedByQuery = new HashMap<>();

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
                "Kairos retrieval evaluation " + dataset.version(),
                "Controlled benchmark corpus for issue #113.",
                USER_ID
        );
        sourceRepository.save(source);

        int chunkIndex = 0;
        for (ScenarioFixture scenario : dataset.scenarios()) {
            for (QueryFixture query : scenario.queries()) {
                seedByQuery.put(query.text(), scenario.seedConcept());
            }
            for (PassageFixture passage : scenario.passages()) {
                persistPassage(
                        source,
                        scenario.id() + "/" + passage.key(),
                        passage,
                        chunkIndex++,
                        graphStore
                );
            }
        }

        for (PassageFixture distractor : dataset.distractors()) {
            persistPassage(
                    source,
                    "distractor/" + distractor.key(),
                    distractor,
                    chunkIndex++,
                    graphStore
            );
        }

        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void cleanGraph() {
        if (driver == null) {
            return;
        }
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        } finally {
            driver.close();
            driver = null;
        }
    }

    @Test
    void comparesVectorOnlyAgainstGraphAugmentedRetrievalAndWritesReferenceArtifacts() throws IOException {
        List<QueryCase> queries = queryCases();

        assertThat(queries).hasSize(60);
        assertThat(queries.stream().filter(query -> query.type() == QueryType.SINGLE_HOP)).hasSize(30);
        assertThat(queries.stream().filter(query -> query.type() == QueryType.MULTI_HOP)).hasSize(30);

        queries.stream()
                .limit(WARMUP_QUERIES)
                .forEach(query -> {
                    runVector(query);
                    runGraph(query);
                });

        List<QueryEvaluation> evaluations = new ArrayList<>();
        for (QueryCase query : queries) {
            List<Double> vectorLatencies = new ArrayList<>();
            List<Double> graphLatencies = new ArrayList<>();
            List<Map<String, Double>> graphStages = new ArrayList<>();
            RetrievalRun vectorReference = null;
            RetrievalRun graphReference = null;

            for (int repetition = 0; repetition < REPETITIONS; repetition++) {
                RetrievalRun vectorRun = runVector(query);
                RetrievalRun graphRun = runGraph(query);

                vectorLatencies.add(vectorRun.totalLatencyMs());
                graphLatencies.add(graphRun.totalLatencyMs());
                graphStages.add(graphRun.stageLatencyMs());

                if (repetition == 0) {
                    vectorReference = vectorRun;
                    graphReference = graphRun;
                }
            }

            assertThat(vectorReference).isNotNull();
            assertThat(graphReference).isNotNull();
            assertThat(vectorReference.retrieved()).isNotEmpty();
            assertThat(graphReference.retrieved()).isNotEmpty();

            evaluations.add(new QueryEvaluation(
                    query.id(),
                    query.type(),
                    query.text(),
                    query.judgments(),
                    vectorReference.retrieved(),
                    graphReference.retrieved(),
                    RetrievalMetrics.score(vectorReference.retrieved(), query.judgments()),
                    RetrievalMetrics.score(graphReference.retrieved(), query.judgments()),
                    vectorLatencies,
                    graphLatencies,
                    graphStages
            ));
        }

        BenchmarkReport report = buildReport(evaluations);
        writeArtifacts(report, evaluations);

        assertThat(report.queryCount()).isEqualTo(60);
        assertThat(report.overall().vector().recallAt10()).isBetween(0.0d, 1.0d);
        assertThat(report.overall().graph().recallAt10()).isBetween(0.0d, 1.0d);
        assertThat(report.latency().vector().p95Ms()).isPositive();
        assertThat(report.latency().graph().p95Ms()).isPositive();
    }

    private void persistPassage(
            Source source,
            String logicalKey,
            PassageFixture fixture,
            int index,
            KnowledgeGraphStoreAdapter graphStore
    ) {
        Chunk chunk = Chunk.create(
                UUID.randomUUID(),
                source,
                fixture.content(),
                index,
                true,
                embeddingProvider.embed(fixture.content())
        );
        chunkRepository.save(chunk);
        passageIds.put(logicalKey, chunk.getId());

        List<TripleExtracted> relationalTriples = new ArrayList<>();
        List<KnowledgeTriple> graphTriples = new ArrayList<>();

        for (TripleFixture triple : fixture.triples()) {
            TripleExtracted extracted = TripleExtracted.create(
                    triple.subject(),
                    triple.predicate(),
                    triple.object(),
                    chunk
            );
            extracted.addEmbedding(embeddingProvider.embed(
                    triple.subject() + " " + readablePredicate(triple.predicate()) + " " + triple.object()
            ));
            relationalTriples.add(extracted);
            graphTriples.add(KnowledgeTriple.create(
                    triple.subject(),
                    triple.predicate(),
                    triple.object(),
                    Passage.fromChunkId(chunk.getId()),
                    1.0d
            ));
        }

        tripleRepository.saveAll(relationalTriples);
        graphStore.saveAllForChunk(chunk.getId(), USER_ID, graphTriples);
    }

    private String readablePredicate(String predicate) {
        return predicate.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private List<QueryCase> queryCases() {
        List<QueryCase> queries = new ArrayList<>();

        for (ScenarioFixture scenario : dataset.scenarios()) {
            Map<UUID, Integer> judgments = scenario.judgments().stream()
                    .collect(Collectors.toMap(
                            judgment -> passageId(scenario.id() + "/" + judgment.passageKey()),
                            JudgmentFixture::grade,
                            Math::max,
                            LinkedHashMap::new
                    ));

            for (QueryFixture query : scenario.queries()) {
                queries.add(new QueryCase(
                        query.id(),
                        QueryType.valueOf(scenario.type()),
                        query.text(),
                        judgments
                ));
            }
        }

        return List.copyOf(queries);
    }

    private UUID passageId(String key) {
        UUID id = passageIds.get(key);
        if (id == null) {
            throw new IllegalStateException("Dataset judgment references unknown passage: " + key);
        }
        return id;
    }

    private RetrievalRun runVector(QueryCase query) {
        Map<String, Double> stages = new LinkedHashMap<>();
        long totalStart = System.nanoTime();

        long embeddingStart = System.nanoTime();
        float[] queryVector = embeddingProvider.embed(query.text());
        stages.put("embedding", elapsedMs(embeddingStart));

        long searchStart = System.nanoTime();
        List<UUID> ids = semanticSearch.findPassageCandidate(queryVector, USER_ID, TOP_K).stream()
                .map(PassageCandidate::chunkId)
                .toList();
        stages.put("dense_passage_search", elapsedMs(searchStart));

        return new RetrievalRun(ids, elapsedMs(totalStart), stages);
    }

    private RetrievalRun runGraph(QueryCase query) {
        StageTimer timer = new StageTimer();

        EmbeddingProvider timedEmbedding = text ->
                timer.measure("embedding", () -> embeddingProvider.embed(text));

        SemanticSearch timedSemantic = new TimedSemanticSearch(semanticSearch, timer);

        RecognitionMemory recognition = (searchTerm, candidates, maxSeeds) ->
                timer.measure("recognition", () -> deterministicRecognition(searchTerm, candidates, maxSeeds));

        KnowledgeGraphSearch timedGraph = request ->
                timer.measure("graph_ppr", () -> graphSearch.expandKnowledge(request));

        RequestContextProvider requestContextProvider = () ->
                new RequestContext(USER_ID, "evaluation@kairos.local", List.of());

        SearchSourceUseCase useCase = new SearchSourceUseCase(
                timedEmbedding,
                timedGraph,
                timedSemantic,
                recognition,
                requestContextProvider,
                historyRepository,
                new RetrievalProperties(10, TOP_K, 30, 10, 0.45d, 0.85d)
        );

        long totalStart = System.nanoTime();
        SearchResult result = useCase.execute(SearchSourceQuery.of(query.text()));
        double totalMs = elapsedMs(totalStart);

        return new RetrievalRun(
                result.chunks().stream().map(RankedChunk::chunk).map(Chunk::getId).toList(),
                totalMs,
                timer.milliseconds()
        );
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

    private BenchmarkReport buildReport(List<QueryEvaluation> evaluations) {
        AggregateComparison overall = aggregate(evaluations);
        Map<QueryType, AggregateComparison> byType = new EnumMap<>(QueryType.class);
        for (QueryType type : QueryType.values()) {
            byType.put(type, aggregate(evaluations.stream()
                    .filter(result -> result.type() == type)
                    .toList()));
        }

        List<Double> vectorLatencies = evaluations.stream()
                .flatMap(result -> result.vectorLatenciesMs().stream())
                .toList();
        List<Double> graphLatencies = evaluations.stream()
                .flatMap(result -> result.graphLatenciesMs().stream())
                .toList();

        Map<String, Percentiles> graphStages = evaluations.stream()
                .flatMap(result -> result.graphStageLatenciesMs().stream())
                .flatMap(stageMap -> stageMap.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList()),
                                this::percentiles
                        )
                ));

        return new BenchmarkReport(
                dataset.version(),
                evaluations.size(),
                REPETITIONS,
                overall,
                byType,
                new LatencyComparison(
                        percentiles(vectorLatencies),
                        percentiles(graphLatencies),
                        graphStages
                ),
                new BenchmarkBoundary(
                        "Production all-MiniLM-L6-v2 through the JVM ONNX provider",
                        "Real PostgreSQL 16 + pgvector HNSW queries",
                        "Real Neo4j 5.26 Graph Data Science Personalized PageRank",
                        "Deterministic recognition-memory seed selection constrained to concepts present in dense triple candidates; Gemini is intentionally excluded from the reference run"
                )
        );
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

    private Percentiles percentiles(List<Double> values) {
        if (values.isEmpty()) {
            return new Percentiles(0, 0, 0);
        }
        List<Double> sorted = values.stream().sorted().toList();
        return new Percentiles(
                percentile(sorted, 0.50d),
                percentile(sorted, 0.95d),
                percentile(sorted, 0.99d)
        );
    }

    private double percentile(List<Double> sorted, double quantile) {
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private void writeArtifacts(BenchmarkReport report, List<QueryEvaluation> evaluations) throws IOException {
        Files.createDirectories(OUTPUT_DIRECTORY);

        RunMetadata metadata = new RunMetadata(
                "kairos",
                "retrieval-graph-vs-vector",
                issueReference(),
                environment("GITHUB_SHA", "local"),
                environment("GITHUB_REF_NAME", "local"),
                dataset.version(),
                Instant.now().toString(),
                System.getProperty("java.version"),
                System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory(),
                WARMUP_QUERIES,
                REPETITIONS,
                TOP_K,
                "bash scripts/ai/validate.sh",
                report.boundary()
        );

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(OUTPUT_DIRECTORY.resolve("run-metadata.json").toFile(), metadata);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(OUTPUT_DIRECTORY.resolve("raw-results.json").toFile(), evaluations);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(OUTPUT_DIRECTORY.resolve("report.json").toFile(), report);
        Files.writeString(OUTPUT_DIRECTORY.resolve("summary.md"), renderSummary(report));
    }

    private String renderSummary(BenchmarkReport report) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Kairos retrieval evaluation — issue #113\n\n");
        markdown.append("Dataset: `").append(report.datasetVersion()).append("`  \n");
        markdown.append("Queries: ").append(report.queryCount())
                .append(" (30 single-hop, 30 multi-hop)  \n");
        markdown.append("Timing repetitions per query after warm-up: ").append(report.repetitions()).append("\n\n");

        markdown.append("## Retrieval quality\n\n");
        markdown.append("| Segment | Mode | Recall@5 | Recall@10 | MRR@10 | nDCG@10 |\n");
        markdown.append("| --- | --- | ---: | ---: | ---: | ---: |\n");
        appendQualityRows(markdown, "All", report.overall());
        appendQualityRows(markdown, "Single-hop", report.byType().get(QueryType.SINGLE_HOP));
        appendQualityRows(markdown, "Multi-hop", report.byType().get(QueryType.MULTI_HOP));

        markdown.append("\n### Relative lift — graph vs vector\n\n");
        markdown.append("| Segment | Recall@10 lift | nDCG@10 lift |\n");
        markdown.append("| --- | ---: | ---: |\n");
        appendLiftRow(markdown, "All", report.overall());
        appendLiftRow(markdown, "Single-hop", report.byType().get(QueryType.SINGLE_HOP));
        appendLiftRow(markdown, "Multi-hop", report.byType().get(QueryType.MULTI_HOP));

        markdown.append("\n## Query-path latency\n\n");
        markdown.append("| Mode | p50 | p95 | p99 |\n");
        markdown.append("| --- | ---: | ---: | ---: |\n");
        markdown.append("| Vector-only | ")
                .append(ms(report.latency().vector().p50Ms())).append(" | ")
                .append(ms(report.latency().vector().p95Ms())).append(" | ")
                .append(ms(report.latency().vector().p99Ms())).append(" |\n");
        markdown.append("| Graph-augmented | ")
                .append(ms(report.latency().graph().p50Ms())).append(" | ")
                .append(ms(report.latency().graph().p95Ms())).append(" | ")
                .append(ms(report.latency().graph().p99Ms())).append(" |\n");

        markdown.append("\n## Graph path stage latency\n\n");
        markdown.append("| Stage | p50 | p95 | p99 |\n");
        markdown.append("| --- | ---: | ---: | ---: |\n");
        report.latency().graphStages().forEach((stage, values) ->
                markdown.append("| ").append(stage).append(" | ")
                        .append(ms(values.p50Ms())).append(" | ")
                        .append(ms(values.p95Ms())).append(" | ")
                        .append(ms(values.p99Ms())).append(" |\n")
        );

        markdown.append("\n## Benchmark boundary\n\n");
        markdown.append("- Embeddings: ").append(report.boundary().embeddings()).append(".\n");
        markdown.append("- Dense retrieval: ").append(report.boundary().denseRetrieval()).append(".\n");
        markdown.append("- Graph expansion: ").append(report.boundary().graphExpansion()).append(".\n");
        markdown.append("- Recognition memory: ").append(report.boundary().recognitionMemory()).append(".\n");
        markdown.append("\nThese are reproducible local/CI benchmark measurements, not production SLAs. ")
                .append("The reference run intentionally excludes live Gemini recognition latency and variance.\n");

        return markdown.toString();
    }

    private void appendQualityRows(StringBuilder markdown, String segment, AggregateComparison comparison) {
        appendScoreRow(markdown, segment, "Vector-only", comparison.vector());
        appendScoreRow(markdown, segment, "Graph-augmented", comparison.graph());
    }

    private void appendScoreRow(
            StringBuilder markdown,
            String segment,
            String mode,
            RetrievalMetrics.Scores scores
    ) {
        markdown.append("| ").append(segment).append(" | ").append(mode).append(" | ")
                .append(metric(scores.recallAt5())).append(" | ")
                .append(metric(scores.recallAt10())).append(" | ")
                .append(metric(scores.mrrAt10())).append(" | ")
                .append(metric(scores.ndcgAt10())).append(" |\n");
    }

    private void appendLiftRow(StringBuilder markdown, String segment, AggregateComparison comparison) {
        markdown.append("| ").append(segment).append(" | ")
                .append(relativeLift(comparison.vector().recallAt10(), comparison.graph().recallAt10())).append(" | ")
                .append(relativeLift(comparison.vector().ndcgAt10(), comparison.graph().ndcgAt10())).append(" |\n");
    }

    private String relativeLift(double baseline, double candidate) {
        if (baseline == 0.0d) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%+.1f%%", ((candidate - baseline) / baseline) * 100.0d);
    }

    private String metric(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private String ms(double value) {
        return String.format(Locale.ROOT, "%.2f ms", value);
    }

    private String issueReference() {
        return "Luca5Eckert/Kairos#113";
    }

    private String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private double elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0d;
    }

    private static final class TimedSemanticSearch implements SemanticSearch {

        private final SemanticSearch delegate;
        private final StageTimer timer;

        private TimedSemanticSearch(SemanticSearch delegate, StageTimer timer) {
            this.delegate = delegate;
            this.timer = timer;
        }

        @Override
        public List<PassageCandidate> findPassageCandidate(float[] queryVector, UUID userId, int k) {
            return timer.measure(
                    "dense_passage_search",
                    () -> delegate.findPassageCandidate(queryVector, userId, k)
            );
        }

        @Override
        public List<Chunk> findChunks(List<UUID> chunkIds, UUID userId) {
            return timer.measure("chunk_lookup", () -> delegate.findChunks(chunkIds, userId));
        }

        @Override
        public List<RankedChunk> hydrateAndRankChunks(List<ScoredPassage> scoredPassages, UUID userId) {
            return timer.measure(
                    "hydration",
                    () -> delegate.hydrateAndRankChunks(scoredPassages, userId)
            );
        }

        @Override
        public List<TripleCandidate> findTripleCandidates(float[] queryVector, UUID userId, int limit) {
            return timer.measure(
                    "dense_triple_search",
                    () -> delegate.findTripleCandidates(queryVector, userId, limit)
            );
        }
    }

    private static final class StageTimer {

        private final Map<String, Long> nanos = new LinkedHashMap<>();

        private <T> T measure(String stage, Supplier<T> supplier) {
            long start = System.nanoTime();
            try {
                return supplier.get();
            } finally {
                nanos.merge(stage, System.nanoTime() - start, Long::sum);
            }
        }

        private Map<String, Double> milliseconds() {
            return nanos.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue() / 1_000_000.0d,
                            Double::sum,
                            LinkedHashMap::new
                    ));
        }
    }

    private enum QueryType {
        SINGLE_HOP,
        MULTI_HOP
    }

    private record DatasetFixture(
            String version,
            String description,
            List<ScenarioFixture> scenarios,
            List<PassageFixture> distractors
    ) {
    }

    private record ScenarioFixture(
            String id,
            String type,
            String seedConcept,
            List<QueryFixture> queries,
            List<PassageFixture> passages,
            List<JudgmentFixture> judgments
    ) {
    }

    private record QueryFixture(String id, String text) {
    }

    private record PassageFixture(
            String key,
            String content,
            List<TripleFixture> triples
    ) {
    }

    private record TripleFixture(
            String subject,
            String predicate,
            String object
    ) {
    }

    private record JudgmentFixture(String passageKey, int grade) {
    }

    private record QueryCase(
            String id,
            QueryType type,
            String text,
            Map<UUID, Integer> judgments
    ) {
    }

    private record RetrievalRun(
            List<UUID> retrieved,
            double totalLatencyMs,
            Map<String, Double> stageLatencyMs
    ) {
    }

    private record QueryEvaluation(
            String id,
            QueryType type,
            String text,
            Map<UUID, Integer> judgments,
            List<UUID> vectorRetrieved,
            List<UUID> graphRetrieved,
            RetrievalMetrics.Scores vectorScores,
            RetrievalMetrics.Scores graphScores,
            List<Double> vectorLatenciesMs,
            List<Double> graphLatenciesMs,
            List<Map<String, Double>> graphStageLatenciesMs
    ) {
    }

    private record AggregateComparison(
            RetrievalMetrics.Scores vector,
            RetrievalMetrics.Scores graph
    ) {
    }

    private record Percentiles(
            double p50Ms,
            double p95Ms,
            double p99Ms
    ) {
    }

    private record LatencyComparison(
            Percentiles vector,
            Percentiles graph,
            Map<String, Percentiles> graphStages
    ) {
    }

    private record BenchmarkBoundary(
            String embeddings,
            String denseRetrieval,
            String graphExpansion,
            String recognitionMemory
    ) {
    }

    private record BenchmarkReport(
            String datasetVersion,
            int queryCount,
            int repetitions,
            AggregateComparison overall,
            Map<QueryType, AggregateComparison> byType,
            LatencyComparison latency,
            BenchmarkBoundary boundary
    ) {
    }

    private record RunMetadata(
            String project,
            String experimentId,
            String issue,
            String commitSha,
            String gitRef,
            String datasetVersion,
            String timestamp,
            String javaVersion,
            String os,
            int availableProcessors,
            long maxJvmMemoryBytes,
            int warmupQueries,
            int repetitions,
            int topK,
            String canonicalCommand,
            BenchmarkBoundary boundary
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
