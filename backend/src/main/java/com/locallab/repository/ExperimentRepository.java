package com.locallab.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.locallab.model.Experiment;
import com.locallab.model.enums.ExperimentStatus;

/**
 * Repository interface for {@link Experiment} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with custom query methods
 * for status filtering, task template association, and ordered retrieval. Spring Data JPA
 * auto-implements the derived query methods.
 *
 * <p>The custom query methods enable:
 *
 * <ul>
 *   <li><strong>Status filtering:</strong> Find experiments by their lifecycle status (e.g., DRAFT,
 *       RUNNING, COMPLETED), ordered by creation date descending (newest first).
 *   <li><strong>Task template filtering:</strong> Find experiments associated with a specific task
 *       template, ordered by creation date descending.
 *   <li><strong>Ordered retrieval:</strong> Retrieve all experiments ordered by creation date
 *       descending to display newest experiments first.
 * </ul>
 *
 * @see Experiment
 * @see ExperimentStatus
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, Long> {

    /**
     * Finds all experiments with the specified status, ordered by creation date descending.
     *
     * <p>This method enables filtering experiments by their lifecycle state, such as finding all
     * running experiments or all completed experiments. Results are ordered with the most recently
     * created experiments first.
     *
     * <p>For example, to find all experiments currently in progress:
     *
     * <pre>{@code
     * List<Experiment> runningExperiments =
     *     experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.RUNNING);
     * }</pre>
     *
     * @param status the experiment status to filter by
     * @return a list of experiments with the specified status, ordered by creation date descending,
     *     or an empty list if none found
     */
    List<Experiment> findByStatusOrderByCreatedAtDesc(ExperimentStatus status);

    /**
     * Finds all experiments associated with the specified task template, ordered by creation date
     * descending.
     *
     * <p>This method enables retrieval of all experiments that use a particular task template. This
     * is useful for viewing the history of experiments run against a specific task. Results are
     * ordered with the most recently created experiments first.
     *
     * <p>For example, to find all experiments for a code review task:
     *
     * <pre>{@code
     * List<Experiment> experiments =
     *     experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(codeReviewTask.getId());
     * }</pre>
     *
     * @param taskTemplateId the ID of the task template to filter by
     * @return a list of experiments using the specified task template, ordered by creation date
     *     descending, or an empty list if none found
     */
    List<Experiment> findByTaskTemplateIdOrderByCreatedAtDesc(Long taskTemplateId);

    /**
     * Retrieves all experiments ordered by creation date descending.
     *
     * <p>This method provides a consistent ordering for experiment listings, ensuring the most
     * recently created experiments appear first. This is the recommended method for retrieving all
     * experiments when displaying them to users.
     *
     * <p>For example, to populate an experiment list view:
     *
     * <pre>{@code
     * List<Experiment> allExperiments = experimentRepository.findAllByOrderByCreatedAtDesc();
     * }</pre>
     *
     * @return a list of all experiments ordered by creation date descending, or an empty list if no
     *     experiments exist
     */
    List<Experiment> findAllByOrderByCreatedAtDesc();
}
