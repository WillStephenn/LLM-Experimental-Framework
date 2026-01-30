package com.locallab.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import com.locallab.dto.ModelMetrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for model comparison data within an experiment.
 *
 * <p>Contains metrics for all models used in a specific experiment, enabling side-by-side
 * comparison of model performance.
 *
 * @see ModelMetrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelComparisonResponse {

    /** The unique identifier of the experiment. */
    private Long experimentId;

    /** The name of the experiment. */
    private String experimentName;

    /**
     * Metrics for each model in the experiment.
     *
     * <p>The key is the model name (e.g., "qwen2.5-coder:7b"), and the value contains the
     * aggregated metrics for that model.
     */
    private Map<String, ModelMetrics> models;

    /**
     * Timestamp when this comparison was generated.
     *
     * <p>Uses ISO 8601 format when serialised.
     */
    private LocalDateTime generatedAt;
}
