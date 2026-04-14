package com.kairos.infrastructure.gemini;

import com.kairos.infrastructure.gemini.config.GeminiProperties;
import com.kairos.infrastructure.gemini.dto.GeminiResponse;
import com.kairos.infrastructure.gemini.exception.GeminiIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiRestClientTest {

    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "gemini-2.0-flash";

    private static final String EXPECTED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + API_KEY;

    private MockRestServiceServer server;
    private GeminiRestClient geminiRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        GeminiProperties properties = new GeminiProperties(
                API_KEY,
                MODEL,
                0.5,
                1024,
                10,
                new GeminiProperties.Retry(
                        3,
                        Duration.ofSeconds(1),
                        2.0,
                        Duration.ofSeconds(8)
                )
        );

        geminiRestClient = new GeminiRestClient(builder.build(), properties);
    }

    // --- SUCCESS ---

    @Test
    void call_returnsResponse_whenApiReturnsValidBody() throws Exception {
        GeminiResponse stub = buildResponse("ok");

        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(objectMapper.writeValueAsString(stub), MediaType.APPLICATION_JSON));

        GeminiResponse result = geminiRestClient.call("prompt");

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("ok");
    }

    // --- REQUEST SHAPE ---

    @Test
    void call_sendsPostRequest() throws Exception {
        server.expect(method(HttpMethod.POST))
                .andExpect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        geminiRestClient.call("prompt");

        server.verify();
    }

    @Test
    void call_sendsJsonContentType() throws Exception {
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        geminiRestClient.call("prompt");
    }

    @Test
    void call_bodyContainsPrompt() throws Exception {
        String prompt = "extract triples";

        server.expect(requestTo(EXPECTED_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(prompt)))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        geminiRestClient.call(prompt);
    }

    @Test
    void call_bodyRequestsJsonMimeType() throws Exception {
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"responseMimeType\":\"application/json\"")))
                .andRespond(withSuccess(emptyResponse(), MediaType.APPLICATION_JSON));

        geminiRestClient.call("prompt");
    }



    @Test
    void call_throwsGeminiIntegrationException_whenResponseIsNull() throws Exception {
        server.expect(requestTo(EXPECTED_URL))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> geminiRestClient.call("prompt"))
                .isInstanceOf(GeminiIntegrationException.class)
                .hasMessageContaining("Failed to call Gemini API");
    }

    // --- FIXTURES ---

    private GeminiResponse buildResponse(String text) {
        return new GeminiResponse(
                java.util.List.of(new GeminiResponse.Candidate(
                        new GeminiResponse.Content(
                                java.util.List.of(new GeminiResponse.Part(text)))
                ))
        );
    }

    private String emptyResponse() {
        return "{\"candidates\": []}";
    }
}
