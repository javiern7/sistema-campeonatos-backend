package com.multideporte.backend.competition.dto.response;

import java.util.List;

public record CompetitionAdvancedResultsResponse(
        Long tournamentId,
        Long stageId,
        Long groupId,
        int totalClosedMatches,
        List<ResultEntry> results
) {

    public record ResultEntry(
            CompetitionAdvancedMatchSummary match,
            boolean affectsStandings,
            String standingScope,
            String standingStatus
    ) {
    }
}
