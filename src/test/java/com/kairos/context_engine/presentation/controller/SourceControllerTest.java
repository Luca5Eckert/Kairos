package com.kairos.context_engine.presentation.controller;

import com.kairos.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.context_engine.domain.model.SearchResult;
import com.kairos.context_engine.presentation.dto.response.ContextResponse;
import com.kairos.context_engine.presentation.mapper.SourceMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class SourceControllerTest {

    @Mock
    private UploadSourceUseCase uploadSourceUseCase;

    @Mock
    private SearchSourceUseCase searchSourceUseCase;

    @Mock
    private SourceMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SourceController(uploadSourceUseCase, searchSourceUseCase, mapper))
                .build();
    }

    @Test
    void uploadSource_postsSourceAndReturnsCreated() throws Exception {
        UUID authorId = UUID.randomUUID();

        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "RAG notes",
                                  "content": "Knowledge graphs improve retrieval.",
                                  "authorId": "%s"
                                }
                                """.formatted(authorId)))
                .andExpect(status().isCreated());

        verify(uploadSourceUseCase).execute(argThat(command ->
                command.title().equals("RAG notes")
                        && command.content().equals("Knowledge graphs improve retrieval.")
                        && command.authorId().equals(authorId)
        ));
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
                .andExpect(status().isOk());

        verify(mapper).toContextResponse(result);
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
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(searchSourceUseCase, mapper);
    }
}
