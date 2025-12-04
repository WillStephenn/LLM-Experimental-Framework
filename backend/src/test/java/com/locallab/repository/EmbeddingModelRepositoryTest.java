package com.locallab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.locallab.model.EmbeddingModel;

/**
 * Repository tests for {@link EmbeddingModelRepository}.
 *
 * <p>Uses {@link DataJpaTest} to configure an in-memory H2 database with JPA repositories. Tests
 * verify the custom {@code findByName} query method and unique constraint behaviour.
 *
 * <p>Standard JPA CRUD operations (save, findById, findAll, delete, etc.) are provided by Spring
 * Data JPA and are not tested here as they are framework behaviour.
 */
@DataJpaTest
class EmbeddingModelRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private EmbeddingModelRepository embeddingModelRepository;

    @Nested
    @DisplayName("findByName Query")
    class FindByNameQuery {

        @Test
        @DisplayName("Should find embedding model by exact name")
        void shouldFindEmbeddingModelByExactName() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Nomic Embed Text")
                            .ollamaModelName("nomic-embed-text")
                            .dimensions(768)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found =
                    embeddingModelRepository.findByName("Nomic Embed Text");

            assertTrue(found.isPresent());
            assertEquals("Nomic Embed Text", found.get().getName());
            assertEquals("nomic-embed-text", found.get().getOllamaModelName());
            assertEquals(768, found.get().getDimensions());
        }

        @Test
        @DisplayName("Should return empty Optional when name not found")
        void shouldReturnEmptyOptionalWhenNameNotFound() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Existing Model")
                            .ollamaModelName("existing-model")
                            .dimensions(384)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found =
                    embeddingModelRepository.findByName("Nonexistent Model");

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should perform case-sensitive name search")
        void shouldPerformCaseSensitiveNameSearch() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("MXBai Embed Large")
                            .ollamaModelName("mxbai-embed-large")
                            .dimensions(1024)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> foundExact =
                    embeddingModelRepository.findByName("MXBai Embed Large");
            Optional<EmbeddingModel> foundLowercase =
                    embeddingModelRepository.findByName("mxbai embed large");
            Optional<EmbeddingModel> foundUppercase =
                    embeddingModelRepository.findByName("MXBAI EMBED LARGE");

            assertTrue(foundExact.isPresent());
            assertFalse(foundLowercase.isPresent());
            assertFalse(foundUppercase.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional when searching with null name")
        void shouldReturnEmptyOptionalWhenSearchingWithNullName() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Test Model")
                            .ollamaModelName("test-model")
                            .dimensions(512)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found = embeddingModelRepository.findByName(null);

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should not find embedding model with partial name match")
        void shouldNotFindEmbeddingModelWithPartialNameMatch() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("All MiniLM L6 v2")
                            .ollamaModelName("all-minilm-l6-v2")
                            .dimensions(384)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> foundPartial =
                    embeddingModelRepository.findByName("All MiniLM");
            Optional<EmbeddingModel> foundSuffix = embeddingModelRepository.findByName("L6 v2");

            assertFalse(foundPartial.isPresent());
            assertFalse(foundSuffix.isPresent());
        }
    }

    @Nested
    @DisplayName("findByOllamaModelName Query")
    class FindByOllamaModelNameQuery {

        @Test
        @DisplayName("Should find embedding model by exact Ollama model name")
        void shouldFindEmbeddingModelByExactOllamaModelName() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Nomic Embed Text")
                            .ollamaModelName("nomic-embed-text")
                            .dimensions(768)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found =
                    embeddingModelRepository.findByOllamaModelName("nomic-embed-text");

            assertTrue(found.isPresent());
            assertEquals("Nomic Embed Text", found.get().getName());
            assertEquals("nomic-embed-text", found.get().getOllamaModelName());
            assertEquals(768, found.get().getDimensions());
        }

        @Test
        @DisplayName("Should return empty Optional when Ollama model name not found")
        void shouldReturnEmptyOptionalWhenOllamaModelNameNotFound() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Existing Model")
                            .ollamaModelName("existing-model")
                            .dimensions(384)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found =
                    embeddingModelRepository.findByOllamaModelName("nonexistent-model");

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should perform case-sensitive Ollama model name search")
        void shouldPerformCaseSensitiveOllamaModelNameSearch() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("MXBai Embed Large")
                            .ollamaModelName("mxbai-embed-large")
                            .dimensions(1024)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> foundExact =
                    embeddingModelRepository.findByOllamaModelName("mxbai-embed-large");
            Optional<EmbeddingModel> foundUppercase =
                    embeddingModelRepository.findByOllamaModelName("MXBAI-EMBED-LARGE");

            assertTrue(foundExact.isPresent());
            assertFalse(foundUppercase.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional when searching with null Ollama model name")
        void shouldReturnEmptyOptionalWhenSearchingWithNullOllamaModelName() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Test Model")
                            .ollamaModelName("test-model")
                            .dimensions(512)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found = embeddingModelRepository.findByOllamaModelName(null);

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should find embedding model with version tag in name")
        void shouldFindEmbeddingModelWithVersionTagInName() {
            EmbeddingModel model =
                    EmbeddingModel.builder()
                            .name("Model with Version")
                            .ollamaModelName("model:v1.0")
                            .dimensions(768)
                            .build();
            entityManager.persistAndFlush(model);

            Optional<EmbeddingModel> found =
                    embeddingModelRepository.findByOllamaModelName("model:v1.0");

            assertTrue(found.isPresent());
            assertEquals("model:v1.0", found.get().getOllamaModelName());
        }
    }

    @Nested
    @DisplayName("Unique Name Constraint")
    class UniqueNameConstraint {

        @Test
        @DisplayName("Should enforce unique name constraint")
        void shouldEnforceUniqueNameConstraint() {
            EmbeddingModel model1 =
                    EmbeddingModel.builder()
                            .name("Duplicate Name")
                            .ollamaModelName("model-1")
                            .dimensions(768)
                            .build();
            entityManager.persistAndFlush(model1);

            EmbeddingModel model2 =
                    EmbeddingModel.builder()
                            .name("Duplicate Name")
                            .ollamaModelName("model-2")
                            .dimensions(384)
                            .build();

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> {
                        embeddingModelRepository.save(model2);
                        entityManager.flush();
                    });
        }

        @Test
        @DisplayName("Should allow different names")
        void shouldAllowDifferentNames() {
            EmbeddingModel model1 =
                    EmbeddingModel.builder()
                            .name("Unique Name 1")
                            .ollamaModelName("model-1")
                            .dimensions(768)
                            .build();
            EmbeddingModel model2 =
                    EmbeddingModel.builder()
                            .name("Unique Name 2")
                            .ollamaModelName("model-2")
                            .dimensions(384)
                            .build();

            entityManager.persist(model1);
            entityManager.persist(model2);
            entityManager.flush();

            assertEquals(2, embeddingModelRepository.count());
        }
    }
}
