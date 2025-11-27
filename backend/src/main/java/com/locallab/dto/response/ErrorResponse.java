package com.locallab.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
 * @author LocalLab
 * @see com.locallab.exception.GlobalExceptionHandler
 */
public class ErrorResponse {

    /** ISO 8601 timestamp of when the error occurred. */
    private Instant timestamp;

    /** HTTP status code. */
    private int status;

    /** HTTP status reason phrase (e.g., "Bad Request", "Not Found"). */
    private String error;

    /** Human-readable error message describing what went wrong. */
    private String message;

    /** The request path that triggered the error. */
    private String path;

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
     * Returns the timestamp of when the error occurred.
     *
     * @return the error timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of when the error occurred.
     *
     * @param timestamp the error timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param status the HTTP status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Returns the HTTP status reason phrase.
     *
     * @return the HTTP status reason phrase
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the HTTP status reason phrase.
     *
     * @param error the HTTP status reason phrase
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Returns the human-readable error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the human-readable error message.
     *
     * @param message the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns the request path that triggered the error.
     *
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the request path that triggered the error.
     *
     * @param path the request path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the list of field-level validation errors.
     *
     * @return the list of field errors, never null
     */
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
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

        /**
         * Returns the field name that failed validation.
         *
         * @return the field name
         */
        public String getField() {
            return field;
        }

        /**
         * Sets the field name that failed validation.
         *
         * @param field the field name
         */
        public void setField(String field) {
            this.field = field;
        }

        /**
         * Returns the validation error message.
         *
         * @return the error message
         */
        public String getMessage() {
            return message;
        }

        /**
         * Sets the validation error message.
         *
         * @param message the error message
         */
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
