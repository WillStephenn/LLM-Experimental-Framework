package com.locallab.client.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.locallab.config.OllamaConfig;
import com.locallab.dto.request.EmbeddingRequest;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.EmbeddingResponse;
import com.locallab.dto.response.GenerationResponse;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.embeddings.OllamaEmbedRequestModel;
import io.github.ollama4j.models.embeddings.OllamaEmbedResponseModel;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import io.github.ollama4j.models.request.OllamaGenerateEndpointCaller;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;

/**
 * Unit tests for {@link OllamaClientImpl}.
 *
 * <p>These tests verify the behaviour of the Ollama client implementation, including successful
 * operations, error handling, and retry logic. The tests use mocked ollama4j dependencies.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
class OllamaClientImplTest {

    @Mock private OllamaAPI ollamaApi;
    @Mock private OllamaGenerateEndpointCaller generateCaller;

    private OllamaConfig config;
    private OllamaClientImpl ollamaClient;

    @BeforeEach
    void setUp() {
        config = createDefaultConfig();
        ollamaClient = new OllamaClientImpl(config, ollamaApi, generateCaller);
    }

    private OllamaConfig createDefaultConfig() {
        OllamaConfig config = new OllamaConfig();
        config.setBaseUrl("http://localhost:11434");
        config.setTimeoutSeconds(120);

        OllamaConfig.RetryConfig retryConfig = new OllamaConfig.RetryConfig();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialDelayMs(10); // Short delay for tests
        retryConfig.setMultiplier(2.0);
        config.setRetry(retryConfig);

        return config;
    }

    @Nested
    @DisplayName("listModels")
    class ListModelsTests {

        @Test
        @DisplayName("should return list of model names successfully")
        void shouldReturnModelNames() throws Exception {
            // Arrange
            Model model1 = mock(Model.class);
            Model model2 = mock(Model.class);
            when(model1.getName()).thenReturn("llama3:8b");
            when(model2.getName()).thenReturn("qwen2.5-coder:7b");
            when(ollamaApi.listModels()).thenReturn(Arrays.asList(model1, model2));

            // Act
            List<String> models = ollamaClient.listModels();

            // Assert
            assertEquals(2, models.size());
            assertTrue(models.contains("llama3:8b"));
            assertTrue(models.contains("qwen2.5-coder:7b"));
            verify(ollamaApi).listModels();
        }

        @Test
        @DisplayName("should return empty list when no models available")
        void shouldReturnEmptyListWhenNoModels() throws Exception {
            // Arrange
            when(ollamaApi.listModels()).thenReturn(Collections.emptyList());

            // Act
            List<String> models = ollamaClient.listModels();

            // Assert
            assertTrue(models.isEmpty());
        }

        @Test
        @DisplayName("should throw ResponseStatusException when Ollama is unavailable")
        void shouldThrowExceptionWhenUnavailable() throws Exception {
            // Arrange
            when(ollamaApi.listModels()).thenThrow(new OllamaBaseException("Connection refused"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertTrue(exception.getReason().contains("listModels"));
        }

        @Test
        @DisplayName("should retry on transient failures")
        void shouldRetryOnTransientFailures() throws Exception {
            // Arrange
            Model model = mock(Model.class);
            when(model.getName()).thenReturn("llama3:8b");
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("Temporary error"))
                    .thenReturn(Collections.singletonList(model));

            // Act
            List<String> models = ollamaClient.listModels();

            // Assert
            assertEquals(1, models.size());
            assertEquals("llama3:8b", models.get(0));
            verify(ollamaApi, times(2)).listModels();
        }
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        @DisplayName("should return true when Ollama is responding")
        void shouldReturnTrueWhenAvailable() {
            // Arrange
            when(ollamaApi.ping()).thenReturn(true);

            // Act
            boolean available = ollamaClient.isAvailable();

            // Assert
            assertTrue(available);
        }

        @Test
        @DisplayName("should return false when Ollama ping fails")
        void shouldReturnFalseWhenPingFails() {
            // Arrange
            when(ollamaApi.ping()).thenReturn(false);

            // Act
            boolean available = ollamaClient.isAvailable();

            // Assert
            assertFalse(available);
        }

        @Test
        @DisplayName("should return false when exception occurs")
        void shouldReturnFalseOnException() {
            // Arrange
            when(ollamaApi.ping()).thenThrow(new RuntimeException("Connection error"));

            // Act
            boolean available = ollamaClient.isAvailable();

            // Assert
            assertFalse(available);
        }
    }

    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("should generate response successfully")
        void shouldGenerateSuccessfully() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello, world!")
                            .temperature(0.7)
                            .build();

            OllamaResult result = createMockOllamaResult("Hello! How can I help you?");
            when(ollamaApi.generate(
                            eq("llama3:8b"), anyString(), eq(false), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertEquals("Hello! How can I help you?", response.getResponse());
            assertEquals("llama3:8b", response.getModel());
            assertNotNull(response.getDurationMs());
        }

        @Test
        @DisplayName("should include system prompt in generation")
        void shouldIncludeSystemPrompt() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("What is 2+2?")
                            .systemPrompt("You are a helpful assistant.")
                            .build();

            OllamaResult result = createMockOllamaResult("4");
            when(ollamaApi.generate(
                            eq("llama3:8b"),
                            contains("You are a helpful assistant."),
                            eq(false),
                            any(Options.class),
                            isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertEquals("4", response.getResponse());
        }

        @Test
        @DisplayName("should throw BadRequest for invalid model")
        void shouldThrowBadRequestForInvalidModel() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("nonexistent-model").prompt("Hello").build();

            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenThrow(new OllamaBaseException("model 'nonexistent-model' not found"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaClient.generate(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        @DisplayName("should throw ServiceUnavailable when connection fails")
        void shouldThrowServiceUnavailableOnConnectionFailure() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenThrow(new IOException("Connection refused"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaClient.generate(request));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        @Test
        @DisplayName("should calculate metrics from OllamaResult")
        void shouldCalculateMetrics() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = createMockOllamaResultWithMetrics();
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertNotNull(response.getTokensPerSecond());
            assertNotNull(response.getPromptTokens());
            assertNotNull(response.getCompletionTokens());
        }

        @Test
        @DisplayName("should apply all hyperparameters")
        void shouldApplyAllHyperparameters() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Test")
                            .temperature(0.5)
                            .topP(0.8)
                            .topK(50)
                            .contextWindow(8192)
                            .maxTokens(100)
                            .build();

            OllamaResult result = createMockOllamaResult("Response");
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertNotNull(response);
            verify(ollamaApi)
                    .generate(
                            eq("llama3:8b"),
                            eq("Test"),
                            eq(false),
                            argThat(
                                    options ->
                                            options != null
                                                    && options.getOptionsMap()
                                                            .containsKey("temperature")),
                            isNull());
        }

        @Test
        @DisplayName("should request JSON response when jsonMode is enabled")
        void shouldRequestJsonResponseWhenEnabled() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Return JSON")
                            .jsonMode(true)
                            .build();

            OllamaResult result = createMockOllamaResult("{\"status\":\"ok\"}");
            when(generateCaller.callSync(any(OllamaGenerateRequest.class))).thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertEquals("{\"status\":\"ok\"}", response.getResponse());
            verify(generateCaller)
                    .callSync(
                            argThat(
                                    generateRequest ->
                                            Boolean.TRUE.equals(
                                                    generateRequest.getReturnFormatJson())));
        }
    }

    @Nested
    @DisplayName("embed")
    class EmbedTests {

        @Test
        @DisplayName("should generate embeddings successfully")
        void shouldGenerateEmbeddings() throws Exception {
            // Arrange
            EmbeddingRequest request =
                    EmbeddingRequest.builder()
                            .model("nomic-embed-text")
                            .input("Test input for embedding")
                            .build();

            List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
            OllamaEmbedResponseModel embedResponse = mock(OllamaEmbedResponseModel.class);
            when(embedResponse.getEmbeddings())
                    .thenReturn(Collections.singletonList(mockEmbedding));
            when(ollamaApi.embed(any(OllamaEmbedRequestModel.class))).thenReturn(embedResponse);

            // Act
            EmbeddingResponse response = ollamaClient.embed(request);

            // Assert
            assertEquals(5, response.getDimensions());
            assertEquals("nomic-embed-text", response.getModel());
            assertEquals(mockEmbedding, response.getEmbedding());
        }

        @Test
        @DisplayName("should return empty embedding when no results")
        void shouldReturnEmptyEmbeddingWhenNoResults() throws Exception {
            // Arrange
            EmbeddingRequest request =
                    EmbeddingRequest.builder().model("nomic-embed-text").input("Test").build();

            OllamaEmbedResponseModel embedResponse = mock(OllamaEmbedResponseModel.class);
            when(embedResponse.getEmbeddings()).thenReturn(Collections.emptyList());
            when(ollamaApi.embed(any(OllamaEmbedRequestModel.class))).thenReturn(embedResponse);

            // Act
            EmbeddingResponse response = ollamaClient.embed(request);

            // Assert
            assertTrue(response.getEmbedding().isEmpty());
            assertEquals(0, response.getDimensions());
        }

        @Test
        @DisplayName("should throw ResponseStatusException when embedding fails")
        void shouldThrowExceptionWhenEmbeddingFails() throws Exception {
            // Arrange
            EmbeddingRequest request =
                    EmbeddingRequest.builder().model("invalid-model").input("Test").build();

            when(ollamaApi.embed(any(OllamaEmbedRequestModel.class)))
                    .thenThrow(new OllamaBaseException("model 'invalid-model' not found"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.embed(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryLogicTests {

        @Test
        @DisplayName("should retry up to max attempts on transient failure")
        void shouldRetryUpToMaxAttempts() throws Exception {
            // Arrange
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("Connection timeout"))
                    .thenThrow(new OllamaBaseException("Connection timeout"))
                    .thenThrow(new OllamaBaseException("Connection timeout"));

            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());
            verify(ollamaApi, times(3)).listModels();
        }

        @Test
        @DisplayName("should not retry for non-retryable errors")
        void shouldNotRetryForNonRetryableErrors() throws Exception {
            // Arrange
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("model 'test' not found"));

            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());
            verify(ollamaApi, times(1)).listModels();
        }

        @Test
        @DisplayName("should retry on IOException")
        void shouldRetryOnIOException() throws Exception {
            // Arrange
            Model model = mock(Model.class);
            when(model.getName()).thenReturn("llama3:8b");
            when(ollamaApi.listModels())
                    .thenThrow(new IOException("Network error"))
                    .thenReturn(Collections.singletonList(model));

            // Act
            List<String> models = ollamaClient.listModels();

            // Assert
            assertEquals(1, models.size());
            verify(ollamaApi, times(2)).listModels();
        }

        @Test
        @DisplayName("should not retry when error contains both 'model' and 'not found'")
        void shouldNotRetryOnModelNotFoundError() throws Exception {
            // Arrange - test the combined check: contains("model") && contains("not found")
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("pull model xyz not found"));

            // Act & Assert
            assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());
            verify(ollamaApi, times(1)).listModels(); // Should not retry
        }
    }

    @Nested
    @DisplayName("Configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("should use configuration values from OllamaConfig")
        void shouldUseConfigurationValues() {
            // Arrange
            OllamaConfig customConfig = new OllamaConfig();
            customConfig.setBaseUrl("http://custom:11434");
            customConfig.setTimeoutSeconds(60);

            // Act - create new instance with custom config (uses public constructor)
            OllamaClientImpl customClient = new OllamaClientImpl(customConfig);

            // Assert - client was created without exception
            assertNotNull(customClient);
        }
    }

    @Nested
    @DisplayName("Error Mapping")
    class ErrorMappingTests {

        @Test
        @DisplayName("should map invalid request error correctly")
        void shouldMapInvalidRequestError() throws Exception {
            // Arrange
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("invalid request format"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        @Test
        @DisplayName("should map timeout error to ServiceUnavailable")
        void shouldMapTimeoutError() throws Exception {
            // Arrange
            when(ollamaApi.listModels())
                    .thenThrow(new OllamaBaseException("timeout waiting for response"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        @Test
        @DisplayName("should map connection error to ServiceUnavailable")
        void shouldMapConnectionError() throws Exception {
            // Arrange
            when(ollamaApi.listModels()).thenThrow(new OllamaBaseException("connection refused"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        @Test
        @DisplayName("should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() throws Exception {
            // Arrange
            OllamaBaseException nullMessageException = mock(OllamaBaseException.class);
            when(nullMessageException.getMessage()).thenReturn(null);
            when(ollamaApi.listModels()).thenThrow(nullMessageException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            // Exception with null message should default to ServiceUnavailable
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertNotNull(exception.getReason());
        }

        @Test
        @DisplayName("should handle generic exception")
        void shouldHandleGenericException() throws Exception {
            // Arrange
            when(ollamaApi.listModels()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        @Test
        @DisplayName("should map 'model not found' to BAD_REQUEST")
        void shouldMapModelNotFoundToBadRequest() throws Exception {
            // Arrange - test line 449: lowerMessage.contains("model") &&
            // lowerMessage.contains("not found")
            when(ollamaApi.listModels())
                    .thenThrow(
                            new OllamaBaseException(
                                    "The model 'llama-invalid' was not found on the server"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(ResponseStatusException.class, () -> ollamaClient.listModels());

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertTrue(exception.getReason().toLowerCase().contains("model"));
        }
    }

    @Nested
    @DisplayName("Generation Response Metrics")
    class GenerationMetricsTests {

        @Test
        @DisplayName("should handle zero eval duration")
        void shouldHandleZeroEvalDuration() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = mock(OllamaResult.class);
            when(result.getResponse()).thenReturn("Response");
            when(result.getEvalCount()).thenReturn(10);
            when(result.getEvalDuration()).thenReturn(0L); // Zero duration
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert - tokens per second should be null when duration is zero
            assertNull(response.getTokensPerSecond());
        }

        @Test
        @DisplayName("should handle null eval metrics")
        void shouldHandleNullEvalMetrics() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = mock(OllamaResult.class);
            when(result.getResponse()).thenReturn("Response");
            // Only stub methods that are actually called in the code path
            // The code first checks getEvalCount() != null && getEvalDuration() != null
            // Then separately checks getPromptEvalCount() != null
            // Then separately checks getPromptEvalDuration() != null
            when(result.getEvalCount()).thenReturn(null);
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert - should handle null metrics gracefully
            assertNull(response.getTokensPerSecond());
            assertNull(response.getCompletionTokens());
        }

        @Test
        @DisplayName("should handle partial eval metrics")
        void shouldHandlePartialEvalMetrics() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = mock(OllamaResult.class);
            when(result.getResponse()).thenReturn("Response");
            when(result.getEvalCount()).thenReturn(50);
            when(result.getEvalDuration()).thenReturn(null); // null duration
            when(result.getPromptEvalCount()).thenReturn(10);
            when(result.getPromptEvalDuration()).thenReturn(100_000_000L);
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertNull(response.getTokensPerSecond()); // Can't calculate without duration
            assertNull(response.getCompletionTokens()); // Only set when both eval metrics present
            assertEquals(10, response.getPromptTokens());
            assertNotNull(response.getTimeToFirstTokenMs());
        }

        @Test
        @DisplayName("should populate prompt tokens when promptEvalCount is non-null")
        void shouldHandleNonNullPromptEvalCount() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = mock(OllamaResult.class);
            when(result.getResponse()).thenReturn("Response");
            when(result.getEvalCount()).thenReturn(null);
            when(result.getPromptEvalCount()).thenReturn(15);
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert - promptTokens should be set from promptEvalCount
            assertEquals(15, response.getPromptTokens());
        }

        @Test
        @DisplayName("should populate time to first token when promptEvalDuration is non-null")
        void shouldHandleNonNullPromptEvalDuration() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            OllamaResult result = mock(OllamaResult.class);
            when(result.getResponse()).thenReturn("Response");
            when(result.getEvalCount()).thenReturn(null);
            when(result.getPromptEvalDuration()).thenReturn(250_000_000L); // 250ms in nanoseconds
            when(ollamaApi.generate(
                            anyString(), anyString(), anyBoolean(), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert - timeToFirstTokenMs should be set from promptEvalDuration
            assertEquals(250L, response.getTimeToFirstTokenMs());
        }
    }

    @Nested
    @DisplayName("Prompt Building")
    class PromptBuildingTests {

        @Test
        @DisplayName("should handle blank system prompt")
        void shouldHandleBlankSystemPrompt() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .systemPrompt("   ") // Blank (whitespace only)
                            .build();

            OllamaResult result = createMockOllamaResult("Response");
            when(ollamaApi.generate(
                            eq("llama3:8b"),
                            eq("Hello"), // Should not include blank system prompt
                            eq(false),
                            any(Options.class),
                            isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertNotNull(response);
        }

        @Test
        @DisplayName("should handle null system prompt")
        void shouldHandleNullSystemPrompt() throws Exception {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .systemPrompt(null)
                            .build();

            OllamaResult result = createMockOllamaResult("Response");
            when(ollamaApi.generate(
                            eq("llama3:8b"), eq("Hello"), eq(false), any(Options.class), isNull()))
                    .thenReturn(result);

            // Act
            GenerationResponse response = ollamaClient.generate(request);

            // Assert
            assertNotNull(response);
        }
    }

    /** Creates a mock OllamaResult with the specified response text. */
    private OllamaResult createMockOllamaResult(String response) {
        OllamaResult result = mock(OllamaResult.class);
        when(result.getResponse()).thenReturn(response);
        return result;
    }

    /** Creates a mock OllamaResult with metrics. */
    private OllamaResult createMockOllamaResultWithMetrics() {
        OllamaResult result = mock(OllamaResult.class);
        when(result.getResponse()).thenReturn("Test response");
        when(result.getEvalCount()).thenReturn(50);
        when(result.getEvalDuration()).thenReturn(1_000_000_000L); // 1 second in nanoseconds
        when(result.getPromptEvalCount()).thenReturn(10);
        when(result.getPromptEvalDuration()).thenReturn(100_000_000L); // 100ms in nanoseconds
        return result;
    }
}
