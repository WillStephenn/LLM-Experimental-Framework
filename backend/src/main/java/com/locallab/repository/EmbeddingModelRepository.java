package com.locallab.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.locallab.model.EmbeddingModel;

/**
 * Repository interface for {@link EmbeddingModel} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} along with a custom query method
 * for name-based lookup. Spring Data JPA auto-implements the derived query method.
 *
 * <p>Embedding models store the configuration required to use Ollama embedding models for
 * generating vector representations of text in RAG operations. Each model has a unique name
 * allowing identification in the UI.
 *
 * @see EmbeddingModel
 */
@Repository
public interface EmbeddingModelRepository extends JpaRepository<EmbeddingModel, Long> {

    /**
     * Finds an embedding model by its unique name.
     *
     * <p>The name field has a unique constraint in the database, so this method returns an {@link
     * Optional} containing at most one embedding model. This is useful for looking up models by
     * their human-readable display name.
     *
     * <p>For example, names might be "Nomic Embed Text", "MXBai Embed Large", or "All MiniLM".
     *
     * @param name the unique name to search for (exact match, case-sensitive)
     * @return an {@link Optional} containing the matching embedding model, or empty if not found
     */
    Optional<EmbeddingModel> findByName(String name);
}
