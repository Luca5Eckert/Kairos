package com.kairos.share.exception;

import com.kairos.auth.infrastructure.email.EmailConfirmationDeliveryException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalHandlerExceptionTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ErrorContractController())
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

    @Test
    void handleValidationError_returnsErrorEnvelopeWithFieldErrors() throws Exception {
        mockMvc.perform(post("/contract/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.description").value("One or more fields are invalid"))
                .andExpect(jsonPath("$.path").value("/contract/validation"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void handleMalformedJson_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(post("/contract/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/contract/validation"));
    }

    @Test
    void handleMissingRequestParameter_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(get("/contract/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Missing request parameter"))
                .andExpect(jsonPath("$.description").value("Required parameter 'value' of type 'String' is missing"))
                .andExpect(jsonPath("$.path").value("/contract/missing-param"));
    }

    @Test
    void handleTypeMismatch_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(get("/contract/type-mismatch")
                        .param("count", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Type mismatch"))
                .andExpect(jsonPath("$.description").value("Parameter 'count' expects type 'Integer' but received 'not-a-number'"))
                .andExpect(jsonPath("$.path").value("/contract/type-mismatch"));
    }

    @Test
    void handleEntityNotFound_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(get("/contract/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Entity not found"))
                .andExpect(jsonPath("$.description").value("Source not found"))
                .andExpect(jsonPath("$.path").value("/contract/entity-not-found"));
    }

    @Test
    void handleMethodNotAllowed_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(get("/contract/post-only"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405))
                .andExpect(jsonPath("$.message").value("Method not allowed"))
                .andExpect(jsonPath("$.path").value("/contract/post-only"));
    }

    @Test
    void handleEmailDeliveryError_returnsServiceUnavailableEnvelope() throws Exception {
        mockMvc.perform(get("/contract/email-error"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("Email delivery unavailable"))
                .andExpect(jsonPath("$.description").value("SMTP unavailable"))
                .andExpect(jsonPath("$.path").value("/contract/email-error"));
    }

    @Test
    void handleUnexpectedError_returnsInternalServerErrorEnvelope() throws Exception {
        mockMvc.perform(get("/contract/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.description").value("boom"))
                .andExpect(jsonPath("$.path").value("/contract/unexpected-error"));
    }

    @RestController
    private static class ErrorContractController {

        @PostMapping("/contract/validation")
        void validation(@RequestBody @Valid ValidationRequest request) {
        }

        @GetMapping("/contract/missing-param")
        void missingParam(@RequestParam String value) {
        }

        @GetMapping("/contract/type-mismatch")
        void typeMismatch(@RequestParam Integer count) {
        }

        @GetMapping("/contract/entity-not-found")
        void entityNotFound() {
            throw new EntityNotFoundException("Source not found");
        }

        @PostMapping("/contract/post-only")
        void postOnly() {
        }

        @GetMapping("/contract/email-error")
        void emailError() {
            throw new EmailConfirmationDeliveryException("SMTP unavailable", new RuntimeException("down"));
        }

        @GetMapping("/contract/unexpected-error")
        void unexpectedError() throws Exception {
            throw new Exception("boom");
        }
    }

    private record ValidationRequest(@NotBlank String name) {
    }
}
