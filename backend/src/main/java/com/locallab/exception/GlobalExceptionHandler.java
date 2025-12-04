package com.locallab.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.locallab.dto.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the LocalLab application.
 *
 * <p>This class provides centralised exception handling across all {@code @Controller} classes
 * using {@code @ControllerAdvice}. It ensures all exceptions return a consistent JSON structure as
 * defined in the API contract.
 *
 * <p>Handled exceptions:
 *
 * <ul>
 *   <li>{@link ResponseStatusException} - Spring's standard status exception
 *   <li>{@link EntityNotFoundException} - JPA entity not found errors (404)
 *   <li>{@link IllegalArgumentException} - Invalid argument errors (400)
 *   <li>{@link IllegalStateException} - Invalid state errors (409)
 *   <li>{@link MethodArgumentNotValidException} - Bean validation errors
 *   <li>{@link MethodArgumentTypeMismatchException} - Type conversion errors
 *   <li>{@link NoHandlerFoundException} - 404 errors for unknown endpoints
 *   <li>{@link Exception} - Catch-all for unexpected errors
 * </ul>
 *
 * @author William Stephen
 * @see ErrorResponse
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Spring's ResponseStatusException.
     *
     * <p>This handler supports the standard Spring exception pattern where the HTTP status is
     * embedded in the exception itself.
     *
     * @param ex the ResponseStatusException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with appropriate status code
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        LOGGER.warn(
                "ResponseStatusException: {} - Status: {} - Path: {}",
                ex.getReason(),
                status,
                request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        status.value(),
                        status.getReasonPhrase(),
                        ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles JPA EntityNotFoundException.
     *
     * <p>Returns HTTP 404 Not Found when a requested entity does not exist.
     *
     * @param ex the EntityNotFoundException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with 404 status
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {

        LOGGER.warn(
                "EntityNotFoundException: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles IllegalArgumentException.
     *
     * <p>Returns HTTP 400 Bad Request when invalid arguments are provided.
     *
     * @param ex the IllegalArgumentException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        LOGGER.warn(
                "IllegalArgumentException: {} - Path: {}",
                ex.getMessage(),
                request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalStateException.
     *
     * <p>Returns HTTP 409 Conflict when an operation cannot be performed due to the current state.
     *
     * @param ex the IllegalStateException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with 409 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        LOGGER.warn(
                "IllegalStateException: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handles validation errors from {@code @Valid} annotated request bodies.
     *
     * <p>Extracts field-level validation errors and includes them in the response.
     *
     * @param ex the MethodArgumentNotValidException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with field-level details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        LOGGER.warn("Validation failed for request to: {}", request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Validation failed",
                        request.getRequestURI());

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                errorResponse.addFieldError(
                                        fieldError.getField(), fieldError.getDefaultMessage()));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles type mismatch errors (e.g., invalid path variable types).
     *
     * @param ex the MethodArgumentTypeMismatchException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String parameterName = ex.getName();
        String requiredType =
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message =
                String.format("Parameter '%s' must be of type %s", parameterName, requiredType);

        LOGGER.warn("Type mismatch: {} - Path: {}", message, request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        message,
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles 404 errors when no handler is found for a request.
     *
     * @param ex the NoHandlerFoundException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        String message =
                String.format(
                        "No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL());

        LOGGER.warn("No handler found: {}", message);

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        message,
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Catch-all handler for any unexpected exceptions.
     *
     * <p>Logs the full stack trace for debugging and returns a generic error message to avoid
     * exposing internal details to clients.
     *
     * @param ex the Exception thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing a generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        LOGGER.error(
                "Unexpected error processing request to {}: {}",
                request.getRequestURI(),
                ex.getMessage(),
                ex);

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                        "An unexpected error occurred",
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
