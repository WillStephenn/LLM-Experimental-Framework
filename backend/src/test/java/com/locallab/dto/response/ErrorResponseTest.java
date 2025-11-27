package com.locallab.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ErrorResponse}. */
@DisplayName("ErrorResponse")
class ErrorResponseTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor should initialise timestamp and empty field errors")
        void defaultConstructorShouldInitialiseTimestampAndEmptyFieldErrors() {
            Instant beforeCreation = Instant.now();

            ErrorResponse response = new ErrorResponse();

            Instant afterCreation = Instant.now();

            assertNotNull(response.getTimestamp());
            assertTrue(
                    response.getTimestamp().equals(beforeCreation)
                            || response.getTimestamp().isAfter(beforeCreation));
            assertTrue(
                    response.getTimestamp().equals(afterCreation)
                            || response.getTimestamp().isBefore(afterCreation));
            assertNotNull(response.getFieldErrors());
            assertTrue(response.getFieldErrors().isEmpty());
        }

        @Test
        @DisplayName("Parameterised constructor should set all basic fields")
        void parameterisedConstructorShouldSetAllBasicFields() {
            ErrorResponse response =
                    new ErrorResponse(400, "Bad Request", "Validation failed", "/api/tasks");

            assertEquals(400, response.getStatus());
            assertEquals("Bad Request", response.getError());
            assertEquals("Validation failed", response.getMessage());
            assertEquals("/api/tasks", response.getPath());
            assertNotNull(response.getTimestamp());
            assertNotNull(response.getFieldErrors());
            assertTrue(response.getFieldErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("Getter and setter tests")
    class GetterSetterTests {

        @Test
        @DisplayName("setTimestamp should update timestamp")
        void setTimestampShouldUpdateTimestamp() {
            ErrorResponse response = new ErrorResponse();
            Instant customTimestamp = Instant.parse("2025-11-27T10:00:00Z");

            response.setTimestamp(customTimestamp);

            assertEquals(customTimestamp, response.getTimestamp());
        }

        @Test
        @DisplayName("setStatus should update status")
        void setStatusShouldUpdateStatus() {
            ErrorResponse response = new ErrorResponse();

            response.setStatus(404);

            assertEquals(404, response.getStatus());
        }

        @Test
        @DisplayName("setError should update error")
        void setErrorShouldUpdateError() {
            ErrorResponse response = new ErrorResponse();

            response.setError("Not Found");

            assertEquals("Not Found", response.getError());
        }

        @Test
        @DisplayName("setMessage should update message")
        void setMessageShouldUpdateMessage() {
            ErrorResponse response = new ErrorResponse();

            response.setMessage("Resource not found");

            assertEquals("Resource not found", response.getMessage());
        }

        @Test
        @DisplayName("setPath should update path")
        void setPathShouldUpdatePath() {
            ErrorResponse response = new ErrorResponse();

            response.setPath("/api/experiments/123");

            assertEquals("/api/experiments/123", response.getPath());
        }

        @Test
        @DisplayName("setFieldErrors should update field errors list")
        void setFieldErrorsShouldUpdateFieldErrorsList() {
            ErrorResponse response = new ErrorResponse();
            List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(new ErrorResponse.FieldError("name", "must not be blank"));

            response.setFieldErrors(fieldErrors);

            assertEquals(1, response.getFieldErrors().size());
            assertEquals("name", response.getFieldErrors().get(0).getField());
        }

        @Test
        @DisplayName("setFieldErrors with null should create empty list")
        void setFieldErrorsWithNullShouldCreateEmptyList() {
            ErrorResponse response = new ErrorResponse();
            response.addFieldError("name", "test");

            response.setFieldErrors(null);

            assertNotNull(response.getFieldErrors());
            assertTrue(response.getFieldErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("addFieldError tests")
    class AddFieldErrorTests {

        @Test
        @DisplayName("addFieldError should add error to list")
        void addFieldErrorShouldAddErrorToList() {
            ErrorResponse response = new ErrorResponse();

            response.addFieldError("email", "must be a valid email address");

            assertEquals(1, response.getFieldErrors().size());
            assertEquals("email", response.getFieldErrors().get(0).getField());
            assertEquals(
                    "must be a valid email address", response.getFieldErrors().get(0).getMessage());
        }

        @Test
        @DisplayName("addFieldError should allow multiple errors")
        void addFieldErrorShouldAllowMultipleErrors() {
            ErrorResponse response = new ErrorResponse();

            response.addFieldError("name", "must not be blank");
            response.addFieldError("email", "must be a valid email");
            response.addFieldError("age", "must be positive");

            assertEquals(3, response.getFieldErrors().size());
        }
    }

    @Nested
    @DisplayName("FieldError tests")
    class FieldErrorTests {

        @Test
        @DisplayName("Default constructor should create empty FieldError")
        void defaultConstructorShouldCreateEmptyFieldError() {
            ErrorResponse.FieldError fieldError = new ErrorResponse.FieldError();

            // Should not throw, fields will be null
            assertNotNull(fieldError);
        }

        @Test
        @DisplayName("Parameterised constructor should set field and message")
        void parameterisedConstructorShouldSetFieldAndMessage() {
            ErrorResponse.FieldError fieldError =
                    new ErrorResponse.FieldError("username", "is required");

            assertEquals("username", fieldError.getField());
            assertEquals("is required", fieldError.getMessage());
        }

        @Test
        @DisplayName("setField should update field name")
        void setFieldShouldUpdateFieldName() {
            ErrorResponse.FieldError fieldError = new ErrorResponse.FieldError();

            fieldError.setField("password");

            assertEquals("password", fieldError.getField());
        }

        @Test
        @DisplayName("setMessage should update message")
        void setMessageShouldUpdateMessage() {
            ErrorResponse.FieldError fieldError = new ErrorResponse.FieldError();

            fieldError.setMessage("must be at least 8 characters");

            assertEquals("must be at least 8 characters", fieldError.getMessage());
        }
    }
}
