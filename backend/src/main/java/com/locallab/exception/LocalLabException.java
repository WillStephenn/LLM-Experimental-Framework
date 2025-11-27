package com.locallab.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all LocalLab application-specific exceptions.
 *
 * <p>This exception serves as the root of the LocalLab exception hierarchy, providing a consistent
 * way to handle application-specific errors with HTTP status codes.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * throw new LocalLabException("Resource not found", HttpStatus.NOT_FOUND);
 * }</pre>
 *
 * @author LocalLab
 * @see GlobalExceptionHandler
 */
public class LocalLabException extends RuntimeException {

    /** The HTTP status code associated with this exception. */
    private final HttpStatus status;

    /**
     * Constructs a new LocalLabException with the specified message and HTTP status.
     *
     * @param message the detail message describing the error
     * @param status the HTTP status code to return
     */
    public LocalLabException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Constructs a new LocalLabException with the specified message, cause, and HTTP status.
     *
     * @param message the detail message describing the error
     * @param cause the underlying cause of this exception
     * @param status the HTTP status code to return
     */
    public LocalLabException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Constructs a new LocalLabException with the specified message and a default HTTP status of
     * {@link HttpStatus#INTERNAL_SERVER_ERROR}.
     *
     * @param message the detail message describing the error
     */
    public LocalLabException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructs a new LocalLabException with the specified message, cause, and a default HTTP
     * status of {@link HttpStatus#INTERNAL_SERVER_ERROR}.
     *
     * @param message the detail message describing the error
     * @param cause the underlying cause of this exception
     */
    public LocalLabException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the HTTP status code
     */
    public HttpStatus getStatus() {
        return status;
    }
}
