package com.kairos.module.context_engine.presentation.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.model.content.Chunk;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.domain.model.knowledge.KnowledgeTriple;
import com.kairos.module.context_engine.domain.model.knowledge.Passage;
import com.kairos.module.context_engine.domain.model.retrieval.ranking.RankedChunk;
import com.kairos.module.context_engine.domain.model.retrieval.source.RetrievalSource;
import com.kairos.module.context_engine.presentation.dto.response.ContextResponse;
import com.kairos.module.context_engine.presentation.mapper.SourceMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SourceMapperTest {

    private final SourceMapper mapper = new SourceMapper();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toContextResponse_preservesKnowledgeGraphAndChunkContextFields() {
        UUID authorId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        Source source = new Source(sourceId, "RAG notes", "Knowledge graphs improve retrieval.", authorId);
        Chunk chunk = Chunk.create(chunkId, source, "Chunk content", 0, true, new float[]{0.1f, 0.2f});
        KnowledgeTriple triple = KnowledgeTriple.create(
                "HippoRAG",
                "USES",
                "Knowledge graph",
                Passage.fromChunkId(chunkId),
                0.8
        );
        RankedChunk rankedChunk = new RankedChunk(chunk, 1, 0.95, RetrievalSource.HYBRID);
        SearchResult searchResult = SearchResult.from(List.of(triple), List.of(rankedChunk));

        ContextResponse response = mapper.toContextResponse(searchResult);

        assertThat(response.knowledgeGraph()).hasSize(1);
        assertThat(response.knowledgeGraph().getFirst().subject()).isEqualTo("HippoRAG");
        assertThat(response.knowledgeGraph().getFirst().predicate()).isEqualTo("USES");
        assertThat(response.knowledgeGraph().getFirst().object()).isEqualTo("Knowledge graph");
        assertThat(response.knowledgeGraph().getFirst().chunkId()).isEqualTo(chunkId);

        assertThat(response.chunkContexts()).hasSize(1);
        assertThat(response.chunkContexts().getFirst().chunkId()).isEqualTo(chunkId);
        assertThat(response.chunkContexts().getFirst().content()).isEqualTo("Chunk content");
        assertThat(response.chunkContexts().getFirst().rank()).isEqualTo(1);
        assertThat(response.chunkContexts().getFirst().score()).isEqualTo(0.95);
        assertThat(response.chunkContexts().getFirst().source()).isEqualTo(RetrievalSource.HYBRID);
    }

    @Test
    void toProgressUploadResponse_mapsEverySourceProgressField() {
        UUID firstSourceId = UUID.randomUUID();
        UUID secondSourceId = UUID.randomUUID();
        List<SourceProgressUpload> uploads = List.of(
                new SourceProgressUpload(new Source(firstSourceId, "First source", "first", UUID.randomUUID()), 4, 1),
                new SourceProgressUpload(new Source(secondSourceId, "Second source", "second", UUID.randomUUID()), 7, 7)
        );

        var response = mapper.toProgressUploadResponse(uploads);

        var uploadsJson = objectMapper.valueToTree(response).path("sourceProgressUploads");

        assertThat(uploadsJson).hasSize(2);
        assertThat(uploadsJson.get(0).path("sourceId").asText()).isEqualTo(firstSourceId.toString());
        assertThat(uploadsJson.get(0).path("sourceTitle").asText()).isEqualTo("First source");
        assertThat(uploadsJson.get(0).path("totalChunks").asInt()).isEqualTo(4);
        assertThat(uploadsJson.get(0).path("processedChunks").asInt()).isEqualTo(1);
        assertThat(uploadsJson.get(1).path("sourceId").asText()).isEqualTo(secondSourceId.toString());
        assertThat(uploadsJson.get(1).path("sourceTitle").asText()).isEqualTo("Second source");
        assertThat(uploadsJson.get(1).path("totalChunks").asInt()).isEqualTo(7);
        assertThat(uploadsJson.get(1).path("processedChunks").asInt()).isEqualTo(7);
    }

    @Test
    void toProgressUploadResponse_mapsEmptyProgressList() {
        var response = mapper.toProgressUploadResponse(List.of());

        assertThat(objectMapper.valueToTree(response).path("sourceProgressUploads")).isEmpty();
    }
}
