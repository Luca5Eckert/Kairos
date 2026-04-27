package com.kairos.context_engine.infrastructure.ai.gemini;

import com.kairos.context_engine.infrastructure.ai.gemini.config.GeminiProperties;
import com.kairos.context_engine.infrastructure.ai.gemini.dto.GeminiRequest;
import com.kairos.context_engine.infrastructure.ai.gemini.dto.GeminiResponse;
import com.kairos.context_engine.infrastructure.ai.gemini.exception.GeminiIntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiRestClient {

    private final RestClient client;
    private final GeminiProperties properties;

    public GeminiRestClient(RestClient client, GeminiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Retryable(
            maxRetriesString = "${gemini.retry.max-retries}",
            delayString = "${gemini.retry.delay}",
            multiplierString = "${gemini.retry.multiplier}",
            maxDelayString = "${gemini.retry.max-delay}",
            includes = {
                    ResourceAccessException.class,
                    HttpServerErrorException.class,
                    GeminiIntegrationException.class
            },
            excludes = {
                    HttpClientErrorException.class,
                    IllegalArgumentException.class
            }
    )
    public GeminiResponse call(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        try {
            var request = GeminiRequest.of(prompt, properties);

            log.info("Calling Gemini API with model {}", properties.model());

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
                throw new GeminiIntegrationException("Gemini API returned null response");
            }

            return response;

        } catch (HttpClientErrorException e) {
            log.error("Gemini client error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (HttpServerErrorException e) {
            log.warn("Gemini server error [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Gemini network/timeout error: {}", e.getMessage());
            throw e;
        } catch (RestClientException e) {
            log.error("Unexpected RestClient error calling Gemini: {}", e.getMessage(), e);
            throw new GeminiIntegrationException("Unexpected HTTP error calling Gemini", e);
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini: {}", e.getMessage(), e);
            throw new GeminiIntegrationException("Failed to call Gemini API", e);
        }
    }
}
