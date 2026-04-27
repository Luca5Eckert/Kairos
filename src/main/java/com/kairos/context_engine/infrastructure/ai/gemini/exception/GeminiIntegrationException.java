package com.kairos.context_engine.infrastructure.ai.gemini.exception;

public class GeminiIntegrationException extends RuntimeException {

  public GeminiIntegrationException(String message) {
    super(message);
  }

  public GeminiIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}