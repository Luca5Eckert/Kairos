package com.kairos.infrastructure.gemini;

import com.kairos.infrastructure.gemini.exception.GeminiIntegrationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiFailureClassifierTest {

    private final GeminiFailureClassifier classifier = new GeminiFailureClassifier();

    @Test
    void isRetryable_returnsTrueForNetworkFailures() {
        assertThat(classifier.isRetryable(new ResourceAccessException("timeout"))).isTrue();
    }

    @Test
    void isRetryable_returnsTrueForServerFailures() {
        var exception = HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "down", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(classifier.isRetryable(exception)).isTrue();
    }

    @Test
    void isRetryable_returnsFalseForClientFailures() {
        var exception = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", HttpHeaders.EMPTY, new byte[0], null);
        assertThat(classifier.isRetryable(exception)).isFalse();
    }

    @Test
    void isRetryable_returnsTrueForWrappedIntegrationFailures() {
        assertThat(classifier.isRetryable(new GeminiIntegrationException("null response"))).isTrue();
    }
}
