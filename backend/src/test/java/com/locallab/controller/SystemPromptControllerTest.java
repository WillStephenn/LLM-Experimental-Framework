package com.locallab.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locallab.config.CorsProperties;
import com.locallab.dto.request.SystemPromptRequest;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.SystemPrompt;
import com.locallab.service.SystemPromptService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link SystemPromptController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * SystemPromptService} dependency is mocked to verify controller behaviour and request/response
 * handling.
 *
 * <p>The test imports {@link GlobalExceptionHandler} to ensure proper error response formatting.
 * Configuration properties are enabled for {@link CorsProperties}.
 *
 * @see SystemPromptController
 * @see SystemPromptService
 */
@WebMvcTest(controllers = SystemPromptController.class)
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
@DisplayName("SystemPromptController")
class SystemPromptControllerTest {

    private static final String BASE_URL = "/api/system-prompts";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private SystemPromptService systemPromptService;

    private SystemPrompt codeAssistantPrompt;
    private SystemPrompt technicalWriterPrompt;
    private SystemPromptRequest validRequest;

    @BeforeEach
    void setUp() {
        codeAssistantPrompt =
                SystemPrompt.builder()
                        .id(1L)
                        .alias("code-assistant")
                        .content("You are an expert code reviewer...")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        technicalWriterPrompt =
                SystemPrompt.builder()
                        .id(2L)
                        .alias("technical-writer")
                        .content("You are a technical documentation specialist...")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                        .build();

        validRequest =
                SystemPromptRequest.builder()
                        .alias("json-formatter")
                        .content("You always respond with valid JSON format.")
                        .build();
    }

    @Nested
    @DisplayName("GET /api/system-prompts")
    class GetAllSystemPromptsTests {

