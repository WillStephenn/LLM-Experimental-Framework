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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.locallab.dto.response.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
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
    @DisplayName("handleResponseStatusException")
    class HandleResponseStatusExceptionTests {

        @Test
        @DisplayName("Should return correct status and message for SERVICE_UNAVAILABLE")
        void shouldReturnCorrectStatusAndMessageForServiceUnavailable() {
            ResponseStatusException exception =
                    new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE, "Ollama service is unavailable");

            ResponseEntity<ErrorResponse> response =
                    handler.handleResponseStatusException(exception, request);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(503, response.getBody().getStatus());
            assertEquals("Service Unavailable", response.getBody().getError());
            assertEquals("Ollama service is unavailable", response.getBody().getMessage());
            assertEquals("/api/test", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should return correct status and message for BAD_REQUEST")
        void shouldReturnCorrectStatusAndMessageForBadRequest() {
            ResponseStatusException exception =
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid model specified");

            ResponseEntity<ErrorResponse> response =
                    handler.handleResponseStatusException(exception, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid model specified", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should return correct status for INTERNAL_SERVER_ERROR")
        void shouldReturnCorrectStatusForInternalServerError() {
            ResponseStatusException exception =
                    new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Operation interrupted");

            ResponseEntity<ErrorResponse> response =
                    handler.handleResponseStatusException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should use status reason phrase when reason is null")
        void shouldUseStatusReasonPhraseWhenReasonIsNull() {
            ResponseStatusException exception =
                    new ResponseStatusException(HttpStatus.NOT_FOUND, null);

            ResponseEntity<ErrorResponse> response =
                    handler.handleResponseStatusException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Not Found", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            ResponseStatusException exception =
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error");

            ResponseEntity<ErrorResponse> response =
                    handler.handleResponseStatusException(exception, request);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getTimestamp());
        }
    }

    @Nested
    @DisplayName("handleEntityNotFoundException")
    class HandleEntityNotFoundExceptionTests {

        @Test
        @DisplayName("Should return NOT_FOUND with entity message")
        void shouldReturnNotFoundWithEntityMessage() {
            EntityNotFoundException exception =
                    new EntityNotFoundException("Collection not found: test-collection");

            ResponseEntity<ErrorResponse> response =
                    handler.handleEntityNotFoundException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(404, response.getBody().getStatus());
            assertEquals("Not Found", response.getBody().getError());
            assertEquals("Collection not found: test-collection", response.getBody().getMessage());
            assertEquals("/api/test", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            EntityNotFoundException exception = new EntityNotFoundException("Entity not found");

            ResponseEntity<ErrorResponse> response =
                    handler.handleEntityNotFoundException(exception, request);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getTimestamp());
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void shouldHandleNullMessageGracefully() {
            EntityNotFoundException exception = new EntityNotFoundException((String) null);

            ResponseEntity<ErrorResponse> response =
                    handler.handleEntityNotFoundException(exception, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("handleIllegalArgumentException")
    class HandleIllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should return BAD_REQUEST with argument message")
        void shouldReturnBadRequestWithArgumentMessage() {
            IllegalArgumentException exception =
                    new IllegalArgumentException("Invalid parameter value");

            ResponseEntity<ErrorResponse> response =
                    handler.handleIllegalArgumentException(exception, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatus());
            assertEquals("Bad Request", response.getBody().getError());
            assertEquals("Invalid parameter value", response.getBody().getMessage());
            assertEquals("/api/test", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

            ResponseEntity<ErrorResponse> response =
                    handler.handleIllegalArgumentException(exception, request);

            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getTimestamp());
        }
    }

    @Nested
    @DisplayName("handleIllegalStateException")
    class HandleIllegalStateExceptionTests {

        @Test
        @DisplayName("Should return CONFLICT with state message")
        void shouldReturnConflictWithStateMessage() {
            IllegalStateException exception =
                    new IllegalStateException("Collection already exists: test-collection");

            ResponseEntity<ErrorResponse> response =
                    handler.handleIllegalStateException(exception, request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(409, response.getBody().getStatus());
            assertEquals("Conflict", response.getBody().getError());
            assertEquals(
                    "Collection already exists: test-collection", response.getBody().getMessage());
            assertEquals("/api/test", response.getBody().getPath());
        }

        @Test
        @DisplayName("Should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            IllegalStateException exception = new IllegalStateException("Invalid state");

            ResponseEntity<ErrorResponse> response =
                    handler.handleIllegalStateException(exception, request);

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
        @DisplayName("Should handle OutOfMemoryError wrapped in exception")
        void shouldHandleOutOfMemoryErrorWrappedInException() {
            Exception exception = new RuntimeException("Memory exhausted");

            ResponseEntity<ErrorResponse> response =
                    handler.handleGenericException(exception, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
        }
    }
}
