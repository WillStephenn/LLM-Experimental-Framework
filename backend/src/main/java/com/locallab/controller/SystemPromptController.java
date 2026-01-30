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
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.request.SystemPromptRequest;
import com.locallab.dto.response.SystemPromptResponse;
import com.locallab.model.SystemPrompt;
import com.locallab.service.SystemPromptService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for managing SystemPrompt resources.
 *
 * <p>This controller provides full CRUD operations for system prompts, following the API contract
 * defined in the project specification. All endpoints return consistent response structures and
 * appropriate HTTP status codes.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/system-prompts} - List all system prompts
 *   <li>{@code GET /api/system-prompts/{id}} - Get a single system prompt by ID
 *   <li>{@code POST /api/system-prompts} - Create a new system prompt
 *   <li>{@code PUT /api/system-prompts/{id}} - Update an existing system prompt
 *   <li>{@code DELETE /api/system-prompts/{id}} - Delete a system prompt
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see SystemPromptService
 * @see SystemPromptRequest
 * @see SystemPromptResponse
 */
@RestController
@RequestMapping("/api/system-prompts")
@RequiredArgsConstructor
public class SystemPromptController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemPromptController.class);

    private final SystemPromptService systemPromptService;

    /**
     * Retrieves all system prompts.
     *
     * @return a list of all system prompts, or an empty list if none exist
     */
    @GetMapping
    public ResponseEntity<List<SystemPromptResponse>> getAllSystemPrompts() {

        LOGGER.debug("Received request to list all system prompts");

        List<SystemPrompt> prompts = systemPromptService.findAll();

        List<SystemPromptResponse> response =
                prompts.stream().map(SystemPromptResponse::fromEntity).toList();

        LOGGER.debug("Returning {} system prompts", response.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single system prompt by its identifier.
     *
     * @param id the unique identifier of the system prompt
     * @return the system prompt with the specified ID
     * @throws jakarta.persistence.EntityNotFoundException if no prompt exists with the given ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SystemPromptResponse> getSystemPromptById(@PathVariable Long id) {

        LOGGER.debug("Received request to get system prompt with id: {}", id);

        SystemPrompt prompt = systemPromptService.findById(id);
        SystemPromptResponse response = SystemPromptResponse.fromEntity(prompt);

        LOGGER.debug("Returning system prompt: {}", prompt.getAlias());
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new system prompt.
     *
     * <p>The request body is validated against the constraints defined in {@link
     * SystemPromptRequest}. A 400 Bad Request response is returned if validation fails. A 409
     * Conflict response is returned if a system prompt with the same alias already exists.
     *
     * @param request the system prompt creation request containing all required fields
     * @return the newly created system prompt with a 201 Created status
     */
    @PostMapping
    public ResponseEntity<SystemPromptResponse> createSystemPrompt(
            @Valid @RequestBody SystemPromptRequest request) {

        LOGGER.info("Received request to create system prompt: {}", request.getAlias());

        SystemPrompt createdPrompt = systemPromptService.create(request);
        SystemPromptResponse response = SystemPromptResponse.fromEntity(createdPrompt);

        LOGGER.info("Created system prompt with id: {}", createdPrompt.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing system prompt.
     *
     * <p>The request body is validated against the constraints defined in {@link
     * SystemPromptRequest}. A 400 Bad Request response is returned if validation fails. A 409
     * Conflict response is returned if the new alias conflicts with another system prompt.
     *
     * @param id the identifier of the system prompt to update
     * @param request the system prompt update request containing updated fields
     * @return the updated system prompt
     * @throws jakarta.persistence.EntityNotFoundException if no prompt exists with the given ID
     */
    @PutMapping("/{id}")
    public ResponseEntity<SystemPromptResponse> updateSystemPrompt(
            @PathVariable Long id, @Valid @RequestBody SystemPromptRequest request) {

        LOGGER.info("Received request to update system prompt with id: {}", id);

        SystemPrompt updatedPrompt = systemPromptService.update(id, request);
        SystemPromptResponse response = SystemPromptResponse.fromEntity(updatedPrompt);

        LOGGER.info("Updated system prompt: {}", updatedPrompt.getAlias());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a system prompt by its identifier.
     *
     * @param id the identifier of the system prompt to delete
     * @return a 204 No Content response on successful deletion
     * @throws jakarta.persistence.EntityNotFoundException if no prompt exists with the given ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSystemPrompt(@PathVariable Long id) {

        LOGGER.info("Received request to delete system prompt with id: {}", id);

        systemPromptService.delete(id);

        LOGGER.info("Deleted system prompt with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
