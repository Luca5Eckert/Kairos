package com.kairos.infrastructure.gemini;

import com.kairos.infrastructure.gemini.config.GeminiProperties;
import com.kairos.infrastructure.gemini.dto.GeminiRequest;
import com.kairos.infrastructure.gemini.dto.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiRestClient {

    private final RestClient client;
    private final GeminiProperties properties;

    public GeminiRestClient(RestClient client, GeminiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public GeminiResponse call(String prompt) {
        try {
            var request = GeminiRequest.of(prompt, properties);

            log.info("Calling Gemini API with model: {}", properties.model());

            GeminiResponse response = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.model()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null) {
                throw new RuntimeException("Gemini API returned null response");
            }

            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call Gemini API, " + e.getMessage());
        }
    }

}