package com.kairos.share.exception;

import com.kairos.context_engine.domain.exception.EmbeddingException;
import com.kairos.context_engine.infrastructure.ai.gemini.exception.ApiException;
import com.kairos.context_engine.infrastructure.ai.gemini.exception.GeminiIntegrationException;
import com.kairos.context_engine.infrastructure.ai.gemini.exception.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@ControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler({
            RuntimeException.class,
            ApiException.class,
            GeminiIntegrationException.class,
            JsonProcessingException.class,
            EmbeddingException.class
    })
    public ResponseEntity<ErrorResponse> handle(RuntimeException e, HttpServletRequest request) {
        log.warn("[400 BAD_REQUEST] {} {} | {}: {}",
                request.getMethod(), request.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage());

        var response = ErrorResponse.toInstance(
                HttpStatus.BAD_REQUEST.value(),
                "Runtime exception",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e, HttpServletRequest request) {
        var fieldErrors = e.getFieldErrors().stream()
                .map(fieldError -> new InvalidFieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                )).toList();

        log.warn("[400 BAD_REQUEST] {} {} | Validation failed on fields: {}",
                request.getMethod(), request.getRequestURI(),
                fieldErrors.stream().map(InvalidFieldError::field).toList());

        var response = ErrorResponse.toInstance(
                HttpStatus.BAD_REQUEST.value(),
                "Validation error",
                "One or more fields are invalid",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("[400 BAD_REQUEST] {} {} | Malformed JSON: {}",
                request.getMethod(), request.getRequestURI(),
                e.getMostSpecificCause().getMessage());

        var response = ErrorResponse.toInstance(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request",
                e.getMostSpecificCause().getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handle(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("[400 BAD_REQUEST] {} {} | Missing parameter: '{}' (type: {})",
                request.getMethod(), request.getRequestURI(),
                e.getParameterName(), e.getParameterType());

        var response = ErrorResponse.toInstance(
                HttpStatus.BAD_REQUEST.value(),
                "Missing request parameter",
                "Required parameter '%s' of type '%s' is missing".formatted(e.getParameterName(), e.getParameterType()),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        var expectedType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";

        log.warn("[400 BAD_REQUEST] {} {} | Type mismatch on parameter '{}': expected '{}', received '{}'",
                request.getMethod(), request.getRequestURI(),
                e.getName(), expectedType, e.getValue());

        var response = ErrorResponse.toInstance(
                HttpStatus.BAD_REQUEST.value(),
                "Type mismatch",
                "Parameter '%s' expects type '%s' but received '%s'".formatted(e.getName(), expectedType, e.getValue()),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(EntityNotFoundException e, HttpServletRequest request) {
        log.warn("[404 NOT_FOUND] {} {} | {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());

        var response = ErrorResponse.toInstance(
                HttpStatus.NOT_FOUND.value(),
                "Entity not found",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handle(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("[404 NOT_FOUND] {} {} | No handler found",
                request.getMethod(), request.getRequestURI());

        var response = ErrorResponse.toInstance(
                HttpStatus.NOT_FOUND.value(),
                "Route not found",
                "No handler found for %s %s".formatted(e.getHttpMethod(), e.getRequestURL()),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handle(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("[405 METHOD_NOT_ALLOWED] {} {} | Supported: {}",
                request.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());

        var response = ErrorResponse.toInstance(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method not allowed",
                "Method '%s' is not supported for this endpoint. Supported: %s".formatted(e.getMethod(), e.getSupportedHttpMethods()),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e, HttpServletRequest request) {
        log.error("[500 INTERNAL_SERVER_ERROR] {} {} | {}: {}",
                request.getMethod(), request.getRequestURI(),
                e.getClass().getSimpleName(), e.getMessage(), e);

        var response = ErrorResponse.toInstance(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}