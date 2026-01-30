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
import com.locallab.dto.ExperimentConfig;
import com.locallab.dto.Hyperparameters;
import com.locallab.dto.request.ExperimentRequest;
import com.locallab.exception.GlobalExceptionHandler;
import com.locallab.model.Experiment;
import com.locallab.model.TaskTemplate;
import com.locallab.model.enums.ExperimentStatus;
import com.locallab.service.ExperimentService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link ExperimentController}.
 *
 * <p>Uses {@link WebMvcTest} to test the controller layer in isolation with MockMvc. The {@link
 * ExperimentService} dependency is mocked to verify controller behaviour and request/response
 * handling.
 *
 * <p>The test imports {@link GlobalExceptionHandler} to ensure proper error response formatting.
 * Configuration properties are enabled for {@link com.locallab.config.CorsProperties}.
 *
 * @see ExperimentController
 * @see ExperimentService
 */
@WebMvcTest(controllers = ExperimentController.class)
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
@DisplayName("ExperimentController")
class ExperimentControllerTest {

    private static final String BASE_URL = "/api/experiments";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockBean private ExperimentService experimentService;

    private TaskTemplate codeReviewTask;
    private Experiment draftExperiment;
    private Experiment runningExperiment;
    private Experiment pausedExperiment;
    private ExperimentConfig sampleConfig;
    private ExperimentRequest validRequest;

    @BeforeEach
    void setUp() {
        codeReviewTask =
                TaskTemplate.builder()
                        .id(1L)
                        .name("Code Review Task")
                        .description("Review code for best practices")
                        .promptTemplate("Review the following code:\n\n{{code}}")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        sampleConfig =
                ExperimentConfig.builder()
                        .models(List.of("qwen2.5-coder:7b", "codellama:7b"))
                        .iterations(3)
                        .contextMode("NONE")
                        .hyperparameters(
                                Hyperparameters.builder()
                                        .temperature(0.7)
                                        .topP(0.9)
                                        .topK(40)
                                        .contextWindow(4096)
                                        .build())
                        .build();

        draftExperiment =
                Experiment.builder()
                        .id(1L)
                        .name("LLM Code Review Comparison")
                        .taskTemplate(codeReviewTask)
                        .status(ExperimentStatus.DRAFT)
                        .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":3}")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        runningExperiment =
                Experiment.builder()
                        .id(2L)
                        .name("Running Experiment")
                        .status(ExperimentStatus.RUNNING)
                        .config("{\"models\":[\"codellama:7b\"],\"iterations\":5}")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                        .build();

        pausedExperiment =
                Experiment.builder()
                        .id(3L)
                        .name("Paused Experiment")
                        .status(ExperimentStatus.PAUSED)
                        .config("{\"models\":[\"deepseek-coder:6.7b\"],\"iterations\":2}")
                        .createdAt(LocalDateTime.of(2025, 11, 27, 12, 0))
                        .build();

