package com.locallab.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Document} entity.
 *
 * <p>Tests verify builder pattern, field assignments, default values, and entity construction.
 * Database-level constraints and LOB storage are tested in repository integration tests.
 */
class DocumentTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create Document with all fields via builder")
        void shouldCreateDocumentWithAllFieldsViaBuilder() {
            Document document =
                    Document.builder()
                            .id(1L)
                            .filename("architecture.pdf")
                            .content("This is the document content...")
                            .chunkCount(45)
                            .build();

            assertEquals(1L, document.getId());
            assertEquals("architecture.pdf", document.getFilename());
            assertEquals("This is the document content...", document.getContent());
            assertEquals(45, document.getChunkCount());
            assertNull(document.getCreatedAt());
        }

        @Test
        @DisplayName("Should create Document with minimal fields via builder")
        void shouldCreateDocumentWithMinimalFieldsViaBuilder() {
            Document document =
                    Document.builder().filename("minimal.txt").content("Minimal content").build();

            assertNull(document.getId());
            assertEquals("minimal.txt", document.getFilename());
            assertEquals("Minimal content", document.getContent());
            assertEquals(0, document.getChunkCount());
        }

        @Test
        @DisplayName("Should default chunkCount to zero when not specified")
        void shouldDefaultChunkCountToZeroWhenNotSpecified() {
            Document document =
                    Document.builder().filename("test.txt").content("Test content").build();

            assertEquals(0, document.getChunkCount());
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty Document with no-args constructor")
        void shouldCreateEmptyDocumentWithNoArgsConstructor() {
            Document document = new Document();

            assertNull(document.getId());
            assertNull(document.getFilename());
            assertNull(document.getContent());
            // chunkCount is initialised to 0 via @Builder.Default field initialiser
            assertNotNull(document.getChunkCount());
            assertEquals(0, document.getChunkCount());
            assertNull(document.getCreatedAt());
        }

        @Test
        @DisplayName("Should create Document with all-args constructor")
        void shouldCreateDocumentWithAllArgsConstructor() {
            Document document = new Document(1L, "test.pdf", "Test content", 10, null);

            assertEquals(1L, document.getId());
            assertEquals("test.pdf", document.getFilename());
            assertEquals("Test content", document.getContent());
            assertEquals(10, document.getChunkCount());
        }
    }

    @Nested
    @DisplayName("Setter Tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void shouldUpdateFieldsViaSetters() {
            Document document = new Document();

            document.setId(99L);
            document.setFilename("updated.txt");
            document.setContent("Updated content");
            document.setChunkCount(25);

            assertEquals(99L, document.getId());
            assertEquals("updated.txt", document.getFilename());
            assertEquals("Updated content", document.getContent());
            assertEquals(25, document.getChunkCount());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            Document document1 =
                    Document.builder()
                            .id(1L)
                            .filename("same.txt")
                            .content("Same content")
                            .chunkCount(10)
                            .build();

            Document document2 =
                    Document.builder()
                            .id(1L)
                            .filename("same.txt")
                            .content("Same content")
                            .chunkCount(10)
                            .build();

            assertEquals(document1, document2);
            assertEquals(document1.hashCode(), document2.hashCode());
        }

        @Test
        @DisplayName("Should have consistent toString output")
        void shouldHaveConsistentToStringOutput() {
            Document document =
                    Document.builder()
                            .id(1L)
                            .filename("test.pdf")
                            .content("Test content")
                            .chunkCount(5)
                            .build();

            String toString = document.toString();

            assertNotNull(toString);
            // Lombok @Data generates a toString that includes field names and values
            assertEquals(true, toString.contains("filename=test.pdf"));
            assertEquals(true, toString.contains("chunkCount=5"));
        }
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("Should use default chunkCount of zero from builder")
        void shouldUseDefaultChunkCountOfZeroFromBuilder() {
            Document document =
                    Document.builder().filename("new-upload.txt").content("Fresh content").build();

            assertEquals(0, document.getChunkCount());
        }

        @Test
        @DisplayName("Should allow overriding default chunkCount")
        void shouldAllowOverridingDefaultChunkCount() {
            Document document =
                    Document.builder()
                            .filename("processed.txt")
                            .content("Chunked content")
                            .chunkCount(100)
                            .build();

            assertEquals(100, document.getChunkCount());
        }
    }
}
