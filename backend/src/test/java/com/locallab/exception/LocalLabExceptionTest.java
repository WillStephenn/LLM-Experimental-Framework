package com.locallab.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Unit tests for {@link LocalLabException}. */
@DisplayName("LocalLabException")
class LocalLabExceptionTest {

    @Nested
    @DisplayName("Constructor with message and status")
    class MessageAndStatusConstructorTests {

        @Test
        @DisplayName("Should create exception with message and status")
        void shouldCreateExceptionWithMessageAndStatus() {
            LocalLabException exception =
                    new LocalLabException("Resource not found", HttpStatus.NOT_FOUND);

            assertEquals("Resource not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should handle different HTTP status codes")
        void shouldHandleDifferentHttpStatusCodes() {
            LocalLabException badRequest =
                    new LocalLabException("Invalid input", HttpStatus.BAD_REQUEST);
            LocalLabException conflict =
                    new LocalLabException("Already exists", HttpStatus.CONFLICT);
            LocalLabException unavailable =
                    new LocalLabException("Service down", HttpStatus.SERVICE_UNAVAILABLE);

            assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatus());
            assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatus());
        }
    }

    @Nested
    @DisplayName("Constructor with message, cause, and status")
    class MessageCauseAndStatusConstructorTests {

        @Test
        @DisplayName("Should create exception with message, cause, and status")
        void shouldCreateExceptionWithMessageCauseAndStatus() {
            RuntimeException cause = new RuntimeException("Original error");
            LocalLabException exception =
                    new LocalLabException("Wrapped error", cause, HttpStatus.BAD_REQUEST);

            assertEquals("Wrapped error", exception.getMessage());
            assertSame(cause, exception.getCause());
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        @Test
        @DisplayName("Should handle null cause")
        void shouldHandleNullCause() {
            LocalLabException exception =
                    new LocalLabException("Error without cause", null, HttpStatus.NOT_FOUND);

            assertEquals("Error without cause", exception.getMessage());
            assertNull(exception.getCause());
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Constructor with message only")
    class MessageOnlyConstructorTests {

        @Test
        @DisplayName("Should default to INTERNAL_SERVER_ERROR status")
        void shouldDefaultToInternalServerErrorStatus() {
            LocalLabException exception = new LocalLabException("Something went wrong");

            assertEquals("Something went wrong", exception.getMessage());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
            assertNull(exception.getCause());
        }
    }

    @Nested
    @DisplayName("Constructor with message and cause")
    class MessageAndCauseConstructorTests {

        @Test
        @DisplayName("Should default to INTERNAL_SERVER_ERROR status with cause")
        void shouldDefaultToInternalServerErrorStatusWithCause() {
            IllegalArgumentException cause = new IllegalArgumentException("Bad argument");
            LocalLabException exception = new LocalLabException("Processing failed", cause);

            assertEquals("Processing failed", exception.getMessage());
            assertSame(cause, exception.getCause());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Exception hierarchy")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("Should be a RuntimeException")
        void shouldBeARuntimeException() {
            LocalLabException exception = new LocalLabException("Test");

            assertNotNull(exception);
            // This compiles, proving it's a RuntimeException
            RuntimeException runtimeException = exception;
            assertNotNull(runtimeException);
        }

        @Test
        @DisplayName("Should be throwable without declaration")
        void shouldBeThrowableWithoutDeclaration() {
            // This test verifies that LocalLabException can be thrown
            // without declaring it in the method signature (unchecked exception)
            boolean thrown = false;
            try {
                throw new LocalLabException("Test exception", HttpStatus.BAD_REQUEST);
            } catch (LocalLabException e) {
                thrown = true;
                assertEquals("Test exception", e.getMessage());
            }
            assertEquals(true, thrown);
        }
    }

    @Nested
    @DisplayName("getStatus tests")
    class GetStatusTests {

        @Test
        @DisplayName("Should return the correct status")
        void shouldReturnTheCorrectStatus() {
            LocalLabException exception = new LocalLabException("Error", HttpStatus.FORBIDDEN);

            HttpStatus status = exception.getStatus();

            assertEquals(HttpStatus.FORBIDDEN, status);
            assertEquals(403, status.value());
            assertEquals("Forbidden", status.getReasonPhrase());
        }
    }
}
