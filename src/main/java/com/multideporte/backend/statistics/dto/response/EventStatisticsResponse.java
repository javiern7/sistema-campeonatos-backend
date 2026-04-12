package com.multideporte.backend.statistics.dto.response;

import java.util.List;

public record EventStatisticsResponse(
        Long tournamentId,
        EventStatisticsFiltersResponse filters,
        EventStatisticsSummaryResponse summary,
        List<EventStatisticsPlayerResponse> players,
        List<EventStatisticsTeamResponse> teams,
        List<EventStatisticsMatchResponse> matches,
        EventStatisticsTraceabilityResponse traceability
) {
}
