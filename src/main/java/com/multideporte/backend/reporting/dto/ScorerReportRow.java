package com.multideporte.backend.reporting.dto;

public record ScorerReportRow(
        Long playerId,
        String playerName,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        Integer goals
) {
}
