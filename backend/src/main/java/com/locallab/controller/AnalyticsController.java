package com.locallab.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.locallab.dto.LeaderboardData;
import com.locallab.dto.LeaderboardFilter;
import com.locallab.dto.ModelMetrics;
import com.locallab.dto.response.LeaderboardResponse;
import com.locallab.dto.response.ModelComparisonResponse;
import com.locallab.dto.response.ModelMetricsResponse;
import com.locallab.model.Experiment;
import com.locallab.service.AnalyticsService;
import com.locallab.service.ExperimentService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for analytics and metrics endpoints.
 *
 * <p>This controller provides endpoints for leaderboard data, model-specific metrics, and model
 * comparison within experiments. All endpoints return consistent response structures as defined in
 * the API contract.
 *
 * <h3>Endpoint Summary:</h3>
 *
 * <ul>
 *   <li>{@code GET /api/analytics/leaderboard} - Get leaderboard data with optional filtering
 *   <li>{@code GET /api/analytics/models/{modelName}} - Get detailed metrics for a specific model
 *   <li>{@code GET /api/experiments/{id}/comparison} - Compare models within an experiment
 * </ul>
 *
 * <h3>Error Handling:</h3>
 *
 * <p>All exceptions are handled by {@link com.locallab.exception.GlobalExceptionHandler} which
 * converts exceptions to consistent JSON error responses as defined in the API contract.
 *
 * @author William Stephen
 * @see AnalyticsService
 * @see LeaderboardResponse
 * @see ModelMetricsResponse
 * @see ModelComparisonResponse
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private final ExperimentService experimentService;

    /**
     * Retrieves leaderboard data based on optional filter criteria.
     *
     * <p>The leaderboard aggregates model performance metrics across experiment runs. Results can
     * be filtered by experiment, model name, or embedding model. Results are sorted according to
     * the sortBy and sortOrder parameters.
     *
     * <h3>Filter Parameters:</h3>
     *
     * <ul>
     *   <li>{@code experimentId} - Filter to a specific experiment
     *   <li>{@code modelName} - Filter to a specific model
     *   <li>{@code embeddingModel} - Filter to runs using a specific embedding model
     *   <li>{@code sortBy} - Field to sort by (tps, duration, ttft, successRate)
     *   <li>{@code sortOrder} - Sort direction (asc, desc)
     * </ul>
     *
     * @param experimentId optional experiment filter
     * @param modelName optional model name filter
     * @param embeddingModel optional embedding model filter
     * @param sortBy optional sort field (defaults to "tps")
     * @param sortOrder optional sort direction (defaults to "desc")
     * @return leaderboard data with ranked model entries
     */
    @GetMapping("/analytics/leaderboard")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(required = false) Long experimentId,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String embeddingModel,
            @RequestParam(required = false, defaultValue = "tps") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        LOGGER.debug(
                "Received leaderboard request - experimentId: {}, modelName: {}, "
                        + "embeddingModel: {}, sortBy: {}, sortOrder: {}",
                experimentId,
                modelName,
                embeddingModel,
                sortBy,
                sortOrder);

        LeaderboardFilter filter =
                LeaderboardFilter.builder()
                        .experimentId(experimentId)
                        .modelName(modelName)
                        .embeddingModel(embeddingModel)
                        .sortBy(sortBy)
                        .sortOrder(sortOrder)
                        .build();

        LeaderboardData data = analyticsService.getLeaderboard(filter);
        LeaderboardResponse response = LeaderboardResponse.fromData(data);

        LOGGER.debug("Returning leaderboard with {} entries", response.getEntries().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves detailed metrics for a specific model.
     *
     * <p>Returns comprehensive performance statistics including run counts, success rates, average
     * metrics, and per-iteration breakdowns. Optionally scoped to a specific experiment.
     *
     * @param modelName the model name to retrieve metrics for
     * @param experimentId optional experiment filter to scope metrics
     * @return detailed model metrics
     * @throws jakarta.persistence.EntityNotFoundException if no runs exist for the specified model
     */
    @GetMapping("/analytics/models/{modelName}")
    public ResponseEntity<ModelMetricsResponse> getModelMetrics(
            @PathVariable String modelName, @RequestParam(required = false) Long experimentId) {

        LOGGER.debug(
                "Received model metrics request - modelName: {}, experimentId: {}",
                modelName,
                experimentId);

        ModelMetrics metrics = analyticsService.getModelMetrics(modelName, experimentId);
        ModelMetricsResponse response = ModelMetricsResponse.fromMetrics(metrics);

        LOGGER.debug(
                "Returning metrics for model {} - {} total runs",
                modelName,
                metrics.getTotalRuns());
        return ResponseEntity.ok(response);
    }

    /**
     * Compares all models within a specific experiment.
     *
     * <p>Returns aggregated metrics for each unique model used in the experiment, enabling
     * side-by-side comparison of model performance.
     *
     * @param id the experiment identifier
     * @return comparison data for all models in the experiment
     * @throws jakarta.persistence.EntityNotFoundException if the experiment does not exist
     */
    @GetMapping("/experiments/{id}/comparison")
    public ResponseEntity<ModelComparisonResponse> compareModels(@PathVariable Long id) {

        LOGGER.debug("Received model comparison request for experiment: {}", id);

        // Get experiment to verify it exists and get its name
        Experiment experiment = experimentService.findById(id);

        Map<String, ModelMetrics> comparison = analyticsService.compareModels(id);

        ModelComparisonResponse response =
                ModelComparisonResponse.builder()
                        .experimentId(id)
                        .experimentName(experiment.getName())
                        .models(comparison)
                        .generatedAt(LocalDateTime.now())
                        .build();

        LOGGER.debug(
                "Returning comparison for experiment {} with {} models", id, comparison.size());
        return ResponseEntity.ok(response);
    }
}
