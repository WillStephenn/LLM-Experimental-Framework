package com.locallab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SystemPrompt} entity.
 *
 * <p>Tests verify builder pattern, field assignments, and entity construction. Database-level
 * constraints (unique alias) are tested in repository integration tests.
 */
class SystemPromptTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create SystemPrompt with all fields via builder")
        void shouldCreateSystemPromptWithAllFieldsViaBuilder() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("code-assistant")
                            .content("You are an expert code reviewer...")
                            .build();

            assertEquals(1L, systemPrompt.getId());
            assertEquals("code-assistant", systemPrompt.getAlias());
            assertEquals("You are an expert code reviewer...", systemPrompt.getContent());
            assertNull(systemPrompt.getCreatedAt());
        }

        @Test
        @DisplayName("Should create SystemPrompt with minimal fields via builder")
        void shouldCreateSystemPromptWithMinimalFieldsViaBuilder() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder().alias("minimal-prompt").content("Basic content").build();

            assertNull(systemPrompt.getId());
            assertEquals("minimal-prompt", systemPrompt.getAlias());
            assertEquals("Basic content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty SystemPrompt with no-args constructor")
        void shouldCreateEmptySystemPromptWithNoArgsConstructor() {
            SystemPrompt systemPrompt = new SystemPrompt();

            assertNull(systemPrompt.getId());
            assertNull(systemPrompt.getAlias());
            assertNull(systemPrompt.getContent());
            assertNull(systemPrompt.getCreatedAt());
        }

        @Test
        @DisplayName("Should create SystemPrompt with all-args constructor")
        void shouldCreateSystemPromptWithAllArgsConstructor() {
            SystemPrompt systemPrompt = new SystemPrompt(1L, "test-alias", "Test content", null);

            assertEquals(1L, systemPrompt.getId());
            assertEquals("test-alias", systemPrompt.getAlias());
            assertEquals("Test content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            SystemPrompt systemPrompt = new SystemPrompt();

            systemPrompt.setId(99L);
            systemPrompt.setAlias("updated-alias");
            systemPrompt.setContent("Updated content");

            assertEquals(99L, systemPrompt.getId());
            assertEquals("updated-alias", systemPrompt.getAlias());
            assertEquals("Updated content", systemPrompt.getContent());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            SystemPrompt prompt1 =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("same-alias")
                            .content("Same content")
                            .build();

            SystemPrompt prompt2 =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("same-alias")
                            .content("Same content")
                            .build();

            assertEquals(prompt1, prompt2);
            assertEquals(prompt1.hashCode(), prompt2.hashCode());
        }

        @Test
        @DisplayName("Should have consistent toString output")
        void shouldHaveConsistentToStringOutput() {
            SystemPrompt systemPrompt =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias("test-alias")
                            .content("Test content")
                            .build();

            String toString = systemPrompt.toString();

            assertNotNull(toString);
            // Lombok @Data generates a toString that includes field names and values
            assertEquals(true, toString.contains("alias=test-alias"));
            assertEquals(true, toString.contains("content=Test content"));
        }
    }
}
