package com.locallab.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.locallab.dto.response.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/** Unit tests for {@link GlobalExceptionHandler}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    @InjectMocks private GlobalExceptionHandler handler;

    @Mock private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Nested
    @DisplayName("handleLocalLabException")
    class HandleLocalLabExceptionTests {

        @Test
        @DisplayName("Should return correct status and message for NOT_FOUND")
        void shouldReturnCorrectStatusAndMessageForNotFound() {
            LocalLabException exception =
                    new LocalLabException("Task not found", HttpStatus.NOT_FOUND);

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatus());
            assertEquals("Not Found", response.getBody().getError());
            assertEquals("Task not found", response.getBody().getMessage());
            assertEquals("/api/test", response.getBody().getPath());
            assertTrue(response.getBody().getFieldErrors().isEmpty());
        }

        @Test
        @DisplayName("Should return correct status and message for BAD_REQUEST")
        void shouldReturnCorrectStatusAndMessageForBadRequest() {
            LocalLabException exception =
                    new LocalLabException("Invalid configuration", HttpStatus.BAD_REQUEST);

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid configuration", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should return correct status and message for CONFLICT")
        void shouldReturnCorrectStatusAndMessageForConflict() {
            LocalLabException exception =
                    new LocalLabException("Resource already exists", HttpStatus.CONFLICT);

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("Conflict", response.getBody().getError());
        }

        @Test
        @DisplayName("Should return correct status and message for SERVICE_UNAVAILABLE")
        void shouldReturnCorrectStatusAndMessageForServiceUnavailable() {
            LocalLabException exception =
                    new LocalLabException("Ollama is not running", HttpStatus.SERVICE_UNAVAILABLE);

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(503, response.getBody().getStatus());
            assertEquals("Service Unavailable", response.getBody().getError());
        }

        @Test
        @DisplayName("Should return correct status for INTERNAL_SERVER_ERROR")
        void shouldReturnCorrectStatusForInternalServerError() {
            LocalLabException exception = new LocalLabException("Unexpected error");

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            LocalLabException exception = new LocalLabException("Error", HttpStatus.BAD_REQUEST);

            ResponseEntity<ErrorResponse> response =
                    handler.handleLocalLabException(exception, request);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getTimestamp());
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    class HandleValidationExceptionTests {

        @Test
        @DisplayName("Should return BAD_REQUEST with field errors")
        void shouldReturnBadRequestWithFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError1 = new FieldError("object", "name", "must not be blank");
            FieldError fieldError2 = new FieldError("object", "email", "must be a valid email");
            when(bindingResult.getFieldErrors())
                    .thenReturn(Arrays.asList(fieldError1, fieldError2));

            MethodParameter methodParameter = mock(MethodParameter.class);
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationException(exception, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Validation failed", response.getBody().getMessage());
            assertEquals(2, response.getBody().getFieldErrors().size());
        }

        @Test
        @DisplayName("Should correctly map field error details")
        void shouldCorrectlyMapFieldErrorDetails() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError =
                    new FieldError("taskRequest", "promptTemplate", "must not be blank");
            when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

            MethodParameter methodParameter = mock(MethodParameter.class);
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationException(exception, request);

            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getFieldErrors().size());
            assertEquals("promptTemplate", response.getBody().getFieldErrors().get(0).getField());
            assertEquals(
                    "must not be blank", response.getBody().getFieldErrors().get(0).getMessage());
        }

        @Test
        @DisplayName("Should handle empty field errors")
        void shouldHandleEmptyFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(Collections.emptyList());

            MethodParameter methodParameter = mock(MethodParameter.class);
            MethodArgumentNotValidException exception =
                    new MethodArgumentNotValidException(methodParameter, bindingResult);

            ResponseEntity<ErrorResponse> response =
                    handler.handleValidationException(exception, request);

            assertNotNull(response.getBody());
            assertTrue(response.getBody().getFieldErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("handleTypeMismatchException")
    class HandleTypeMismatchExceptionTests {

        @Test
        @DisplayName("Should return BAD_REQUEST with type mismatch details")
        void shouldReturnBadRequestWithTypeMismatchDetails() {
            MethodArgumentTypeMismatchException exception =
                    new MethodArgumentTypeMismatchException(
                            "abc",
                            Long.class,
                            "id",
                            null,
                            new NumberFormatException("For input string: \"abc\""));

            ResponseEntity<ErrorResponse> response =
                    handler.handleTypeMismatchException(exception, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertTrue(response.getBody().getMessage().contains("id"));
            assertTrue(response.getBody().getMessage().contains("Long"));
        }

        @Test
        @DisplayName("Should handle null required type gracefully")
        void shouldHandleNullRequiredTypeGracefully() {
            MethodArgumentTypeMismatchException exception =
                    new MethodArgumentTypeMismatchException(
                            "value", null, "param", null, new RuntimeException("test"));

            ResponseEntity<ErrorResponse> response =
                    handler.handleTypeMismatchException(exception, request);

            assertNotNull(response.getBody());
            assertTrue(response.getBody().getMessage().contains("param"));
            assertTrue(response.getBody().getMessage().contains("unknown"));
        }
    }

    @Nested
    @DisplayName("handleNoHandlerFoundException")
    class HandleNoHandlerFoundExceptionTests {

        @Test
        @DisplayName("Should return NOT_FOUND with endpoint details")
        void shouldReturnNotFoundWithEndpointDetails() {
            NoHandlerFoundException exception =
                    new NoHandlerFoundException("GET", "/api/nonexistent", null);

            ResponseEntity<ErrorResponse> response =
                    handler.handleNoHandlerFoundException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatus());
            assertEquals("Not Found", response.getBody().getError());
            assertTrue(response.getBody().getMessage().contains("GET"));
            assertTrue(response.getBody().getMessage().contains("/api/nonexistent"));
        }

        @Test
        @DisplayName("Should include request path in response")
        void shouldIncludeRequestPathInResponse() {
            when(request.getRequestURI()).thenReturn("/api/unknown/path");
            NoHandlerFoundException exception =
                    new NoHandlerFoundException("POST", "/api/unknown/path", null);

            ResponseEntity<ErrorResponse> response =
                    handler.handleNoHandlerFoundException(exception, request);

            assertNotNull(response.getBody());
            assertEquals("/api/unknown/path", response.getBody().getPath());
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericExceptionTests {

        @Test
        @DisplayName("Should return INTERNAL_SERVER_ERROR with generic message")
        void shouldReturnInternalServerErrorWithGenericMessage() {
            Exception exception = new RuntimeException("Database connection failed");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
            assertEquals("Internal Server Error", response.getBody().getError());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should not expose internal error details")
        void shouldNotExposeInternalErrorDetails() {
            Exception exception = new RuntimeException("Sensitive database password exposed");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertNotNull(response.getBody());
            // The actual error message should NOT contain the sensitive details
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should include request path in response")
        void shouldIncludeRequestPathInResponse() {
            when(request.getRequestURI()).thenReturn("/api/experiments/123");
            Exception exception = new RuntimeException("Error");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertNotNull(response.getBody());
            assertEquals("/api/experiments/123", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            Exception exception = new NullPointerException("null reference");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should handle IllegalStateException")
        void shouldHandleIllegalStateException() {
            Exception exception = new IllegalStateException("Invalid state");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }
}
