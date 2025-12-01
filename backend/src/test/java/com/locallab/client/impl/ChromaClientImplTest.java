package com.locallab.client.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.locallab.config.ChromaConfig;
import com.locallab.dto.ChromaDocument;
import com.locallab.dto.ChromaQueryResult;
import com.locallab.exception.LocalLabException;

/**
 * Unit tests for {@link ChromaClientImpl}.
 *
 * <p>These tests verify the behaviour of the Chroma client implementation, including successful
 * operations, error handling, and HTTP error mapping. The tests use mocked RestTemplate
 * dependencies.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
class ChromaClientImplTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String COLLECTIONS_URL = BASE_URL + "/api/v1/collections";

    @Mock private RestTemplate restTemplate;

    private ChromaConfig config;
    private ChromaClientImpl chromaClient;

    @BeforeEach
    void setUp() {
        config = createDefaultConfig();
        chromaClient = new ChromaClientImpl(config, restTemplate);
    }

    private ChromaConfig createDefaultConfig() {
        ChromaConfig config = new ChromaConfig();
        config.setBaseUrl(BASE_URL);
        config.setTimeoutSeconds(30);
        return config;
    }

    @Nested
    @DisplayName("createCollection")
    class CreateCollectionTests {

        @Test
        @DisplayName("should create collection successfully")
        void shouldCreateCollectionSuccessfully() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

            // Act & Assert
            assertDoesNotThrow(() -> chromaClient.createCollection(collectionName, 768));
            verify(restTemplate)
                    .postForEntity(eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class));
        }

        @Test
        @DisplayName("should throw CONFLICT when collection already exists")
        void shouldThrowConflictWhenCollectionExists() {
            // Arrange
            String collectionName = "existing-collection";
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.createCollection(collectionName, 768));

            assertEquals(HttpStatus.CONFLICT, exception.getStatus());
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("should throw CONFLICT when BAD_REQUEST with 'already exists' message")
        void shouldThrowConflictOnBadRequestWithAlreadyExistsMessage() {
            // Arrange
            String collectionName = "existing-collection";
            HttpClientErrorException exception =
                    HttpClientErrorException.create(
                            HttpStatus.BAD_REQUEST,
                            "Bad Request",
                            null,
                            "Collection already exists".getBytes(),
                            null);
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(exception);

            // Act & Assert
            LocalLabException thrown =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.createCollection(collectionName, 768));

            assertEquals(HttpStatus.CONFLICT, thrown.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on server error")
        void shouldThrowServiceUnavailableOnServerError() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.createCollection(collectionName, 768));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on connection failure")
        void shouldThrowServiceUnavailableOnConnectionFailure() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.createCollection(collectionName, 768));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
            assertTrue(exception.getMessage().contains("Cannot connect to Chroma"));
        }

        @Test
        @DisplayName("should propagate other client errors with original status")
        void shouldPropagateOtherClientErrors() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL), any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(
                            new HttpClientErrorException(
                                    HttpStatus.BAD_REQUEST, "Invalid request"));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.createCollection(collectionName, 768));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("deleteCollection")
    class DeleteCollectionTests {

        @Test
        @DisplayName("should delete collection successfully")
        void shouldDeleteCollectionSuccessfully() {
            // Arrange
            String collectionName = "test-collection";
            doNothing().when(restTemplate).delete(COLLECTIONS_URL + "/" + collectionName);

            // Act & Assert
            assertDoesNotThrow(() -> chromaClient.deleteCollection(collectionName));
            verify(restTemplate).delete(COLLECTIONS_URL + "/" + collectionName);
        }

        @Test
        @DisplayName("should not throw when collection does not exist (idempotent)")
        void shouldNotThrowWhenCollectionNotExists() {
            // Arrange
            String collectionName = "non-existent";
            doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                    .when(restTemplate)
                    .delete(COLLECTIONS_URL + "/" + collectionName);

            // Act & Assert
            assertDoesNotThrow(() -> chromaClient.deleteCollection(collectionName));
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on server error")
        void shouldThrowServiceUnavailableOnServerError() {
            // Arrange
            String collectionName = "test-collection";
            doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                    .when(restTemplate)
                    .delete(COLLECTIONS_URL + "/" + collectionName);

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.deleteCollection(collectionName));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on connection failure")
        void shouldThrowServiceUnavailableOnConnectionFailure() {
            // Arrange
            String collectionName = "test-collection";
            doThrow(new ResourceAccessException("Connection refused"))
                    .when(restTemplate)
                    .delete(COLLECTIONS_URL + "/" + collectionName);

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.deleteCollection(collectionName));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should propagate other client errors with original status")
        void shouldPropagateOtherClientErrors() {
            // Arrange
            String collectionName = "test-collection";
            doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                    .when(restTemplate)
                    .delete(COLLECTIONS_URL + "/" + collectionName);

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.deleteCollection(collectionName));

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("collectionExists")
    class CollectionExistsTests {

        @Test
        @DisplayName("should return true when collection exists")
        void shouldReturnTrueWhenCollectionExists() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

            // Act
            boolean exists = chromaClient.collectionExists(collectionName);

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("should return false when collection does not exist")
        void shouldReturnFalseWhenCollectionNotExists() {
            // Arrange
            String collectionName = "non-existent";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            // Act
            boolean exists = chromaClient.collectionExists(collectionName);

            // Assert
            assertFalse(exists);
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on server error")
        void shouldThrowServiceUnavailableOnServerError() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.collectionExists(collectionName));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on connection failure")
        void shouldThrowServiceUnavailableOnConnectionFailure() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.collectionExists(collectionName));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should propagate other client errors with original status")
        void shouldPropagateOtherClientErrors() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.collectionExists(collectionName));

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("addDocuments")
    class AddDocumentsTests {

        private static final String COLLECTION_NAME = "test-collection";
        private static final String COLLECTION_ID = "collection-uuid-123";

        @Test
        @DisplayName("should add documents successfully")
        void shouldAddDocumentsSuccessfully() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);
            collectionResponse.put("name", COLLECTION_NAME);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/add"),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

            List<ChromaDocument> documents = createTestDocuments();

            // Act & Assert
            assertDoesNotThrow(() -> chromaClient.addDocuments(COLLECTION_NAME, documents));
        }

        @Test
        @DisplayName("should throw NOT_FOUND when collection does not exist")
        void shouldThrowNotFoundWhenCollectionNotExists() {
            // Arrange
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            List<ChromaDocument> documents = createTestDocuments();

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(COLLECTION_NAME, documents));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertTrue(exception.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on server error during add")
        void shouldThrowServiceUnavailableOnServerError() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/add"),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            List<ChromaDocument> documents = createTestDocuments();

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(COLLECTION_NAME, documents));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on connection failure during add")
        void shouldThrowServiceUnavailableOnConnectionFailure() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/add"),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            List<ChromaDocument> documents = createTestDocuments();

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(COLLECTION_NAME, documents));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_FOUND when add returns 404")
        void shouldThrowNotFoundWhenAddReturns404() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/add"),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            List<ChromaDocument> documents = createTestDocuments();

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(COLLECTION_NAME, documents));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("should handle documents with null metadata")
        void shouldHandleDocumentsWithNullMetadata() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.postForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/add"),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(new HashMap<>(), HttpStatus.OK));

            ChromaDocument doc =
                    ChromaDocument.builder()
                            .id("chunk-0")
                            .content("Test content")
                            .embedding(new double[] {0.1, 0.2, 0.3})
                            .metadata(null)
                            .build();

            // Act & Assert
            assertDoesNotThrow(() -> chromaClient.addDocuments(COLLECTION_NAME, List.of(doc)));
        }

        private List<ChromaDocument> createTestDocuments() {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("documentId", 1L);
            metadata.put("chunkIndex", 0);

            return List.of(
                    ChromaDocument.builder()
                            .id("chunk-0")
                            .content("Test content 0")
                            .embedding(new double[] {0.1, 0.2, 0.3})
                            .metadata(metadata)
                            .build(),
                    ChromaDocument.builder()
                            .id("chunk-1")
                            .content("Test content 1")
                            .embedding(new double[] {0.4, 0.5, 0.6})
                            .metadata(metadata)
                            .build());
        }
    }

    @Nested
    @DisplayName("query")
    class QueryTests {

        private static final String COLLECTION_NAME = "test-collection";
        private static final String COLLECTION_ID = "collection-uuid-123";

        @Test
        @DisplayName("should query and return results successfully")
        void shouldQueryAndReturnResultsSuccessfully() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            Map<String, Object> queryResponse = createQueryResponse();
            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(queryResponse, HttpStatus.OK));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act
            List<ChromaQueryResult> results = chromaClient.query(COLLECTION_NAME, embedding, 5);

            // Assert
            assertEquals(2, results.size());
            assertEquals("chunk-0", results.get(0).getId());
            assertEquals("Test content 0", results.get(0).getContent());
            assertEquals(0.15, results.get(0).getDistance());
        }

        @Test
        @DisplayName("should return empty list when no results")
        void shouldReturnEmptyListWhenNoResults() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("ids", List.of(List.of()));
            emptyResponse.put("documents", List.of(List.of()));
            emptyResponse.put("distances", List.of(List.of()));
            emptyResponse.put("metadatas", List.of(List.of()));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(emptyResponse, HttpStatus.OK));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act
            List<ChromaQueryResult> results = chromaClient.query(COLLECTION_NAME, embedding, 5);

            // Assert
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return empty list when response body is null")
        void shouldReturnEmptyListWhenResponseBodyIsNull() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act
            List<ChromaQueryResult> results = chromaClient.query(COLLECTION_NAME, embedding, 5);

            // Assert
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should throw NOT_FOUND when collection does not exist")
        void shouldThrowNotFoundWhenCollectionNotExists() {
            // Arrange
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.query(COLLECTION_NAME, embedding, 5));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on server error during query")
        void shouldThrowServiceUnavailableOnServerError() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.query(COLLECTION_NAME, embedding, 5));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE on connection failure during query")
        void shouldThrowServiceUnavailableOnConnectionFailure() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.query(COLLECTION_NAME, embedding, 5));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw NOT_FOUND when query returns 404")
        void shouldThrowNotFoundWhenQueryReturns404() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.query(COLLECTION_NAME, embedding, 5));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        }

        @Test
        @DisplayName("should handle partial response data gracefully")
        void shouldHandlePartialResponseDataGracefully() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            // Response with ids but missing other fields
            Map<String, Object> partialResponse = new HashMap<>();
            partialResponse.put("ids", List.of(Arrays.asList("chunk-0", "chunk-1")));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(partialResponse, HttpStatus.OK));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act
            List<ChromaQueryResult> results = chromaClient.query(COLLECTION_NAME, embedding, 5);

            // Assert
            assertEquals(2, results.size());
            assertEquals("chunk-0", results.get(0).getId());
            assertNull(results.get(0).getContent());
            assertNull(results.get(0).getDistance());
        }

        @Test
        @DisplayName("should handle response with null ids")
        void shouldHandleResponseWithNullIds() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            Map<String, Object> responseWithNullIds = new HashMap<>();
            responseWithNullIds.put("ids", null);

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(responseWithNullIds, HttpStatus.OK));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act
            List<ChromaQueryResult> results = chromaClient.query(COLLECTION_NAME, embedding, 5);

            // Assert
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should propagate other client errors with original status")
        void shouldPropagateOtherClientErrors() {
            // Arrange
            Map<String, Object> collectionResponse = new HashMap<>();
            collectionResponse.put("id", COLLECTION_ID);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_NAME), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(collectionResponse, HttpStatus.OK));

            when(restTemplate.exchange(
                            eq(COLLECTIONS_URL + "/" + COLLECTION_ID + "/query"),
                            eq(HttpMethod.POST),
                            any(HttpEntity.class),
                            eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

            double[] embedding = new double[] {0.1, 0.2, 0.3};

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.query(COLLECTION_NAME, embedding, 5));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        }

        private Map<String, Object> createQueryResponse() {
            Map<String, Object> metadata0 = new HashMap<>();
            metadata0.put("documentId", 1L);
            metadata0.put("chunkIndex", 0);

            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("documentId", 1L);
            metadata1.put("chunkIndex", 1);

            Map<String, Object> response = new HashMap<>();
            response.put("ids", List.of(Arrays.asList("chunk-0", "chunk-1")));
            response.put("documents", List.of(Arrays.asList("Test content 0", "Test content 1")));
            response.put("distances", List.of(Arrays.asList(0.15, 0.25)));
            response.put("metadatas", List.of(Arrays.asList(metadata0, metadata1)));

            return response;
        }
    }

    @Nested
    @DisplayName("getCollectionId")
    class GetCollectionIdTests {

        @Test
        @DisplayName("should throw when collection response missing ID field")
        void shouldThrowWhenCollectionResponseMissingIdField() {
            // Arrange
            String collectionName = "test-collection";
            Map<String, Object> responseWithoutId = new HashMap<>();
            responseWithoutId.put("name", collectionName);

            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenReturn(new ResponseEntity<>(responseWithoutId, HttpStatus.OK));

            List<ChromaDocument> documents =
                    List.of(
                            ChromaDocument.builder()
                                    .id("chunk-0")
                                    .content("Test")
                                    .embedding(new double[] {0.1})
                                    .build());

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(collectionName, documents));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
            assertTrue(exception.getMessage().contains("missing ID field"));
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE when getCollectionId connection fails")
        void shouldThrowServiceUnavailableWhenGetCollectionIdConnectionFails() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            List<ChromaDocument> documents =
                    List.of(
                            ChromaDocument.builder()
                                    .id("chunk-0")
                                    .content("Test")
                                    .embedding(new double[] {0.1})
                                    .build());

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(collectionName, documents));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should throw SERVICE_UNAVAILABLE when getCollectionId server error")
        void shouldThrowServiceUnavailableWhenGetCollectionIdServerError() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            List<ChromaDocument> documents =
                    List.of(
                            ChromaDocument.builder()
                                    .id("chunk-0")
                                    .content("Test")
                                    .embedding(new double[] {0.1})
                                    .build());

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(collectionName, documents));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        }

        @Test
        @DisplayName("should propagate client errors from getCollectionId")
        void shouldPropagateClientErrorsFromGetCollectionId() {
            // Arrange
            String collectionName = "test-collection";
            when(restTemplate.getForEntity(
                            eq(COLLECTIONS_URL + "/" + collectionName), eq(Map.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

            List<ChromaDocument> documents =
                    List.of(
                            ChromaDocument.builder()
                                    .id("chunk-0")
                                    .content("Test")
                                    .embedding(new double[] {0.1})
                                    .build());

            // Act & Assert
            LocalLabException exception =
                    assertThrows(
                            LocalLabException.class,
                            () -> chromaClient.addDocuments(collectionName, documents));

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("should use configuration values from ChromaConfig")
        void shouldUseConfigurationValues() {
            // Arrange
            ChromaConfig customConfig = new ChromaConfig();
            customConfig.setBaseUrl("http://custom:8000");
            customConfig.setTimeoutSeconds(60);

            // Act - create new instance with custom config
            ChromaClientImpl customClient = new ChromaClientImpl(customConfig, restTemplate);

            // Assert - client was created without exception
            assertNotNull(customClient);
        }

        @Test
        @DisplayName("should use default configuration values")
        void shouldUseDefaultConfigurationValues() {
            // Arrange
            ChromaConfig defaultConfig = new ChromaConfig();

            // Assert
            assertEquals("http://localhost:8000", defaultConfig.getBaseUrl());
            assertEquals(30, defaultConfig.getTimeoutSeconds());
        }
    }
}
