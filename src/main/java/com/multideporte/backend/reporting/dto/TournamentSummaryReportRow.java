package com.multideporte.backend.reporting.dto;

public record TournamentSummaryReportRow(
        Long tournamentId,
        String tournamentName,
        Long teams,
        Long matches,
        Long standings,
        Integer activeEvents,
        Integer goals,
        Integer yellowCards,
        Integer redCards
) {
}
