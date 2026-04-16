package com.multideporte.backend.reporting.dto;

public record CardReportRow(
        Long playerId,
        String playerName,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        Integer yellowCards,
        Integer redCards,
        Integer totalCards
) {
}
