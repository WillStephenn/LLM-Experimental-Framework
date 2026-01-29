package com.locallab.dto.response;

import java.time.LocalDateTime;

import com.locallab.model.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Document entities.
 *
 * <p>This class encapsulates all fields returned when retrieving documents. The {@code content}
 * field is intentionally excluded from responses to reduce payload size, as per the API contract.
 *
 * <p>Example JSON output:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "filename": "architecture.pdf",
 *   "chunkCount": 45,
 *   "createdAt": "2025-11-27T10:00:00"
 * }
 * }</pre>
 *
 * @author William Stephen
 * @see Document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    /** Unique identifier for the document. */
    private Long id;

    /** Original filename of the uploaded document. */
    private String filename;

    /** Number of chunks generated from this document. */
    private Integer chunkCount;

    /** Timestamp when this document was uploaded. */
    private LocalDateTime createdAt;

    /**
     * Creates a DocumentResponse from a Document entity.
     *
     * <p>This factory method provides a convenient way to convert entity objects to response DTOs.
     * Note that the content field is intentionally excluded to reduce payload size.
     *
     * @param document the document entity to convert (must not be null)
     * @return a new DocumentResponse containing all display-relevant entity data
     * @throws IllegalArgumentException if document is null
     */
    public static DocumentResponse fromEntity(Document document) {
        if (document == null) {
            throw new IllegalArgumentException("Document must not be null");
        }
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .chunkCount(document.getChunkCount())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
