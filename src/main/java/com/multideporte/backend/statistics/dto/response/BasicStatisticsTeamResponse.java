package com.multideporte.backend.statistics.dto.response;

public record BasicStatisticsTeamResponse(
        Long tournamentTeamId,
        Long teamId,
        String name,
        String shortName,
        String code,
        Integer seedNumber,
        Integer rankPosition
) {
}
