package com.locallab.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

import com.locallab.client.OllamaClient;
import com.locallab.config.OllamaConfig;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.dto.response.OllamaStatusResponse;

/**
 * Unit tests for {@link OllamaService}.
 *
 * <p>These tests verify the behaviour of the Ollama service layer, including model listing,
 * connectivity checking, and text generation. The tests use a mocked {@link OllamaClient}.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
class OllamaServiceTest {

    @Mock private OllamaClient ollamaClient;

    private OllamaConfig ollamaConfig;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        ollamaConfig = createDefaultConfig();
        ollamaService = new OllamaService(ollamaClient, ollamaConfig);
    }

    private OllamaConfig createDefaultConfig() {
        OllamaConfig config = new OllamaConfig();
        config.setBaseUrl("http://localhost:11434");
        config.setTimeoutSeconds(120);
        return config;
    }

    @Nested
    @DisplayName("getAvailableModels")
    class GetAvailableModelsTests {

        @Test
        @DisplayName("should return list of model names successfully")
        void shouldReturnModelNames() {
            // Arrange
            List<String> expectedModels =
                    Arrays.asList("llama3:8b", "qwen2.5-coder:7b", "codellama:7b");
            when(ollamaClient.listModels()).thenReturn(expectedModels);

            // Act
            List<String> models = ollamaService.getAvailableModels();

            // Assert
            assertEquals(3, models.size());
            assertTrue(models.contains("llama3:8b"));
            assertTrue(models.contains("qwen2.5-coder:7b"));
            assertTrue(models.contains("codellama:7b"));
            verify(ollamaClient).listModels();
        }

        @Test
        @DisplayName("should return empty list when no models available")
        void shouldReturnEmptyListWhenNoModels() {
            // Arrange
            when(ollamaClient.listModels()).thenReturn(Collections.emptyList());

            // Act
            List<String> models = ollamaService.getAvailableModels();

            // Assert
            assertTrue(models.isEmpty());
            verify(ollamaClient).listModels();
        }

        @Test
        @DisplayName("should propagate ResponseStatusException from client")
        void shouldPropagateResponseStatusException() {
            // Arrange
            ResponseStatusException clientException =
                    new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Ollama listModels failed: Connection refused");
            when(ollamaClient.listModels()).thenThrow(clientException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class,
                            () -> ollamaService.getAvailableModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertEquals("Ollama listModels failed: Connection refused", exception.getReason());
        }

        @Test
        @DisplayName("should wrap unexpected exceptions in ResponseStatusException")
        void shouldWrapUnexpectedExceptions() {
            // Arrange
            when(ollamaClient.listModels()).thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class,
                            () -> ollamaService.getAvailableModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Unexpected error"));
        }
    }

    @Nested
    @DisplayName("checkConnectivity")
    class CheckConnectivityTests {

        @Test
        @DisplayName("should return available status when Ollama is running")
        void shouldReturnAvailableStatus() {
            // Arrange
            List<String> models = Arrays.asList("llama3:8b", "qwen2.5-coder:7b");
            when(ollamaClient.isAvailable()).thenReturn(true);
            when(ollamaClient.listModels()).thenReturn(models);

            // Act
            OllamaStatusResponse status = ollamaService.checkConnectivity();

            // Assert
            assertTrue(status.getAvailable());
            assertEquals("http://localhost:11434", status.getBaseUrl());
            assertEquals(2, status.getModelCount());
            assertEquals("Ollama is running", status.getMessage());
        }

        @Test
        @DisplayName("should return unavailable status when Ollama is not running")
        void shouldReturnUnavailableStatus() {
            // Arrange
            when(ollamaClient.isAvailable()).thenReturn(false);

            // Act
            OllamaStatusResponse status = ollamaService.checkConnectivity();

            // Assert
            assertFalse(status.getAvailable());
            assertEquals("http://localhost:11434", status.getBaseUrl());
            assertEquals(0, status.getModelCount());
            assertEquals("Ollama is not available", status.getMessage());
            verify(ollamaClient, never()).listModels();
        }

        @Test
        @DisplayName("should handle model listing failure gracefully")
        void shouldHandleModelListingFailure() {
            // Arrange
            when(ollamaClient.isAvailable()).thenReturn(true);
            when(ollamaClient.listModels()).thenThrow(new RuntimeException("Model listing failed"));

            // Act
            OllamaStatusResponse status = ollamaService.checkConnectivity();

            // Assert
            assertTrue(status.getAvailable());
            assertEquals("http://localhost:11434", status.getBaseUrl());
            assertEquals(0, status.getModelCount());
            assertTrue(status.getMessage().contains("Model listing failed"));
        }

        @Test
        @DisplayName("should return zero model count when no models installed")
        void shouldReturnZeroModelCount() {
            // Arrange
            when(ollamaClient.isAvailable()).thenReturn(true);
            when(ollamaClient.listModels()).thenReturn(Collections.emptyList());

            // Act
            OllamaStatusResponse status = ollamaService.checkConnectivity();

            // Assert
            assertTrue(status.getAvailable());
            assertEquals(0, status.getModelCount());
            assertEquals("Ollama is running", status.getMessage());
        }
    }

    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("should generate response successfully")
        void shouldGenerateSuccessfully() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello, world!")
                            .temperature(0.7)
                            .build();

            GenerationResponse expectedResponse =
                    GenerationResponse.builder()
                            .response("Hello! How can I help you today?")
                            .model("llama3:8b")
                            .durationMs(2500L)
                            .tokensPerSecond(45.5)
                            .timeToFirstTokenMs(120L)
                            .promptTokens(10)
                            .completionTokens(50)
                            .build();

            when(ollamaClient.generate(request)).thenReturn(expectedResponse);

            // Act
            GenerationResponse response = ollamaService.generate(request);

            // Assert
            assertEquals("Hello! How can I help you today?", response.getResponse());
            assertEquals("llama3:8b", response.getModel());
            assertEquals(2500L, response.getDurationMs());
            assertEquals(45.5, response.getTokensPerSecond());
            verify(ollamaClient).generate(request);
        }

        @Test
        @DisplayName("should generate response with all hyperparameters")
        void shouldGenerateWithAllHyperparameters() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder()
                            .model("qwen2.5-coder:7b")
                            .prompt("Write a function to calculate factorial")
                            .systemPrompt("You are a helpful coding assistant.")
                            .temperature(0.5)
                            .topP(0.9)
                            .topK(40)
                            .contextWindow(8192)
                            .maxTokens(500)
                            .build();

            GenerationResponse expectedResponse =
                    GenerationResponse.builder()
                            .response(
                                    "def factorial(n):\n"
                                            + "    if n <= 1:\n"
                                            + "        return 1\n"
                                            + "    return n * factorial(n - 1)")
                            .model("qwen2.5-coder:7b")
                            .durationMs(1500L)
                            .build();

            when(ollamaClient.generate(request)).thenReturn(expectedResponse);

            // Act
            GenerationResponse response = ollamaService.generate(request);

            // Assert
            assertNotNull(response);
            assertEquals("qwen2.5-coder:7b", response.getModel());
            verify(ollamaClient).generate(request);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null request")
        void shouldThrowExceptionForNullRequest() {
            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(null));

            assertEquals("Generation request must not be null", exception.getMessage());
            verify(ollamaClient, never()).generate(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null model")
        void shouldThrowExceptionForNullModel() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model(null).prompt("Hello").build();

            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(request));

            assertEquals("Model name must not be blank", exception.getMessage());
            verify(ollamaClient, never()).generate(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for blank model")
        void shouldThrowExceptionForBlankModel() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("   ").prompt("Hello").build();

            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(request));

            assertEquals("Model name must not be blank", exception.getMessage());
            verify(ollamaClient, never()).generate(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for null prompt")
        void shouldThrowExceptionForNullPrompt() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt(null).build();

            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(request));

            assertEquals("Prompt must not be blank", exception.getMessage());
            verify(ollamaClient, never()).generate(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for blank prompt")
        void shouldThrowExceptionForBlankPrompt() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("").build();

            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(request));

            assertEquals("Prompt must not be blank", exception.getMessage());
            verify(ollamaClient, never()).generate(any());
        }

        @Test
        @DisplayName("should propagate ResponseStatusException from client")
        void shouldPropagateResponseStatusException() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("nonexistent-model").prompt("Hello").build();

            ResponseStatusException clientException =
                    new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ollama generate failed: model 'nonexistent-model' not found");
            when(ollamaClient.generate(request)).thenThrow(clientException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaService.generate(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertTrue(exception.getReason().contains("nonexistent-model"));
        }

        @Test
        @DisplayName("should wrap unexpected exceptions in ResponseStatusException")
        void shouldWrapUnexpectedExceptions() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            when(ollamaClient.generate(request))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaService.generate(request));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Generation failed"));
            assertTrue(exception.getReason().contains("Unexpected error"));
        }

        @Test
        @DisplayName("should propagate SERVICE_UNAVAILABLE for connection errors")
        void shouldPropagateServiceUnavailable() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            ResponseStatusException clientException =
                    new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Ollama generate failed: Connection refused");
            when(ollamaClient.generate(request)).thenThrow(clientException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaService.generate(request));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("should accept valid request with minimal parameters")
        void shouldAcceptValidMinimalRequest() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Test prompt").build();

            GenerationResponse expectedResponse =
                    GenerationResponse.builder()
                            .response("Response")
                            .model("llama3:8b")
                            .durationMs(1000L)
                            .build();

            when(ollamaClient.generate(request)).thenReturn(expectedResponse);

            // Act
            GenerationResponse response = ollamaService.generate(request);

            // Assert
            assertNotNull(response);
        }

        @Test
        @DisplayName("should accept request with whitespace-only prompt after trimming check fails")
        void shouldRejectWhitespaceOnlyPrompt() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("   \t\n   ").build();

            // Act & Assert
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> ollamaService.generate(request));

            assertEquals("Prompt must not be blank", exception.getMessage());
        }

        @Test
        @DisplayName("should accept request with valid single character model name")
        void shouldAcceptSingleCharModelName() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("a").prompt("Hello").build();

            GenerationResponse expectedResponse =
                    GenerationResponse.builder()
                            .response("Response")
                            .model("a")
                            .durationMs(1000L)
                            .build();

            when(ollamaClient.generate(request)).thenReturn(expectedResponse);

            // Act
            GenerationResponse response = ollamaService.generate(request);

            // Assert
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle ResponseStatusException with null reason in generate")
        void shouldHandleNullReasonInGenerate() {
            // Arrange
            GenerationRequest request =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            ResponseStatusException clientException =
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            when(ollamaClient.generate(request)).thenThrow(clientException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class, () -> ollamaService.generate(request));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }

        @Test
        @DisplayName("should handle ResponseStatusException with null reason in getAvailableModels")
        void shouldHandleNullReasonInGetAvailableModels() {
            // Arrange
            ResponseStatusException clientException =
                    new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            when(ollamaClient.listModels()).thenThrow(clientException);

            // Act & Assert
            ResponseStatusException exception =
                    assertThrows(
                            ResponseStatusException.class,
                            () -> ollamaService.getAvailableModels());

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }
    }
}
