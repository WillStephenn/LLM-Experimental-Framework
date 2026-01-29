package com.locallab.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Container for leaderboard results returned by the analytics service.
 *
 * <p>Contains a list of leaderboard entries (one per model) along with metadata about the total
 * number of runs included in the analysis.
 *
 * @see LeaderboardEntry
 * @see LeaderboardFilter
 * @see com.locallab.service.AnalyticsService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardData {

    /**
     * The list of leaderboard entries, one per model.
     *
     * <p>Entries are sorted according to the filter criteria provided in the request.
     */
    private List<LeaderboardEntry> entries;

    /**
     * The total number of runs included in this leaderboard analysis.
     *
     * <p>This includes both successful and failed runs across all models.
     */
    private int totalRuns;
}
