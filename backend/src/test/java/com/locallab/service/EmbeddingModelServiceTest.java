package com.locallab.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.locallab.dto.request.EmbeddingModelRequest;
import com.locallab.model.EmbeddingModel;
import com.locallab.repository.EmbeddingModelRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link EmbeddingModelService}.
 *
 * <p>Uses Mockito to mock the repository dependency and test service behaviour in isolation.
 *
 * @see EmbeddingModelService
 * @see EmbeddingModelRepository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingModelService")
class EmbeddingModelServiceTest {

    @Mock private EmbeddingModelRepository embeddingModelRepository;

    @InjectMocks private EmbeddingModelService embeddingModelService;

    private EmbeddingModel nomicModel;
    private EmbeddingModel mxbaiModel;

    @BeforeEach
    void setUp() {
        nomicModel =
                EmbeddingModel.builder()
                        .id(1L)
                        .name("Nomic Embed Text")
                        .ollamaModelName("nomic-embed-text")
                        .dimensions(768)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 10, 0))
                        .build();

        mxbaiModel =
                EmbeddingModel.builder()
                        .id(2L)
                        .name("MxBai Embed Large")
                        .ollamaModelName("mxbai-embed-large")
                        .dimensions(1024)
                        .createdAt(LocalDateTime.of(2025, 11, 27, 11, 0))
                        .build();
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Should return all embedding models")
        void shouldReturnAllEmbeddingModels() {
            when(embeddingModelRepository.findAll())
                    .thenReturn(Arrays.asList(nomicModel, mxbaiModel));

            List<EmbeddingModel> result = embeddingModelService.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(nomicModel, mxbaiModel);
            verify(embeddingModelRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no embedding models exist")
        void shouldReturnEmptyListWhenNoEmbeddingModelsExist() {
            when(embeddingModelRepository.findAll()).thenReturn(Collections.emptyList());

            List<EmbeddingModel> result = embeddingModelService.findAll();

            assertThat(result).isEmpty();
            verify(embeddingModelRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return embedding model when found")
        void shouldReturnEmbeddingModelWhenFound() {
            when(embeddingModelRepository.findById(1L)).thenReturn(Optional.of(nomicModel));

            EmbeddingModel result = embeddingModelService.findById(1L);

            assertThat(result).isEqualTo(nomicModel);
            assertThat(result.getName()).isEqualTo("Nomic Embed Text");
            verify(embeddingModelRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(embeddingModelRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> embeddingModelService.findById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Embedding model not found: 999");

            verify(embeddingModelRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Should create embedding model with valid request")
        void shouldCreateEmbeddingModelWithValidRequest() {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("New Model")
                            .ollamaModelName("new-model")
                            .dimensions(512)
                            .build();

            EmbeddingModel savedModel =
                    EmbeddingModel.builder()
                            .id(3L)
                            .name(request.getName())
                            .ollamaModelName(request.getOllamaModelName())
                            .dimensions(request.getDimensions())
                            .createdAt(LocalDateTime.now())
                            .build();

            when(embeddingModelRepository.findByName("New Model")).thenReturn(Optional.empty());
            when(embeddingModelRepository.save(any(EmbeddingModel.class))).thenReturn(savedModel);

            EmbeddingModel result = embeddingModelService.create(request);

            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getName()).isEqualTo("New Model");
            assertThat(result.getOllamaModelName()).isEqualTo("new-model");
            assertThat(result.getDimensions()).isEqualTo(512);

            verify(embeddingModelRepository).findByName("New Model");
            verify(embeddingModelRepository).save(any(EmbeddingModel.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when name already exists")
        void shouldThrowIllegalStateExceptionWhenNameAlreadyExists() {
            EmbeddingModelRequest request =
                    EmbeddingModelRequest.builder()
                            .name("Nomic Embed Text")
                            .ollamaModelName("nomic-embed-text")
                            .dimensions(768)
                            .build();

            when(embeddingModelRepository.findByName("Nomic Embed Text"))
                    .thenReturn(Optional.of(nomicModel));

            assertThatThrownBy(() -> embeddingModelService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Embedding model already exists: Nomic Embed Text");

            verify(embeddingModelRepository).findByName("Nomic Embed Text");
            verify(embeddingModelRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Should delete embedding model when exists")
        void shouldDeleteEmbeddingModelWhenExists() {
            when(embeddingModelRepository.existsById(1L)).thenReturn(true);

            embeddingModelService.delete(1L);

            verify(embeddingModelRepository).existsById(1L);
            verify(embeddingModelRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFoundExceptionWhenNotFound() {
            when(embeddingModelRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> embeddingModelService.delete(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Embedding model not found: 999");

            verify(embeddingModelRepository).existsById(999L);
            verify(embeddingModelRepository, never()).deleteById(any());
        }
    }
}
