package com.locallab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.locallab.model.Document;

/**
 * Repository tests for {@link DocumentRepository}.
 *
 * <p>Uses {@link DataJpaTest} to configure an in-memory H2 database with JPA repositories. Tests
 * verify the custom {@code findByFilename} query method only; standard JPA CRUD operations are not
 * tested as they are already tested by Spring Data JPA.
 */
@DataJpaTest
class DocumentRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private DocumentRepository documentRepository;

    @Nested
    @DisplayName("findByFilename Query")
    class FindByFilenameQuery {

        @Test
        @DisplayName("Should find documents by exact filename")
        void shouldFindDocumentsByExactFilename() {
            Document doc1 =
                    Document.builder()
                            .filename("architecture.pdf")
                            .content("Architecture content 1")
                            .build();
            Document doc2 =
                    Document.builder().filename("codebase.txt").content("Codebase content").build();
            entityManager.persist(doc1);
            entityManager.persist(doc2);
            entityManager.flush();

            List<Document> found = documentRepository.findByFilename("architecture.pdf");

            assertEquals(1, found.size());
            assertEquals("architecture.pdf", found.getFirst().getFilename());
            assertEquals("Architecture content 1", found.getFirst().getContent());
        }

        @Test
        @DisplayName("Should return multiple documents with same filename")
        void shouldReturnMultipleDocumentsWithSameFilename() {
            Document doc1 =
                    Document.builder()
                            .filename("report.pdf")
                            .content("First version of the report")
                            .chunkCount(10)
                            .build();
            Document doc2 =
                    Document.builder()
                            .filename("report.pdf")
                            .content("Second version of the report")
                            .chunkCount(15)
                            .build();
            Document doc3 =
                    Document.builder()
                            .filename("report.pdf")
                            .content("Third version of the report")
                            .chunkCount(20)
                            .build();
            entityManager.persist(doc1);
            entityManager.persist(doc2);
            entityManager.persist(doc3);
            entityManager.flush();

            List<Document> found = documentRepository.findByFilename("report.pdf");

            assertEquals(3, found.size());
            assertTrue(found.stream().allMatch(d -> "report.pdf".equals(d.getFilename())));
        }

        @Test
        @DisplayName("Should return empty list when filename not found")
        void shouldReturnEmptyListWhenFilenameNotFound() {
            Document doc =
                    Document.builder().filename("existing.txt").content("Some content").build();
            entityManager.persistAndFlush(doc);

            List<Document> found = documentRepository.findByFilename("nonexistent.txt");

            assertNotNull(found);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should perform case-sensitive filename search")
        void shouldPerformCaseSensitiveFilenameSearch() {
            Document doc =
                    Document.builder()
                            .filename("MyDocument.pdf")
                            .content("Case sensitive test")
                            .build();
            entityManager.persistAndFlush(doc);

            List<Document> foundExact = documentRepository.findByFilename("MyDocument.pdf");
            List<Document> foundLowercase = documentRepository.findByFilename("mydocument.pdf");
            List<Document> foundUppercase = documentRepository.findByFilename("MYDOCUMENT.PDF");

            assertEquals(1, foundExact.size());
            assertTrue(foundLowercase.isEmpty());
            assertTrue(foundUppercase.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list when searching with null filename")
        void shouldReturnEmptyListWhenSearchingWithNullFilename() {
            Document doc = Document.builder().filename("test.txt").content("Test content").build();
            entityManager.persistAndFlush(doc);

            List<Document> found = documentRepository.findByFilename(null);

            assertNotNull(found);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should not find documents with partial filename match")
        void shouldNotFindDocumentsWithPartialFilenameMatch() {
            Document doc =
                    Document.builder()
                            .filename("full-document-name.txt")
                            .content("Content here")
                            .build();
            entityManager.persistAndFlush(doc);

            List<Document> foundPartial = documentRepository.findByFilename("full-document");
            List<Document> foundExtension = documentRepository.findByFilename(".txt");

            assertTrue(foundPartial.isEmpty());
            assertTrue(foundExtension.isEmpty());
        }
    }
}
