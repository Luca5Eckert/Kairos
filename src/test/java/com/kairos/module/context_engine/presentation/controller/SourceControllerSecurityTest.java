package com.kairos.module.context_engine.presentation.controller;

import com.kairos.module.context_engine.application.use_case.SearchSourceUseCase;
import com.kairos.module.context_engine.application.use_case.GetAllSourceProgressUploadUseCase;
import com.kairos.module.context_engine.application.use_case.UploadSourceUseCase;
import com.kairos.module.context_engine.application.use_case.RetrySourceContextUseCase;
import com.kairos.module.context_engine.presentation.controller.SourceController;
import com.kairos.module.context_engine.presentation.mapper.SourceMapper;
import com.kairos.share.security.config.AuthSecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SourceController.class)
@Import(AuthSecurityConfiguration.class)
class SourceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadSourceUseCase uploadSourceUseCase;

    @MockitoBean
    private SearchSourceUseCase searchSourceUseCase;

    @MockitoBean
    private GetAllSourceProgressUploadUseCase getAllSourceProgressUploadUseCase;

    @MockitoBean
    private RetrySourceContextUseCase retrySourceContextUseCase;

    @MockitoBean
    private SourceMapper mapper;

    @Test
    void uploadSource_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/sources")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "RAG notes",
                                  "content": "Knowledge graphs improve retrieval."
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadSource_withJwtAuthentication_reachesController() throws Exception {
        mockMvc.perform(post("/sources")
                        .with(jwt())
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
}
