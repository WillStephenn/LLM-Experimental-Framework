package com.locallab.client.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.locallab.client.ChromaClient;
import com.locallab.config.ChromaConfig;
import com.locallab.dto.ChromaDocument;
import com.locallab.dto.ChromaQueryResult;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of {@link ChromaClient} using Spring's RestTemplate.
 *
 * <p>This class provides the concrete implementation for interacting with a Chroma vector store
 * instance via its REST API. It handles connection management, request serialisation, response
 * parsing, and error handling for all collection and document operations.
 *
 * <p>All API interactions are logged at DEBUG level for troubleshooting, with INFO level logs for
 * collection operations and ERROR level for failures.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Autowired
 * private ChromaClient chromaClient;
 *
 * // Check if collection exists
 * if (!chromaClient.collectionExists("doc-1-nomic-embed-text")) {
 *     chromaClient.createCollection("doc-1-nomic-embed-text", 768);
 * }
 *
 * // Add documents
 * chromaClient.addDocuments("doc-1-nomic-embed-text", documents);
 *
 * // Query for similar documents
 * List<ChromaQueryResult> results = chromaClient.query("doc-1-nomic-embed-text", embedding, 5);
 * }</pre>
 *
 * @author William Stephen
 * @see ChromaClient
 * @see ChromaConfig
 */
