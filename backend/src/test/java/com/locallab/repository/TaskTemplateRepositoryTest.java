package com.locallab.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.locallab.model.TaskTemplate;

/**
 * Integration tests for {@link TaskTemplateRepository}.
 *
 * <p>Uses {@code @DataJpaTest} to configure an in-memory H2 database and autoconfigure Spring Data
 * JPA repositories. Tests verify custom query methods only; standard JPA CRUD operations are not
 * tested as they are already tested by Spring Data JPA.
 *
 * @see TaskTemplateRepository
 * @see TaskTemplate
 */
@DataJpaTest
class TaskTemplateRepositoryTest {

    @Autowired private TaskTemplateRepository taskTemplateRepository;

    @Autowired private TestEntityManager entityManager;

    private TaskTemplate codeReviewTemplate;
    private TaskTemplate summarisationTemplate;
    private TaskTemplate translationTemplate;

    @BeforeEach
    void setUp() {
        codeReviewTemplate =
                TaskTemplate.builder()
                        .name("Code Review Task")
                        .description("Review code for best practices")
                        .promptTemplate("Review the following code:\n\n{{code}}")
                        .tags("code,review,quality")
                        .evaluationNotes("Look for correctness and style")
                        .build();

        summarisationTemplate =
                TaskTemplate.builder()
                        .name("Document Summarisation")
                        .description("Summarise documents concisely")
                        .promptTemplate("Summarise the following:\n\n{{document}}")
                        .tags("summary,document,nlp")
                        .evaluationNotes("Check for completeness and brevity")
                        .build();

        translationTemplate =
                TaskTemplate.builder()
                        .name("Translation Task")
                        .description("Translate text between languages")
                        .promptTemplate("Translate to {{language}}:\n\n{{text}}")
                        .tags("translation,language,nlp")
                        .evaluationNotes("Verify accuracy and fluency")
                        .build();

        entityManager.persist(codeReviewTemplate);
        entityManager.persist(summarisationTemplate);
        entityManager.persist(translationTemplate);
        entityManager.flush();
    }

    @Nested
    @DisplayName("findByTagsContaining Tests")
    class FindByTagsContainingTests {

        @Test
        @DisplayName("Should find templates by exact tag match")
        void shouldFindTemplatesByExactTagMatch() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("code");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find templates containing tag in multi-tag field")
        void shouldFindTemplatesContainingTagInMultiTagField() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("review");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find multiple templates with common tag")
        void shouldFindMultipleTemplatesWithCommonTag() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("nlp");

            assertEquals(2, results.size());
            assertTrue(
                    results.stream().anyMatch(t -> t.getName().equals("Document Summarisation")));
            assertTrue(results.stream().anyMatch(t -> t.getName().equals("Translation Task")));
        }

        @Test
        @DisplayName("Should return empty list for non-existent tag")
        void shouldReturnEmptyListForNonExistentTag() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("nonexistent");

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should find templates by partial tag match")
        void shouldFindTemplatesByPartialTagMatch() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("lang");

            assertEquals(1, results.size());
            assertEquals("Translation Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should be case-sensitive for tag search")
        void shouldBeCaseSensitiveForTagSearch() {
            List<TaskTemplate> resultsLowerCase =
                    taskTemplateRepository.findByTagsContaining("code");
            List<TaskTemplate> resultsUpperCase =
                    taskTemplateRepository.findByTagsContaining("CODE");

            assertEquals(1, resultsLowerCase.size());
            assertTrue(resultsUpperCase.isEmpty());
        }

        @Test
        @DisplayName("Should handle null tags field gracefully")
        void shouldHandleNullTagsFieldGracefully() {
            TaskTemplate templateWithNullTags =
                    TaskTemplate.builder()
                            .name("No Tags Template")
                            .promptTemplate("Test prompt")
                            .tags(null)
                            .build();
            entityManager.persist(templateWithNullTags);
            entityManager.flush();

            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("any");

            assertFalse(results.stream().anyMatch(t -> t.getName().equals("No Tags Template")));
        }

