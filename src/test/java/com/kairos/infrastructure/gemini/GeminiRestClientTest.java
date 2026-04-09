package com.kairos.infrastructure.gemini;

import com.kairos.infrastructure.gemini.config.GeminiProperties;
import com.kairos.infrastructure.gemini.dto.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiRestClientTest {

    private static final String API_KEY  = "test-api-key";
    private static final String MODEL    = "gemini-2.0-flash";
    private static final String EXPECTED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private MockRestServiceServer server;
    private GeminiRestClient geminiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        GeminiProperties properties = new GeminiProperties(API_KEY, MODEL, 0.5, 1024, 10);
        geminiRestClient = new GeminiRestClient(builder.build(), properties);
    }

    // --- Successful responses ---

    @Test
    void call_returnsGeminiResponse_whenApiRespondsWithValidBody() throws Exception {
        GeminiResponse stubResponse = buildResponse("Spring Boot uses embedded Tomcat.");
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(objectMapper.writeValueAsString(stubResponse), MediaType.APPLICATION_JSON));

        GeminiResponse result = geminiRestClient.call("What is Spring Boot?");

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Spring Boot uses embedded Tomcat.");
        server.verify();
    }

    @Test
    void call_returnsResponseWithEmptyCandidates_whenApiReturnsEmptyArray() throws Exception {
        String body = "{\"candidates\": []}";
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        GeminiResponse result = geminiRestClient.call("any prompt");

        assertThat(result.candidates()).isEmpty();
        assertThat(result.text()).isNull();
    }

    // --- Request shape ---

    @Test
    void call_sendsPostRequest_toCorrectUrl() throws Exception {
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(emptyTriples(), MediaType.APPLICATION_JSON));

        geminiRestClient.call("any prompt");

        server.verify();
    }

    @Test
    void call_sendsJsonContentType() throws Exception {
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(emptyTriples(), MediaType.APPLICATION_JSON));

        geminiRestClient.call("any prompt");

        server.verify();
    }

    @Test
    void call_requestBodyContainsPrompt() throws Exception {
        String prompt = "Extract triples from this sentence.";
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(prompt)))
                .andRespond(withSuccess(emptyTriples(), MediaType.APPLICATION_JSON));

        geminiRestClient.call(prompt);

        server.verify();
    }

    // --- Error handling ---

    @Test
    void call_throwsRuntimeException_whenApiReturnsServerError() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> geminiRestClient.call("any prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call Gemini API");
    }

    @Test
    void call_throwsRuntimeException_whenApiReturnsClientError() {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> geminiRestClient.call("any prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call Gemini API");
    }

    @Test
    void call_throwsRuntimeException_whenResponseBodyIsNull() throws Exception {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiRestClient.call("any prompt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call Gemini API");
    }

    // --- Fixtures ---

    private GeminiResponse buildResponse(String text) {
        return new GeminiResponse(
                java.util.List.of(new GeminiResponse.Candidate(
                        new GeminiResponse.Content(
                                java.util.List.of(new GeminiResponse.Part(text)))))
        );
    }

    private String emptyTriples() {
        return "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{}\"}]}}]}";
    }
}