package com.kairos.infrastructure.gemini;

import com.kairos.infrastructure.gemini.exception.GeminiIntegrationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class GeminiFailureClassifier {

    public boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ResourceAccessException) {
            return true;
        }

        if (throwable instanceof HttpServerErrorException) {
            return true;
        }

        if (throwable instanceof HttpClientErrorException clientErrorException) {
            return isRetryableStatus(clientErrorException.getStatusCode());
        }

        return throwable instanceof GeminiIntegrationException;
    }

    private boolean isRetryableStatus(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError() || statusCode.value() == 429;
    }
}