        @Test
        @DisplayName("Should return all system prompts")
        void shouldReturnAllSystemPrompts() throws Exception {
            List<SystemPrompt> prompts = Arrays.asList(codeAssistantPrompt, technicalWriterPrompt);
            when(systemPromptService.findAll()).thenReturn(prompts);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].alias", is("code-assistant")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].alias", is("technical-writer")));

            verify(systemPromptService).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no prompts exist")
        void shouldReturnEmptyListWhenNoPromptsExist() throws Exception {
            when(systemPromptService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(systemPromptService).findAll();
        }

        @Test
        @DisplayName("Should include all response fields")
        void shouldIncludeAllResponseFields() throws Exception {
            when(systemPromptService.findAll())
                    .thenReturn(Collections.singletonList(codeAssistantPrompt));

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].alias", is("code-assistant")))
                    .andExpect(jsonPath("$[0].content", is("You are an expert code reviewer...")))
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/system-prompts/{id}")
    class GetSystemPromptByIdTests {

        @Test
        @DisplayName("Should return system prompt when found")
        void shouldReturnSystemPromptWhenFound() throws Exception {
            when(systemPromptService.findById(1L)).thenReturn(codeAssistantPrompt);

            mockMvc.perform(get(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.alias", is("code-assistant")))
                    .andExpect(jsonPath("$.content", is("You are an expert code reviewer...")));

            verify(systemPromptService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when system prompt not found")
        void shouldReturn404WhenSystemPromptNotFound() throws Exception {
            when(systemPromptService.findById(999L))
                    .thenThrow(new EntityNotFoundException("System prompt not found: 999"));

            mockMvc.perform(get(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("System prompt not found: 999")));

            verify(systemPromptService).findById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(get(BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("POST /api/system-prompts")
    class CreateSystemPromptTests {

        @Test
        @DisplayName("Should create system prompt with valid request")
        void shouldCreateSystemPromptWithValidRequest() throws Exception {
            SystemPrompt createdPrompt =
                    SystemPrompt.builder()
                            .id(3L)
                            .alias(validRequest.getAlias())
                            .content(validRequest.getContent())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(systemPromptService.create(any(SystemPromptRequest.class)))
                    .thenReturn(createdPrompt);

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.alias", is("json-formatter")))
                    .andExpect(
                            jsonPath(
                                    "$.content", is("You always respond with valid JSON format.")));

            verify(systemPromptService).create(any(SystemPromptRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when alias is missing")
        void shouldReturn400WhenAliasIsMissing() throws Exception {
            SystemPromptRequest invalidRequest =
                    SystemPromptRequest.builder().content("Some content").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'alias')]").exists());

            verify(systemPromptService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when alias is blank")
        void shouldReturn400WhenAliasIsBlank() throws Exception {
            SystemPromptRequest invalidRequest =
                    SystemPromptRequest.builder().alias("   ").content("Some content").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when content is missing")
        void shouldReturn400WhenContentIsMissing() throws Exception {
            SystemPromptRequest invalidRequest =
                    SystemPromptRequest.builder().alias("test-alias").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'content')]").exists());

            verify(systemPromptService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when alias exceeds max length")
        void shouldReturn400WhenAliasExceedsMaxLength() throws Exception {
            String longAlias = "a".repeat(51);
            SystemPromptRequest invalidRequest =
                    SystemPromptRequest.builder().alias(longAlias).content("Some content").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when multiple validation errors occur")
        void shouldReturn400WhenMultipleValidationErrorsOccur() throws Exception {
            SystemPromptRequest invalidRequest = SystemPromptRequest.builder().build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(2)));

            verify(systemPromptService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 409 when alias already exists")
        void shouldReturn409WhenAliasAlreadyExists() throws Exception {
            when(systemPromptService.create(any(SystemPromptRequest.class)))
                    .thenThrow(
                            new IllegalStateException(
                                    "System prompt with alias 'json-formatter' already exists"));

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is(
                                            "System prompt with alias 'json-formatter'"
                                                    + " already exists")));

            verify(systemPromptService).create(any(SystemPromptRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/system-prompts/{id}")
    class UpdateSystemPromptTests {

        @Test
        @DisplayName("Should update system prompt with valid request")
        void shouldUpdateSystemPromptWithValidRequest() throws Exception {
            SystemPrompt updatedPrompt =
                    SystemPrompt.builder()
                            .id(1L)
                            .alias(validRequest.getAlias())
                            .content(validRequest.getContent())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(systemPromptService.update(eq(1L), any(SystemPromptRequest.class)))
                    .thenReturn(updatedPrompt);

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.alias", is("json-formatter")))
                    .andExpect(
                            jsonPath(
                                    "$.content", is("You always respond with valid JSON format.")));

            verify(systemPromptService).update(eq(1L), any(SystemPromptRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent prompt")
        void shouldReturn404WhenUpdatingNonExistentPrompt() throws Exception {
            when(systemPromptService.update(eq(999L), any(SystemPromptRequest.class)))
                    .thenThrow(new EntityNotFoundException("System prompt not found: 999"));

            mockMvc.perform(
                            put(BASE_URL + "/999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("System prompt not found: 999")));

            verify(systemPromptService).update(eq(999L), any(SystemPromptRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid request")
        void shouldReturn400WhenUpdatingWithInvalidRequest() throws Exception {
            SystemPromptRequest invalidRequest = SystemPromptRequest.builder().build();

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).update(any(), any());
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(
                            put(BASE_URL + "/invalid")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).update(any(), any());
        }

        @Test
        @DisplayName("Should return 409 when alias conflicts with another prompt")
        void shouldReturn409WhenAliasConflictsWithAnotherPrompt() throws Exception {
            when(systemPromptService.update(eq(1L), any(SystemPromptRequest.class)))
                    .thenThrow(
                            new IllegalStateException(
                                    "System prompt with alias 'json-formatter' already exists"));

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")));

            verify(systemPromptService).update(eq(1L), any(SystemPromptRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/system-prompts/{id}")
    class DeleteSystemPromptTests {

        @Test
        @DisplayName("Should delete system prompt successfully")
        void shouldDeleteSystemPromptSuccessfully() throws Exception {
            doNothing().when(systemPromptService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(systemPromptService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent prompt")
        void shouldReturn404WhenDeletingNonExistentPrompt() throws Exception {
            doThrow(new EntityNotFoundException("System prompt not found: 999"))
                    .when(systemPromptService)
                    .delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("System prompt not found: 999")));

            verify(systemPromptService).delete(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(systemPromptService, never()).delete(any());
        }
    }
}
