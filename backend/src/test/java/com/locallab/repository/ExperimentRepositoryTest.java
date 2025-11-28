package com.locallab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.locallab.model.Experiment;
import com.locallab.model.TaskTemplate;
import com.locallab.model.enums.ExperimentStatus;

/**
 * Integration tests for {@link ExperimentRepository}.
 *
 * <p>Uses {@code @DataJpaTest} to configure an in-memory H2 database and auto-configure Spring Data
 * JPA repositories. Tests verify custom query methods only; standard JPA CRUD operations are not
 * tested as they are already tested by Spring Data JPA.
 *
 * @see ExperimentRepository
 * @see Experiment
 */
@DataJpaTest
class ExperimentRepositoryTest {

    @Autowired private ExperimentRepository experimentRepository;

    @Autowired private TestEntityManager entityManager;

    private TaskTemplate codeReviewTemplate;
    private TaskTemplate summarisationTemplate;
    private Experiment draftExperiment;
    private Experiment runningExperiment;
    private Experiment completedExperiment;
    private Experiment failedExperiment;

    @BeforeEach
    void setUp() {
        codeReviewTemplate =
                TaskTemplate.builder()
                        .name("Code Review Task")
                        .description("Review code for best practices")
                        .promptTemplate("Review the following code:\n\n{{code}}")
                        .tags("code,review")
                        .build();

        summarisationTemplate =
                TaskTemplate.builder()
                        .name("Summarisation Task")
                        .description("Summarise documents")
                        .promptTemplate("Summarise:\n\n{{document}}")
                        .tags("summary")
                        .build();

        entityManager.persist(codeReviewTemplate);
        entityManager.persist(summarisationTemplate);

        draftExperiment =
                Experiment.builder()
                        .name("Draft Experiment")
                        .taskTemplate(codeReviewTemplate)
                        .status(ExperimentStatus.DRAFT)
                        .config("{\"models\": [\"llama2\"]}")
                        .build();

        runningExperiment =
                Experiment.builder()
                        .name("Running Experiment")
                        .taskTemplate(codeReviewTemplate)
                        .status(ExperimentStatus.RUNNING)
                        .config("{\"models\": [\"mistral\"]}")
                        .build();

        completedExperiment =
                Experiment.builder()
                        .name("Completed Experiment")
                        .taskTemplate(summarisationTemplate)
                        .status(ExperimentStatus.COMPLETED)
                        .config("{\"models\": [\"qwen\"]}")
                        .build();

        failedExperiment =
                Experiment.builder()
                        .name("Failed Experiment")
                        .taskTemplate(summarisationTemplate)
                        .status(ExperimentStatus.FAILED)
                        .config("{\"models\": [\"phi\"]}")
                        .build();

        entityManager.persist(draftExperiment);
        entityManager.persist(runningExperiment);
        entityManager.persist(completedExperiment);
        entityManager.persist(failedExperiment);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByStatusOrderByCreatedAtDesc Tests")
    class FindByStatusOrderByCreatedAtDescTests {

        @Test
        @DisplayName("Should find experiments by DRAFT status")
        void shouldFindExperimentsByDraftStatus() {
            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.DRAFT);

            assertEquals(1, results.size());
            assertEquals("Draft Experiment", results.getFirst().getName());
            assertEquals(ExperimentStatus.DRAFT, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should find experiments by RUNNING status")
        void shouldFindExperimentsByRunningStatus() {
            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.RUNNING);

            assertEquals(1, results.size());
            assertEquals("Running Experiment", results.getFirst().getName());
            assertEquals(ExperimentStatus.RUNNING, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should find experiments by COMPLETED status")
        void shouldFindExperimentsByCompletedStatus() {
            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(
                            ExperimentStatus.COMPLETED);

            assertEquals(1, results.size());
            assertEquals("Completed Experiment", results.getFirst().getName());
            assertEquals(ExperimentStatus.COMPLETED, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should find experiments by FAILED status")
        void shouldFindExperimentsByFailedStatus() {
            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.FAILED);

            assertEquals(1, results.size());
            assertEquals("Failed Experiment", results.getFirst().getName());
            assertEquals(ExperimentStatus.FAILED, results.getFirst().getStatus());
        }

        @Test
        @DisplayName("Should return empty list for PAUSED status when none exist")
        void shouldReturnEmptyListForPausedStatusWhenNoneExist() {
            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.PAUSED);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should find multiple experiments with same status")
        void shouldFindMultipleExperimentsWithSameStatus() {
            Experiment anotherDraftExperiment =
                    Experiment.builder()
                            .name("Another Draft Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"codellama\"]}")
                            .build();
            entityManager.persist(anotherDraftExperiment);
            entityManager.flush();

            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.DRAFT);

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(e -> e.getStatus() == ExperimentStatus.DRAFT));
        }

        @Test
        @DisplayName("Should return results ordered by creation date descending")
        void shouldReturnResultsOrderedByCreationDateDescending() {
            Experiment newerDraftExperiment =
                    Experiment.builder()
                            .name("Newer Draft Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"codellama\"]}")
                            .build();
            entityManager.persist(newerDraftExperiment);
            entityManager.flush();

            List<Experiment> results =
                    experimentRepository.findByStatusOrderByCreatedAtDesc(ExperimentStatus.DRAFT);

            assertEquals(2, results.size());
            assertTrue(
                    results.getFirst().getCreatedAt().isAfter(results.get(1).getCreatedAt())
                            || results.getFirst()
                                    .getCreatedAt()
                                    .isEqual(results.get(1).getCreatedAt()));
        }
    }

    @Nested
    @DisplayName("findByTaskTemplateIdOrderByCreatedAtDesc Tests")
    class FindByTaskTemplateIdOrderByCreatedAtDescTests {

        @Test
        @DisplayName("Should find experiments by task template ID")
        void shouldFindExperimentsByTaskTemplateId() {
            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(
                            codeReviewTemplate.getId());

            assertEquals(2, results.size());
            assertTrue(
                    results.stream()
                            .allMatch(
                                    e ->
                                            e.getTaskTemplate()
                                                    .getId()
                                                    .equals(codeReviewTemplate.getId())));
        }

        @Test
        @DisplayName("Should find experiments for different task template")
        void shouldFindExperimentsForDifferentTaskTemplate() {
            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(
                            summarisationTemplate.getId());

            assertEquals(2, results.size());
            assertTrue(
                    results.stream()
                            .allMatch(
                                    e ->
                                            e.getTaskTemplate()
                                                    .getId()
                                                    .equals(summarisationTemplate.getId())));
        }

        @Test
        @DisplayName("Should return empty list for non-existent task template ID")
        void shouldReturnEmptyListForNonExistentTaskTemplateId() {
            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(99999L);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should return results ordered by creation date descending")
        void shouldReturnResultsOrderedByCreationDateDescending() {
            Experiment newerExperiment =
                    Experiment.builder()
                            .name("Newer Code Review Experiment")
                            .taskTemplate(codeReviewTemplate)
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"deepseek\"]}")
                            .build();
            entityManager.persist(newerExperiment);
            entityManager.flush();

            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(
                            codeReviewTemplate.getId());

            assertEquals(3, results.size());
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(
                        results.get(i).getCreatedAt().isAfter(results.get(i + 1).getCreatedAt())
                                || results.get(i)
                                        .getCreatedAt()
                                        .isEqual(results.get(i + 1).getCreatedAt()));
            }
        }

        @Test
        @DisplayName("Should include experiments with different statuses for same task template")
        void shouldIncludeExperimentsWithDifferentStatusesForSameTaskTemplate() {
            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(
                            codeReviewTemplate.getId());

            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.DRAFT));
            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.RUNNING));
        }

        @Test
        @DisplayName("Should not include experiments without task template")
        void shouldNotIncludeExperimentsWithoutTaskTemplate() {
            Experiment experimentWithoutTemplate =
                    Experiment.builder()
                            .name("No Template Experiment")
                            .taskTemplate(null)
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"llama2\"]}")
                            .build();
            entityManager.persist(experimentWithoutTemplate);
            entityManager.flush();

            List<Experiment> results =
                    experimentRepository.findByTaskTemplateIdOrderByCreatedAtDesc(
                            codeReviewTemplate.getId());

            assertEquals(2, results.size());
            assertTrue(
                    results.stream().noneMatch(e -> e.getName().equals("No Template Experiment")));
        }
    }

    @Nested
    @DisplayName("findAllByOrderByCreatedAtDesc Tests")
    class FindAllByOrderByCreatedAtDescTests {

        @Test
        @DisplayName("Should return all experiments")
        void shouldReturnAllExperiments() {
            List<Experiment> results = experimentRepository.findAllByOrderByCreatedAtDesc();

            assertEquals(4, results.size());
        }

        @Test
        @DisplayName("Should return results ordered by creation date descending")
        void shouldReturnResultsOrderedByCreationDateDescending() {
            Experiment newestExperiment =
                    Experiment.builder()
                            .name("Newest Experiment")
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"llama3\"]}")
                            .build();
            entityManager.persist(newestExperiment);
            entityManager.flush();

            List<Experiment> results = experimentRepository.findAllByOrderByCreatedAtDesc();

            assertEquals(5, results.size());
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(
                        results.get(i).getCreatedAt().isAfter(results.get(i + 1).getCreatedAt())
                                || results.get(i)
                                        .getCreatedAt()
                                        .isEqual(results.get(i + 1).getCreatedAt()));
            }
        }

        @Test
        @DisplayName("Should return empty list when no experiments exist")
        void shouldReturnEmptyListWhenNoExperimentsExist() {
            experimentRepository.deleteAll();
            entityManager.flush();

            List<Experiment> results = experimentRepository.findAllByOrderByCreatedAtDesc();

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should include experiments regardless of status")
        void shouldIncludeExperimentsRegardlessOfStatus() {
            List<Experiment> results = experimentRepository.findAllByOrderByCreatedAtDesc();

            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.DRAFT));
            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.RUNNING));
            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.COMPLETED));
            assertTrue(results.stream().anyMatch(e -> e.getStatus() == ExperimentStatus.FAILED));
        }

        @Test
        @DisplayName("Should include experiments with and without task templates")
        void shouldIncludeExperimentsWithAndWithoutTaskTemplates() {
            Experiment experimentWithoutTemplate =
                    Experiment.builder()
                            .name("No Template Experiment")
                            .taskTemplate(null)
                            .status(ExperimentStatus.DRAFT)
                            .config("{\"models\": [\"llama2\"]}")
                            .build();
            entityManager.persist(experimentWithoutTemplate);
            entityManager.flush();

            List<Experiment> results = experimentRepository.findAllByOrderByCreatedAtDesc();

            assertEquals(5, results.size());
            assertTrue(
                    results.stream().anyMatch(e -> e.getName().equals("No Template Experiment")));
            assertTrue(results.stream().anyMatch(e -> e.getTaskTemplate() != null));
        }
    }
}
