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
import com.locallab.dto.request.TaskTemplateRequest;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.TaskTemplate;
import com.locallab.service.TaskService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link TaskController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * TaskService} dependency is mocked to verify controller behaviour and request/response handling.
 *
 * <p>The test imports {@link GlobalExceptionHandler} to ensure proper error response formatting.
 * Configuration properties are enabled for {@link com.locallab.config.CorsProperties}.
 *
 * @see TaskController
 * @see TaskService
 */
@WebMvcTest(controllers = TaskController.class)
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
@DisplayName("TaskController")
class TaskControllerTest {

    private static final String BASE_URL = "/api/tasks";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private TaskService taskService;

    private TaskTemplate codeReviewTemplate;
    private TaskTemplate summarisationTemplate;
    private TaskTemplateRequest validRequest;

    @BeforeEach
    void setUp() {
        codeReviewTemplate =
                TaskTemplate.builder()
                        .id(1L)
                        .name("Code Review Task")
                        .description("Review code for best practices")
                        .promptTemplate(
                                "Review the following code:\n\n{{code}}\n\nProvide feedback...")
                        .tags("code,review,quality")
                        .evaluationNotes("Look for correctness and style")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        summarisationTemplate =
                TaskTemplate.builder()
                        .id(2L)
                        .name("Document Summarisation")
                        .description("Summarise documents concisely")
                        .promptTemplate("Summarise the following:\n\n{{document}}")
                        .tags("summary,document,nlp")
                        .evaluationNotes("Check for completeness and brevity")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                        .build();

        validRequest =
                TaskTemplateRequest.builder()
                        .name("New Task")
                        .description("A new task description")
                        .promptTemplate("Hello {{name}}, how are you?")
                        .tags("greeting,test")
                        .evaluationNotes("Check for friendliness")
                        .build();
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasksTests {

        @Test
        @DisplayName("Should return all task templates when no query params provided")
        void shouldReturnAllTaskTemplatesWhenNoQueryParamsProvided() throws Exception {
            List<TaskTemplate> templates = Arrays.asList(codeReviewTemplate, summarisationTemplate);
            when(taskService.findAll()).thenReturn(templates);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Code Review Task")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Document Summarisation")));

            verify(taskService).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no templates exist")
        void shouldReturnEmptyListWhenNoTemplatesExist() throws Exception {
            when(taskService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(taskService).findAll();
        }

        @Test
        @DisplayName("Should search by name when search param provided")
        void shouldSearchByNameWhenSearchParamProvided() throws Exception {
            when(taskService.searchByName("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("search", "code")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Code Review Task")));

            verify(taskService).searchByName("code");
            verify(taskService, never()).findAll();
            verify(taskService, never()).filterByTag(any());
        }

        @Test
        @DisplayName("Should filter by tag when tag param provided")
        void shouldFilterByTagWhenTagParamProvided() throws Exception {
            when(taskService.filterByTag("nlp"))
                    .thenReturn(Collections.singletonList(summarisationTemplate));

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("tag", "nlp")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Document Summarisation")));

            verify(taskService).filterByTag("nlp");
            verify(taskService, never()).findAll();
            verify(taskService, never()).searchByName(any());
        }

