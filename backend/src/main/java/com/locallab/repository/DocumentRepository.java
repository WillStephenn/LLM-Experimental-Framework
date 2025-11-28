package com.locallab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.locallab.model.Document;

/**
 * Repository interface for {@link Document} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with custom query methods
 * for document retrieval. Spring Data JPA auto-implements the derived query methods.
 *
 * <p>The {@code findByFilename} method returns a {@link List} rather than a single entity because
 * filenames are not unique - multiple documents may share the same filename (e.g., different
 * uploads of "architecture.pdf").
 *
 * @see Document
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Finds all documents with the specified filename.
     *
     * <p>Performs a case-sensitive exact match on the filename field. Multiple documents may have
     * the same filename, so this method returns a list.
     *
     * @param filename the exact filename to search for (e.g., "architecture.pdf")
     * @return a list of documents matching the filename, or an empty list if none found
     */
    List<Document> findByFilename(String filename);
}
