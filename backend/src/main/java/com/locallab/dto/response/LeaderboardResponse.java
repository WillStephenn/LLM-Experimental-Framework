package com.locallab.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.locallab.dto.LeaderboardData;
import com.locallab.dto.LeaderboardEntry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for leaderboard data returned to clients.
 *
 * <p>Contains ranked entries of model performance metrics along with a timestamp indicating when
 * the leaderboard was generated.
 *
 * @see LeaderboardEntry
 * @see LeaderboardData
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

    /**
     * List of leaderboard entries, one per model.
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

    /**
     * Timestamp when this leaderboard was generated.
     *
     * <p>Uses ISO 8601 format when serialised.
     */
    private LocalDateTime generatedAt;

    /**
     * Creates a LeaderboardResponse from LeaderboardData.
     *
     * @param data the leaderboard data from the analytics service
     * @return a new LeaderboardResponse with the current timestamp
     */
    public static LeaderboardResponse fromData(LeaderboardData data) {
        return LeaderboardResponse.builder()
                .entries(data.getEntries())
                .totalRuns(data.getTotalRuns())
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
