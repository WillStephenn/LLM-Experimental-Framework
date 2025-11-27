package com.locallab.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an uploaded document for use in RAG (Retrieval-Augmented Generation) operations.
 *
 * <p>Documents are uploaded by users and processed into chunks for embedding storage. The entity
 * tracks the original filename, raw content, and the number of chunks generated during processing.
 *
 * <p>The content is stored as a LOB to support large document sizes. Chunk count is initialised to
 * zero and updated after document processing completes.
 *
 * @see com.locallab.model.ExperimentRun
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents")
public class Document {

    /** Unique identifier for the document. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Original filename of the uploaded document.
     *
     * <p>Preserved for display purposes and to help users identify their uploads. Examples:
     * "architecture.pdf", "codebase.txt", "requirements.md".
     */
    @NotBlank(message = "Filename is required")
    @Column(nullable = false)
    private String filename;

    /**
     * The raw textual content of the document.
     *
     * <p>Extracted from the uploaded file (PDF or TXT). Stored as a LOB to accommodate large
     * documents. This content is chunked during RAG processing.
     */
    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * Number of chunks generated from this document.
     *
     * <p>Updated after document processing. Defaults to zero for newly uploaded documents before
     * chunking occurs.
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer chunkCount = 0;

    /**
     * Timestamp when this document was uploaded.
     *
     * <p>Automatically set by Hibernate on entity creation.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
