package com.locallab.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locallab.dto.request.TaskTemplateRequest;
import com.locallab.model.TaskTemplate;
import com.locallab.repository.TaskTemplateRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer for managing TaskTemplate entities.
 *
 * <p>Provides CRUD operations, variable extraction from prompt templates, and search/filter
 * functionality for task templates. All operations are transactional with read-only optimisation
 * for query methods.
 *
 * <h3>Variable Extraction:</h3>
 *
 * <p>The service supports extracting variable placeholders from prompt templates using the syntax
 * {@code {{variableName}}}. Variables are extracted using regex and returned in order of appearance
 * with duplicates removed.
 *
 * <h3>Exception Handling:</h3>
 *
 * <ul>
 *   <li>{@link EntityNotFoundException} - Thrown when a requested template is not found
 *   <li>{@link IllegalArgumentException} - Thrown for validation failures
 * </ul>
 *
 * @author William Stephen
 * @see TaskTemplate
 * @see TaskTemplateRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskService.class);

    /**
     * Regex pattern for extracting variable placeholders from prompt templates.
     *
     * <p>Matches variables in the format {@code {{variableName}}} where variable names consist of
     * word characters (letters, digits, underscores).
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final TaskTemplateRepository taskTemplateRepository;

    /**
     * Retrieves all task templates.
     *
     * @return a list of all task templates, or an empty list if none exist
     */
    public List<TaskTemplate> findAll() {
        LOGGER.debug("Retrieving all task templates");
        List<TaskTemplate> templates = taskTemplateRepository.findAll();
        LOGGER.debug("Found {} task templates", templates.size());
        return templates;
    }

    /**
     * Retrieves a task template by its identifier.
     *
     * @param id the unique identifier of the task template
     * @return the task template with the specified identifier
     * @throws EntityNotFoundException if no task template exists with the given identifier
     */
    public TaskTemplate findById(Long id) {
        LOGGER.debug("Retrieving task template with id: {}", id);
        return taskTemplateRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("Task template not found with id: {}", id);
                            return new EntityNotFoundException("Task template not found: " + id);
                        });
    }

    /**
     * Creates a new task template from the provided request.
     *
     * @param request the task template creation request containing all required fields
     * @return the newly created task template with generated identifier
     */
    @Transactional
    public TaskTemplate create(TaskTemplateRequest request) {
        LOGGER.info("Creating new task template with name: {}", request.getName());

        TaskTemplate taskTemplate =
                TaskTemplate.builder()
                        .name(request.getName())
                        .description(request.getDescription())
                        .promptTemplate(request.getPromptTemplate())
                        .tags(request.getTags())
                        .evaluationNotes(request.getEvaluationNotes())
                        .build();

        TaskTemplate savedTemplate = taskTemplateRepository.save(taskTemplate);
        LOGGER.info("Created task template with id: {}", savedTemplate.getId());
        return savedTemplate;
    }

    /**
     * Updates an existing task template with the provided request data.
     *
     * @param id the identifier of the task template to update
     * @param request the task template update request containing updated fields
     * @return the updated task template
     * @throws EntityNotFoundException if no task template exists with the given identifier
     */
    @Transactional
    public TaskTemplate update(Long id, TaskTemplateRequest request) {
        LOGGER.info("Updating task template with id: {}", id);

        TaskTemplate existingTemplate = findById(id);

        existingTemplate.setName(request.getName());
        existingTemplate.setDescription(request.getDescription());
        existingTemplate.setPromptTemplate(request.getPromptTemplate());
        existingTemplate.setTags(request.getTags());
        existingTemplate.setEvaluationNotes(request.getEvaluationNotes());

        TaskTemplate updatedTemplate = taskTemplateRepository.save(existingTemplate);
        LOGGER.info("Updated task template with id: {}", updatedTemplate.getId());
        return updatedTemplate;
    }

    /**
     * Deletes a task template by its identifier.
     *
     * <p>This operation will cascade delete all associated experiments due to the entity
     * relationship configuration.
     *
     * @param id the identifier of the task template to delete
     * @throws EntityNotFoundException if no task template exists with the given identifier
     */
    @Transactional
    public void delete(Long id) {
        LOGGER.info("Deleting task template with id: {}", id);

        if (!taskTemplateRepository.existsById(id)) {
            LOGGER.warn("Cannot delete task template - not found with id: {}", id);
            throw new EntityNotFoundException("Task template not found: " + id);
        }

        taskTemplateRepository.deleteById(id);
        LOGGER.info("Deleted task template with id: {}", id);
    }

    /**
     * Extracts variable placeholders from a prompt template.
     *
     * <p>Variables use the syntax: {@code {{variableName}}}. Only word characters (letters, digits,
     * underscores) are valid in variable names.
     *
     * <p>Example:
     *
     * <pre>{@code
     * String template = "Hello {{name}}, your score is {{score}}. Hello {{name}}!";
     * List<String> vars = taskService.extractVariables(template);
     * // Returns: ["name", "score"] (duplicates removed, order preserved)
     * }</pre>
     *
     * @param promptTemplate the template string to parse for variables
     * @return a list of unique variable names (without braces) in order of appearance, or an empty
     *     list if the template is null, empty, or contains no variables
     */
    public List<String> extractVariables(String promptTemplate) {
        LOGGER.debug("Extracting variables from prompt template");

        if (promptTemplate == null || promptTemplate.isEmpty()) {
            LOGGER.debug("Prompt template is null or empty, returning empty list");
            return new ArrayList<>();
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(promptTemplate);
        Set<String> variables = new LinkedHashSet<>();

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        LOGGER.debug("Extracted {} unique variables from template", variables.size());
        return new ArrayList<>(variables);
    }

    /**
     * Searches for task templates by name using case-insensitive partial matching.
     *
     * @param query the search string to match against template names
     * @return a list of task templates with names containing the query string, or an empty list if
     *     none found
     */
    public List<TaskTemplate> searchByName(String query) {
        LOGGER.debug("Searching task templates by name: {}", query);

        if (query == null || query.trim().isEmpty()) {
            LOGGER.debug("Search query is empty, returning all templates");
            return findAll();
        }

        List<TaskTemplate> results =
                taskTemplateRepository.findByNameContainingIgnoreCase(query.trim());
        LOGGER.debug("Found {} task templates matching query: {}", results.size(), query);
        return results;
    }

    /**
     * Filters task templates by tag.
     *
     * <p>Performs a partial match on the tags field, so searching for "code" will match templates
     * with tags containing "code" anywhere (e.g., "code,review" or "encode").
     *
     * @param tag the tag to filter by
     * @return a list of task templates containing the specified tag, or an empty list if none found
     */
    public List<TaskTemplate> filterByTag(String tag) {
        LOGGER.debug("Filtering task templates by tag: {}", tag);

        if (tag == null || tag.trim().isEmpty()) {
            LOGGER.debug("Tag is empty, returning all templates");
            return findAll();
        }

        List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining(tag.trim());
        LOGGER.debug("Found {} task templates with tag: {}", results.size(), tag);
        return results;
    }
}
