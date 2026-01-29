package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

import com.locallab.client.ChromaClient;
import com.locallab.client.OllamaClient;
import com.locallab.dto.ChromaDocument;
import com.locallab.dto.ChromaQueryResult;
import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.request.EmbeddingRequest;
import com.locallab.dto.response.EmbeddingResponse;
import com.locallab.model.Document;
import com.locallab.model.EmbeddingModel;
import com.locallab.repository.EmbeddingModelRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link RagService}.
 *
 * <p>Uses Mockito to mock {@link DocumentService}, {@link OllamaClient}, {@link ChromaClient}, and
 * {@link EmbeddingModelRepository} dependencies. Tests cover document chunking, embedding and
 * storage, query and retrieval, and context building operations.
 *
 * @see RagService
 */
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock private DocumentService documentService;

    @Mock private OllamaClient ollamaClient;

    @Mock private ChromaClient chromaClient;

    @Mock private EmbeddingModelRepository embeddingModelRepository;

    @InjectMocks private RagService ragService;

    private Document testDocument;
    private EmbeddingModel testEmbeddingModel;
    private EmbeddingResponse testEmbeddingResponse;

    @BeforeEach
    void setUp() {
        testDocument =
                Document.builder()
                        .id(1L)
                        .filename("test.txt")
                        .content("This is a test document with enough content for chunking.")
                        .chunkCount(0)
                        .createdAt(LocalDateTime.now())
                        .build();

        testEmbeddingModel =
                EmbeddingModel.builder()
                        .id(1L)
                        .name("Nomic Embed Text")
                        .ollamaModelName("nomic-embed-text")
                        .dimensions(768)
                        .createdAt(LocalDateTime.now())
                        .build();

        testEmbeddingResponse =
                EmbeddingResponse.builder()
                        .embedding(Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5))
                        .model("nomic-embed-text")
                        .dimensions(5)
                        .build();
    }

    @Nested
    @DisplayName("chunkDocument Tests")
    class ChunkDocumentTests {

        @Test
        @DisplayName("Should chunk document without overlap")
        void shouldChunkDocumentWithoutOverlap() {
            String content = "ABCDEFGHIJ";
            int chunkSize = 3;
            int overlap = 0;

            List<String> chunks = ragService.chunkDocument(content, chunkSize, overlap);

            assertEquals(4, chunks.size());
            assertEquals("ABC", chunks.get(0));
            assertEquals("DEF", chunks.get(1));
            assertEquals("GHI", chunks.get(2));
            assertEquals("J", chunks.get(3));
        }

        @Test
        @DisplayName("Should chunk document with overlap")
        void shouldChunkDocumentWithOverlap() {
            String content = "ABCDEFGHIJ";
            int chunkSize = 4;
            int overlap = 2;

            List<String> chunks = ragService.chunkDocument(content, chunkSize, overlap);

            assertEquals(5, chunks.size());
            assertEquals("ABCD", chunks.get(0));
            assertEquals("CDEF", chunks.get(1));
            assertEquals("EFGH", chunks.get(2));
            assertEquals("GHIJ", chunks.get(3));
            assertEquals("IJ", chunks.get(4));
        }

        @Test
        @DisplayName("Should return single chunk for content smaller than chunk size")
        void shouldReturnSingleChunkForSmallContent() {
            String content = "ABC";
            int chunkSize = 10;
            int overlap = 2;

            List<String> chunks = ragService.chunkDocument(content, chunkSize, overlap);

            assertEquals(1, chunks.size());
            assertEquals("ABC", chunks.get(0));
        }

        @Test
        @DisplayName("Should return empty list for null content")
        void shouldReturnEmptyListForNullContent() {
            List<String> chunks = ragService.chunkDocument(null, 10, 2);

            assertTrue(chunks.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for empty content")
        void shouldReturnEmptyListForEmptyContent() {
            List<String> chunks = ragService.chunkDocument("", 10, 2);

            assertTrue(chunks.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when overlap equals chunk size")
        void shouldThrowExceptionWhenOverlapEqualsChunkSize() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ragService.chunkDocument("content", 5, 5));

            assertEquals("Chunk overlap must be less than chunk size", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when overlap greater than chunk size")
        void shouldThrowExceptionWhenOverlapGreaterThanChunkSize() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ragService.chunkDocument("content", 5, 10));

            assertEquals("Chunk overlap must be less than chunk size", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when chunk size is zero")
        void shouldThrowExceptionWhenChunkSizeIsZero() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ragService.chunkDocument("content", 0, 0));

            assertEquals("Chunk size must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when chunk size is negative")
        void shouldThrowExceptionWhenChunkSizeIsNegative() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ragService.chunkDocument("content", -1, 0));

            assertEquals("Chunk size must be positive", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when overlap is negative")
        void shouldThrowExceptionWhenOverlapIsNegative() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> ragService.chunkDocument("content", 5, -1));

            assertEquals("Chunk overlap must not be negative", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle exact multiple of chunk size")
        void shouldHandleExactMultipleOfChunkSize() {
            String content = "ABCDEF";
            int chunkSize = 3;
            int overlap = 0;

            List<String> chunks = ragService.chunkDocument(content, chunkSize, overlap);

            assertEquals(2, chunks.size());
            assertEquals("ABC", chunks.get(0));
            assertEquals("DEF", chunks.get(1));
        }

        @Test
        @DisplayName("Should handle large overlap")
        void shouldHandleLargeOverlap() {
            String content = "ABCDEFGHIJ";
            int chunkSize = 5;
            int overlap = 4;

            // Step size = chunkSize - overlap = 5 - 4 = 1
            // So we step by 1 character each time until we've covered the whole string
            List<String> chunks = ragService.chunkDocument(content, chunkSize, overlap);

            assertEquals(10, chunks.size());
            assertEquals("ABCDE", chunks.get(0));
            assertEquals("BCDEF", chunks.get(1));
            assertEquals("CDEFG", chunks.get(2));
            assertEquals("DEFGH", chunks.get(3));
            assertEquals("EFGHI", chunks.get(4));
            assertEquals("FGHIJ", chunks.get(5));
            assertEquals("GHIJ", chunks.get(6));
            assertEquals("HIJ", chunks.get(7));
            assertEquals("IJ", chunks.get(8));
            assertEquals("J", chunks.get(9));
        }
    }

    @Nested
    @DisplayName("embedAndStore Tests")
    class EmbedAndStoreTests {

        @Test
        @DisplayName("Should embed and store document successfully")
        void shouldEmbedAndStoreDocumentSuccessfully() {
            when(documentService.findById(1L)).thenReturn(testDocument);
            when(embeddingModelRepository.findByOllamaModelName("nomic-embed-text"))
                    .thenReturn(Optional.of(testEmbeddingModel));
            when(chromaClient.collectionExists("doc-1-nomic-embed-text")).thenReturn(false);
            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);

            ragService.embedAndStore(1L, "nomic-embed-text", 20, 5);

            verify(documentService).findById(1L);
            verify(embeddingModelRepository).findByOllamaModelName("nomic-embed-text");
            verify(chromaClient).createCollection("doc-1-nomic-embed-text", 768);
            verify(ollamaClient, org.mockito.Mockito.atLeastOnce())
                    .embed(any(EmbeddingRequest.class));
            verify(chromaClient).addDocuments(eq("doc-1-nomic-embed-text"), any());
            // Content is 58 chars, chunkSize=20, overlap=5, step=15
            // Chunks: 0-20, 15-35, 30-50, 45-58 = 4 chunks
            assertEquals(4, testDocument.getChunkCount());
        }

        @Test
        @DisplayName("Should not create collection if it already exists")
        void shouldNotCreateCollectionIfItAlreadyExists() {
            when(documentService.findById(1L)).thenReturn(testDocument);
            when(embeddingModelRepository.findByOllamaModelName("nomic-embed-text"))
                    .thenReturn(Optional.of(testEmbeddingModel));
            when(chromaClient.collectionExists("doc-1-nomic-embed-text")).thenReturn(true);
            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);

            ragService.embedAndStore(1L, "nomic-embed-text", 20, 5);

            verify(chromaClient, never()).createCollection(any(), anyInt());
            verify(chromaClient).addDocuments(eq("doc-1-nomic-embed-text"), any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when document not found")
        void shouldThrowEntityNotFoundExceptionWhenDocumentNotFound() {
            when(documentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Document not found: 999"));

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> ragService.embedAndStore(999L, "nomic-embed-text", 20, 5));

            assertEquals("Document not found: 999", exception.getMessage());
            verify(chromaClient, never()).createCollection(any(), anyInt());
            verify(chromaClient, never()).addDocuments(any(), any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when embedding model not found")
        void shouldThrowEntityNotFoundExceptionWhenEmbeddingModelNotFound() {
            when(documentService.findById(1L)).thenReturn(testDocument);
            when(embeddingModelRepository.findByOllamaModelName("unknown-model"))
                    .thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> ragService.embedAndStore(1L, "unknown-model", 20, 5));

            assertEquals("Embedding model not found: unknown-model", exception.getMessage());
            verify(chromaClient, never()).createCollection(any(), anyInt());
            verify(chromaClient, never()).addDocuments(any(), any());
        }

        @Test
        @DisplayName("Should create correct ChromaDocuments with metadata")
        @SuppressWarnings("unchecked")
        void shouldCreateCorrectChromaDocumentsWithMetadata() {
            testDocument.setContent("Short text");
            when(documentService.findById(1L)).thenReturn(testDocument);
            when(embeddingModelRepository.findByOllamaModelName("nomic-embed-text"))
                    .thenReturn(Optional.of(testEmbeddingModel));
            when(chromaClient.collectionExists("doc-1-nomic-embed-text")).thenReturn(false);
            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);

            ragService.embedAndStore(1L, "nomic-embed-text", 100, 10);

            ArgumentCaptor<List<ChromaDocument>> captor = ArgumentCaptor.forClass(List.class);
            verify(chromaClient).addDocuments(eq("doc-1-nomic-embed-text"), captor.capture());

            List<ChromaDocument> docs = captor.getValue();
            assertEquals(1, docs.size());
            assertEquals("chunk-0", docs.get(0).getId());
            assertEquals("Short text", docs.get(0).getContent());
            assertEquals(1L, docs.get(0).getMetadata().get("documentId"));
            assertEquals(0, docs.get(0).getMetadata().get("chunkIndex"));
            assertEquals("nomic-embed-text", docs.get(0).getMetadata().get("embeddingModel"));
        }

        @Test
        @DisplayName("Should handle embedding model with colons in name")
        void shouldHandleEmbeddingModelWithColonsInName() {
            EmbeddingModel modelWithColons =
                    EmbeddingModel.builder()
                            .id(2L)
                            .name("Test Model")
                            .ollamaModelName("model:version")
                            .dimensions(768)
                            .build();

            testDocument.setContent("Test");
            when(documentService.findById(1L)).thenReturn(testDocument);
            when(embeddingModelRepository.findByOllamaModelName("model:version"))
                    .thenReturn(Optional.of(modelWithColons));
            when(chromaClient.collectionExists("doc-1-model-version")).thenReturn(false);
            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);

            ragService.embedAndStore(1L, "model:version", 100, 10);

            verify(chromaClient).createCollection("doc-1-model-version", 768);
            verify(chromaClient).addDocuments(eq("doc-1-model-version"), any());
        }
    }

    @Nested
    @DisplayName("query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should query and return retrieved chunks")
        void shouldQueryAndReturnRetrievedChunks() {
            ChromaQueryResult result1 =
                    ChromaQueryResult.builder()
                            .id("chunk-0")
                            .content("First chunk content")
                            .distance(0.15)
                            .metadata(Map.of("chunkIndex", 0, "documentId", 1L))
                            .build();

            ChromaQueryResult result2 =
                    ChromaQueryResult.builder()
                            .id("chunk-1")
                            .content("Second chunk content")
                            .distance(0.25)
                            .metadata(Map.of("chunkIndex", 1, "documentId", 1L))
                            .build();

            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);
            when(chromaClient.query(eq("doc-1-nomic-embed-text"), any(double[].class), eq(5)))
                    .thenReturn(Arrays.asList(result1, result2));

            List<RetrievedChunk> chunks =
                    ragService.query("doc-1-nomic-embed-text", "test query", "nomic-embed-text", 5);

            assertEquals(2, chunks.size());
            assertEquals("First chunk content", chunks.get(0).getContent());
            assertEquals(0.15, chunks.get(0).getDistance());
            assertEquals(0, chunks.get(0).getChunkIndex());
            assertEquals("Second chunk content", chunks.get(1).getContent());
            assertEquals(0.25, chunks.get(1).getDistance());
            assertEquals(1, chunks.get(1).getChunkIndex());

            ArgumentCaptor<EmbeddingRequest> requestCaptor =
                    ArgumentCaptor.forClass(EmbeddingRequest.class);
            verify(ollamaClient).embed(requestCaptor.capture());
            assertEquals("nomic-embed-text", requestCaptor.getValue().getModel());
            assertEquals("test query", requestCaptor.getValue().getInput());
        }

        @Test
        @DisplayName("Should return empty list when no results found")
        void shouldReturnEmptyListWhenNoResultsFound() {
            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);
            when(chromaClient.query(any(), any(double[].class), anyInt()))
                    .thenReturn(Collections.emptyList());

            List<RetrievedChunk> chunks =
                    ragService.query("doc-1-nomic-embed-text", "test query", "nomic-embed-text", 5);

            assertTrue(chunks.isEmpty());
        }

        @Test
        @DisplayName("Should handle missing chunk index in metadata")
        void shouldHandleMissingChunkIndexInMetadata() {
            ChromaQueryResult result =
                    ChromaQueryResult.builder()
                            .id("chunk-0")
                            .content("Content without chunk index")
                            .distance(0.15)
                            .metadata(Map.of("documentId", 1L))
                            .build();

            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);
            when(chromaClient.query(any(), any(double[].class), anyInt()))
                    .thenReturn(Collections.singletonList(result));

            List<RetrievedChunk> chunks =
                    ragService.query("doc-1-nomic-embed-text", "test query", "nomic-embed-text", 5);

            assertEquals(1, chunks.size());
            assertEquals("Content without chunk index", chunks.get(0).getContent());
            assertEquals(null, chunks.get(0).getChunkIndex());
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            ChromaQueryResult result =
                    ChromaQueryResult.builder()
                            .id("chunk-0")
                            .content("Content with null metadata")
                            .distance(0.15)
                            .metadata(null)
                            .build();

            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);
            when(chromaClient.query(any(), any(double[].class), anyInt()))
                    .thenReturn(Collections.singletonList(result));

            List<RetrievedChunk> chunks =
                    ragService.query("doc-1-nomic-embed-text", "test query", "nomic-embed-text", 5);

            assertEquals(1, chunks.size());
            assertEquals(null, chunks.get(0).getChunkIndex());
        }

        @Test
        @DisplayName("Should handle chunk index as Long in metadata")
        void shouldHandleChunkIndexAsLongInMetadata() {
            ChromaQueryResult result =
                    ChromaQueryResult.builder()
                            .id("chunk-0")
                            .content("Content")
                            .distance(0.15)
                            .metadata(Map.of("chunkIndex", 5L))
                            .build();

            when(ollamaClient.embed(any(EmbeddingRequest.class))).thenReturn(testEmbeddingResponse);
            when(chromaClient.query(any(), any(double[].class), anyInt()))
                    .thenReturn(Collections.singletonList(result));

            List<RetrievedChunk> chunks =
                    ragService.query("doc-1-nomic-embed-text", "test query", "nomic-embed-text", 5);

            assertEquals(1, chunks.size());
            assertEquals(5, chunks.get(0).getChunkIndex());
        }
    }

    @Nested
    @DisplayName("buildContext Tests")
    class BuildContextTests {

        @Test
        @DisplayName("Should build context from single chunk")
        void shouldBuildContextFromSingleChunk() {
            RetrievedChunk chunk =
                    RetrievedChunk.builder()
                            .content("This is the first chunk.")
                            .distance(0.15)
                            .chunkIndex(0)
                            .build();

            String context = ragService.buildContext(Collections.singletonList(chunk));

            assertNotNull(context);
            assertTrue(context.startsWith("Context:\n\n"));
            assertTrue(context.contains("[1] This is the first chunk."));
        }

        @Test
        @DisplayName("Should build context from multiple chunks")
        void shouldBuildContextFromMultipleChunks() {
            RetrievedChunk chunk1 =
                    RetrievedChunk.builder()
                            .content("First chunk content.")
                            .distance(0.15)
                            .chunkIndex(0)
                            .build();

            RetrievedChunk chunk2 =
                    RetrievedChunk.builder()
                            .content("Second chunk content.")
                            .distance(0.25)
                            .chunkIndex(1)
                            .build();

            RetrievedChunk chunk3 =
                    RetrievedChunk.builder()
                            .content("Third chunk content.")
                            .distance(0.35)
                            .chunkIndex(2)
                            .build();

            String context = ragService.buildContext(Arrays.asList(chunk1, chunk2, chunk3));

            assertNotNull(context);
            assertTrue(context.startsWith("Context:\n\n"));
            assertTrue(context.contains("[1] First chunk content."));
            assertTrue(context.contains("[2] Second chunk content."));
            assertTrue(context.contains("[3] Third chunk content."));
        }

        @Test
        @DisplayName("Should return empty string for null chunks")
        void shouldReturnEmptyStringForNullChunks() {
            String context = ragService.buildContext(null);

            assertEquals("", context);
        }

        @Test
        @DisplayName("Should return empty string for empty chunks list")
        void shouldReturnEmptyStringForEmptyChunksList() {
            String context = ragService.buildContext(Collections.emptyList());

            assertEquals("", context);
        }

        @Test
        @DisplayName("Should number chunks sequentially starting from 1")
        void shouldNumberChunksSequentiallyStartingFromOne() {
            RetrievedChunk chunk1 = RetrievedChunk.builder().content("A").build();
            RetrievedChunk chunk2 = RetrievedChunk.builder().content("B").build();
            RetrievedChunk chunk3 = RetrievedChunk.builder().content("C").build();
            RetrievedChunk chunk4 = RetrievedChunk.builder().content("D").build();
            RetrievedChunk chunk5 = RetrievedChunk.builder().content("E").build();

            String context =
                    ragService.buildContext(Arrays.asList(chunk1, chunk2, chunk3, chunk4, chunk5));

            assertTrue(context.contains("[1] A"));
            assertTrue(context.contains("[2] B"));
            assertTrue(context.contains("[3] C"));
            assertTrue(context.contains("[4] D"));
            assertTrue(context.contains("[5] E"));
        }
    }

    @Nested
    @DisplayName("buildCollectionName Tests")
    class BuildCollectionNameTests {

        @Test
        @DisplayName("Should build collection name without colons")
        void shouldBuildCollectionNameWithoutColons() {
            String collectionName = ragService.buildCollectionName(1L, "nomic-embed-text");

            assertEquals("doc-1-nomic-embed-text", collectionName);
        }

        @Test
        @DisplayName("Should replace colons with hyphens in embedding model name")
        void shouldReplaceColonsWithHyphensInEmbeddingModelName() {
            String collectionName = ragService.buildCollectionName(1L, "model:version:tag");

            assertEquals("doc-1-model-version-tag", collectionName);
        }

        @Test
        @DisplayName("Should handle different document IDs")
        void shouldHandleDifferentDocumentIds() {
            String collectionName1 = ragService.buildCollectionName(1L, "model");
            String collectionName2 = ragService.buildCollectionName(100L, "model");
            String collectionName3 = ragService.buildCollectionName(999999L, "model");

            assertEquals("doc-1-model", collectionName1);
            assertEquals("doc-100-model", collectionName2);
            assertEquals("doc-999999-model", collectionName3);
        }
    }
}
