package com.multideporte.backend.reporting.dto;

public record StandingReportRow(
        Long standingId,
        Integer rankPosition,
        Long stageId,
        Long groupId,
        Long tournamentTeamId,
        Long teamId,
        String teamName,
        Integer played,
        Integer wins,
        Integer draws,
        Integer losses,
        Integer pointsFor,
        Integer pointsAgainst,
        Integer scoreDiff,
        Integer points
) {
}
