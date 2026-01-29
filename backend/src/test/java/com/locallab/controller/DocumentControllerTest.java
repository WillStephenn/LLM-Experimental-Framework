package com.locallab.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.config.CorsProperties;
import com.locallab.dto.RetrievedChunk;
import com.locallab.dto.request.EmbeddingModelRequest;
import com.locallab.dto.request.RagQueryRequest;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.Document;
import com.locallab.model.EmbeddingModel;
import com.locallab.service.DocumentService;
import com.locallab.service.EmbeddingModelService;
import com.locallab.service.RagService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link DocumentController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The service
 * dependencies are mocked to verify controller behaviour and request/response handling.
 *
 * <p>The test imports {@link GlobalExceptionHandler} to ensure proper error response formatting.
 * Configuration properties are enabled for {@link com.locallab.config.CorsProperties}.
 *
 * @see DocumentController
 * @see DocumentService
 * @see RagService
 * @see EmbeddingModelService
 */
@WebMvcTest(controllers = DocumentController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(
        properties = {
            "cors.allowed-origins=http://localhost:5173",
            "cors.allowed-methods=GET,POST,PUT,DELETE",
            "cors.allowed-headers=*",
            "cors.allow-credentials=true",
            "cors.max-age=3600"
        })
@DisplayName("DocumentController")
class DocumentControllerTest {

    private static final String DOCUMENTS_URL = "/api/documents";
    private static final String EMBEDDING_MODELS_URL = "/api/embedding-models";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private DocumentService documentService;

    @MockBean private RagService ragService;

    @MockBean private EmbeddingModelService embeddingModelService;

    private Document testDocument;
    private EmbeddingModel testEmbeddingModel;

    @BeforeEach
    void setUp() {
        testDocument =
                Document.builder()
                        .id(1L)
                        .filename("architecture.pdf")
                        .content("Sample document content for testing")
                        .chunkCount(45)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        testEmbeddingModel =
                EmbeddingModel.builder()
                        .id(1L)
                        .name("Nomic Embed Text")
                        .ollamaModelName("nomic-embed-text")
                        .dimensions(768)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();
    }

    @Nested
    @DisplayName("POST /api/documents")
    class UploadDocumentTests {

        @Test
        @DisplayName("Should upload TXT file successfully")
        void shouldUploadTxtFileSuccessfully() throws Exception {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "test.txt",
                            MediaType.TEXT_PLAIN_VALUE,
                            "Sample text content".getBytes());

            Document uploadedDocument =
                    Document.builder()
                            .id(2L)
                            .filename("test.txt")
                            .content("Sample text content")
                            .chunkCount(0)
                            .createdAt(LocalDateTime.now())
                            .build();

            when(documentService.upload(any())).thenReturn(uploadedDocument);

            mockMvc.perform(multipart(DOCUMENTS_URL).file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.filename", is("test.txt")))
                    .andExpect(jsonPath("$.chunkCount", is(0)))
                    .andExpect(jsonPath("$.createdAt").exists());

            verify(documentService).upload(any());
        }

        @Test
        @DisplayName("Should return 400 for unsupported file type")
        void shouldReturn400ForUnsupportedFileType() throws Exception {
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file", "test.doc", "application/msword", "Sample content".getBytes());

            when(documentService.upload(any()))
                    .thenThrow(
                            new IllegalArgumentException(
                                    "Unsupported file type: doc. Supported types: txt, pdf"));

            mockMvc.perform(multipart(DOCUMENTS_URL).file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(
                            jsonPath("$.message")
                                    .value(
                                            "Unsupported file type: doc. Supported types: txt,"
                                                    + " pdf"));

            verify(documentService).upload(any());
        }
    }

    @Nested
    @DisplayName("GET /api/documents")
    class GetAllDocumentsTests {

        @Test
        @DisplayName("Should return all documents")
        void shouldReturnAllDocuments() throws Exception {
            Document secondDocument =
                    Document.builder()
                            .id(2L)
                            .filename("readme.txt")
                            .content("Another document")
                            .chunkCount(10)
                            .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                            .build();

            when(documentService.findAll()).thenReturn(Arrays.asList(testDocument, secondDocument));

            mockMvc.perform(get(DOCUMENTS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].filename", is("architecture.pdf")))
                    .andExpect(jsonPath("$[0].chunkCount", is(45)))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].filename", is("readme.txt")));

            verify(documentService).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() throws Exception {
            when(documentService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(DOCUMENTS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(documentService).findAll();
        }

        @Test
        @DisplayName("Should not include content field in response")
        void shouldNotIncludeContentFieldInResponse() throws Exception {
            when(documentService.findAll()).thenReturn(Collections.singletonList(testDocument));

            mockMvc.perform(get(DOCUMENTS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].content").doesNotExist());

            verify(documentService).findAll();
        }
    }

    @Nested
    @DisplayName("GET /api/documents/{id}")
    class GetDocumentByIdTests {

        @Test
        @DisplayName("Should return document when found")
        void shouldReturnDocumentWhenFound() throws Exception {
            when(documentService.findById(1L)).thenReturn(testDocument);

            mockMvc.perform(get(DOCUMENTS_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.filename", is("architecture.pdf")))
                    .andExpect(jsonPath("$.chunkCount", is(45)));

            verify(documentService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenDocumentNotFound() throws Exception {
            when(documentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Document not found: 999"));

            mockMvc.perform(get(DOCUMENTS_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Document not found: 999")));

            verify(documentService).findById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(get(DOCUMENTS_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(documentService, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/documents/{id}")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document successfully")
        void shouldDeleteDocumentSuccessfully() throws Exception {
            doNothing().when(documentService).delete(1L);

            mockMvc.perform(delete(DOCUMENTS_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(documentService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent document")
        void shouldReturn404WhenDeletingNonExistentDocument() throws Exception {
            doThrow(new EntityNotFoundException("Document not found: 999"))
                    .when(documentService)
                    .delete(999L);

            mockMvc.perform(delete(DOCUMENTS_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Document not found: 999")));

            verify(documentService).delete(999L);
        }
    }

    @Nested
    @DisplayName("POST /api/documents/{id}/query")
    class QueryDocumentTests {

        @Test
        @DisplayName("Should query document with RAG successfully")
        void shouldQueryDocumentWithRagSuccessfully() throws Exception {
            RagQueryRequest request =
                    RagQueryRequest.builder()
                            .query("What is the architecture?")
                            .embeddingModel("nomic-embed-text")
                            .topK(5)
                            .chunkSize(500)
                            .chunkOverlap(50)
                            .build();

            List<RetrievedChunk> chunks =
                    Arrays.asList(
                            RetrievedChunk.builder()
                                    .content("The system uses layered architecture...")
                                    .distance(0.15)
                                    .chunkIndex(3)
                                    .build(),
                            RetrievedChunk.builder()
                                    .content("Each layer has specific responsibilities...")
                                    .distance(0.25)
                                    .chunkIndex(4)
                                    .build());

            String assembledContext =
                    "Context:\n\n[1] The system uses layered architecture...\n\n"
                            + "[2] Each layer has specific responsibilities...\n\n";

            when(documentService.findById(1L)).thenReturn(testDocument);
            when(ragService.buildCollectionName(1L, "nomic-embed-text"))
                    .thenReturn("doc-1-nomic-embed-text");
            when(ragService.query(
                            "doc-1-nomic-embed-text",
                            "What is the architecture?",
                            "nomic-embed-text",
                            5))
                    .thenReturn(chunks);
            when(ragService.buildContext(chunks)).thenReturn(assembledContext);

            mockMvc.perform(
                            post(DOCUMENTS_URL + "/1/query")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query", is("What is the architecture?")))
                    .andExpect(jsonPath("$.embeddingModel", is("nomic-embed-text")))
                    .andExpect(jsonPath("$.retrievedChunks", hasSize(2)))
                    .andExpect(
                            jsonPath("$.retrievedChunks[0].content")
                                    .value("The system uses layered architecture..."))
                    .andExpect(jsonPath("$.retrievedChunks[0].distance", is(0.15)))
                    .andExpect(jsonPath("$.assembledContext").exists());

            verify(documentService).findById(1L);
            verify(ragService).embedAndStore(eq(1L), eq("nomic-embed-text"), eq(500), eq(50));
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenDocumentNotFound() throws Exception {
            RagQueryRequest request =
                    RagQueryRequest.builder()
                            .query("What is the architecture?")
                            .embeddingModel("nomic-embed-text")
                            .build();

            when(documentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Document not found: 999"));

            mockMvc.perform(
                            post(DOCUMENTS_URL + "/999/query")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));

            verify(documentService).findById(999L);
            verify(ragService, never()).embedAndStore(anyLong(), anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should return 400 when query is missing")
        void shouldReturn400WhenQueryIsMissing() throws Exception {
            RagQueryRequest request =
                    RagQueryRequest.builder().embeddingModel("nomic-embed-text").build();

            mockMvc.perform(
                            post(DOCUMENTS_URL + "/1/query")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(documentService, never()).findById(any());
        }

        @Test
        @DisplayName("Should return 400 when embedding model is missing")
        void shouldReturn400WhenEmbeddingModelIsMissing() throws Exception {
            RagQueryRequest request =
                    RagQueryRequest.builder().query("What is the architecture?").build();

            mockMvc.perform(
                            post(DOCUMENTS_URL + "/1/query")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(documentService, never()).findById(any());
        }

        @Test
        @DisplayName("Should return 400 when topK is out of range")
        void shouldReturn400WhenTopKIsOutOfRange() throws Exception {
            RagQueryRequest request =
                    RagQueryRequest.builder()
                            .query("What is the architecture?")
                            .embeddingModel("nomic-embed-text")
                            .topK(100)
                            .build();

            mockMvc.perform(
                            post(DOCUMENTS_URL + "/1/query")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(documentService, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("GET /api/embedding-models")
    class GetAllEmbeddingModelsTests {

        @Test
        @DisplayName("Should return all embedding models")
        void shouldReturnAllEmbeddingModels() throws Exception {
            EmbeddingModel secondModel =
                    EmbeddingModel.builder()
                            .id(2L)
                            .name("MxBai Embed Large")
                            .ollamaModelName("mxbai-embed-large")
                            .dimensions(1024)
                            .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                            .build();

            when(embeddingModelService.findAll())
                    .thenReturn(Arrays.asList(testEmbeddingModel, secondModel));

            mockMvc.perform(get(EMBEDDING_MODELS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Nomic Embed Text")))
                    .andExpect(jsonPath("$[0].ollamaModelName", is("nomic-embed-text")))
                    .andExpect(jsonPath("$[0].dimensions", is(768)))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("MxBai Embed Large")));

            verify(embeddingModelService).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no embedding models exist")
        void shouldReturnEmptyListWhenNoEmbeddingModelsExist() throws Exception {
            when(embeddingModelService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(EMBEDDING_MODELS_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(embeddingModelService).findAll();
        }
    }

    @Nested
    @DisplayName("POST /api/embedding-models")
    class CreateEmbeddingModelTests {

        @Test
        @DisplayName("Should create embedding model with valid request")
        void shouldCreateEmbeddingModelWithValidRequest() throws Exception {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("New Embedding Model")
                            .ollamaModelName("new-embed-model")
                            .dimensions(512)
                            .build();

            EmbeddingModel createdModel =
                    EmbeddingModel.builder()
                            .id(3L)
                            .name(request.getName())
                            .ollamaModelName(request.getOllamaModelName())
                            .dimensions(request.getDimensions())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(embeddingModelService.create(any(EmbeddingModelRequest.class)))
                    .thenReturn(createdModel);

            mockMvc.perform(
                            post(EMBEDDING_MODELS_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.name", is("New Embedding Model")))
                    .andExpect(jsonPath("$.ollamaModelName", is("new-embed-model")))
                    .andExpect(jsonPath("$.dimensions", is(512)));

            verify(embeddingModelService).create(any(EmbeddingModelRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .ollamaModelName("some-model")
                            .dimensions(768)
                            .build();

            mockMvc.perform(
                            post(EMBEDDING_MODELS_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(embeddingModelService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when dimensions is missing")
        void shouldReturn400WhenDimensionsIsMissing() throws Exception {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("Test Model")
                            .ollamaModelName("some-model")
                            .build();

            mockMvc.perform(
                            post(EMBEDDING_MODELS_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(embeddingModelService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 409 when embedding model name already exists")
        void shouldReturn409WhenEmbeddingModelNameAlreadyExists() throws Exception {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("Nomic Embed Text")
                            .ollamaModelName("nomic-embed-text")
                            .dimensions(768)
                            .build();

            when(embeddingModelService.create(any(EmbeddingModelRequest.class)))
                    .thenThrow(
                            new IllegalStateException(
                                    "Embedding model already exists: Nomic Embed Text"));

            mockMvc.perform(
                            post(EMBEDDING_MODELS_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath("$.message")
                                    .value("Embedding model already exists: Nomic Embed Text"));

            verify(embeddingModelService).create(any(EmbeddingModelRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when dimensions exceeds maximum")
        void shouldReturn400WhenDimensionsExceedsMaximum() throws Exception {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("Large Model")
                            .ollamaModelName("large-model")
                            .dimensions(10000)
                            .build();

            mockMvc.perform(
                            post(EMBEDDING_MODELS_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(embeddingModelService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/embedding-models/{id}")
    class DeleteEmbeddingModelTests {

        @Test
        @DisplayName("Should delete embedding model successfully")
        void shouldDeleteEmbeddingModelSuccessfully() throws Exception {
            doNothing().when(embeddingModelService).delete(1L);

            mockMvc.perform(
                            delete(EMBEDDING_MODELS_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(embeddingModelService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent embedding model")
        void shouldReturn404WhenDeletingNonExistentEmbeddingModel() throws Exception {
            doThrow(new EntityNotFoundException("Embedding model not found: 999"))
                    .when(embeddingModelService)
                    .delete(999L);

            mockMvc.perform(
                            delete(EMBEDDING_MODELS_URL + "/999")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Embedding model not found: 999")));

            verify(embeddingModelService).delete(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(
                            delete(EMBEDDING_MODELS_URL + "/invalid")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(embeddingModelService, never()).delete(any());
        }
    }
}
