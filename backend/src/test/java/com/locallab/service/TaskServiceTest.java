package com.locallab.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

import com.locallab.dto.request.TaskTemplateRequest;
import com.locallab.model.TaskTemplate;
import com.locallab.repository.TaskTemplateRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link TaskService}.
 *
 * <p>Uses Mockito to mock the {@link TaskTemplateRepository} dependency. Tests cover all CRUD
 * operations, variable extraction, search functionality, and error handling scenarios.
 *
 * @see TaskService
 * @see TaskTemplateRepository
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskTemplateRepository taskTemplateRepository;

    @InjectMocks private TaskService taskService;

    private TaskTemplate codeReviewTemplate;
    private TaskTemplate summarisationTemplate;
    private TaskTemplateRequest createRequest;

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
                        .createdAt(LocalDateTime.now())
                        .build();

        summarisationTemplate =
                TaskTemplate.builder()
                        .id(2L)
                        .name("Document Summarisation")
                        .description("Summarise documents concisely")
                        .promptTemplate("Summarise the following:\n\n{{document}}")
                        .tags("summary,document,nlp")
                        .evaluationNotes("Check for completeness and brevity")
                        .createdAt(LocalDateTime.now())
                        .build();

        createRequest =
                TaskTemplateRequest.builder()
                        .name("New Task")
                        .description("A new task description")
                        .promptTemplate("Hello {{name}}, how are you?")
                        .tags("greeting,test")
                        .evaluationNotes("Check for friendliness")
                        .build();
    }

    @Nested
    @DisplayName("findAll Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all task templates")
        void shouldReturnAllTaskTemplates() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.findAll();

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no templates exist")
        void shouldReturnEmptyListWhenNoTemplatesExist() {
            when(taskTemplateRepository.findAll()).thenReturn(Collections.emptyList());

            List<TaskTemplate> results = taskService.findAll();

            assertTrue(results.isEmpty());
            verify(taskTemplateRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return task template when found")
        void shouldReturnTaskTemplateWhenFound() {
            when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(codeReviewTemplate));

            TaskTemplate result = taskService.findById(1L);

            assertNotNull(result);
            assertEquals("Code Review Task", result.getName());
            verify(taskTemplateRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(taskTemplateRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class, () -> taskService.findById(999L));

            assertEquals("Task template not found: 999", exception.getMessage());
            verify(taskTemplateRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create task template with all fields")
        void shouldCreateTaskTemplateWithAllFields() {
            TaskTemplate savedTemplate =
                    TaskTemplate.builder()
                            .id(3L)
                            .name(createRequest.getName())
                            .description(createRequest.getDescription())
                            .promptTemplate(createRequest.getPromptTemplate())
                            .tags(createRequest.getTags())
                            .evaluationNotes(createRequest.getEvaluationNotes())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(taskTemplateRepository.save(any(TaskTemplate.class))).thenReturn(savedTemplate);

            TaskTemplate result = taskService.create(createRequest);

            assertNotNull(result);
            assertEquals(3L, result.getId());
            assertEquals("New Task", result.getName());

            ArgumentCaptor<TaskTemplate> captor = ArgumentCaptor.forClass(TaskTemplate.class);
            verify(taskTemplateRepository).save(captor.capture());

            TaskTemplate captured = captor.getValue();
            assertEquals("New Task", captured.getName());
            assertEquals("A new task description", captured.getDescription());
            assertEquals("Hello {{name}}, how are you?", captured.getPromptTemplate());
            assertEquals("greeting,test", captured.getTags());
            assertEquals("Check for friendliness", captured.getEvaluationNotes());
        }

        @Test
        @DisplayName("Should create task template with minimal fields")
        void shouldCreateTaskTemplateWithMinimalFields() {
            TaskTemplateRequest minimalRequest =
                    TaskTemplateRequest.builder()
                            .name("Minimal Task")
                            .promptTemplate("Simple prompt")
                            .build();

            TaskTemplate savedTemplate =
                    TaskTemplate.builder()
                            .id(4L)
                            .name("Minimal Task")
                            .promptTemplate("Simple prompt")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(taskTemplateRepository.save(any(TaskTemplate.class))).thenReturn(savedTemplate);

            TaskTemplate result = taskService.create(minimalRequest);

            assertNotNull(result);
            assertEquals("Minimal Task", result.getName());
            verify(taskTemplateRepository).save(any(TaskTemplate.class));
        }
    }

    @Nested
    @DisplayName("update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update task template successfully")
        void shouldUpdateTaskTemplateSuccessfully() {
            TaskTemplateRequest updateRequest =
                    TaskTemplateRequest.builder()
                            .name("Updated Task")
                            .description("Updated description")
                            .promptTemplate("Updated prompt {{var}}")
                            .tags("updated,tags")
                            .evaluationNotes("Updated notes")
                            .build();

            when(taskTemplateRepository.findById(1L)).thenReturn(Optional.of(codeReviewTemplate));
            when(taskTemplateRepository.save(any(TaskTemplate.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            TaskTemplate result = taskService.update(1L, updateRequest);

            assertNotNull(result);
            assertEquals("Updated Task", result.getName());
            assertEquals("Updated description", result.getDescription());
            assertEquals("Updated prompt {{var}}", result.getPromptTemplate());
            assertEquals("updated,tags", result.getTags());
            assertEquals("Updated notes", result.getEvaluationNotes());

            verify(taskTemplateRepository).findById(1L);
            verify(taskTemplateRepository).save(any(TaskTemplate.class));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating non-existent template")
        void shouldThrowEntityNotFoundExceptionWhenUpdatingNonExistentTemplate() {
            TaskTemplateRequest updateRequest =
                    TaskTemplateRequest.builder()
                            .name("Updated Task")
                            .promptTemplate("Updated prompt")
                            .build();

            when(taskTemplateRepository.findById(999L)).thenReturn(Optional.empty());

            EntityNotFoundException exception =
                    assertThrows(
                            EntityNotFoundException.class,
                            () -> taskService.update(999L, updateRequest));

            assertEquals("Task template not found: 999", exception.getMessage());
            verify(taskTemplateRepository).findById(999L);
            verify(taskTemplateRepository, never()).save(any(TaskTemplate.class));
        }
    }

    @Nested
    @DisplayName("delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete task template successfully")
        void shouldDeleteTaskTemplateSuccessfully() {
            when(taskTemplateRepository.existsById(1L)).thenReturn(true);

            taskService.delete(1L);

            verify(taskTemplateRepository).existsById(1L);
            verify(taskTemplateRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when deleting non-existent template")
        void shouldThrowEntityNotFoundExceptionWhenDeletingNonExistentTemplate() {
            when(taskTemplateRepository.existsById(999L)).thenReturn(false);

            EntityNotFoundException exception =
                    assertThrows(EntityNotFoundException.class, () -> taskService.delete(999L));

            assertEquals("Task template not found: 999", exception.getMessage());
            verify(taskTemplateRepository).existsById(999L);
            verify(taskTemplateRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("extractVariables Tests")
    class ExtractVariablesTests {

        @Test
        @DisplayName("Should extract single variable from template")
        void shouldExtractSingleVariableFromTemplate() {
            String template = "Hello {{name}}!";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(1, variables.size());
            assertEquals("name", variables.get(0));
        }

        @Test
        @DisplayName("Should extract multiple variables from template")
        void shouldExtractMultipleVariablesFromTemplate() {
            String template = "Hello {{name}}, your score is {{score}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(2, variables.size());
            assertEquals("name", variables.get(0));
            assertEquals("score", variables.get(1));
        }

        @Test
        @DisplayName("Should remove duplicate variables and preserve order")
        void shouldRemoveDuplicateVariablesAndPreserveOrder() {
            String template = "Hello {{name}}, {{greeting}} {{name}}! Your {{score}} is {{score}}.";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(3, variables.size());
            assertEquals("name", variables.get(0));
            assertEquals("greeting", variables.get(1));
            assertEquals("score", variables.get(2));
        }

        @Test
        @DisplayName("Should return empty list for null template")
        void shouldReturnEmptyListForNullTemplate() {
            List<String> variables = taskService.extractVariables(null);

            assertTrue(variables.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for empty template")
        void shouldReturnEmptyListForEmptyTemplate() {
            List<String> variables = taskService.extractVariables("");

            assertTrue(variables.isEmpty());
        }

        @Test
        @DisplayName("Should return empty list for template without variables")
        void shouldReturnEmptyListForTemplateWithoutVariables() {
            String template = "This is a plain text template with no variables.";

            List<String> variables = taskService.extractVariables(template);

            assertTrue(variables.isEmpty());
        }

        @Test
        @DisplayName("Should handle variables with underscores")
        void shouldHandleVariablesWithUnderscores() {
            String template = "User: {{user_name}}, ID: {{user_id}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(2, variables.size());
            assertEquals("user_name", variables.get(0));
            assertEquals("user_id", variables.get(1));
        }

        @Test
        @DisplayName("Should handle variables with numbers")
        void shouldHandleVariablesWithNumbers() {
            String template = "Value1: {{value1}}, Value2: {{value2}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(2, variables.size());
            assertEquals("value1", variables.get(0));
            assertEquals("value2", variables.get(1));
        }

        @Test
        @DisplayName("Should not match invalid variable syntax")
        void shouldNotMatchInvalidVariableSyntax() {
            String template = "Invalid: {name}, {{name}, {name}}, {{ name }}";

            List<String> variables = taskService.extractVariables(template);

            assertTrue(variables.isEmpty());
        }

        @Test
        @DisplayName("Should handle complex template with multiple variables")
        void shouldHandleComplexTemplateWithMultipleVariables() {
            String template =
                    "Review the following {{language}} code:\n\n"
                            + "```{{language}}\n{{code}}\n```\n\n"
                            + "Focus on: {{focus_areas}}\n"
                            + "Style guide: {{style_guide}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(4, variables.size());
            assertEquals("language", variables.get(0));
            assertEquals("code", variables.get(1));
            assertEquals("focus_areas", variables.get(2));
            assertEquals("style_guide", variables.get(3));
        }

        @Test
        @DisplayName("Should handle adjacent variables")
        void shouldHandleAdjacentVariables() {
            String template = "{{first}}{{second}}{{third}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(3, variables.size());
            assertEquals("first", variables.get(0));
            assertEquals("second", variables.get(1));
            assertEquals("third", variables.get(2));
        }

        @Test
        @DisplayName("Should handle variables at start and end of template")
        void shouldHandleVariablesAtStartAndEndOfTemplate() {
            String template = "{{start}} middle {{end}}";

            List<String> variables = taskService.extractVariables(template);

            assertEquals(2, variables.size());
            assertEquals("start", variables.get(0));
            assertEquals("end", variables.get(1));
        }
    }

    @Nested
    @DisplayName("searchByName Tests")
    class SearchByNameTests {

        @Test
        @DisplayName("Should return matching templates for valid query")
        void shouldReturnMatchingTemplatesForValidQuery() {
            when(taskTemplateRepository.findByNameContainingIgnoreCase("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            List<TaskTemplate> results = taskService.searchByName("code");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.get(0).getName());
            verify(taskTemplateRepository).findByNameContainingIgnoreCase("code");
        }

        @Test
        @DisplayName("Should return empty list for non-matching query")
        void shouldReturnEmptyListForNonMatchingQuery() {
            when(taskTemplateRepository.findByNameContainingIgnoreCase("nonexistent"))
                    .thenReturn(Collections.emptyList());

            List<TaskTemplate> results = taskService.searchByName("nonexistent");

            assertTrue(results.isEmpty());
            verify(taskTemplateRepository).findByNameContainingIgnoreCase("nonexistent");
        }

        @Test
        @DisplayName("Should return all templates for null query")
        void shouldReturnAllTemplatesForNullQuery() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.searchByName(null);

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
            verify(taskTemplateRepository, never()).findByNameContainingIgnoreCase(any());
        }

        @Test
        @DisplayName("Should return all templates for empty query")
        void shouldReturnAllTemplatesForEmptyQuery() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.searchByName("");

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
        }

        @Test
        @DisplayName("Should return all templates for whitespace-only query")
        void shouldReturnAllTemplatesForWhitespaceOnlyQuery() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.searchByName("   ");

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
        }

        @Test
        @DisplayName("Should trim query before searching")
        void shouldTrimQueryBeforeSearching() {
            when(taskTemplateRepository.findByNameContainingIgnoreCase("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            List<TaskTemplate> results = taskService.searchByName("  code  ");

            assertEquals(1, results.size());
            verify(taskTemplateRepository).findByNameContainingIgnoreCase("code");
        }
    }

    @Nested
    @DisplayName("filterByTag Tests")
    class FilterByTagTests {

        @Test
        @DisplayName("Should return matching templates for valid tag")
        void shouldReturnMatchingTemplatesForValidTag() {
            when(taskTemplateRepository.findByTagsContaining("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            List<TaskTemplate> results = taskService.filterByTag("code");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.get(0).getName());
            verify(taskTemplateRepository).findByTagsContaining("code");
        }

        @Test
        @DisplayName("Should return empty list for non-matching tag")
        void shouldReturnEmptyListForNonMatchingTag() {
            when(taskTemplateRepository.findByTagsContaining("nonexistent"))
                    .thenReturn(Collections.emptyList());

            List<TaskTemplate> results = taskService.filterByTag("nonexistent");

            assertTrue(results.isEmpty());
            verify(taskTemplateRepository).findByTagsContaining("nonexistent");
        }

        @Test
        @DisplayName("Should return all templates for null tag")
        void shouldReturnAllTemplatesForNullTag() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.filterByTag(null);

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
            verify(taskTemplateRepository, never()).findByTagsContaining(any());
        }

        @Test
        @DisplayName("Should return all templates for empty tag")
        void shouldReturnAllTemplatesForEmptyTag() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.filterByTag("");

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
        }

        @Test
        @DisplayName("Should return all templates for whitespace-only tag")
        void shouldReturnAllTemplatesForWhitespaceOnlyTag() {
            when(taskTemplateRepository.findAll())
                    .thenReturn(Arrays.asList(codeReviewTemplate, summarisationTemplate));

            List<TaskTemplate> results = taskService.filterByTag("   ");

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findAll();
        }

        @Test
        @DisplayName("Should trim tag before filtering")
        void shouldTrimTagBeforeFiltering() {
            when(taskTemplateRepository.findByTagsContaining("code"))
                    .thenReturn(Collections.singletonList(codeReviewTemplate));

            List<TaskTemplate> results = taskService.filterByTag("  code  ");

            assertEquals(1, results.size());
            verify(taskTemplateRepository).findByTagsContaining("code");
        }

        @Test
        @DisplayName("Should return multiple templates with common tag")
        void shouldReturnMultipleTemplatesWithCommonTag() {
            when(taskTemplateRepository.findByTagsContaining("nlp"))
                    .thenReturn(Arrays.asList(summarisationTemplate, codeReviewTemplate));

            List<TaskTemplate> results = taskService.filterByTag("nlp");

            assertEquals(2, results.size());
            verify(taskTemplateRepository).findByTagsContaining("nlp");
        }
    }
}