        @Test
        @DisplayName("Should prioritise search over tag when both params provided")
        void shouldPrioritiseSearchOverTagWhenBothParamsProvided() throws Exception {
            when(taskService.searchByName("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("search", "code")
                                    .param("tag", "nlp")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Code Review Task")));

            verify(taskService).searchByName("code");
            verify(taskService, never()).filterByTag(any());
        }

        @Test
        @DisplayName("Should return all templates when search param is empty")
        void shouldReturnAllTemplatesWhenSearchParamIsEmpty() throws Exception {
            List<TaskTemplate> templates = Arrays.asList(codeReviewTemplate, summarisationTemplate);
            when(taskService.findAll()).thenReturn(templates);

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("search", "")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(taskService).findAll();
            verify(taskService, never()).searchByName(any());
        }

        @Test
        @DisplayName("Should return all templates when search param is whitespace only")
        void shouldReturnAllTemplatesWhenSearchParamIsWhitespaceOnly() throws Exception {
            List<TaskTemplate> templates = Arrays.asList(codeReviewTemplate, summarisationTemplate);
            when(taskService.findAll()).thenReturn(templates);

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("search", "   ")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(taskService).findAll();
        }

        @Test
        @DisplayName("Should include all response fields")
        void shouldIncludeAllResponseFields() throws Exception {
            when(taskService.findAll()).thenReturn(Collections.singletonList(codeReviewTemplate));

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Code Review Task")))
                    .andExpect(jsonPath("$[0].description", is("Review code for best practices")))
                    .andExpect(
                            jsonPath(
                                    "$[0].promptTemplate",
                                    is(
                                            "Review the following code:\n\n"
                                                    + "{{code}}\n\n"
                                                    + "Provide feedback...")))
                    .andExpect(jsonPath("$[0].tags", is("code,review,quality")))
                    .andExpect(
                            jsonPath("$[0].evaluationNotes", is("Look for correctness and style")))
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return task template when found")
        void shouldReturnTaskTemplateWhenFound() throws Exception {
            when(taskService.findById(1L)).thenReturn(codeReviewTemplate);

            mockMvc.perform(get(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Code Review Task")))
                    .andExpect(jsonPath("$.description", is("Review code for best practices")))
                    .andExpect(jsonPath("$.tags", is("code,review,quality")));

            verify(taskService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when task template not found")
        void shouldReturn404WhenTaskTemplateNotFound() throws Exception {
            when(taskService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Task template not found: 999"));

            mockMvc.perform(get(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("Task template not found: 999")));

            verify(taskService).findById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(get(BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTaskTests {

        @Test
        @DisplayName("Should create task template with valid request")
        void shouldCreateTaskTemplateWithValidRequest() throws Exception {
            TaskTemplate createdTemplate =
                    TaskTemplate.builder()
                            .id(3L)
                            .name(validRequest.getName())
                            .description(validRequest.getDescription())
                            .promptTemplate(validRequest.getPromptTemplate())
                            .tags(validRequest.getTags())
                            .evaluationNotes(validRequest.getEvaluationNotes())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(taskService.create(any(TaskTemplateRequest.class))).thenReturn(createdTemplate);

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.name", is("New Task")))
                    .andExpect(jsonPath("$.description", is("A new task description")))
                    .andExpect(jsonPath("$.promptTemplate", is("Hello {{name}}, how are you?")))
                    .andExpect(jsonPath("$.tags", is("greeting,test")));

            verify(taskService).create(any(TaskTemplateRequest.class));
        }

        @Test
        @DisplayName("Should create task template with minimal required fields")
        void shouldCreateTaskTemplateWithMinimalRequiredFields() throws Exception {
            TaskTemplateRequest minimalRequest =
                    TaskTemplateRequest.builder()
                            .name("Minimal Task")
                            .promptTemplate("Simple prompt")
                            .build();

            TaskTemplate createdTemplate =
                    TaskTemplate.builder()
                            .id(4L)
                            .name("Minimal Task")
                            .promptTemplate("Simple prompt")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(taskService.create(any(TaskTemplateRequest.class))).thenReturn(createdTemplate);

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(4)))
                    .andExpect(jsonPath("$.name", is("Minimal Task")));

            verify(taskService).create(any(TaskTemplateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            TaskTemplateRequest invalidRequest =
                    TaskTemplateRequest.builder().promptTemplate("Some prompt").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            TaskTemplateRequest invalidRequest =
                    TaskTemplateRequest.builder().name("   ").promptTemplate("Some prompt").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when promptTemplate is missing")
        void shouldReturn400WhenPromptTemplateIsMissing() throws Exception {
            TaskTemplateRequest invalidRequest =
                    TaskTemplateRequest.builder().name("Task Name").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'promptTemplate')]").exists());

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when name exceeds max length")
        void shouldReturn400WhenNameExceedsMaxLength() throws Exception {
            String longName = "a".repeat(101);
            TaskTemplateRequest invalidRequest =
                    TaskTemplateRequest.builder()
                            .name(longName)
                            .promptTemplate("Some prompt")
                            .build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when multiple validation errors occur")
        void shouldReturn400WhenMultipleValidationErrorsOccur() throws Exception {
            TaskTemplateRequest invalidRequest = TaskTemplateRequest.builder().build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(2)));

            verify(taskService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should update task template with valid request")
        void shouldUpdateTaskTemplateWithValidRequest() throws Exception {
            TaskTemplate updatedTemplate =
                    TaskTemplate.builder()
                            .id(1L)
                            .name(validRequest.getName())
                            .description(validRequest.getDescription())
                            .promptTemplate(validRequest.getPromptTemplate())
                            .tags(validRequest.getTags())
                            .evaluationNotes(validRequest.getEvaluationNotes())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(taskService.update(eq(1L), any(TaskTemplateRequest.class)))
                    .thenReturn(updatedTemplate);

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("New Task")))
                    .andExpect(jsonPath("$.description", is("A new task description")));

            verify(taskService).update(eq(1L), any(TaskTemplateRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent template")
        void shouldReturn404WhenUpdatingNonExistentTemplate() throws Exception {
            when(taskService.update(eq(999L), any(TaskTemplateRequest.class)))
                    .thenThrow(new EntityNotFoundException("Task template not found: 999"));

            mockMvc.perform(
                            put(BASE_URL + "/999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Task template not found: 999")));

            verify(taskService).update(eq(999L), any(TaskTemplateRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid request")
        void shouldReturn400WhenUpdatingWithInvalidRequest() throws Exception {
            TaskTemplateRequest invalidRequest = TaskTemplateRequest.builder().build();

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).update(any(), any());
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

            verify(taskService, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should delete task template successfully")
        void shouldDeleteTaskTemplateSuccessfully() throws Exception {
            doNothing().when(taskService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(taskService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent template")
        void shouldReturn404WhenDeletingNonExistentTemplate() throws Exception {
            doThrow(new EntityNotFoundException("Task template not found: 999"))
                    .when(taskService)
                    .delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Task template not found: 999")));

            verify(taskService).delete(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(taskService, never()).delete(any());
        }
    }
}
