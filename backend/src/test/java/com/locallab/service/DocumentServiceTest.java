package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.locallab.model.Document;
import com.locallab.repository.DocumentRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link DocumentService}.
 *
 * <p>Uses Mockito to mock the {@link DocumentRepository} dependency. Tests cover all CRUD
 * operations, file upload with text extraction, and error handling scenarios.
 *
 * @see DocumentService
 * @see DocumentRepository
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;

    @InjectMocks private DocumentService documentService;

    private Document architectureDocument;
    private Document codebaseDocument;

    @BeforeEach
    void setUp() {
        architectureDocument =
                Document.builder()
                        .id(1L)
                        .filename("architecture.pdf")
                        .content("This is the architecture document content.")
                        .chunkCount(5)
                        .createdAt(LocalDateTime.now())
                        .build();

        codebaseDocument =
                Document.builder()
                        .id(2L)
                        .filename("codebase.txt")
                        .content("This is the codebase documentation.")
                        .chunkCount(3)
                        .createdAt(LocalDateTime.now())
                        .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all documents")
        void shouldReturnAllDocuments() {
            when(documentRepository.findAll())
                    .thenReturn(Arrays.asList(architectureDocument, codebaseDocument));

            List<Document> results = documentService.findAll();

            assertEquals(2, results.size());
            verify(documentRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() {
            when(documentRepository.findAll()).thenReturn(Collections.emptyList());

            List<Document> results = documentService.findAll();

            assertTrue(results.isEmpty());
            verify(documentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return document when found")
        void shouldReturnDocumentWhenFound() {
            when(documentRepository.findById(1L)).thenReturn(Optional.of(architectureDocument));

            Document result = documentService.findById(1L);

            assertNotNull(result);
            assertEquals("architecture.pdf", result.getFilename());
            verify(documentRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class, () -> documentService.findById(999L));

            assertEquals("Document not found: 999", exception.getMessage());
            verify(documentRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("upload Tests")
    class UploadTests {

        @Test
        @DisplayName("Should upload TXT file successfully")
        void shouldUploadTxtFileSuccessfully() {
            String content = "This is a test document.";
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "test.txt",
                            "text/plain",
                            content.getBytes(StandardCharsets.UTF_8));

            Document savedDocument =
                    Document.builder()
                            .id(3L)
                            .filename("test.txt")
                            .content(content)
                            .chunkCount(0)
                            .createdAt(LocalDateTime.now())
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals(3L, result.getId());
            assertEquals("test.txt", result.getFilename());
            assertEquals(content, result.getContent());
            assertEquals(0, result.getChunkCount());

            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository).save(captor.capture());

            Document captured = captor.getValue();
            assertEquals("test.txt", captured.getFilename());
            assertEquals(content, captured.getContent());
            assertEquals(0, captured.getChunkCount());
        }

        @Test
        @DisplayName("Should upload TXT file with UTF-8 content")
        void shouldUploadTxtFileWithUtf8Content() {
            String content = "Hello, 世界! Привет мир! こんにちは世界!";
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "unicode.txt",
                            "text/plain",
                            content.getBytes(StandardCharsets.UTF_8));

            Document savedDocument =
                    Document.builder()
                            .id(4L)
                            .filename("unicode.txt")
                            .content(content)
                            .chunkCount(0)
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals(content, result.getContent());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for null filename")
        void shouldThrowIllegalArgumentExceptionForNullFilename() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file", null, "text/plain", "content".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals("Filename is required", exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty filename")
        void shouldThrowIllegalArgumentExceptionForEmptyFilename() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file", "", "text/plain", "content".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals("Filename is required", exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for filename without extension")
        void shouldThrowIllegalArgumentExceptionForFilenameWithoutExtension() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "noextension",
                            "text/plain",
                            "content".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals(
                    "File must have an extension. Supported types: txt, pdf",
                    exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for filename ending with dot")
        void shouldThrowIllegalArgumentExceptionForFilenameEndingWithDot() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "filename.",
                            "text/plain",
                            "content".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals(
                    "File must have an extension. Supported types: txt, pdf",
                    exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unsupported file type")
        void shouldThrowIllegalArgumentExceptionForUnsupportedFileType() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "document.docx",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "content".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals(
                    "Unsupported file type: docx. Supported types: txt, pdf",
                    exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for XML file type")
        void shouldThrowIllegalArgumentExceptionForXmlFileType() {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "document.xml",
                            "application/xml",
                            "<?xml version=\"1.0\"?>".getBytes(StandardCharsets.UTF_8));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> documentService.upload(file));

            assertEquals(
                    "Unsupported file type: xml. Supported types: txt, pdf",
                    exception.getMessage());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should handle TXT extension case insensitively")
        void shouldHandleTxtExtensionCaseInsensitively() {
            String content = "Test content";
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "TEST.TXT",
                            "text/plain",
                            content.getBytes(StandardCharsets.UTF_8));

            Document savedDocument =
                    Document.builder()
                            .id(5L)
                            .filename("TEST.TXT")
                            .content(content)
                            .chunkCount(0)
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals("TEST.TXT", result.getFilename());
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should handle PDF extension case insensitively")
        void shouldHandlePdfExtensionCaseInsensitively() throws IOException {
            // Create a minimal valid PDF byte array
            byte[] pdfBytes = createMinimalPdfBytes();

            MockMultipartFile file =
                    new MockMultipartFile("file", "DOCUMENT.PDF", "application/pdf", pdfBytes);

            Document savedDocument =
                    Document.builder()
                            .id(6L)
                            .filename("DOCUMENT.PDF")
                            .content("Hello World")
                            .chunkCount(0)
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals("DOCUMENT.PDF", result.getFilename());
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ResponseStatusException for TXT file read error")
        void shouldThrowResponseStatusExceptionForTxtFileReadError() throws IOException {
            MockMultipartFile file =
                    new MockMultipartFile("file", "error.txt", "text/plain", (byte[]) null) {
                        @Override
                        public byte[] getBytes() throws IOException {
                            throw new IOException("Simulated read error");
                        }
                    };

            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> documentService.upload(file));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals("Failed to read text file", exception.getReason());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ResponseStatusException for PDF extraction error")
        void shouldThrowResponseStatusExceptionForPdfExtractionError() throws IOException {
            // Create an invalid PDF that will cause PDFBox to fail
            byte[] invalidPdfBytes = "This is not a valid PDF".getBytes(StandardCharsets.UTF_8);

            MockMultipartFile file =
                    new MockMultipartFile(
                            "file", "invalid.pdf", "application/pdf", invalidPdfBytes);

            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> documentService.upload(file));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals("Failed to extract text from PDF", exception.getReason());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should throw ResponseStatusException for PDF input stream error")
        void shouldThrowResponseStatusExceptionForPdfInputStreamError() throws IOException {
            MockMultipartFile file =
                    new MockMultipartFile("file", "error.pdf", "application/pdf", new byte[0]) {
                        @Override
                        public InputStream getInputStream() throws IOException {
                            throw new IOException("Simulated stream error");
                        }
                    };

            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> documentService.upload(file));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertEquals("Failed to extract text from PDF", exception.getReason());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("Should upload valid PDF file successfully")
        void shouldUploadValidPdfFileSuccessfully() throws IOException {
            byte[] pdfBytes = createMinimalPdfBytes();

            MockMultipartFile file =
                    new MockMultipartFile("file", "valid.pdf", "application/pdf", pdfBytes);

            Document savedDocument =
                    Document.builder()
                            .id(7L)
                            .filename("valid.pdf")
                            .content("Hello World")
                            .chunkCount(0)
                            .createdAt(LocalDateTime.now())
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals("valid.pdf", result.getFilename());
            assertEquals(0, result.getChunkCount());

            ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository).save(captor.capture());

            Document captured = captor.getValue();
            assertEquals("valid.pdf", captured.getFilename());
            assertNotNull(captured.getContent());
            assertTrue(captured.getContent().contains("Hello World"));
        }

        @Test
        @DisplayName("Should upload empty TXT file")
        void shouldUploadEmptyTxtFile() {
            MockMultipartFile file =
                    new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

            Document savedDocument =
                    Document.builder()
                            .id(8L)
                            .filename("empty.txt")
                            .content("")
                            .chunkCount(0)
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals("", result.getContent());
        }

        @Test
        @DisplayName("Should handle filename with multiple dots")
        void shouldHandleFilenameWithMultipleDots() {
            String content = "Test content";
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "my.document.file.txt",
                            "text/plain",
                            content.getBytes(StandardCharsets.UTF_8));

            Document savedDocument =
                    Document.builder()
                            .id(9L)
                            .filename("my.document.file.txt")
                            .content(content)
                            .chunkCount(0)
                            .build();

            when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

            Document result = documentService.upload(file);

            assertNotNull(result);
            assertEquals("my.document.file.txt", result.getFilename());
        }

        /**
         * Creates a minimal valid PDF byte array for testing.
         *
         * @return byte array representing a valid PDF document
         * @throws IOException if PDF creation fails
         */
        private byte[] createMinimalPdfBytes() throws IOException {
            org.apache.pdfbox.pdmodel.PDDocument pdfDocument =
                    new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            pdfDocument.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream contentStream =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDocument, page);
            contentStream.beginText();
            contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();
            contentStream.close();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            pdfDocument.save(outputStream);
            pdfDocument.close();

            return outputStream.toByteArray();
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocumentSuccessfully() {
            when(documentRepository.existsById(1L)).thenReturn(true);

            documentService.delete(1L);

            verify(documentRepository).existsById(1L);
            verify(documentRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent document")
        void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentDocument() {
            when(documentRepository.existsById(999L)).thenReturn(false);

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class, () -> documentService.delete(999L));

            assertEquals("Document not found: 999", exception.getMessage());
            verify(documentRepository).existsById(999L);
            verify(documentRepository, never()).deleteById(any());
        }
    }
}