        validRequest =
                ExperimentRequest.builder()
                        .name("New Experiment")
                        .taskTemplateId(1L)
                        .config(sampleConfig)
                        .build();
    }

    @Nested
    @DisplayName("GET /api/experiments")
    class GetAllExperimentsTests {

        @Test
        @DisplayName("Should return all experiments when no status filter provided")
        void shouldReturnAllExperimentsWhenNoStatusFilterProvided() throws Exception {
            List<Experiment> experiments = Arrays.asList(draftExperiment, runningExperiment);
            when(experimentService.findAll()).thenReturn(experiments);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("LLM Code Review Comparison")))
                    .andExpect(jsonPath("$[0].status", is("DRAFT")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Running Experiment")))
                    .andExpect(jsonPath("$[1].status", is("RUNNING")));

            verify(experimentService).findAll();
            verify(experimentService, never()).findByStatus(any());
        }

        @Test
        @DisplayName("Should return empty list when no experiments exist")
        void shouldReturnEmptyListWhenNoExperimentsExist() throws Exception {
            when(experimentService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(experimentService).findAll();
        }

        @Test
        @DisplayName("Should filter by status when status param provided")
        void shouldFilterByStatusWhenStatusParamProvided() throws Exception {
            when(experimentService.findByStatus(ExperimentStatus.DRAFT))
                    .thenReturn(Collections.singletonList(draftExperiment));
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(
                            get(BASE_URL)
                                    .param("status", "DRAFT")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("LLM Code Review Comparison")))
                    .andExpect(jsonPath("$[0].status", is("DRAFT")));

            verify(experimentService).findByStatus(ExperimentStatus.DRAFT);
            verify(experimentService, never()).findAll();
        }

        @Test
        @DisplayName("Should include task template summary in response")
        void shouldIncludeTaskTemplateSummaryInResponse() throws Exception {
            when(experimentService.findAll())
                    .thenReturn(Collections.singletonList(draftExperiment));
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].taskTemplate.id", is(1)))
                    .andExpect(jsonPath("$[0].taskTemplate.name", is("Code Review Task")));
        }

        @Test
        @DisplayName("Should not include task template when experiment has no template")
        void shouldNotIncludeTaskTemplateWhenExperimentHasNoTemplate() throws Exception {
            when(experimentService.findAll())
                    .thenReturn(Collections.singletonList(runningExperiment));
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].taskTemplate").doesNotExist());
        }

        @Test
        @DisplayName("Should include config and run counts in response")
        void shouldIncludeConfigAndRunCountsInResponse() throws Exception {
            when(experimentService.findAll())
                    .thenReturn(Collections.singletonList(draftExperiment));
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].config").exists())
                    .andExpect(jsonPath("$[0].totalRuns", is(6)))
                    .andExpect(jsonPath("$[0].completedRuns", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/experiments/{id}")
    class GetExperimentByIdTests {

        @Test
        @DisplayName("Should return experiment when found")
        void shouldReturnExperimentWhenFound() throws Exception {
            when(experimentService.findById(1L)).thenReturn(draftExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(get(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("LLM Code Review Comparison")))
                    .andExpect(jsonPath("$.status", is("DRAFT")))
                    .andExpect(jsonPath("$.taskTemplate.id", is(1)))
                    .andExpect(jsonPath("$.taskTemplate.name", is("Code Review Task")));

            verify(experimentService).findById(1L);
        }

        @Test
        @DisplayName("Should return 404 when experiment not found")
        void shouldReturn404WhenExperimentNotFound() throws Exception {
            when(experimentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(get(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));

            verify(experimentService).findById(999L);
        }

        @Test
        @DisplayName("Should return 400 for invalid ID format")
        void shouldReturn400ForInvalidIdFormat() throws Exception {
            mockMvc.perform(get(BASE_URL + "/invalid").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(experimentService, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("POST /api/experiments")
    class CreateExperimentTests {

        @Test
        @DisplayName("Should create experiment with valid request")
        void shouldCreateExperimentWithValidRequest() throws Exception {
            Experiment createdExperiment =
                    Experiment.builder()
                            .id(4L)
                            .name(validRequest.getName())
                            .taskTemplate(codeReviewTask)
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":3}")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(experimentService.create(any(ExperimentRequest.class)))
                    .thenReturn(createdExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(4)))
                    .andExpect(jsonPath("$.name", is("New Experiment")))
                    .andExpect(jsonPath("$.status", is("DRAFT")));

            verify(experimentService).create(any(ExperimentRequest.class));
        }

        @Test
        @DisplayName("Should create experiment without task template")
        void shouldCreateExperimentWithoutTaskTemplate() throws Exception {
            ExperimentRequest requestWithoutTemplate =
                    ExperimentRequest.builder()
                            .name("Ad-hoc Experiment")
                            .config(sampleConfig)
                            .build();

            Experiment createdExperiment =
                    Experiment.builder()
                            .id(5L)
                            .name("Ad-hoc Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":3}")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(experimentService.create(any(ExperimentRequest.class)))
                    .thenReturn(createdExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    requestWithoutTemplate)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(5)))
                    .andExpect(jsonPath("$.taskTemplate").doesNotExist());

            verify(experimentService).create(any(ExperimentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void shouldReturn400WhenNameIsMissing() throws Exception {
            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder().config(sampleConfig).build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists());

            verify(experimentService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when config is missing")
        void shouldReturn400WhenConfigIsMissing() throws Exception {
            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder().name("Test Experiment").build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'config')]").exists());

            verify(experimentService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 400 when name exceeds max length")
        void shouldReturn400WhenNameExceedsMaxLength() throws Exception {
            String longName = "a".repeat(201);
            ExperimentRequest invalidRequest =
                    ExperimentRequest.builder().name(longName).config(sampleConfig).build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(experimentService, never()).create(any());
        }

        @Test
        @DisplayName("Should return 404 when task template not found")
        void shouldReturn404WhenTaskTemplateNotFound() throws Exception {
            when(experimentService.create(any(ExperimentRequest.class)))
                    .thenThrow(new EntityNotFoundException("Task template not found: 999"));

            ExperimentRequest requestWithInvalidTemplate =
                    ExperimentRequest.builder()
                            .name("Test Experiment")
                            .taskTemplateId(999L)
                            .config(sampleConfig)
                            .build();

            mockMvc.perform(
                            post(BASE_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    requestWithInvalidTemplate)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Task template not found: 999")));
        }
    }

    @Nested
    @DisplayName("PUT /api/experiments/{id}")
    class UpdateExperimentTests {

        @Test
        @DisplayName("Should update experiment with valid request")
        void shouldUpdateExperimentWithValidRequest() throws Exception {
            Experiment updatedExperiment =
                    Experiment.builder()
                            .id(1L)
                            .name(validRequest.getName())
                            .taskTemplate(codeReviewTask)
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":3}")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(experimentService.update(eq(1L), any(ExperimentRequest.class)))
                    .thenReturn(updatedExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("New Experiment")));

            verify(experimentService).update(eq(1L), any(ExperimentRequest.class));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent experiment")
        void shouldReturn404WhenUpdatingNonExistentExperiment() throws Exception {
            when(experimentService.update(eq(999L), any(ExperimentRequest.class)))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(
                            put(BASE_URL + "/999")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));

            verify(experimentService).update(eq(999L), any(ExperimentRequest.class));
        }

        @Test
        @DisplayName("Should return 409 when updating non-draft experiment")
        void shouldReturn409WhenUpdatingNonDraftExperiment() throws Exception {
            when(experimentService.update(eq(2L), any(ExperimentRequest.class)))
                    .thenThrow(
                            new IllegalStateException(
                                    "Cannot update experiment with status: RUNNING"));

            mockMvc.perform(
                            put(BASE_URL + "/2")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Cannot update experiment with status: RUNNING")));
        }

        @Test
        @DisplayName("Should return 400 when updating with invalid request")
        void shouldReturn400WhenUpdatingWithInvalidRequest() throws Exception {
            ExperimentRequest invalidRequest = ExperimentRequest.builder().build();

            mockMvc.perform(
                            put(BASE_URL + "/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)));

            verify(experimentService, never()).update(any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/experiments/{id}")
    class DeleteExperimentTests {

        @Test
        @DisplayName("Should delete experiment successfully")
        void shouldDeleteExperimentSuccessfully() throws Exception {
            doNothing().when(experimentService).delete(1L);

            mockMvc.perform(delete(BASE_URL + "/1").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(experimentService).delete(1L);
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent experiment")
        void shouldReturn404WhenDeletingNonExistentExperiment() throws Exception {
            doThrow(new EntityNotFoundException("Experiment not found: 999"))
                    .when(experimentService)
                    .delete(999L);

            mockMvc.perform(delete(BASE_URL + "/999").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));

            verify(experimentService).delete(999L);
        }

        @Test
        @DisplayName("Should return 409 when deleting running experiment")
        void shouldReturn409WhenDeletingRunningExperiment() throws Exception {
            doThrow(new IllegalStateException("Cannot delete a running experiment"))
                    .when(experimentService)
                    .delete(2L);

            mockMvc.perform(delete(BASE_URL + "/2").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Cannot delete a running experiment")));

            verify(experimentService).delete(2L);
        }
    }

    @Nested
    @DisplayName("POST /api/experiments/{id}/start")
    class StartExperimentTests {

        @Test
        @DisplayName("Should start draft experiment successfully")
        void shouldStartDraftExperimentSuccessfully() throws Exception {
            Experiment startedExperiment =
                    Experiment.builder()
                            .id(1L)
                            .name("LLM Code Review Comparison")
                            .taskTemplate(codeReviewTask)
                            .status(ExperimentStatus.RUNNING)
                            .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":3}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                            .build();

            when(experimentService.findById(1L)).thenReturn(draftExperiment);
            when(experimentService.updateStatus(1L, ExperimentStatus.RUNNING))
                    .thenReturn(startedExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(6);

            mockMvc.perform(post(BASE_URL + "/1/start").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.status", is("RUNNING")));

            verify(experimentService).updateStatus(1L, ExperimentStatus.RUNNING);
        }

        @Test
        @DisplayName("Should return 409 when starting non-draft experiment")
        void shouldReturn409WhenStartingNonDraftExperiment() throws Exception {
            when(experimentService.findById(2L)).thenReturn(runningExperiment);

            mockMvc.perform(post(BASE_URL + "/2/start").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Cannot start experiment with status: RUNNING")));

            verify(experimentService, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("Should return 404 when starting non-existent experiment")
        void shouldReturn404WhenStartingNonExistentExperiment() throws Exception {
            when(experimentService.findById(999L))
                    .thenThrow(new EntityNotFoundException("Experiment not found: 999"));

            mockMvc.perform(post(BASE_URL + "/999/start").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Experiment not found: 999")));
        }
    }

    @Nested
    @DisplayName("POST /api/experiments/{id}/pause")
    class PauseExperimentTests {

        @Test
        @DisplayName("Should pause running experiment successfully")
        void shouldPauseRunningExperimentSuccessfully() throws Exception {
            Experiment pausedExperimentResult =
                    Experiment.builder()
                            .id(2L)
                            .name("Running Experiment")
                            .status(ExperimentStatus.PAUSED)
                            .config("{\"models\":[\"codellama:7b\"],\"iterations\":5}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                            .build();

            when(experimentService.findById(2L)).thenReturn(runningExperiment);
            when(experimentService.updateStatus(2L, ExperimentStatus.PAUSED))
                    .thenReturn(pausedExperimentResult);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(5);

            mockMvc.perform(post(BASE_URL + "/2/pause").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.status", is("PAUSED")));

            verify(experimentService).updateStatus(2L, ExperimentStatus.PAUSED);
        }

        @Test
        @DisplayName("Should return 409 when pausing non-running experiment")
        void shouldReturn409WhenPausingNonRunningExperiment() throws Exception {
            when(experimentService.findById(1L)).thenReturn(draftExperiment);

            mockMvc.perform(post(BASE_URL + "/1/pause").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message", is("Cannot pause experiment with status: DRAFT")));

            verify(experimentService, never()).updateStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/experiments/{id}/resume")
    class ResumeExperimentTests {

        @Test
        @DisplayName("Should resume paused experiment successfully")
        void shouldResumePausedExperimentSuccessfully() throws Exception {
            Experiment resumedExperiment =
                    Experiment.builder()
                            .id(3L)
                            .name("Paused Experiment")
                            .status(ExperimentStatus.RUNNING)
                            .config("{\"models\":[\"deepseek-coder:6.7b\"],\"iterations\":2}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 12, 0))
                            .build();

            when(experimentService.findById(3L)).thenReturn(pausedExperiment);
            when(experimentService.updateStatus(3L, ExperimentStatus.RUNNING))
                    .thenReturn(resumedExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(2);

            mockMvc.perform(post(BASE_URL + "/3/resume").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.status", is("RUNNING")));

            verify(experimentService).updateStatus(3L, ExperimentStatus.RUNNING);
        }

        @Test
        @DisplayName("Should return 409 when resuming non-paused experiment")
        void shouldReturn409WhenResumingNonPausedExperiment() throws Exception {
            when(experimentService.findById(1L)).thenReturn(draftExperiment);

            mockMvc.perform(post(BASE_URL + "/1/resume").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Cannot resume experiment with status: DRAFT")));

            verify(experimentService, never()).updateStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /api/experiments/{id}/cancel")
    class CancelExperimentTests {

        @Test
        @DisplayName("Should cancel running experiment successfully")
        void shouldCancelRunningExperimentSuccessfully() throws Exception {
            Experiment cancelledExperiment =
                    Experiment.builder()
                            .id(2L)
                            .name("Running Experiment")
                            .status(ExperimentStatus.FAILED)
                            .config("{\"models\":[\"codellama:7b\"],\"iterations\":5}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                            .build();

            when(experimentService.findById(2L)).thenReturn(runningExperiment);
            when(experimentService.updateStatus(2L, ExperimentStatus.FAILED))
                    .thenReturn(cancelledExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(5);

            mockMvc.perform(post(BASE_URL + "/2/cancel").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(2)))
                    .andExpect(jsonPath("$.status", is("FAILED")));

            verify(experimentService).updateStatus(2L, ExperimentStatus.FAILED);
        }

        @Test
        @DisplayName("Should cancel paused experiment successfully")
        void shouldCancelPausedExperimentSuccessfully() throws Exception {
            Experiment cancelledExperiment =
                    Experiment.builder()
                            .id(3L)
                            .name("Paused Experiment")
                            .status(ExperimentStatus.FAILED)
                            .config("{\"models\":[\"deepseek-coder:6.7b\"],\"iterations\":2}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 12, 0))
                            .build();

            when(experimentService.findById(3L)).thenReturn(pausedExperiment);
            when(experimentService.updateStatus(3L, ExperimentStatus.FAILED))
                    .thenReturn(cancelledExperiment);
            when(experimentService.parseConfig(any())).thenReturn(sampleConfig);
            when(experimentService.calculateTotalRuns(any())).thenReturn(2);

            mockMvc.perform(post(BASE_URL + "/3/cancel").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.status", is("FAILED")));

            verify(experimentService).updateStatus(3L, ExperimentStatus.FAILED);
        }

        @Test
        @DisplayName("Should return 409 when cancelling draft experiment")
        void shouldReturn409WhenCancellingDraftExperiment() throws Exception {
            when(experimentService.findById(1L)).thenReturn(draftExperiment);

            mockMvc.perform(post(BASE_URL + "/1/cancel").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Cannot cancel experiment with status: DRAFT")));

            verify(experimentService, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("Should return 409 when cancelling completed experiment")
        void shouldReturn409WhenCancellingCompletedExperiment() throws Exception {
            Experiment completedExperiment =
                    Experiment.builder()
                            .id(4L)
                            .name("Completed Experiment")
                            .status(ExperimentStatus.COMPLETED)
                            .config("{\"models\":[\"qwen2.5-coder:7b\"],\"iterations\":1}")
                            .createdAt(LocalDateTime.of(2025, 11, 27, 13, 0))
                            .build();

            when(experimentService.findById(4L)).thenReturn(completedExperiment);

            mockMvc.perform(post(BASE_URL + "/4/cancel").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(
                            jsonPath(
                                    "$.message",
                                    is("Cannot cancel experiment with status: COMPLETED")));

            verify(experimentService, never()).updateStatus(any(), any());
        }
    }
}
