package com.multideporte.backend.statistics.dto.response;

public record EventStatisticsFiltersResponse(
        Long matchId,
        Long tournamentTeamId,
        Long teamId,
        Long playerId
) {
}
