package com.locallab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter criteria for generating leaderboard data.
 *
 * <p>This DTO allows clients to specify various filtering and sorting options when retrieving
 * leaderboard data from the analytics service. All fields are optional, allowing for flexible
 * querying.
 *
 * <h3>Supported Sort Fields:</h3>
 *
 * <ul>
 *   <li>{@code "tps"} - Sort by average tokens per second
 *   <li>{@code "duration"} - Sort by average duration in milliseconds
 *   <li>{@code "ttft"} - Sort by average time to first token in milliseconds
 *   <li>{@code "successRate"} - Sort by success rate percentage
 * </ul>
 *
 * <h3>Sort Order:</h3>
 *
 * <ul>
 *   <li>{@code "asc"} - Ascending order (lowest first)
 *   <li>{@code "desc"} - Descending order (highest first)
 * </ul>
 *
 * @see com.locallab.dto.LeaderboardData
 * @see com.locallab.service.AnalyticsService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardFilter {

    /**
     * Filter results to a specific experiment.
     *
     * <p>When provided, only runs belonging to this experiment will be included in the leaderboard.
     */
    private Long experimentId;

    /**
     * Filter results to a specific model.
     *
     * <p>When provided, only runs using this model will be included in the leaderboard.
     */
    private String modelName;

    /**
     * Filter results to runs using a specific embedding model.
     *
     * <p>When provided, only RAG runs using this embedding model will be included.
     */
    private String embeddingModel;

    /**
     * The field to sort results by.
     *
     * <p>Valid values: {@code "tps"}, {@code "duration"}, {@code "ttft"}, {@code "successRate"}.
     * Defaults to {@code "tps"} if not specified.
     */
    @Builder.Default private String sortBy = "tps";

    /**
     * The sort direction.
     *
     * <p>Valid values: {@code "asc"}, {@code "desc"}. Defaults to {@code "desc"} if not specified.
     */
    @Builder.Default private String sortOrder = "desc";
}
