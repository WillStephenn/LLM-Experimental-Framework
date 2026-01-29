package com.locallab.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.locallab.dto.request.SystemPromptRequest;
import com.locallab.model.SystemPrompt;
import com.locallab.repository.SystemPromptRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service class for managing system prompts.
 *
 * <p>Provides CRUD operations for {@link SystemPrompt} entities with alias uniqueness validation.
 * System prompts are reusable instructions for LLMs that shape response behaviour.
 *
 * <p>All read operations are transactional with read-only optimisation. Write operations use full
 * transactional support to ensure data consistency.
 *
 * @author William Stephen
 * @see SystemPrompt
 * @see SystemPromptRepository
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemPromptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemPromptService.class);

    private final SystemPromptRepository systemPromptRepository;

    /**
     * Retrieves all system prompts.
     *
     * @return list of all system prompts, may be empty
     */
    public List<SystemPrompt> findAll() {
        LOGGER.debug("Retrieving all system prompts");
        List<SystemPrompt> prompts = systemPromptRepository.findAll();
        LOGGER.info("Retrieved {} system prompts", prompts.size());
        return prompts;
    }

    /**
     * Finds a system prompt by its unique identifier.
     *
     * @param id the system prompt ID
     * @return the system prompt
     * @throws EntityNotFoundException if no system prompt exists with the given ID
     */
    public SystemPrompt findById(Long id) {
        LOGGER.debug("Finding system prompt by ID: {}", id);
        return systemPromptRepository
                .findById(id)
                .orElseThrow(
                        () -> {
                            LOGGER.warn("System prompt not found with ID: {}", id);
                            return new EntityNotFoundException("System prompt not found: " + id);
                        });
    }

    /**
     * Finds a system prompt by its unique alias.
     *
     * @param alias the unique alias to search for (exact match, case-sensitive)
     * @return an {@link Optional} containing the matching system prompt, or empty if not found
     */
    public Optional<SystemPrompt> findByAlias(String alias) {
        LOGGER.debug("Finding system prompt by alias: {}", alias);
        Optional<SystemPrompt> result = systemPromptRepository.findByAlias(alias);
        if (result.isPresent()) {
            LOGGER.debug("Found system prompt with alias: {}", alias);
        } else {
            LOGGER.debug("No system prompt found with alias: {}", alias);
        }
        return result;
    }

    /**
     * Creates a new system prompt.
     *
     * @param request the system prompt data
     * @return the created system prompt with generated ID and timestamp
     * @throws IllegalStateException if a system prompt with the same alias already exists
     */
    @Transactional
    public SystemPrompt create(SystemPromptRequest request) {
        LOGGER.debug("Creating system prompt with alias: {}", request.getAlias());

        if (systemPromptRepository.findByAlias(request.getAlias()).isPresent()) {
            LOGGER.warn(
                    "Attempted to create duplicate system prompt with alias: {}",
                    request.getAlias());
            throw new IllegalStateException(
                    "System prompt with alias '" + request.getAlias() + "' already exists");
        }

        SystemPrompt systemPrompt =
                SystemPrompt.builder()
                        .alias(request.getAlias())
                        .content(request.getContent())
                        .build();

        SystemPrompt saved = systemPromptRepository.save(systemPrompt);
        LOGGER.info(
                "Created system prompt with ID: {} and alias: {}", saved.getId(), saved.getAlias());
        return saved;
    }

    /**
     * Updates an existing system prompt.
     *
     * @param id the system prompt ID
     * @param request the updated data
     * @return the updated system prompt
     * @throws EntityNotFoundException if no system prompt exists with the given ID
     * @throws IllegalStateException if the new alias conflicts with another system prompt
     */
    @Transactional
    public SystemPrompt update(Long id, SystemPromptRequest request) {
        LOGGER.debug("Updating system prompt with ID: {}", id);

        SystemPrompt existing = findById(id);

        if (!existing.getAlias().equals(request.getAlias())) {
            if (systemPromptRepository.findByAlias(request.getAlias()).isPresent()) {
                LOGGER.warn(
                        "Attempted to update system prompt ID: {} with duplicate alias: {}",
                        id,
                        request.getAlias());
                throw new IllegalStateException(
                        "System prompt with alias '" + request.getAlias() + "' already exists");
            }
        }

        existing.setAlias(request.getAlias());
        existing.setContent(request.getContent());

        SystemPrompt saved = systemPromptRepository.save(existing);
        LOGGER.info(
                "Updated system prompt with ID: {} and alias: {}", saved.getId(), saved.getAlias());
        return saved;
    }

    /**
     * Deletes a system prompt by its unique identifier.
     *
     * @param id the system prompt ID
     * @throws EntityNotFoundException if no system prompt exists with the given ID
     */
    @Transactional
    public void delete(Long id) {
        LOGGER.debug("Deleting system prompt with ID: {}", id);

        SystemPrompt existing = findById(id);
        systemPromptRepository.delete(existing);

        LOGGER.info("Deleted system prompt with ID: {} and alias: {}", id, existing.getAlias());
    }
}
