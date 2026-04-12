package com.multideporte.backend.statistics.dto.response;

public record EventStatisticsPlayerResponse(
        Long playerId,
        String firstName,
        String lastName,
        String displayName,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        String teamShortName,
        Integer goals,
        Integer yellowCards,
        Integer redCards,
        Integer activeEvents
) {
}
