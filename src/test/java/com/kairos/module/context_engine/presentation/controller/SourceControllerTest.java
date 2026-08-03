package com.kairos.module.context_engine.presentation.controller;

import com.kairos.module.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.module.context_engine.application.use_case.GetAllSourceProgressUploadUseCase;
import com.kairos.module.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.module.context_engine.application.use_case.RetrySourceContextUseCase;
import com.kairos.module.context_engine.domain.model.SearchResult;
import com.kairos.module.context_engine.domain.exception.SourceRetryConflictException;
import com.kairos.module.context_engine.domain.model.content.Source;
import com.kairos.module.context_engine.domain.model.progress.SourceProgressUpload;
import com.kairos.module.context_engine.presentation.controller.SourceController;
import com.kairos.module.context_engine.presentation.dto.response.ContextResponse;
import com.kairos.module.context_engine.presentation.mapper.SourceMapper;
import com.kairos.share.exception.GlobalHandlerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SourceControllerTest {

    @Mock
    private UploadSourceUseCase uploadSourceUseCase;

    @Mock
    private SearchSourceUseCase searchSourceUseCase;

    @Mock
    private GetAllSourceProgressUploadUseCase getAllSourceProgressUploadUseCase;

    @Mock
    private RetrySourceContextUseCase retrySourceContextUseCase;

    @Mock
    private SourceMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SourceController(
                uploadSourceUseCase,
                searchSourceUseCase,
                getAllSourceProgressUploadUseCase,
                retrySourceContextUseCase,
                mapper
        ))
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void uploadSource_postsSourceAndReturnsCreated() throws Exception {
        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "RAG notes",
                                  "content": "Knowledge graphs improve retrieval."
                                }
                                """))
                .andExpect(status().isCreated());

        verify(uploadSourceUseCase).execute(argThat(command ->
                command.title().equals("RAG notes")
                        && command.content().equals("Knowledge graphs improve retrieval.")
        ));
    }

    @Test
    void uploadSource_doesNotMapAuthorIdSentInRequestBody() throws Exception {
        UUID requestAuthorId = UUID.randomUUID();

        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "RAG notes",
                                  "content": "Knowledge graphs improve retrieval.",
                                  "authorId": "%s"
                                }
                                """.formatted(requestAuthorId)))
                .andExpect(status().isCreated());

        verify(uploadSourceUseCase).execute(argThat(command ->
                command.title().equals("RAG notes")
                        && command.content().equals("Knowledge graphs improve retrieval.")
        ));
    }

    @Test
    void uploadSource_missingRequiredFields_returnsValidationError() throws Exception {
        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "",
                                  "content": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.description").value("One or more fields are invalid"))
                .andExpect(jsonPath("$.path").value("/sources"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        verifyNoInteractions(uploadSourceUseCase);
    }

    @Test
    void uploadSource_malformedJson_returnsErrorResponse() throws Exception {
        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "RAG notes",
                                  "content":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/sources"));

        verifyNoInteractions(uploadSourceUseCase);
    }

    @Test
    void searchSourceContext_usesPostSearchEndpoint() throws Exception {
        SearchResult result = SearchResult.empty();
        ContextResponse response = new ContextResponse(List.of(), List.of());

        when(searchSourceUseCase.execute(argThat(query ->
                query.searchTerm().equals("How does graph retrieval work?")
        ))).thenReturn(result);
        when(mapper.toContextResponse(result)).thenReturn(response);

        mockMvc.perform(post("/sources/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "termQuery": "How does graph retrieval work?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeGraph.length()").value(0))
                .andExpect(jsonPath("$.chunkContexts.length()").value(0));

        verify(mapper).toContextResponse(result);
    }

    @Test
    void searchSourceContext_blankTermQuery_returnsValidationError() throws Exception {
        mockMvc.perform(post("/sources/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "termQuery": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.path").value("/sources/search"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("termQuery"));

        verifyNoInteractions(searchSourceUseCase, mapper);
    }

    @Test
    void searchSourceContext_doesNotExposeGetWithRequestBody() throws Exception {
        mockMvc.perform(get("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "termQuery": "How does graph retrieval work?"
                                }
                                """))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("Method not allowed"))
                .andExpect(jsonPath("$.path").value("/sources"));

        verifyNoInteractions(searchSourceUseCase, mapper);
    }

    @Test
    void getAllSourceProgressUpload_returnsMappedProgressForAuthenticatedUser() throws Exception {
        Source source = new Source(UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"), "RAG notes", "content");
        List<SourceProgressUpload> progress = List.of(new SourceProgressUpload(source, 5, 3));
        var response = com.kairos.module.context_engine.presentation.dto.response.ProgressUploadResponse.of(progress);
        when(getAllSourceProgressUploadUseCase.execute()).thenReturn(progress);
        when(mapper.toProgressUploadResponse(progress)).thenReturn(response);

        mockMvc.perform(get("/sources/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceProgressUploads.length()").value(1))
                .andExpect(jsonPath("$.sourceProgressUploads[0].sourceId").value(source.getId().toString()))
                .andExpect(jsonPath("$.sourceProgressUploads[0].sourceTitle").value("RAG notes"))
                .andExpect(jsonPath("$.sourceProgressUploads[0].totalChunks").value(5))
                .andExpect(jsonPath("$.sourceProgressUploads[0].status").value("PENDING"))
                .andExpect(jsonPath("$.sourceProgressUploads[0].pendingChunks").value(2))
                .andExpect(jsonPath("$.sourceProgressUploads[0].processingChunks").value(0))
                .andExpect(jsonPath("$.sourceProgressUploads[0].completedChunks").value(3))
                .andExpect(jsonPath("$.sourceProgressUploads[0].failedChunks").value(0));

        verify(getAllSourceProgressUploadUseCase).execute();
        verify(mapper).toProgressUploadResponse(progress);
    }

    @Test
    void retrySourceContext_returnsAccepted() throws Exception {
        UUID sourceId = UUID.randomUUID();

        mockMvc.perform(post("/sources/{sourceId}/retry", sourceId))
                .andExpect(status().isAccepted());

        verify(retrySourceContextUseCase).execute(sourceId);
    }

    @Test
    void retrySourceContext_withoutEligibleFailuresReturnsConflict() throws Exception {
        UUID sourceId = UUID.randomUUID();
        doThrow(new SourceRetryConflictException("Source has no failed chunks to retry"))
                .when(retrySourceContextUseCase).execute(sourceId);

        mockMvc.perform(post("/sources/{sourceId}/retry", sourceId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Source retry conflict"));
    }
    @Test
    void searchSourceContext_passesExistingQuestionId() throws Exception {
        UUID questionId = UUID.randomUUID();
        SearchResult result = SearchResult.empty();
        ContextResponse response = new ContextResponse(List.of(), List.of());
        when(searchSourceUseCase.execute(argThat(query ->
                query.searchTerm().equals("Repeat this question") && questionId.equals(query.questionId())
        ))).thenReturn(result);
        when(mapper.toContextResponse(result)).thenReturn(response);

        mockMvc.perform(post("/sources/search")
                        .contentType("application/json")
                        .content("""
                                {
                                  "termQuery": "Repeat this question",
                                  "questionId": "%s"
                                }
                                """.formatted(questionId)))
                .andExpect(status().isOk());

        verify(searchSourceUseCase).execute(argThat(query -> questionId.equals(query.questionId())));
    }
}