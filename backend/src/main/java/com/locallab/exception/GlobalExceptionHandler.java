package com.locallab.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.locallab.dto.response.ErrorResponse;

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
 *   <li>{@link LocalLabException} - Application-specific exceptions
 *   <li>{@link MethodArgumentNotValidException} - Bean validation errors
 *   <li>{@link MethodArgumentTypeMismatchException} - Type conversion errors
 *   <li>{@link NoHandlerFoundException} - 404 errors for unknown endpoints
 *   <li>{@link Exception} - Catch-all for unexpected errors
 * </ul>
 *
 * @author LocalLab
 * @see ErrorResponse
 * @see LocalLabException
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles LocalLab application-specific exceptions.
     *
     * @param ex the LocalLabException thrown
     * @param request the HTTP request that triggered the exception
     * @return ResponseEntity containing the error response with appropriate status code
     */
    @ExceptionHandler(LocalLabException.class)
    public ResponseEntity<ErrorResponse> handleLocalLabException(
            LocalLabException ex, HttpServletRequest request) {

        LOGGER.warn("LocalLabException: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse =
                new ErrorResponse(
                        ex.getStatus().value(),
                        ex.getStatus().getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI());

        return new ResponseEntity<>(errorResponse, ex.getStatus());
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
