package com.locallab.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Standard error response DTO for consistent JSON error structure across all API endpoints.
 *
 * <p>This class provides a consistent format for error responses as defined in the API contract.
 * All exceptions handled by {@link com.locallab.exception.GlobalExceptionHandler} will return this
 * structure.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "timestamp": "2025-11-27T10:00:00Z",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "path": "/api/tasks",
 *   "fieldErrors": [
 *     {
 *       "field": "name",
 *       "message": "must not be blank"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see com.locallab.exception.GlobalExceptionHandler
 */
@Getter
public class ErrorResponse {

    /** ISO 8601 timestamp of when the error occurred. */
    @Setter private Instant timestamp;

    /** HTTP status code. */
    @Setter private int status;

    /** HTTP status reason phrase (e.g., "Bad Request", "Not Found"). */
    @Setter private String error;

    /** Human-readable error message describing what went wrong. */
    @Setter private String message;

    /** The request path that triggered the error. */
    @Setter private String path;

    /** List of field-level validation errors. Empty if not a validation error. */
    private List<FieldError> fieldErrors;

    /** Default constructor initialising timestamp to current time and empty field errors list. */
    public ErrorResponse() {
        this.timestamp = Instant.now();
        this.fieldErrors = new ArrayList<>();
    }

    /**
     * Constructs an ErrorResponse with basic error information.
     *
     * @param status HTTP status code
     * @param error HTTP status reason phrase
     * @param message Human-readable error message
     * @param path Request path that triggered the error
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Sets the list of field-level validation errors.
     *
     * @param fieldErrors the list of field errors
     */
    public void setFieldErrors(List<FieldError> fieldErrors) {
        this.fieldErrors = fieldErrors != null ? fieldErrors : new ArrayList<>();
    }

    /**
     * Adds a single field error to the list.
     *
     * @param field the field name that failed validation
     * @param message the validation error message
     */
    public void addFieldError(String field, String message) {
        this.fieldErrors.add(new FieldError(field, message));
    }

    /** Represents a single field-level validation error. */
    @Data
    public static class FieldError {

        /** The name of the field that failed validation. */
        private String field;

        /** The validation error message for this field. */
        private String message;

        /** Default constructor for serialisation. */
        public FieldError() {}

        /**
         * Constructs a FieldError with the specified field name and message.
         *
         * @param field the field name that failed validation
         * @param message the validation error message
         */
        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
