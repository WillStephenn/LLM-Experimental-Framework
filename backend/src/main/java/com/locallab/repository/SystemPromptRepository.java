package com.locallab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.locallab.model.SystemPrompt;

/**
 * Repository interface for {@link SystemPrompt} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with a custom query method
 * for alias-based lookup. Spring Data JPA auto-implements the derived query method.
 *
 * <p>System prompts are reusable instructions for LLMs that shape response behaviour. Each prompt
 * has a unique alias allowing quick identification and selection in the UI.
 *
 * @see SystemPrompt
 */
@Repository
public interface SystemPromptRepository extends JpaRepository<SystemPrompt, Long> {

    /**
     * Finds a system prompt by its unique alias.
     *
     * <p>The alias field has a unique constraint in the database, so this method returns an {@link
     * Optional} containing at most one system prompt. This is useful for looking up prompts by
     * their human-readable identifier.
     *
     * <p>For example, aliases might be "code-assistant", "technical-writer", or "json-formatter".
     *
     * @param alias the unique alias to search for (exact match, case-sensitive)
     * @return an {@link Optional} containing the matching system prompt, or empty if not found
     */
    Optional<SystemPrompt> findByAlias(String alias);
}
