package com.locallab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.locallab.dto.request.SystemPromptRequest;
import com.locallab.model.SystemPrompt;
import com.locallab.repository.SystemPromptRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link SystemPromptService}.
 *
 * <p>Tests all CRUD operations, alias uniqueness validation, and error handling scenarios using
 * mocked repository dependencies.
 *
 * @author William Stephen
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemPromptService")
class SystemPromptServiceTest {

    @Mock private SystemPromptRepository systemPromptRepository;

    @InjectMocks private SystemPromptService systemPromptService;

    private SystemPrompt testPrompt;
    private SystemPromptRequest testRequest;

    @BeforeEach
    void setUp() {
        testPrompt =
                SystemPrompt.builder()
                        .id(1L)
                        .alias("code-assistant")
                        .content("You are a helpful coding assistant.")
                        .createdAt(LocalDateTime.now())
                        .build();

        testRequest =
                SystemPromptRequest.builder()
                        .alias("code-assistant")
                        .content("You are a helpful coding assistant.")
                        .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("should return all system prompts")
        void shouldReturnAllSystemPrompts() {
            SystemPrompt secondPrompt =
                    SystemPrompt.builder()
                            .id(2L)
                            .alias("json-formatter")
                            .content("Format all responses as JSON.")
                            .createdAt(LocalDateTime.now())
                            .build();

            when(systemPromptRepository.findAll()).thenReturn(List.of(testPrompt, secondPrompt));

            List<SystemPrompt> result = systemPromptService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(SystemPrompt::getAlias)
                    .containsExactly("code-assistant", "json-formatter");
            verify(systemPromptRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no system prompts exist")
        void shouldReturnEmptyListWhenNoSystemPromptsExist() {
            when(systemPromptRepository.findAll()).thenReturn(Collections.emptyList());

            List<SystemPrompt> result = systemPromptService.findAll();

            assertThat(result).isEmpty();
            verify(systemPromptRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should return system prompt when found")
        void shouldReturnSystemPromptWhenFound() {
            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));

            SystemPrompt result = systemPromptService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAlias()).isEqualTo("code-assistant");
            verify(systemPromptRepository).findById(1L);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(systemPromptRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemPromptService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("System prompt not found: 999");

            verify(systemPromptRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("findByAlias")
    class FindByAliasTests {

        @Test
        @DisplayName("should return optional with system prompt when found")
        void shouldReturnOptionalWithSystemPromptWhenFound() {
            when(systemPromptRepository.findByAlias("code-assistant"))
                    .thenReturn(Optional.of(testPrompt));

            Optional<SystemPrompt> result = systemPromptService.findByAlias("code-assistant");

            assertThat(result).isPresent();
            assertThat(result.get().getAlias()).isEqualTo("code-assistant");
            verify(systemPromptRepository).findByAlias("code-assistant");
        }

        @Test
        @DisplayName("should return empty optional when not found")
        void shouldReturnEmptyOptionalWhenNotFound() {
            when(systemPromptRepository.findByAlias("non-existent")).thenReturn(Optional.empty());

            Optional<SystemPrompt> result = systemPromptService.findByAlias("non-existent");

            assertThat(result).isEmpty();
            verify(systemPromptRepository).findByAlias("non-existent");
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("should create system prompt successfully")
        void shouldCreateSystemPromptSuccessfully() {
            when(systemPromptRepository.findByAlias("code-assistant")).thenReturn(Optional.empty());
            when(systemPromptRepository.save(any(SystemPrompt.class))).thenReturn(testPrompt);

            SystemPrompt result = systemPromptService.create(testRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAlias()).isEqualTo("code-assistant");
            assertThat(result.getContent()).isEqualTo("You are a helpful coding assistant.");

            ArgumentCaptor<SystemPrompt> captor = ArgumentCaptor.forClass(SystemPrompt.class);
            verify(systemPromptRepository).save(captor.capture());

            SystemPrompt captured = captor.getValue();
            assertThat(captured.getAlias()).isEqualTo("code-assistant");
            assertThat(captured.getContent()).isEqualTo("You are a helpful coding assistant.");
        }

        @Test
        @DisplayName("should throw IllegalStateException when alias already exists")
        void shouldThrowIllegalStateExceptionWhenAliasAlreadyExists() {
            when(systemPromptRepository.findByAlias("code-assistant"))
                    .thenReturn(Optional.of(testPrompt));

            assertThatThrownBy(() -> systemPromptService.create(testRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("System prompt with alias 'code-assistant' already exists");

            verify(systemPromptRepository).findByAlias("code-assistant");
            verify(systemPromptRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("should update system prompt successfully without changing alias")
        void shouldUpdateSystemPromptSuccessfullyWithoutChangingAlias() {
            SystemPromptRequest updateRequest =
                    SystemPromptRequest.builder()
                            .alias("code-assistant")
                            .content("Updated content for the assistant.")
                            .build();

            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));
            when(systemPromptRepository.save(any(SystemPrompt.class))).thenReturn(testPrompt);

            SystemPrompt result = systemPromptService.update(1L, updateRequest);

            assertThat(result).isNotNull();
            verify(systemPromptRepository).findById(1L);
            verify(systemPromptRepository).save(any(SystemPrompt.class));
            verify(systemPromptRepository, never()).findByAlias(any());
        }

        @Test
        @DisplayName("should update system prompt successfully with new alias")
        void shouldUpdateSystemPromptSuccessfullyWithNewAlias() {
            SystemPromptRequest updateRequest =
                    SystemPromptRequest.builder()
                            .alias("new-alias")
                            .content("Updated content.")
                            .build();

            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));
            when(systemPromptRepository.findByAlias("new-alias")).thenReturn(Optional.empty());
            when(systemPromptRepository.save(any(SystemPrompt.class)))
                    .thenAnswer(
                            invocation -> {
                                SystemPrompt saved = invocation.getArgument(0);
                                return saved;
                            });

            SystemPrompt result = systemPromptService.update(1L, updateRequest);

            assertThat(result).isNotNull();
            assertThat(result.getAlias()).isEqualTo("new-alias");
            assertThat(result.getContent()).isEqualTo("Updated content.");
            verify(systemPromptRepository).findById(1L);
            verify(systemPromptRepository).findByAlias("new-alias");
            verify(systemPromptRepository).save(any(SystemPrompt.class));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when system prompt not found")
        void shouldThrowEntityNotFoundExceptionWhenSystemPromptNotFound() {
            when(systemPromptRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemPromptService.update(999L, testRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("System prompt not found: 999");

            verify(systemPromptRepository).findById(999L);
            verify(systemPromptRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when new alias conflicts")
        void shouldThrowIllegalStateExceptionWhenNewAliasConflicts() {
            SystemPromptRequest updateRequest =
                    SystemPromptRequest.builder()
                            .alias("existing-alias")
                            .content("Updated content.")
                            .build();

            SystemPrompt existingWithNewAlias =
                    SystemPrompt.builder()
                            .id(2L)
                            .alias("existing-alias")
                            .content("Other prompt content.")
                            .build();

            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));
            when(systemPromptRepository.findByAlias("existing-alias"))
                    .thenReturn(Optional.of(existingWithNewAlias));

            assertThatThrownBy(() -> systemPromptService.update(1L, updateRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("System prompt with alias 'existing-alias' already exists");

            verify(systemPromptRepository).findById(1L);
            verify(systemPromptRepository).findByAlias("existing-alias");
            verify(systemPromptRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("should delete system prompt successfully")
        void shouldDeleteSystemPromptSuccessfully() {
            when(systemPromptRepository.findById(1L)).thenReturn(Optional.of(testPrompt));

            systemPromptService.delete(1L);

            verify(systemPromptRepository).findById(1L);
            verify(systemPromptRepository).delete(testPrompt);
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when system prompt not found")
        void shouldThrowEntityNotFoundExceptionWhenSystemPromptNotFound() {
            when(systemPromptRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> systemPromptService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("System prompt not found: 999");

            verify(systemPromptRepository).findById(999L);
            verify(systemPromptRepository, never()).delete(any());
        }
    }
}
