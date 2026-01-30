package com.locallab.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.config.CorsProperties;
import com.locallab.dto.request.GenerationRequest;
import com.locallab.dto.response.GenerationResponse;
import com.locallab.dto.response.OllamaStatusResponse;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.service.OllamaService;

/**
 * Unit tests for {@link OllamaController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * OllamaService} dependency is mocked to verify controller behaviour and request/response handling.
 *
 * <p>The test imports {@link GlobalExceptionHandler} to ensure proper error response formatting.
 * Configuration properties are enabled for {@link com.locallab.config.CorsProperties}.
 *
 * @see OllamaController
 * @see OllamaService
 */
@WebMvcTest(controllers = OllamaController.class)
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
@DisplayName("OllamaController")
class OllamaControllerTest {

    private static final String BASE_URL = "/api/ollama";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private OllamaService ollamaService;

    private GenerationRequest validGenerationRequest;
    private GenerationResponse generationResponse;
    private OllamaStatusResponse availableStatusResponse;
    private OllamaStatusResponse unavailableStatusResponse;

    @BeforeEach
    void setUp() {
        validGenerationRequest =
                GenerationRequest.builder()
                        .model("qwen2.5-coder:7b")
                        .prompt("Explain dependency injection")
                        .temperature(0.7)
                        .topP(0.9)
                        .topK(40)
                        .build();

        generationResponse =
                GenerationResponse.builder()
                        .response(
                                "Dependency injection is a design pattern where dependencies are "
                                        + "provided to a class rather than created internally.")
                        .model("qwen2.5-coder:7b")
                        .durationMs(2500L)
                        .tokensPerSecond(45.5)
                        .timeToFirstTokenMs(120L)
                        .promptTokens(15)
                        .completionTokens(80)
                        .build();

        availableStatusResponse =
                OllamaStatusResponse.builder()
                        .available(true)
                        .baseUrl("http://localhost:11434")
                        .modelCount(5)
                        .message("Ollama is running")
                        .build();

        unavailableStatusResponse =
                OllamaStatusResponse.builder()
                        .available(false)
                        .baseUrl("http://localhost:11434")
                        .modelCount(0)
                        .message("Ollama is not available")
                        .build();
    }

    @Nested
    @DisplayName("GET /api/ollama/models")
    class ListModelsTests {