@Component
@EnableConfigurationProperties(ChromaConfig.class)
public class ChromaClientImpl implements ChromaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChromaClientImpl.class);

    private static final String API_V1_COLLECTIONS = "/api/v1/collections";

    private final RestTemplate restTemplate;
    private final ChromaConfig config;

    /**
     * Constructs a new ChromaClientImpl with the specified configuration.
     *
     * @param config the Chroma configuration containing connection settings
     * @param restTemplateBuilder the builder for creating RestTemplate instances
     */
    @Autowired
    public ChromaClientImpl(ChromaConfig config, RestTemplateBuilder restTemplateBuilder) {
        this.config = config;
        this.restTemplate =
                restTemplateBuilder
                        .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .readTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .build();
        LOGGER.info(
                "ChromaClient initialised with base URL: {}, timeout: {}s",
                config.getBaseUrl(),
                config.getTimeoutSeconds());
    }

    /**
     * Constructor for testing with a custom RestTemplate instance.
     *
     * @param config the Chroma configuration
     * @param restTemplate the RestTemplate instance to use
     */
    ChromaClientImpl(ChromaConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    @Override
    public void createCollection(String name, int dimensions) {
        LOGGER.debug("Creating collection: {} with dimensions: {}", name, dimensions);

        String url = config.getBaseUrl() + API_V1_COLLECTIONS;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dimensions", dimensions);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", name);
        requestBody.put("metadata", metadata);

        HttpHeaders headers = createJsonHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForEntity(url, request, Map.class);
            LOGGER.info("Created collection: {}", name);
        } catch (HttpClientErrorException e) {
            handleCreateCollectionError(e, name);
        } catch (HttpServerErrorException e) {
            LOGGER.error(
                    "Chroma server error while creating collection {}: {}", name, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chroma service error while creating collection: " + e.getMessage(),
                    e);
        } catch (ResourceAccessException e) {
            LOGGER.error(
                    "Cannot connect to Chroma while creating collection {}: {}",
                    name,
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void deleteCollection(String name) {
        LOGGER.debug("Deleting collection: {}", name);

        String url = config.getBaseUrl() + API_V1_COLLECTIONS + "/" + name;

        try {
            restTemplate.delete(url);
            LOGGER.info("Deleted collection: {}", name);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.debug("Collection {} does not exist, nothing to delete", name);
                // Idempotent - collection doesn't exist, which is the desired state
                return;
            }
            LOGGER.error("Error deleting collection {}: {}", name, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Failed to delete collection: " + e.getMessage(),
                    e);
        } catch (HttpServerErrorException e) {
            LOGGER.error(
                    "Chroma server error while deleting collection {}: {}", name, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chroma service error while deleting collection: " + e.getMessage(),
                    e);
        } catch (ResourceAccessException e) {
            LOGGER.error(
                    "Cannot connect to Chroma while deleting collection {}: {}",
                    name,
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public boolean collectionExists(String name) {
        LOGGER.debug("Checking if collection exists: {}", name);

        String url = config.getBaseUrl() + API_V1_COLLECTIONS + "/" + name;

        try {
            restTemplate.getForEntity(url, Map.class);
            LOGGER.debug("Collection {} exists", name);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.debug("Collection {} does not exist", name);
                return false;
            }
            LOGGER.error("Error checking collection existence {}: {}", name, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Failed to check collection existence: " + e.getMessage(),
                    e);
        } catch (HttpServerErrorException e) {
            LOGGER.error(
                    "Chroma server error while checking collection {}: {}", name, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Chroma service error: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            LOGGER.error(
                    "Cannot connect to Chroma while checking collection {}: {}",
                    name,
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    @Override
    public void addDocuments(String collection, List<ChromaDocument> documents) {
        LOGGER.debug("Adding {} documents to collection: {}", documents.size(), collection);

        String collectionId = getCollectionId(collection);
        String url = config.getBaseUrl() + API_V1_COLLECTIONS + "/" + collectionId + "/add";

        Map<String, Object> requestBody = buildAddDocumentsRequest(documents);
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.postForEntity(url, request, Map.class);
            LOGGER.debug("Added {} documents to collection: {}", documents.size(), collection);
        } catch (HttpClientErrorException e) {
            handleAddDocumentsError(e, collection);
        } catch (HttpServerErrorException e) {
            LOGGER.error(
                    "Chroma server error while adding documents to {}: {}",
                    collection,
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chroma service error while adding documents: " + e.getMessage(),
                    e);
        } catch (ResourceAccessException e) {
            LOGGER.error(
                    "Cannot connect to Chroma while adding documents to {}: {}",
                    collection,
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ChromaQueryResult> query(String collection, double[] embedding, int topK) {
        LOGGER.debug("Querying collection {} with topK: {}", collection, topK);

        String collectionId = getCollectionId(collection);
        String url = config.getBaseUrl() + API_V1_COLLECTIONS + "/" + collectionId + "/query";

        Map<String, Object> requestBody = buildQueryRequest(embedding, topK);
        HttpHeaders headers = createJsonHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            List<ChromaQueryResult> results = parseQueryResponse(response.getBody());
            LOGGER.debug(
                    "Query returned {} results from collection {}", results.size(), collection);
            return results;
        } catch (HttpClientErrorException e) {
            handleQueryError(e, collection);
            return new ArrayList<>(); // Never reached, handleQueryError always throws
        } catch (HttpServerErrorException e) {
            LOGGER.error("Chroma server error while querying {}: {}", collection, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chroma service error while querying: " + e.getMessage(),
                    e);
        } catch (ResourceAccessException e) {
            LOGGER.error(
                    "Cannot connect to Chroma while querying {}: {}", collection, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Retrieves the collection ID by name from Chroma.
     *
     * <p>Chroma's API requires the collection ID (UUID) for document operations, not the name. This
     * method fetches the collection details and extracts the ID.
     *
     * @param name the collection name
     * @return the collection UUID
     * @throws EntityNotFoundException if the collection is not found
     * @throws ResponseStatusException if the service is unavailable
     */
    @SuppressWarnings("unchecked")
    private String getCollectionId(String name) {
        String url = config.getBaseUrl() + API_V1_COLLECTIONS + "/" + name;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("id")) {
                return body.get("id").toString();
            }
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Collection response missing ID field for: " + name);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn("Collection not found: {}", name);
                throw new EntityNotFoundException("Collection not found: " + name);
            }
            throw new ResponseStatusException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    "Failed to get collection: " + e.getMessage(),
                    e);
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Chroma service error: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Cannot connect to Chroma: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Creates HTTP headers for JSON requests.
     *
     * @return headers configured for JSON content type
     */
    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Builds the request body for adding documents to Chroma.
     *
     * @param documents the documents to add
     * @return the request body as a map
     */
    private Map<String, Object> buildAddDocumentsRequest(List<ChromaDocument> documents) {
        List<String> ids = new ArrayList<>();
        List<double[]> embeddings = new ArrayList<>();
        List<String> documentsContent = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();

        for (ChromaDocument doc : documents) {
            ids.add(doc.getId());
            embeddings.add(doc.getEmbedding());
            documentsContent.add(doc.getContent());
            metadatas.add(doc.getMetadata() != null ? doc.getMetadata() : new HashMap<>());
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ids", ids);
        requestBody.put("embeddings", embeddings);
        requestBody.put("documents", documentsContent);
        requestBody.put("metadatas", metadatas);

        return requestBody;
    }

    /**
     * Builds the request body for querying Chroma.
     *
     * @param embedding the query embedding vector
     * @param topK the number of results to return
     * @return the request body as a map
     */
    private Map<String, Object> buildQueryRequest(double[] embedding, int topK) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query_embeddings", new double[][] {embedding});
        requestBody.put("n_results", topK);
        requestBody.put("include", List.of("documents", "distances", "metadatas"));
        return requestBody;
    }

    /**
     * Parses the query response from Chroma into ChromaQueryResult objects.
     *
     * @param responseBody the response body from Chroma
     * @return a list of query results
     */
    @SuppressWarnings("unchecked")
    private List<ChromaQueryResult> parseQueryResponse(Map<String, Object> responseBody) {
        List<ChromaQueryResult> results = new ArrayList<>();

        if (responseBody == null) {
            return results;
        }

        List<List<String>> ids = (List<List<String>>) responseBody.get("ids");
        List<List<String>> documents = (List<List<String>>) responseBody.get("documents");
        List<List<Double>> distances = (List<List<Double>>) responseBody.get("distances");
        List<List<Map<String, Object>>> metadatas =
                (List<List<Map<String, Object>>>) responseBody.get("metadatas");

        if (ids == null || ids.isEmpty() || ids.get(0) == null) {
            return results;
        }

        List<String> resultIds = ids.get(0);
        List<String> resultDocuments =
                documents != null && !documents.isEmpty() ? documents.get(0) : null;
        List<Double> resultDistances =
                distances != null && !distances.isEmpty() ? distances.get(0) : null;
        List<Map<String, Object>> resultMetadatas =
                metadatas != null && !metadatas.isEmpty() ? metadatas.get(0) : null;

        for (int i = 0; i < resultIds.size(); i++) {
            ChromaQueryResult result =
                    ChromaQueryResult.builder()
                            .id(resultIds.get(i))
                            .content(
                                    resultDocuments != null && i < resultDocuments.size()
                                            ? resultDocuments.get(i)
                                            : null)
                            .distance(
                                    resultDistances != null && i < resultDistances.size()
                                            ? resultDistances.get(i)
                                            : null)
                            .metadata(
                                    resultMetadatas != null && i < resultMetadatas.size()
                                            ? resultMetadatas.get(i)
                                            : null)
                            .build();
            results.add(result);
        }

        return results;
    }

    /**
     * Handles errors during collection creation.
     *
     * @param e the HTTP client error exception
     * @param name the collection name
     * @throws IllegalStateException if collection already exists
     * @throws ResponseStatusException for other client errors
     */
    private void handleCreateCollectionError(HttpClientErrorException e, String name) {
        boolean isConflict = e.getStatusCode() == HttpStatus.CONFLICT;
        boolean isBadRequestWithAlreadyExists =
                e.getStatusCode() == HttpStatus.BAD_REQUEST
                        && e.getResponseBodyAsString().toLowerCase().contains("already exists");

        if (isConflict || isBadRequestWithAlreadyExists) {
            LOGGER.warn("Collection already exists: {}", name);
            throw new IllegalStateException("Collection already exists: " + name);
        }
        LOGGER.error("Error creating collection {}: {}", name, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()),
                "Failed to create collection: " + e.getMessage(),
                e);
    }

    /**
     * Handles errors during document addition.
     *
     * @param e the HTTP client error exception
     * @param collection the collection name
     * @throws EntityNotFoundException if collection not found
     * @throws ResponseStatusException for other client errors
     */
    private void handleAddDocumentsError(HttpClientErrorException e, String collection) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            LOGGER.warn("Collection not found while adding documents: {}", collection);
            throw new EntityNotFoundException("Collection not found: " + collection);
        }
        LOGGER.error("Error adding documents to collection {}: {}", collection, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()),
                "Failed to add documents: " + e.getMessage(),
                e);
    }

    /**
     * Handles errors during query operation.
     *
     * @param e the HTTP client error exception
     * @param collection the collection name
     * @throws EntityNotFoundException if collection not found
     * @throws ResponseStatusException for other client errors
     */
    private void handleQueryError(HttpClientErrorException e, String collection) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            LOGGER.warn("Collection not found while querying: {}", collection);
            throw new EntityNotFoundException("Collection not found: " + collection);
        }
        LOGGER.error("Error querying collection {}: {}", collection, e.getMessage());
        throw new ResponseStatusException(
                HttpStatus.valueOf(e.getStatusCode().value()),
                "Failed to query collection: " + e.getMessage(),
                e);
    }
}
