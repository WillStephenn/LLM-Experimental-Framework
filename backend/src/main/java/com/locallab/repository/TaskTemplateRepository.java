package com.locallab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.locallab.model.TaskTemplate;

/**
 * Repository interface for {@link TaskTemplate} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with custom query methods
 * for tag filtering and name-based search. Spring Data JPA auto-implements the derived query
 * methods.
 *
 * <p>The custom query methods enable:
 *
 * <ul>
 *   <li><strong>Tag filtering:</strong> Find templates that contain a specific tag within their
 *       comma-separated tags field using a {@code LIKE %tag%} query.
 *   <li><strong>Name search:</strong> Find templates by partial, case-insensitive name match for
 *       search functionality in the UI.
 * </ul>
 *
 * @see TaskTemplate
 */
@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    /**
     * Finds all task templates whose tags field contains the specified tag.
     *
     * <p>Performs a {@code LIKE %tag%} query on the tags field. This will match any template where
     * the tag appears anywhere in the comma-separated tags string. Note that this may produce false
     * positives for partial matches (e.g., searching for "code" will match "encode").
     *
     * <p>For example, a template with tags "code,review,quality" would be returned when searching
     * for "review", "code", or "quality".
     *
     * @param tag the tag substring to search for within the tags field
     * @return a list of task templates containing the tag, or an empty list if none found
     */
    List<TaskTemplate> findByTagsContaining(String tag);

    /**
     * Finds all task templates whose name contains the specified search string (case-insensitive).
     *
     * <p>Performs a case-insensitive partial match on the name field. This is intended for search
     * functionality where users type partial names to filter templates.
     *
     * <p>For example, searching for "code" would match templates named "Code Review Task",
     * "Codebase Analysis", or "Source Code Checker".
     *
     * @param name the search string to match against template names (case-insensitive)
     * @return a list of task templates with matching names, or an empty list if none found
     */
    List<TaskTemplate> findByNameContainingIgnoreCase(String name);
}
