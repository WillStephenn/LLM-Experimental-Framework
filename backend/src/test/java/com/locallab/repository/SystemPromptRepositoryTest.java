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

import com.locallab.model.SystemPrompt;

/**
 * Repository tests for {@link SystemPromptRepository}.
 *
 * <p>Uses {@link DataJpaTest} to configure an in-memory H2 database with JPA repositories. Tests
 * verify the custom {@code findByAlias} query method and unique constraint behaviour only; standard
 * JPA CRUD operations are not tested as they are already tested by Spring Data JPA.
 */
@DataJpaTest
class SystemPromptRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private SystemPromptRepository systemPromptRepository;

    @Nested
    @DisplayName("findByAlias Query")
    class FindByAliasQuery {

        @Test
        @DisplayName("Should find system prompt by exact alias")
        void shouldFindSystemPromptByExactAlias() {
            SystemPrompt prompt =
                    SystemPrompt.builder()
                            .alias("json-formatter")
                            .content("You format all responses as valid JSON.")
                            .build();
            entityManager.persistAndFlush(prompt);

            Optional<SystemPrompt> found = systemPromptRepository.findByAlias("json-formatter");

            assertTrue(found.isPresent());
            assertEquals("json-formatter", found.get().getAlias());
            assertEquals("You format all responses as valid JSON.", found.get().getContent());
        }

        @Test
        @DisplayName("Should return empty Optional when alias not found")
        void shouldReturnEmptyOptionalWhenAliasNotFound() {
            SystemPrompt prompt =
                    SystemPrompt.builder().alias("existing-alias").content("Some content").build();
            entityManager.persistAndFlush(prompt);

            Optional<SystemPrompt> found = systemPromptRepository.findByAlias("nonexistent-alias");

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should perform case-sensitive alias search")
        void shouldPerformCaseSensitiveAliasSearch() {
            SystemPrompt prompt =
                    SystemPrompt.builder()
                            .alias("Code-Assistant")
                            .content("Case sensitive test")
                            .build();
            entityManager.persistAndFlush(prompt);

            Optional<SystemPrompt> foundExact =
                    systemPromptRepository.findByAlias("Code-Assistant");
            Optional<SystemPrompt> foundLowercase =
                    systemPromptRepository.findByAlias("code-assistant");
            Optional<SystemPrompt> foundUppercase =
                    systemPromptRepository.findByAlias("CODE-ASSISTANT");

            assertTrue(foundExact.isPresent());
            assertFalse(foundLowercase.isPresent());
            assertFalse(foundUppercase.isPresent());
        }

        @Test
        @DisplayName("Should return empty Optional when searching with null alias")
        void shouldReturnEmptyOptionalWhenSearchingWithNullAlias() {
            SystemPrompt prompt =
                    SystemPrompt.builder().alias("test-alias").content("Test content").build();
            entityManager.persistAndFlush(prompt);

            Optional<SystemPrompt> found = systemPromptRepository.findByAlias(null);

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Should not find system prompt with partial alias match")
        void shouldNotFindSystemPromptWithPartialAliasMatch() {
            SystemPrompt prompt =
                    SystemPrompt.builder().alias("full-alias-name").content("Content here").build();
            entityManager.persistAndFlush(prompt);

            Optional<SystemPrompt> foundPartial = systemPromptRepository.findByAlias("full-alias");
            Optional<SystemPrompt> foundSuffix = systemPromptRepository.findByAlias("-name");

            assertFalse(foundPartial.isPresent());
            assertFalse(foundSuffix.isPresent());
        }
    }

    @Nested
    @DisplayName("Unique Alias Constraint")
    class UniqueAliasConstraint {

        @Test
        @DisplayName("Should enforce unique alias constraint")
        void shouldEnforceUniqueAliasConstraint() {
            SystemPrompt prompt1 =
                    SystemPrompt.builder()
                            .alias("duplicate-alias")
                            .content("First prompt content")
                            .build();
            entityManager.persistAndFlush(prompt1);

            SystemPrompt prompt2 =
                    SystemPrompt.builder()
                            .alias("duplicate-alias")
                            .content("Second prompt content")
                            .build();

            assertThrows(
                    DataIntegrityViolationException.class,
                    () -> {
                        systemPromptRepository.save(prompt2);
                        entityManager.flush();
                    });
        }

        @Test
        @DisplayName("Should allow different aliases")
        void shouldAllowDifferentAliases() {
            SystemPrompt prompt1 =
                    SystemPrompt.builder()
                            .alias("unique-alias-1")
                            .content("First prompt content")
                            .build();
            SystemPrompt prompt2 =
                    SystemPrompt.builder()
                            .alias("unique-alias-2")
                            .content("Second prompt content")
                            .build();

            entityManager.persist(prompt1);
            entityManager.persist(prompt2);
            entityManager.flush();

            assertEquals(2, systemPromptRepository.count());
        }
    }
}