        @Test
        @DisplayName("Should return list of available models")
        void shouldReturnListOfAvailableModels() throws Exception {
            List<String> models =
                    Arrays.asList(
                            "qwen2.5-coder:7b",
                            "codellama:7b",
                            "deepseek-coder:6.7b",
                            "nomic-embed-text",
                            "mxbai-embed-large");
            when(ollamaService.getAvailableModels()).thenReturn(models);

            mockMvc.perform(get(BASE_URL + "/models").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.models", hasSize(5)))
                    .andExpect(
                            jsonPath(
                                    "$.models",
                                    containsInAnyOrder(
                                            "qwen2.5-coder:7b",
                                            "codellama:7b",
                                            "deepseek-coder:6.7b",
                                            "nomic-embed-text",
                                            "mxbai-embed-large")));

            verify(ollamaService).getAvailableModels();
        }

        @Test
        @DisplayName("Should return empty list when no models available")
        void shouldReturnEmptyListWhenNoModelsAvailable() throws Exception {
            when(ollamaService.getAvailableModels()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL + "/models").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.models", hasSize(0)));

            verify(ollamaService).getAvailableModels();
        }

        @Test
        @DisplayName("Should return 503 when Ollama is unavailable")
        void shouldReturn503WhenOllamaIsUnavailable() throws Exception {
            when(ollamaService.getAvailableModels())
                    .thenThrow(
                            new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Failed to retrieve models from Ollama: Connection refused"));

            mockMvc.perform(get(BASE_URL + "/models").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status", is(503)))
                    .andExpect(jsonPath("$.error", is("Service Unavailable")))
                    .andExpect(jsonPath("$.message").exists());

            verify(ollamaService).getAvailableModels();
        }
    }

    @Nested
    @DisplayName("GET /api/ollama/status")
    class CheckStatusTests {

        @Test
        @DisplayName("Should return available status when Ollama is running")
        void shouldReturnAvailableStatusWhenOllamaIsRunning() throws Exception {
            when(ollamaService.checkConnectivity()).thenReturn(availableStatusResponse);

            mockMvc.perform(get(BASE_URL + "/status").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available", is(true)))
                    .andExpect(jsonPath("$.baseUrl", is("http://localhost:11434")))
                    .andExpect(jsonPath("$.modelCount", is(5)))
                    .andExpect(jsonPath("$.message", is("Ollama is running")));

            verify(ollamaService).checkConnectivity();
        }

        @Test
        @DisplayName("Should return unavailable status when Ollama is not running")
        void shouldReturnUnavailableStatusWhenOllamaIsNotRunning() throws Exception {
            when(ollamaService.checkConnectivity()).thenReturn(unavailableStatusResponse);

            mockMvc.perform(get(BASE_URL + "/status").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available", is(false)))
                    .andExpect(jsonPath("$.baseUrl", is("http://localhost:11434")))
                    .andExpect(jsonPath("$.modelCount", is(0)))
                    .andExpect(jsonPath("$.message", is("Ollama is not available")));

            verify(ollamaService).checkConnectivity();
        }

        @Test
        @DisplayName("Should return status with zero models when none installed")
        void shouldReturnStatusWithZeroModelsWhenNoneInstalled() throws Exception {
            OllamaStatusResponse statusWithNoModels =
                    OllamaStatusResponse.builder()
                            .available(true)
                            .baseUrl("http://localhost:11434")
                            .modelCount(0)
                            .message("Ollama is running")
                            .build();
            when(ollamaService.checkConnectivity()).thenReturn(statusWithNoModels);

            mockMvc.perform(get(BASE_URL + "/status").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.available", is(true)))
                    .andExpect(jsonPath("$.modelCount", is(0)));

            verify(ollamaService).checkConnectivity();
        }
    }

    @Nested
    @DisplayName("POST /api/ollama/generate")
    class GenerateTests {

        @Test
        @DisplayName("Should generate response with valid request")
        void shouldGenerateResponseWithValidRequest() throws Exception {
            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenReturn(generationResponse);

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    validGenerationRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").exists())
                    .andExpect(jsonPath("$.model", is("qwen2.5-coder:7b")))
                    .andExpect(jsonPath("$.durationMs", is(2500)))
                    .andExpect(jsonPath("$.tokensPerSecond", is(45.5)))
                    .andExpect(jsonPath("$.timeToFirstTokenMs", is(120)))
                    .andExpect(jsonPath("$.promptTokens", is(15)))
                    .andExpect(jsonPath("$.completionTokens", is(80)));

            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("Should generate response with minimal required fields")
        void shouldGenerateResponseWithMinimalRequiredFields() throws Exception {
            GenerationRequest minimalRequest =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").build();

            GenerationResponse minimalResponse =
                    GenerationResponse.builder()
                            .response("Hello! How can I help you?")
                            .model("llama3:8b")
                            .durationMs(500L)
                            .build();

            when(ollamaService.generate(any(GenerationRequest.class))).thenReturn(minimalResponse);

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response", is("Hello! How can I help you?")))
                    .andExpect(jsonPath("$.model", is("llama3:8b")));

            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when model is missing")
        void shouldReturn400WhenModelIsMissing() throws Exception {
            GenerationRequest invalidRequest = GenerationRequest.builder().prompt("Hello").build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'model')]").exists());

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when model is blank")
        void shouldReturn400WhenModelIsBlank() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder().model("   ").prompt("Hello").build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when prompt is missing")
        void shouldReturn400WhenPromptIsMissing() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder().model("llama3:8b").build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'prompt')]").exists());

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when prompt is blank")
        void shouldReturn400WhenPromptIsBlank() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder().model("llama3:8b").prompt("   ").build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when temperature is below minimum")
        void shouldReturn400WhenTemperatureIsBelowMinimum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .temperature(-0.1)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when temperature exceeds maximum")
        void shouldReturn400WhenTemperatureExceedsMaximum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .temperature(2.1)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when topP is below minimum")
        void shouldReturn400WhenTopPIsBelowMinimum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .topP(-0.1)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when topP exceeds maximum")
        void shouldReturn400WhenTopPExceedsMaximum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .topP(1.1)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when topK is below minimum")
        void shouldReturn400WhenTopKIsBelowMinimum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder().model("llama3:8b").prompt("Hello").topK(0).build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when topK exceeds maximum")
        void shouldReturn400WhenTopKExceedsMaximum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .topK(101)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when multiple validation errors occur")
        void shouldReturn400WhenMultipleValidationErrorsOccur() throws Exception {
            GenerationRequest invalidRequest = GenerationRequest.builder().build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(2)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 503 when Ollama is unavailable during generation")
        void shouldReturn503WhenOllamaIsUnavailableDuringGeneration() throws Exception {
            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenThrow(
                            new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Generation failed: Connection refused"));

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    validGenerationRequest)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status", is(503)))
                    .andExpect(jsonPath("$.error", is("Service Unavailable")));

            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when model is not found")
        void shouldReturn400WhenModelIsNotFound() throws Exception {
            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenThrow(new IllegalArgumentException("Model name must not be blank"));

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    validGenerationRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("Should accept request with all optional hyperparameters")
        void shouldAcceptRequestWithAllOptionalHyperparameters() throws Exception {
            GenerationRequest fullRequest =
                    GenerationRequest.builder()
                            .model("qwen2.5-coder:7b")
                            .prompt("Write a function to calculate factorial")
                            .systemPrompt("You are a helpful coding assistant.")
                            .temperature(0.5)
                            .topP(0.9)
                            .topK(40)
                            .contextWindow(8192)
                            .maxTokens(500)
                            .stream(false)
                            .build();

            when(ollamaService.generate(any(GenerationRequest.class)))
                    .thenReturn(generationResponse);

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(fullRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.model", is("qwen2.5-coder:7b")));

            verify(ollamaService).generate(any(GenerationRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when contextWindow is below minimum")
        void shouldReturn400WhenContextWindowIsBelowMinimum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .contextWindow(511)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when contextWindow exceeds maximum")
        void shouldReturn400WhenContextWindowExceedsMaximum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .contextWindow(128001)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when maxTokens is below minimum")
        void shouldReturn400WhenMaxTokensIsBelowMinimum() throws Exception {
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .maxTokens(0)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when prompt exceeds maximum length")
        void shouldReturn400WhenPromptExceedsMaximumLength() throws Exception {
            String longPrompt = "a".repeat(100001);
            GenerationRequest invalidRequest =
                    GenerationRequest.builder().model("llama3:8b").prompt(longPrompt).build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }

        @Test
        @DisplayName("Should return 400 when systemPrompt exceeds maximum length")
        void shouldReturn400WhenSystemPromptExceedsMaximumLength() throws Exception {
            String longSystemPrompt = "a".repeat(50001);
            GenerationRequest invalidRequest =
                    GenerationRequest.builder()
                            .model("llama3:8b")
                            .prompt("Hello")
                            .systemPrompt(longSystemPrompt)
                            .build();

            mockMvc.perform(
                            post(BASE_URL + "/generate")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(ollamaService, never()).generate(any());
        }
    }
}
