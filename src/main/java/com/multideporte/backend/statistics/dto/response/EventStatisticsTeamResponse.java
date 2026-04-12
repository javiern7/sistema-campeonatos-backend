package com.multideporte.backend.statistics.dto.response;

public record EventStatisticsTeamResponse(
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        String teamShortName,
        String teamCode,
        Integer seedNumber,
        Integer goals,
        Integer yellowCards,
        Integer redCards,
        Integer activeEvents
) {
}
