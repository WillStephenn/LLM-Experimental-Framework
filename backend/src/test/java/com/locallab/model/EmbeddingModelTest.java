package com.locallab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for {@link EmbeddingModel} entity.
 *
 * <p>These tests verify the entity's structure and validation constraints as defined in the API
 * contract.
 */
class EmbeddingModelTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Field Tests")
    class FieldTests {

        @Test
        @DisplayName("Should create entity with all fields")
        void shouldCreateEntityWithAllFields() {
            EmbeddingModel model = new EmbeddingModel();
            model.setId(1L);
            model.setName("Nomic Embed Text");
            model.setOllamaModelName("nomic-embed-text");
            model.setDimensions(768);

            assertEquals(1L, model.getId());
            assertEquals("Nomic Embed Text", model.getName());
            assertEquals("nomic-embed-text", model.getOllamaModelName());
            assertEquals(768, model.getDimensions());
        }

        @Test
        @DisplayName("Should have null values by default")
        void shouldHaveNullValuesByDefault() {
            EmbeddingModel model = new EmbeddingModel();

            assertNull(model.getId());
            assertNull(model.getName());
            assertNull(model.getOllamaModelName());
            assertNull(model.getDimensions());
            assertNull(model.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid fields")
        void shouldPassValidationWithValidFields() {
            EmbeddingModel model = createValidEmbeddingModel();

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation when name is blank")
        void shouldFailValidationWhenNameIsBlank() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setName("");

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when name is null")
        void shouldFailValidationWhenNameIsNull() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setName(null);

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when name exceeds 100 characters")
        void shouldFailValidationWhenNameExceedsMaxLength() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setName("a".repeat(101));

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertEquals("Name must not exceed 100 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when ollamaModelName is blank")
        void shouldFailValidationWhenOllamaModelNameIsBlank() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setOllamaModelName("");

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("ollamaModelName", violation.getPropertyPath().toString());
            assertEquals("Ollama model name is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when ollamaModelName is null")
        void shouldFailValidationWhenOllamaModelNameIsNull() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setOllamaModelName(null);

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("ollamaModelName", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when ollamaModelName exceeds 100 characters")
        void shouldFailValidationWhenOllamaModelNameExceedsMaxLength() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setOllamaModelName("a".repeat(101));

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("ollamaModelName", violation.getPropertyPath().toString());
            assertEquals(
                    "Ollama model name must not exceed 100 characters", violation.getMessage());
        }

        @Test
        @DisplayName("Should fail validation when dimensions is null")
        void shouldFailValidationWhenDimensionsIsNull() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setDimensions(null);

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(1, violations.size());
            ConstraintViolation<EmbeddingModel> violation = violations.iterator().next();
            assertEquals("dimensions", violation.getPropertyPath().toString());
            assertEquals("Dimensions is required", violation.getMessage());
        }

        @Test
        @DisplayName("Should accept name at maximum length boundary")
        void shouldAcceptNameAtMaximumLengthBoundary() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setName("a".repeat(100));

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should accept ollamaModelName at maximum length boundary")
        void shouldAcceptOllamaModelNameAtMaximumLengthBoundary() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setOllamaModelName("a".repeat(100));

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should fail validation with multiple invalid fields")
        void shouldFailValidationWithMultipleInvalidFields() {
            EmbeddingModel model = new EmbeddingModel();

            Set<ConstraintViolation<EmbeddingModel>> violations = validator.validate(model);

            assertEquals(3, violations.size());
        }
    }

    @Nested
    @DisplayName("Lombok Generated Methods Tests")
    class LombokTests {

        @Test
        @DisplayName("Should generate equals method correctly")
        void shouldGenerateEqualsMethodCorrectly() {
            EmbeddingModel model1 = createValidEmbeddingModel();
            model1.setId(1L);

            EmbeddingModel model2 = createValidEmbeddingModel();
            model2.setId(1L);

            assertEquals(model1, model2);
        }

        @Test
        @DisplayName("Should generate hashCode method correctly")
        void shouldGenerateHashCodeMethodCorrectly() {
            EmbeddingModel model1 = createValidEmbeddingModel();
            model1.setId(1L);

            EmbeddingModel model2 = createValidEmbeddingModel();
            model2.setId(1L);

            assertEquals(model1.hashCode(), model2.hashCode());
        }

        @Test
        @DisplayName("Should generate toString method correctly")
        void shouldGenerateToStringMethodCorrectly() {
            EmbeddingModel model = createValidEmbeddingModel();
            model.setId(1L);

            String toStringResult = model.toString();

            assertNotNull(toStringResult);
            assertTrue(toStringResult.contains("EmbeddingModel"));
            assertTrue(toStringResult.contains("id=1"));
            assertTrue(toStringResult.contains("name=Nomic Embed Text"));
            assertTrue(toStringResult.contains("ollamaModelName=nomic-embed-text"));
            assertTrue(toStringResult.contains("dimensions=768"));
        }
    }

    /**
     * Creates a valid EmbeddingModel instance for testing.
     *
     * @return a fully populated EmbeddingModel with valid field values
     */
    private EmbeddingModel createValidEmbeddingModel() {
        EmbeddingModel model = new EmbeddingModel();
        model.setName("Nomic Embed Text");
        model.setOllamaModelName("nomic-embed-text");
        model.setDimensions(768);
        return model;
    }
}
