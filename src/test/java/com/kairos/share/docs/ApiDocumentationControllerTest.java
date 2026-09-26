package com.kairos.share.docs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiDocumentationController.class)
@Import(com.kairos.share.security.config.AuthSecurityConfiguration.class)
@TestPropertySource(properties = "kairos.api-docs.enabled=true")
class ApiDocumentationControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void servesVersionedContractAndInteractiveUi() throws Exception {
        mockMvc.perform(get("/api-docs/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openapi: 3.1.0")));
        mockMvc.perform(get("/swagger-ui"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api-docs/openapi.yaml")));
    }
}