        @Test
        @DisplayName("Should find template by first tag in list")
        void shouldFindTemplateByFirstTagInList() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("summary");

            assertEquals(1, results.size());
            assertEquals("Document Summarisation", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find template by last tag in list")
        void shouldFindTemplateByLastTagInList() {
            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("quality");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should handle special characters in tags")
        void shouldHandleSpecialCharactersInTags() {
            TaskTemplate specialTagsTemplate =
                    TaskTemplate.builder()
                            .name("Special Tags Template")
                            .promptTemplate("Test prompt")
                            .tags("test-tag,tag_underscore,tag.dot")
                            .build();
            entityManager.persist(specialTagsTemplate);
            entityManager.flush();

            List<TaskTemplate> results = taskTemplateRepository.findByTagsContaining("test-tag");

            assertEquals(1, results.size());
            assertEquals("Special Tags Template", results.getFirst().getName());
        }
    }

    @Nested
    @DisplayName("findByNameContainingIgnoreCase Tests")
    class FindByNameContainingIgnoreCaseTests {

        @Test
        @DisplayName("Should find templates by exact name match")
        void shouldFindTemplatesByExactNameMatch() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("Code Review Task");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find templates by partial name match")
        void shouldFindTemplatesByPartialNameMatch() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("Task");

            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(t -> t.getName().equals("Code Review Task")));
            assertTrue(results.stream().anyMatch(t -> t.getName().equals("Translation Task")));
        }

        @Test
        @DisplayName("Should find templates case-insensitively with lowercase search")
        void shouldFindTemplatesCaseInsensitivelyWithLowercaseSearch() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("code review");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find templates case-insensitively with uppercase search")
        void shouldFindTemplatesCaseInsensitivelyWithUppercaseSearch() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("CODE REVIEW");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should find templates case-insensitively with mixed case search")
        void shouldFindTemplatesCaseInsensitivelyWithMixedCaseSearch() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("cOdE rEvIeW");

            assertEquals(1, results.size());
            assertEquals("Code Review Task", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should return empty list for non-matching name")
        void shouldReturnEmptyListForNonMatchingName() {
            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("nonexistent");

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Should find all templates with common word")
        void shouldFindAllTemplatesWithCommonWord() {
            TaskTemplate anotherDocTemplate =
                    TaskTemplate.builder()
                            .name("Document Review")
                            .promptTemplate("Review document: {{doc}}")
                            .build();
            entityManager.persist(anotherDocTemplate);
            entityManager.flush();

            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("document");

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should handle empty search string")
        void shouldHandleEmptySearchString() {
            List<TaskTemplate> results = taskTemplateRepository.findByNameContainingIgnoreCase("");

            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("Should handle special characters in name search")
        void shouldHandleSpecialCharactersInNameSearch() {
            TaskTemplate specialTemplate =
                    TaskTemplate.builder()
                            .name("Test (Special) Task - v2.0")
                            .promptTemplate("Test prompt")
                            .build();
            entityManager.persist(specialTemplate);
            entityManager.flush();

            List<TaskTemplate> results =
                    taskTemplateRepository.findByNameContainingIgnoreCase("(Special)");

            assertEquals(1, results.size());
            assertEquals("Test (Special) Task - v2.0", results.getFirst().getName());
        }

        @Test
        @DisplayName("Should return results in consistent order")
        void shouldReturnResultsInConsistentOrder() {
            List<TaskTemplate> firstCall =
                    taskTemplateRepository.findByNameContainingIgnoreCase("Task");
            List<TaskTemplate> secondCall =
                    taskTemplateRepository.findByNameContainingIgnoreCase("Task");

            assertEquals(firstCall.size(), secondCall.size());
            for (int i = 0; i < firstCall.size(); i++) {
                assertEquals(firstCall.get(i).getId(), secondCall.get(i).getId());
            }
        }
    }
}
