package com.locallab.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a reusable system prompt that can be applied to model generations.
 *
 * <p>System prompts provide instructions to the LLM that shape its behaviour and response style.
 * They are stored as lookup entities that can be referenced by {@code ExperimentRun} instances via
 * a unidirectional {@code @ManyToOne} relationship.
 *
 * <p>Each system prompt has a unique alias for easy identification and selection in the UI.
 *
 * @see com.locallab.model.ExperimentRun
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_prompts")
public class SystemPrompt {

    /** Unique identifier for the system prompt. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Short, unique alias for the system prompt.
     *
     * <p>Used as a human-readable identifier for quick selection. Examples: "code-assistant",
     * "technical-writer", "json-formatter".
     */
    @NotBlank(message = "Alias is required")
    @Column(unique = true, nullable = false)
    private String alias;

    /**
     * The full content of the system prompt.
     *
     * <p>This text is sent to the LLM as the system message to guide its behaviour. Stored as a LOB
     * to support lengthy prompt content.
     */
    @Lob
    @NotBlank(message = "Content is required")
    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    /**
     * Timestamp when this system prompt was created.
     *
     * <p>Automatically set by Hibernate on entity creation.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
