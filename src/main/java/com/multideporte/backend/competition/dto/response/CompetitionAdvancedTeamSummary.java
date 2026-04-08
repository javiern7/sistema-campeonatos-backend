package com.multideporte.backend.competition.dto.response;

public record CompetitionAdvancedTeamSummary(
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        String shortName,
        String code,
        Integer seedNumber
) {
}
