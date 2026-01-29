package com.locallab.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.request.TaskTemplateRequest;
import com.locallab.dto.response.TaskTemplateResponse;
import com.locallab.model.TaskTemplate;
import com.locallab.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing TaskTemplate resources.
 *
 * <p>This controller provides full CRUD operations for task templates, following the API contract
 * defined in the project specification. All endpoints return consistent response structures and
 * appropriate HTTP status codes.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/tasks} - List all task templates with optional search/filter
 *   <li>{@code GET /api/tasks/{id}} - Get a single task template by ID
 *   <li>{@code POST /api/tasks} - Create a new task template
 *   <li>{@code PUT /api/tasks/{id}} - Update an existing task template
 *   <li>{@code DELETE /api/tasks/{id}} - Delete a task template
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see TaskService
 * @see TaskTemplateRequest
 * @see TaskTemplateResponse
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    /**
     * Retrieves all task templates with optional filtering.
     *
     * <p>When both {@code search} and {@code tag} parameters are provided, the search parameter
     * takes precedence. When neither is provided, all templates are returned.
     *
     * @param search optional search string for case-insensitive partial name matching
     * @param tag optional tag to filter by (partial match on tags field)
     * @return a list of task templates matching the criteria, or all templates if no filters
     *     provided
     */
    @GetMapping
    public ResponseEntity<List<TaskTemplateResponse>> getAllTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag) {

        LOGGER.debug(
                "Received request to list task templates - search: '{}', tag: '{}'", search, tag);

        List<TaskTemplate> templates;

        if (search != null && !search.trim().isEmpty()) {
            templates = taskService.searchByName(search);
        } else if (tag != null && !tag.trim().isEmpty()) {
            templates = taskService.filterByTag(tag);
        } else {
            templates = taskService.findAll();
        }

        List<TaskTemplateResponse> response =
                templates.stream().map(TaskTemplateResponse::fromEntity).toList();

        LOGGER.debug("Returning {} task templates", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single task template by its identifier.
     *
     * @param id the unique identifier of the task template
     * @return the task template with the specified ID
     * @throws jakarta.persistence.EntityNotFoundException if no template exists with the given ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskTemplateResponse> getTaskById(@PathVariable Long id) {

        LOGGER.debug("Received request to get task template with id: {}", id);

        TaskTemplate template = taskService.findById(id);
        TaskTemplateResponse response = TaskTemplateResponse.fromEntity(template);

        LOGGER.debug("Returning task template: {}", template.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new task template.
     *
     * <p>The request body is validated against the constraints defined in {@link
     * TaskTemplateRequest}. A 400 Bad Request response is returned if validation fails.
     *
     * @param request the task template creation request containing all required fields
     * @return the newly created task template with a 201 Created status
     */
    @PostMapping
    public ResponseEntity<TaskTemplateResponse> createTask(
            @Valid @RequestBody TaskTemplateRequest request) {

        LOGGER.info("Received request to create task template: {}", request.getName());

        TaskTemplate createdTemplate = taskService.create(request);
        TaskTemplateResponse response = TaskTemplateResponse.fromEntity(createdTemplate);

        LOGGER.info("Created task template with id: {}", createdTemplate.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing task template.
     *
     * <p>The request body is validated against the constraints defined in {@link
     * TaskTemplateRequest}. A 400 Bad Request response is returned if validation fails.
     *
     * @param id the identifier of the task template to update
     * @param request the task template update request containing updated fields
     * @return the updated task template
     * @throws jakarta.persistence.EntityNotFoundException if no template exists with the given ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskTemplateResponse> updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskTemplateRequest request) {

        LOGGER.info("Received request to update task template with id: {}", id);

        TaskTemplate updatedTemplate = taskService.update(id, request);
        TaskTemplateResponse response = TaskTemplateResponse.fromEntity(updatedTemplate);

        LOGGER.info("Updated task template: {}", updatedTemplate.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a task template by its identifier.
     *
     * <p>This operation will cascade delete all associated experiments due to the entity
     * relationship configuration.
     *
     * @param id the identifier of the task template to delete
     * @return a 204 No Content response on successful deletion
     * @throws jakarta.persistence.EntityNotFoundException if no template exists with the given ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {

        LOGGER.info("Received request to delete task template with id: {}", id);

        taskService.delete(id);

        LOGGER.info("Deleted task template with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
