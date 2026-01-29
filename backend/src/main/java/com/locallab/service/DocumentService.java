package com.locallab.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.locallab.model.Document;
import com.locallab.repository.DocumentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for managing Document entities.
 *
 * <p>Provides document upload with text extraction (PDF and TXT files), retrieval, and deletion
 * operations. All operations are transactional with read-only optimisation for query methods.
 *
 * <h3>Supported File Types:</h3>
 *
 * <ul>
 *   <li><strong>TXT files:</strong> Content is read directly as UTF-8 encoded text
 *   <li><strong>PDF files:</strong> Text is extracted using Apache PDFBox
 * </ul>
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when a requested document is not found
 *   <li>{@link IllegalArgumentException} - Thrown for validation failures (unsupported file type,
 *       missing filename)
 *   <li>{@link ResponseStatusException} - Thrown when text extraction fails
 * </ul>
 *
 * @author William Stephen
 * @see Document
 * @see DocumentRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;

    /**
     * Retrieves all documents.
     *
     * @return a list of all documents, or an empty list if none exist
     */
    public List<Document> findAll() {
        LOGGER.debug("Retrieving all documents");
        List<Document> documents = documentRepository.findAll();
        LOGGER.debug("Found {} documents", documents.size());
        return documents;
    }

    /**
     * Retrieves a document by its identifier.
     *
     * @param id the unique identifier of the document
     * @return the document with the specified identifier
     * @throws EntityNotFoundException if no document exists with the given identifier
     */
    public Document findById(Long id) {
        LOGGER.debug("Retrieving document with id: {}", id);
        return documentRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Document not found with id: {}", id);
                            return new EntityNotFoundException("Document not found: " + id);
                        });
    }

    /**
     * Uploads and stores a document.
     *
     * <p>Supports TXT and PDF file types. Text content is extracted from the uploaded file and
     * stored in the database. The chunk count is initialised to zero and will be updated when the
     * document is processed for RAG operations.
     *
     * @param file the uploaded file (must be TXT or PDF)
     * @return the created document entity with generated identifier
     * @throws IllegalArgumentException if the file type is unsupported or filename is missing
     * @throws ResponseStatusException if text extraction fails
     */
    @Transactional
    public Document upload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        LOGGER.info("Uploading document: {}", filename);

        String content = extractContent(file);

        Document document =
                Document.builder().filename(filename).content(content).chunkCount(0).build();

        Document savedDocument = documentRepository.save(document);
        LOGGER.info(
                "Uploaded document with id: {}, filename: {}",
                savedDocument.getId(),
                savedDocument.getFilename());
        return savedDocument;
    }

    /**
     * Deletes a document by its identifier.
     *
     * @param id the identifier of the document to delete
     * @throws EntityNotFoundException if no document exists with the given identifier
     */
    @Transactional
    public void delete(Long id) {
        LOGGER.info("Deleting document with id: {}", id);

        if (!documentRepository.existsById(id)) {
            LOGGER.warn("Cannot delete document - not found with id: {}", id);
            throw new EntityNotFoundException("Document not found: " + id);
        }

        documentRepository.deleteById(id);
        LOGGER.info("Deleted document with id: {}", id);
    }

    /**
     * Extracts text content from the uploaded file.
     *
     * <p>Determines the file type based on the extension and delegates to the appropriate
     * extraction method.
     *
     * @param file the uploaded file
     * @return the extracted text content
     * @throws IllegalArgumentException if the filename is missing or file type is unsupported
     * @throws ResponseStatusException if extraction fails due to an I/O error
     */
    private String extractContent(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            LOGGER.warn("Upload attempted with missing filename");
            throw new IllegalArgumentException("Filename is required");
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            LOGGER.warn("Upload attempted with file missing extension: {}", filename);
            throw new IllegalArgumentException(
                    "File must have an extension. Supported types: txt, pdf");
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        LOGGER.debug("Extracting content from {} file: {}", extension, filename);

        return switch (extension) {
            case "txt" -> extractTextFromTxt(file);
            case "pdf" -> extractTextFromPdf(file);
            default -> {
                LOGGER.warn("Unsupported file type attempted: {}", extension);
                throw new IllegalArgumentException(
                        "Unsupported file type: " + extension + ". Supported types: txt, pdf");
            }
        };
    }

    /**
     * Extracts text content from a TXT file.
     *
     * @param file the uploaded TXT file
     * @return the text content as a UTF-8 encoded string
     * @throws ResponseStatusException if reading the file fails
     */
    private String extractTextFromTxt(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            LOGGER.debug(
                    "Extracted {} characters from TXT file: {}",
                    content.length(),
                    file.getOriginalFilename());
            return content;
        } catch (IOException e) {
            LOGGER.error("Failed to read text file: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read text file", e);
        }
    }

    /**
     * Extracts text content from a PDF file using Apache PDFBox.
     *
     * @param file the uploaded PDF file
     * @return the extracted text content
     * @throws ResponseStatusException if reading or parsing the PDF fails
     */
    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            LOGGER.debug(
                    "Extracted {} characters from PDF file: {}",
                    content.length(),
                    file.getOriginalFilename());
            return content;
        } catch (IOException e) {
            LOGGER.error("Failed to extract text from PDF: {}", file.getOriginalFilename(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed to extract text from PDF", e);
        }
    }
}
